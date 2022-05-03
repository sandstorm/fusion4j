/*
 * MIT License
 *
 * Copyright (c) 2022 Sandstorm Media GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Authors:
 *  - Eric Kloss
 */

package io.neos.fusion4j.test.bdd.steps

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.neos.fusion4j.lang.FusionError
import io.neos.fusion4j.lang.model.EelHelperContextName
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.values.StringValue
import io.neos.fusion4j.lang.parser.RawFusionModel
import io.neos.fusion4j.lang.semantic.PrototypeStoreConfiguration
import io.neos.fusion4j.lang.semantic.SemanticallyNormalizedFusionModel
import io.neos.fusion4j.lang.util.FusionProfiler
import io.neos.fusion4j.runtime.DefaultFusionRuntime
import io.neos.fusion4j.runtime.FusionContext
import io.neos.fusion4j.runtime.FusionObjectImplementation
import io.neos.fusion4j.runtime.eel.ClassLoadingEelHelperFactory
import io.neos.fusion4j.runtime.eel.JexlEelEvaluator
import io.neos.fusion4j.runtime.model.FusionDataStructure
import io.neos.fusion4j.test.bdd.impl.StaticValueImplementation
import io.neos.fusion4j.test.bdd.impl.TestImplementationFactory
import mu.KLogger
import mu.KotlinLogging
import org.junit.Assert.*

private val log: KLogger = KotlinLogging.logger {}

class FusionRuntimeSteps : En {
    companion object {
        var lastInstance: FusionRuntimeSteps? = null
        var profiler: FusionProfiler = FusionProfiler.disabled()
    }

    var runtime: DefaultFusionRuntime? = null
    private val lastRuntimeErrors: MutableMap<String, FusionError> = mutableMapOf()
    private val evaluationByPath: MutableMap<String, Any?> = mutableMapOf()
    val mockStaticImplementations: MutableMap<String, FusionObjectImplementation> = mutableMapOf()
    var eelClassMapping: Map<EelHelperContextName, Class<*>>? = null
    private val availableContextVariables: MutableMap<String, Any?> = mutableMapOf()

    init {

        Before { _ ->
            lastInstance = this
        }

        After { _ ->
            lastInstance = null
        }

        DataTableType { entry: Map<String, String> ->
            MockEelClassMapping(
                entry["name"] ?: error("name not given"),
                entry["class"] ?: error("class not given")
            )
        }

        Given("a Fusion runtime") {
            val semanticallyNormalizedFusionModel = SemanticallyNormalizedFusionModel(
                fusionDeclaration = RawFusionModel(FusionParserSteps.expectCompletelySuccessfulParseResult()),
                packageLoadOrder = FusionCodeDefinitionSteps.getPackageLoadOrder(),
                packageEntrypoints = FusionCodeDefinitionSteps.getPackageEntrypoints(),
                // TODO make configurable via step
                prototypeStoreConfiguration = PrototypeStoreConfiguration()
            )

            runtime = DefaultFusionRuntime(
                semanticallyNormalizedFusionModel = semanticallyNormalizedFusionModel,
                eelEvaluator = JexlEelEvaluator(
                    semanticallyNormalizedFusionModel.rawIndex.pathIndex,
                    ClassLoadingEelHelperFactory(
                        eelClassMapping ?: emptyMap()
                    ),
                    true,
                    profiler
                ),
                implementationFactory = TestImplementationFactory(),
                fusionProfiler = profiler
            )
        }

        Given("the test Fusion implementation {string} with static string output {string}") { className: String, output: String ->
            mockStaticImplementations[className] = StaticValueImplementation(output)
        }

        Given("the following EEL helper class mapping") { classMapping: DataTable ->
            eelClassMapping = classMapping.toMockList<MockEelClassMapping>()
                .associate {
                    EelHelperContextName(it.contextName) to
                            FusionRuntimeSteps::class.java.classLoader.loadClass(it.className)
                }
        }

        Given("a Fusion data structure context variable {string} with the following JSON value") { varName: String, valueAsJson: String ->
            availableContextVariables[varName] = createDataStructureFromJson(valueAsJson)
        }

        Given("a Fusion data structure context variable {string} with the following JSON value {int} times") { varName: String, times: Int, valueAsJson: String ->
            availableContextVariables[varName] = FusionDataStructure.fromList<Any?>(
                (0..times).map {
                    createDataStructureFromJson(valueAsJson)
                }
            )
        }

        When("I evaluate the Fusion path {string}") { path: String ->
            evaluate(path, FusionContext.empty())
        }

        When("I evaluate the Fusion path {string} {int} times") { path: String, times: Int ->
            (1..times).forEach {
                evaluate(path, FusionContext.empty(), it)
            }
        }

        When("I evaluate the Fusion path {string} with context vars") { path: String, varNames: DataTable ->
            evaluate(
                path,
                FusionContext.create(
                    varNames.asList().associateWith {
                        if (!availableContextVariables.containsKey(it)) {
                            fail("Test Fusion context var $it not defined; declare with steps before evaluation")
                        }
                        availableContextVariables[it]
                    }
                )
            )
        }

        When("I evaluate the Fusion path {string} {int} times with context vars") { path: String, times: Int, varNames: DataTable ->
            val context = FusionContext.create(
                varNames.asList().associateWith {
                    if (!availableContextVariables.containsKey(it)) {
                        fail("Test Fusion context var $it not defined; declare with steps before evaluation")
                    }
                    availableContextVariables[it]
                }
            )
            repeat(times) {
                evaluate(
                    path,
                    context,
                    it
                )
            }
        }

        Then("the evaluated output for path {string} must be of type {string}") { path: String, expectedOutputType: String ->
            if (!evaluationByPath.containsKey(path)) {
                val error = lastRuntimeErrors[path]
                if (error != null) {
                    fail("a runtime error has occurred during evaluation of path '$path';\n --- runtime error below:\n$error")
                } else {
                    fail("no evaluation performed for path $path; call 'When I evaluate the Fusion path \"$path\"' before this step")
                }
            } else {
                assertNotNull("evaluation of path $path must not be null", evaluationByPath[path])
                assertEquals(
                    "evaluation output type mismatch for path $path",
                    expectedOutputType,
                    evaluationByPath[path]!!::class.java.name
                )
            }
        }

        Then("the evaluated output for path {string} must be null") { path: String ->
            assertNull("evaluation must be null; but was: ${evaluationByPath[path]}", evaluationByPath[path])
        }

        Then("the evaluated output for path {string} must be") { path: String, expectedOutput: String ->
            val actualValue = evaluationByPath[path].toString()
            assertEquals("evaluation output mismatch", expectedOutput.trim(), actualValue.trim())
        }

        Then("the evaluated output for path {string} must be a special whitespace char {string}") { path: String, expectedWhitespaceChar: String ->
            val expectedChar = StringValue.SPECIAL_WHITESPACE_CHARS[expectedWhitespaceChar]
                ?: throw IllegalArgumentException("Unhandled expected whitespace char: $expectedWhitespaceChar")
            val actualValue = evaluationByPath[path].toString()
            assertEquals("evaluation output mismatch", expectedChar, actualValue)
        }

        Then("the evaluated output for path {string} must be like") { path: String, expectedOutput: String ->
            val sanitize = { value: String ->
                value
                    .replace(Regex("\\s+"), "")
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace("\t", "")
            }
            assertEquals(
                "evaluation output mismatch",
                sanitize(expectedOutput.trim()),
                sanitize(evaluationByPath[path].toString().trim())
            )
        }

        Then("the evaluated output for path {string} must have the collection size {int}") { path: String, expectedCollectionSize: Int ->
            assertOutput<Collection<*>>(path) { actualCollection ->
                assertEquals("collection size mismatch", expectedCollectionSize, actualCollection.size)
            }
        }

        Then("the evaluated output for path {string} must be empty") { path: String ->
            assertEquals("evaluation output must be empty", "", evaluationByPath[path].toString().trim())
        }

        Then("there should be an error for evaluation of path {string} containing the following message") { path: String, expectedMessage: String ->
            val errorForPath = lastRuntimeErrors[path]
            if (errorForPath != null) {
                val errorMessage = unpackErrorMessage(errorForPath)
                expectedMessage.split("\n")
                    .forEach { expectedLine ->
                        val messagePattern = Regex(
                            "^.*?" +
                                    expectedLine
                                        .trim()
                                        .split(Regex("\\[\\*]"))
                                        .joinToString(".*?") { Regex.escape(it) } +
                                    ".*?$",
                            RegexOption.DOT_MATCHES_ALL
                        )
                        assertTrue(
                            "error line message mismatch \n exp: $expectedLine\n act: $errorMessage",
                            errorMessage.matches(messagePattern)
                        )
                    }
            } else {
                fail("no evaluation error found for path '$path'")
            }
        }
    }

