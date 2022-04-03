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

package bdd

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionPathDecl
import io.neos.fusion4j.lang.model.decl.PrototypeDecl
import io.neos.fusion4j.lang.model.values.ErasedValue
import io.neos.fusion4j.lang.parser.RawFusionModel
import io.neos.fusion4j.lang.semantic.*
import io.neos.fusion4j.test.bdd.steps.FusionCodeDefinitionSteps
import io.neos.fusion4j.test.bdd.steps.FusionParserSteps
import org.junit.Assert.*
import java.lang.reflect.Type


@Suppress("unused")
class FusionIndexSteps : En {
    var semanticallyNormalizedFusionModel: SemanticallyNormalizedFusionModel? = null
    var lastIndexError: FusionIndexError? = null
    var loadedInstances: MutableMap<String, FusionObjectInstance> = mutableMapOf()

    companion object {
        var lastInstance: FusionIndexSteps? = null
    }

    private fun getSemanticModel(): SemanticallyNormalizedFusionModel =
        semanticallyNormalizedFusionModel
            ?: throw IllegalStateException("No Fusion instance loader set, index your test Fusion with 'When all Fusion files are indexed with the following load order'")

    private fun loadTestInstance(path: String) {
        val loader = getSemanticModel().fusionObjectInstanceLoader
        val evaluationPath = EvaluationPath.parseFromString(path)
        loadedInstances[path] = loader.loadInstance(
            evaluationPath,
            // this is a hacky workaround, but in tests we usually don't need accurate AST references here
            getSemanticModel().prototypeStore.get(evaluationPath.currentType!!).declarations.first()
        )
    }