    private fun evaluate(path: String, context: FusionContext, idx: Int? = null) {
        if (runtime == null) {
            fail("no runtime initialized, use 'Given a Fusion runtime'")
        }
        try {
            val start = System.currentTimeMillis()
            /*
            val evaluatedValue = (1..1000).fold(runtime!!.evaluate(FusionPathName.parseAbsolute(path), Any::class.java)) { result, current ->
                runtime!!.evaluate(FusionPathName.parseAbsolute(path), Any::class.java)
            }
             */
            val evaluatedValue = runtime!!.evaluate(FusionPathName.parseAbsolute(path), Any::class.java, context)
            evaluationByPath[path] = evaluatedValue
            println("evaluation " + (if (idx != null) "#$idx " else "") + "of $path took ${System.currentTimeMillis() - start} ms")
        } catch (error: FusionError) {
            log.error("error during test evaluation of path '$path'", error)
            lastRuntimeErrors[path] = error
        }
    }

    private fun createDataStructureFromJson(json: String): FusionDataStructure<Any?> =
        FusionDataStructure.fromMap(
            jacksonObjectMapper().readValue<Map<String, Any?>>(json)
                .mapValues {
                    mapDataStructures(it.value)
                }
        )

    private fun mapDataStructures(value: Any?): Any? =
        when (value) {
            null -> null
            is FusionDataStructure<*> -> value
            is List<*> -> FusionDataStructure.fromList(
                value.map(this@FusionRuntimeSteps::mapDataStructures)
            )
            is Map<*, *> -> FusionDataStructure.fromMap(
                value
                    .mapKeys { it.key.toString() }
                    .mapValues {
                        mapDataStructures(it.value)
                    }
            )
            else -> value
        }

    private inline fun <reified T> assertOutput(evaluationPath: String, code: (T) -> Unit) {
        if (!evaluationByPath.containsKey(evaluationPath)) {
            fail("no evaluation found for path '$evaluationPath'")
        }
        val output = evaluationByPath[evaluationPath]
        if (output != null) {
            if (output is T?) {
                code(output)
            } else {
                fail("output type mismatch for path '$evaluationPath'; expected: ${T::class.java}; actual: ${output::class.java}")
            }
        } else {
            fail("expect output for path '$evaluationPath' to be of type ${T::class.java}; but was null")
        }
    }

    private fun unpackErrorMessage(throwable: Throwable): String =
        throwable.message + if (throwable.cause != null) "; caused by: " + unpackErrorMessage(throwable.cause!!) else ""

}

data class MockEelClassMapping(
    val contextName: String,
    val className: String
)