    init {

        Before { _ ->
            lastInstance = this
        }

        After { _ ->
            lastInstance = null
        }

        DataTableType { entry: Map<String, String> ->
            MockRawPathAssignment(
                value = entry["value"] ?: error("value not given"),
                type = entry["type"] ?: error("type not given"),
                source = entry["source"] ?: error("source not given")
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockNestedValue(
                path = entry["path"] ?: error("path not given"),
                value = entry["value"] ?: error("value not given"),
                type = entry["type"] ?: error("type not given"),
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockPrototypeAttribute(
                absolutePath = entry["absolutePath"] ?: error("absolutePath not given"),
                relativePath = entry["relativePath"] ?: error("relativePath not given"),
                value = entry["value"] ?: error("value not given"),
                type = entry["type"] ?: error("type not given"),
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockPrototypeApplyAttribute(
                absolutePath = entry["absolutePath"] ?: error("absolutePath not given"),
                applyName = entry["applyName"] ?: error("applyName not given"),
                expression = entry["expression"] ?: error("expression not given"),
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockAttributeKeyPosition(
                key = entry["key"] ?: error("key not given"),
                type = entry["type"] ?: error("type not given"),
                subject = entry["subject"] ?: error("subject not given"),
                weight = entry["weight"]?.toInt() ?: 0,
                refKey = entry["refKey"],
                numericIndex = entry["numericIndex"]?.toInt()
            )
        }

        When("all Fusion files are indexed") {
            indexFusion { error ->
                error.printStackTrace()
                lastIndexError = error
            }
        }

        When("all Fusion files are indexed without errors") {
            indexFusion { error ->
                fail("index error: $error")
            }
        }

        Then("there should be no index errors") {
            assertNotNull("expected no index error; but was $lastIndexError", lastIndexError)
        }

        Then("there should be an index error {string}") { expectedMessage: String ->
            assertNotNull("expected index error; but was null", lastIndexError)
            val actualMessage = lastIndexError!!.message!!
            assertTrue(
                "error message mismatch, expected\n$actualMessage\nto start with\n$expectedMessage",
                actualMessage.startsWith(expectedMessage)
            )
        }

        When("I load the Fusion object instance for evaluation path {string}") { path: String ->
            loadTestInstance(path)
        }

        Then("the loaded Fusion object instance for path {string} must be of prototype {string}") { path: String, expectedPrototypeName: String ->
            assertLoadedInstance(path) { actualInstance ->
                assertEquals(
                    "instance prototype mismatch",
                    expectedPrototypeName,
                    actualInstance.prototype.prototypeName.qualifiedName
                )
            }
        }

        Then("the loaded prototype {string} must have the following attributes") { prototypeName: String, expectedAttributesData: DataTable ->
            val prototype = getLoadedPrototype(prototypeName)
            val expectedAttributes = expectedAttributesData.toMockList<MockPrototypeAttribute>()
            val actualAttributes = prototype.attributes.filter { it.value.fusionValue !is ErasedValue }
            assertEquals("attribute count mismatch", expectedAttributes.size, actualAttributes.size)
            expectedAttributes.forEach { expectedAttribute ->
                val expectedPathName = FusionPathName.parse(".${expectedAttribute.relativePath}")
                val actualAttribute = actualAttributes[expectedPathName]
                if (actualAttribute != null) {
                    assertEquals(
                        "prototype attribute relative path mismatch",
                        expectedAttribute.relativePath,
                        actualAttribute.relativePath.pathAsString
                    )
                    assertEquals(
                        "prototype attribute absolute path mismatch",
                        expectedAttribute.absolutePath,
                        actualAttribute.absolutePath.pathAsString
                    )
                    assertEquals(
                        "prototype attribute value mismatch",
                        expectedAttribute.value,
                        actualAttribute.fusionValue.getReadableValue()
                    )
                    assertEquals(
                        "prototype attribute type mismatch",
                        expectedAttribute.type,
                        actualAttribute.fusionValue.getReadableType()
                    )
                } else {
                    fail("expected prototype attribute '$expectedPathName' not found in $actualAttributes")
                }
            }
        }

        Then("the loaded Fusion object instance for path {string} must have the following attributes") { path: String, expectedAttributesData: DataTable ->
            assertLoadedInstanceAttributes(
                path,
                expectedAttributesData.toMockList(),
                FusionObjectInstance::attributes
            )
        }

        Then("the loaded Fusion object instance for path {string} must have the attribute {string} with absolute path {string} of type {string} with the following value") { path: String, attributeName: String, expectedAbsolutePath: String, expectedType: String, expectedValue: String ->
            assertLoadedInstance(
                path
            ) { actualInstance ->
                val expectedRelativePath = FusionPathName.parseRelativePrependDot(attributeName)
                val actualAttribute = actualInstance.attributes[expectedRelativePath]
                if (actualAttribute != null) {
                    assertAttribute(
                        actualAttribute,
                        attributeName,
                        expectedAbsolutePath,
                        expectedValue,
                        expectedType
                    ) {
                        it.replace(Regex("\\s+"), " ")
                    }
                } else {
                    fail("Instance attribute $expectedRelativePath not found in: $actualInstance")
                }
            }
        }

        Then("the loaded Fusion object instance for path {string} must have the following apply attributes") { path: String, expectedAttributesData: DataTable ->
            assertLoadedInstanceAttributes(
                path,
                expectedAttributesData
                    .toMockList<MockPrototypeApplyAttribute>()
                    .map {
                        MockPrototypeAttribute(
                            it.applyName,
                            it.absolutePath,
                            "\${${it.expression}}",
                            "[EEL]"
                        )
                    },
            ) { it.applyAttributes.toMap() }
        }

        Then("the loaded Fusion object instance for path {string} must have the following attribute key positions") { path: String, expectedPositionsData: DataTable ->
            assertLoadedInstance(path) { actualInstance ->
                val expectedPositions = expectedPositionsData.toMockList<MockAttributeKeyPosition>()
                val actualPositions = actualInstance.attributePositions
                assertKeyPositions(expectedPositions, actualPositions)
            }
        }

        Then("the prototype store must contain {int} prototypes") { expectedCount: Int ->
            assertEquals("prototype count mismatch", expectedCount, getPrototypeStore().size)
        }

        Then("the loaded prototype {string} must have no attributes") { prototypeName: String ->
            val prototype = getLoadedPrototype(prototypeName)
            assertTrue(
                "prototype attributes must be empty but was: ${prototype.declaredAttributes}",
                prototype.declaredAttributes.filter { it.value.fusionValue !is ErasedValue }.isEmpty()
            )
        }

        Then("the path {string} must have the following attributes") { path: String, expectedValuesData: DataTable ->
            val expectedValues = expectedValuesData.toMockList<MockNestedValue>()
            val pathName = FusionPathName.parseAbsolute(path)
            val actualValues = getRawFusionIndex().resolveNestedAttributeFusionValues(pathName)
            assertEquals("Nested value count mismatch", expectedValues.size, actualValues.size)
            expectedValues.forEach { expectedValue ->
                val expectedPathName = FusionPathName.parse(".${expectedValue.path}") as RelativeFusionPathName
                val actualAttribute = actualValues[expectedPathName]
                if (actualAttribute != null) {
                    assertEquals(
                        "prototype attribute value mismatch",
                        expectedValue.value,
                        actualAttribute.fusionValue.getReadableValue()
                    )
                    assertEquals(
                        "raw path effective type mismatch",
                        expectedValue.type,
                        actualAttribute.fusionValue.getReadableType()
                    )
                } else {
                    fail("expected attribute '$expectedPathName' not found in $actualValues")
                }
            }
        }

        Then("the path {string} must have the following attribute key positions") { path: String, expectedValuesData: DataTable ->
            val expectedPositions = expectedValuesData.toMockList<MockAttributeKeyPosition>()
            val actualPositions = getRawFusionIndex().resolveChildPathKeyPositions(
                FusionPathName.parseAbsolute(path),
                FusionPaths.POSITION_META_ATTRIBUTE
            )
            assertKeyPositions(expectedPositions, actualPositions)
        }

        Then("the raw path index for {string} should contain the following assignments") { path: String, expectedIndexTable: DataTable ->
            val expectedIndex = expectedIndexTable.toMockList<MockRawPathAssignment>()
            assertRawPathIndexEntries(
                path,
                expectedIndex,
                FusionPathIndexEntry::assignments
            ) { expectedAssignment, actualAssignment ->
                assertEquals(
                    "raw path index assignment value mismatch",
                    expectedAssignment.value,
                    actualAssignment.valueDeclaration.fusionValue.getReadableValue()
                )
                assertEquals(
                    "raw path index assignment type mismatch",
                    expectedAssignment.type,
                    actualAssignment.valueDeclaration.fusionValue.getReadableType()
                )
                assertEquals(
                    "raw path index assignment source mismatch",
                    expectedAssignment.source,
                    actualAssignment.sourceIdentifier.identifierAsString
                )
            }
        }

        Then("the raw path index for {string} must contain {int} configurations") { path: String, expectedCount: Int ->
            assertEquals("raw path index count mismatch", expectedCount, getPathIndexEntry(path).configurations.size)
        }

        Then("the raw path index for {string} must contain {int} erasures") { path: String, expectedCount: Int ->
            assertEquals("raw path index count mismatch", expectedCount, getPathIndexEntry(path).erasures.size)
        }

        Then("the prototype store for {string} must contain {int} declarations") { prototypeName: String, expectedCount: Int ->
            val prototypeStore = getPrototypeStore()
            val actualIndex: List<PrototypeDecl> =
                prototypeStore.get(
                    QualifiedPrototypeName.fromString(
                        prototypeName
                    )
                ).rootPrototypeDeclarations
            assertEquals("raw prototype index count mismatch", expectedCount, actualIndex.size)
        }

        Then("the effective Fusion value of path {string} must be {string} with type {string}") { path: String, expectedValue: String, expectedType: String ->
            val effectiveValue =
                getRawFusionIndex().getFusionValueForPath(FusionPathName.parseAbsolute(path)).fusionValue
            assertEquals(
                "raw path effective value mismatch",
                expectedValue,
                effectiveValue.getReadableValue()
            )
            assertEquals(
                "raw path effective type mismatch",
                expectedType,
                effectiveValue.getReadableType()
            )
        }
    }

    private fun indexFusion(errorHandler: (FusionIndexError) -> Unit) {
        if (semanticallyNormalizedFusionModel == null) {
            try {
                val rawFusionModel = RawFusionModel(FusionParserSteps.expectCompletelySuccessfulParseResult())
                semanticallyNormalizedFusionModel = SemanticallyNormalizedFusionModel(
                    rawFusionModel,
                    FusionCodeDefinitionSteps.getPackageLoadOrder(),
                    FusionCodeDefinitionSteps.getPackageEntrypoints(),
                    // TODO make configurable via step
                    PrototypeStoreConfiguration(false)
                )
            } catch (error: FusionIndexError) {
                errorHandler(error)
            }
        } else {
            throw IllegalStateException("Fusion indexing step could only be called once per scenario")
        }
    }

    private fun assertLoadedInstanceAttributes(
        path: String,
        expectedAttributes: List<MockPrototypeAttribute>,
        actuals: (FusionObjectInstance) -> Map<RelativeFusionPathName, FusionValueReference>
    ) =
        assertLoadedInstance(path) { actualInstance ->
            val actualAttributes = actuals(actualInstance).filter { it.value.fusionValue !is ErasedValue }
            assertEquals("instance attribute count mismatch", expectedAttributes.size, actualAttributes.size)
            expectedAttributes.forEach { expectedAttribute ->
                val relativePath = expectedAttribute.relativePath
                val absolutePath = expectedAttribute.absolutePath
                val type = expectedAttribute.type
                val expectedPathName = FusionPathName.parse(".$relativePath")
                val actualAttribute = actualAttributes[expectedPathName]
                val expectedValue = if (expectedAttribute.value.startsWith("\"")) {
                    expectedAttribute.value.trim('"')
                } else {
                    expectedAttribute.value
                }
                if (actualAttribute != null) {
                    assertAttribute(actualAttribute, relativePath, absolutePath, expectedValue, type)
                } else {
                    fail("expected instance attribute '$expectedPathName' not found in ${actualAttributes.keys}")
                }
            }
        }

    private fun assertAttribute(
        actualAttribute: FusionValueReference,
        relativePath: String,
        absolutePath: String,
        expectedValue: String,
        type: String,
        sanitizer: (String) -> String = { it -> it }
    ) {
        assertEquals(
            "instance attribute relative path mismatch",
            relativePath,
            actualAttribute.relativePath.pathAsString
        )
        assertEquals(
            "instance attribute absolute path mismatch",
            absolutePath,
            actualAttribute.absolutePath.pathAsString
        )
        assertEquals(
            "instance attribute value mismatch",
            sanitizer(expectedValue),
            sanitizer(actualAttribute.fusionValue.getReadableValue())
        )
        assertEquals(
            "instance attribute type mismatch",
            type,
            actualAttribute.fusionValue.getReadableType()
        )
    }

    private fun assertLoadedInstance(path: String, assertionCode: (FusionObjectInstance) -> Unit) {
        val actualInstance = loadedInstances[path]
        if (actualInstance != null) {
            assertionCode.invoke(actualInstance)
        } else {
            fail("no loaded instance for path $path")
        }
    }

    private fun getLoadedPrototype(prototypeName: String): FusionPrototype {
        return getSemanticModel().prototypeStore.get(QualifiedPrototypeName.fromString(prototypeName))
    }

    private fun assertKeyPositions(
        expectedPositions: List<MockAttributeKeyPosition>,
        actualPositions: Map<RelativeFusionPathName, KeyPosition>
    ) {
        assertEquals(
            "key position size mismatch; expected: $expectedPositions, actual: $actualPositions",
            expectedPositions.size,
            actualPositions.size
        )
        expectedPositions.forEach { expectedPosition ->
            val actualPosition = actualPositions[FusionPathName.attribute(expectedPosition.key)]
            if (actualPosition != null) {
                assertEquals(
                    "key position type mismatch",
                    expectedPosition.type + "Position",
                    actualPosition::class.java.simpleName
                )
                assertEquals(
                    "key position subject mismatch",
                    expectedPosition.subject,
                    actualPosition.subject
                )
                val assertWeight = { expectedWeight: Int, actualWeight: Int ->
                    assertEquals("key position weight mismatch", expectedWeight, actualWeight)
                }
                val assertRefKey =
                    { expectedRefKey: RelativeFusionPathName, actualRefKey: RelativeFusionPathName ->
                        assertEquals("key position ref key mismatch", expectedRefKey, actualRefKey)
                    }
                if (actualPosition is BeforePosition) {
                    assertWeight(expectedPosition.weight, actualPosition.weight)
                    assertRefKey(FusionPathName.attribute(expectedPosition.refKey!!), actualPosition.beforeKey)
                }
                if (actualPosition is AfterPosition) {
                    assertWeight(expectedPosition.weight, actualPosition.weight)
                    assertRefKey(FusionPathName.attribute(expectedPosition.refKey!!), actualPosition.afterKey)
                }
                if (actualPosition is StartPosition) {
                    assertWeight(expectedPosition.weight, actualPosition.weight)
                }
                if (actualPosition is EndPosition) {
                    assertWeight(expectedPosition.weight, actualPosition.weight)
                }
            } else {
                fail("actual position with key ${expectedPosition.key} not found in $actualPositions")
            }
        }
    }

}

internal inline fun <reified T> DataTable.toMockList(): List<T> =
    asList(T::class.java as Type)

private inline fun <TMock, reified TActual : FusionPathDecl> assertRawPathIndexEntries(
    path: String,
    expectedIndex: List<TMock>,
    entriesMapper: (FusionPathIndexEntry) -> List<TActual>,
    assertCode: (TMock, TActual) -> Unit
) {
    val actualDeclarations = entriesMapper.invoke(getPathIndexEntry(path))
    assertEquals("raw path index count mismatch", expectedIndex.size, actualDeclarations.size)
    expectedIndex.forEachIndexed { index, expectedAssignment ->
        val actualAssignment = actualDeclarations[index]
        assertCode.invoke(expectedAssignment, actualAssignment)
    }
}

private fun getRawFusionIndex() = FusionIndexSteps.lastInstance!!.semanticallyNormalizedFusionModel?.rawIndex
    ?: throw IllegalStateException("Fusion indexing step must be called once before this step")

private fun getPrototypeStore() = FusionIndexSteps.lastInstance!!.semanticallyNormalizedFusionModel?.prototypeStore
    ?: throw IllegalStateException("Fusion indexing step must be called once before this step")

private fun getPathIndexEntry(path: String): FusionPathIndexEntry =
    getRawFusionIndex().pathIndex.get(FusionPathName.parseAbsolute(path))

data class MockRawPathAssignment(
    val value: String,
    val type: String,
    val source: String
)

data class MockNestedValue(
    val path: String,
    val value: String,
    val type: String,
)

data class MockPrototypeAttribute(
    val relativePath: String,
    val absolutePath: String,
    val value: String,
    val type: String,
)

data class MockPrototypeApplyAttribute(
    val applyName: String,
    val absolutePath: String,
    val expression: String
)

data class MockAttributeKeyPosition(
    val key: String,
    val type: String,
    val subject: String,
    val weight: Int,
    val refKey: String?,
    val numericIndex: Int?
)