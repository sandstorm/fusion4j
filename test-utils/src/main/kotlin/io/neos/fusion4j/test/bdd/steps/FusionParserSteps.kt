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

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.neos.fusion4j.lang.file.FusionResourceName
import io.neos.fusion4j.lang.file.InMemoryFusionFile
import io.neos.fusion4j.lang.model.*
import io.neos.fusion4j.lang.model.decl.*
import io.neos.fusion4j.lang.model.decl.values.FusionObjectValueDecl
import io.neos.fusion4j.lang.model.values.StringValue
import io.neos.fusion4j.lang.parseFusionPackages
import io.neos.fusion4j.lang.parser.FusionParseException
import io.neos.fusion4j.lang.parser.FusionSyntaxError
import io.neos.fusion4j.lang.parser.ParseResult
import io.neos.fusion4j.lang.util.prettyPrintFusionModel
import org.junit.Assert.*
import java.lang.reflect.Type

@Suppress("unused")
class FusionParserSteps : En {
    var parsedPaths: MutableMap<String, StandalonePathParseResult> = mutableMapOf()

    private var parsedResult: Set<ParseResult>? = null

    private val parsedFusionModel: Map<String, RootFusionDecl>
        get() =
            expectCompletelySuccessfulParseResult()
                .associateBy { it.sourceIdentifier.resourceName.name }

    companion object {
        var lastInstance: FusionParserSteps? = null

        fun getInstance(): FusionParserSteps =
            lastInstance ?: throw IllegalStateException("No instance of FusionParserSteps found")

        fun getParseResult(): Set<ParseResult> =
            getInstance().parsedResult
                ?: throw IllegalStateException("No Fusion code was parsed before; use 'When I parse all packages ...'")

        fun expectCompletelySuccessfulParseResult(): Set<RootFusionDecl> {
            val parseResult = getParseResult()
            if (parseResult.any { it.success == null }) {
                fail("cannot create Fusion runtime; there are parse errors: " + parseResult.mapNotNull { it.error })
            }
            return parseResult.map { it.success!! }.toSet()
        }
    }

    init {
        Before { _ ->
            lastInstance = this
            parsedPaths.clear()
        }

        After { _ ->
            lastInstance = null
        }

        DataTableType { entry: Map<String, String> ->
            MockPathAssignment(
                codeIndex = (entry["index"] ?: error("index not given")).toInt(),
                path = entry["path"] ?: error("path not given"),
                value = entry["value"] ?: error("value not given"),
                type = entry["type"] ?: error("type not given")
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockPrototypeDeclaration(
                codeIndex = (entry["index"] ?: error("index not given")).toInt(),
                prototypeName = entry["name"] ?: error("name not given"),
                inheritPrototypeName = if (entry["inherit"].isNullOrBlank()) null else entry["inherit"],
                hasBody = (entry["hasBody"] ?: "true").toBoolean()
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockPathErasure(
                codeIndex = (entry["index"] ?: error("index not given")).toInt(),
                path = entry["path"] ?: error("path not given")
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockElement(
                codeIndex = (entry["index"] ?: error("index not given")).toInt(),
                type = "${entry["type"]}Decl"
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockPathSegmentAstReference(
                name = entry["name"] ?: error("name not given"),
                type = entry["type"] ?: error("type not given"),
                startLine = (entry["startLine"] ?: error("start line not given")).toInt(),
                startChar = (entry["startChar"] ?: error("start char not given")).toInt(),
                endLine = (entry["endLine"] ?: error("end line not given")).toInt(),
                endChar = (entry["endChar"] ?: error("end char not given")).toInt()
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockParseError(
                line = (entry["line"] ?: error("line not given")).toInt(),
                char = (entry["char"] ?: error("char not given")).toInt(),
                message = entry["message"] ?: error("message not given"),
                offendingText = entry["text"] ?: error("text not given"),
                offendingLine = (entry["offendingLine"] ?: error("offendingLine not given")).toInt(),
                offendingChar = (entry["offendingChar"] ?: error("offendingChar not given")).toInt()
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockLexerError(
                line = (entry["line"] ?: error("line not given")).toInt(),
                char = (entry["char"] ?: error("char not given")).toInt(),
                message = entry["message"] ?: error("message not given")
            )
        }

        DataTableType { entry: Map<String, String> ->
            MockStandalonePathSegment(
                name = entry["name"] ?: error("name not given"),
                type = entry["type"] ?: error("type not given"),
                quoting = PathNameSegmentQuoting.valueOf(entry["quoting"] ?: error("quoting not given"))
            )
        }

        When("all Fusion packages are parsed") {
            if (parsedResult == null) {
                parsedResult = parseFusionPackages(FusionCodeDefinitionSteps.getFusionPackages())
                    .onEach { parseResult ->
                        run {
                            if (parseResult.success != null) {
                                //println("parsed Fusion for '${parseResult.input}'")
                                //prettyPrintFusionModel(parseResult.success!!)
                            }
                            if (parseResult.error != null) {
                                println("parse error(s): ${parseResult.error}")
                            }
                        }
                    }
            } else {
                fail("Fusion parse step could only be called once per scenario")
            }
        }

        When("I parse the standalone Fusion path {string}") { path: String ->
            try {
                parsedPaths[path] = StandalonePathParseResult(
                    FusionPathName.parse(path), null
                )
            } catch (parseError: FusionParseException) {
                parsedPaths[path] = StandalonePathParseResult(null, parseError)
            }
        }

        Then("there must be no parse errors for standalone Fusion path {string}") { path: String ->
            val standalonePathParseResult = parsedPaths[path]
            assertNotNull("no parse result for path $path", standalonePathParseResult)
            assertNotNull("parse errors: ${standalonePathParseResult!!.error}", standalonePathParseResult.pathName)
        }

        Then("the parsed standalone Fusion path {string} must have the following path segments") { path: String, expectedSegmentsData: DataTable ->
            val expectedSegments = expectedSegmentsData.toMockList<MockStandalonePathSegment>()
            val actualSegments = parsedPaths[path]!!.pathName!!.segments
            assertEquals("standalone path segment count mismatch", expectedSegments.size, actualSegments.size)
            expectedSegments.forEachIndexed { index, expectedSegment ->
                val actualSegment = actualSegments[index]
                val actualPropertySegment = when (expectedSegment.type) {
                    "Property" -> {
                        assertEquals(
                            "standalone path segment type mismatch",
                            PropertyPathSegment::class.java,
                            actualSegment::class.java
                        )
                        assertEquals(
                            "standalone path segment quoting mismatch",
                            expectedSegment.quoting,
                            (actualSegment as PropertyPathSegment).quoting
                        )
                        actualSegment
                    }
                    "Meta" -> {
                        assertEquals(
                            "standalone path segment type mismatch",
                            MetaPropertyPathSegment::class.java,
                            actualSegment::class.java
                        )
                        actualSegment as FusionPropertyPathSegment
                    }
                    else -> throw IllegalArgumentException("unsupported expected standalone path segment type: ${expectedSegment.type}; supported is 'Property' and 'Meta'")
                }
                assertEquals("standalone path segment name mismatch", expectedSegment.name, actualPropertySegment.name)
            }
        }

        Then("the model at {string} must contain {int} root prototype declarations") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: RootFusionDecl ->
                assertEquals(
                    "Root prototype declaration count mismatch",
                    expectedCount,
                    model.rootPrototypeDeclarations.size
                )
            }
        }

        Then("the model at {string} must contain {int} file includes") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: RootFusionDecl ->
                assertEquals("File include count mismatch", expectedCount, model.fileIncludes.size)
            }
        }

        Then("the file include at {string} must have the pattern {string}") { path: String, expectedPattern: String ->
            assertModel(path, parsedFusionModel) { model: FusionFileIncludeDecl ->
                assertEquals("file include pattern mismatch", expectedPattern, model.patternAsString)
            }
        }

        Then("the model at {string} must contain {int} namespace aliases") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: RootFusionDecl ->
                assertEquals("Namespace alias count mismatch", expectedCount, model.namespaceAliases.size)
            }
        }

        Then("the namespace alias at {string} must declare {string} as alias for {string}") { path: String, expectedAlias: String, expectedTarget: String ->
            assertModel(path, parsedFusionModel) { model: NamespaceAliasDecl ->
                assertEquals("namespace alias mismatch", expectedAlias, model.alias.name.name)
                assertEquals("namespace alias target mismatch", expectedTarget, model.targetNamespace.name.name)
            }
        }

        Then("the model at {string} must contain the root prototype declarations") { path: String, expectedPrototypeDeclarations: DataTable ->
            assertModel(path, parsedFusionModel) { model: RootFusionDecl ->
                expectedPrototypeDeclarations.toMockList<MockPrototypeDeclaration>().forEach { expectedPrototype ->
                    assertPrototypeDeclarationExists(expectedPrototype, model.rootPrototypeDeclarations)
                }
            }
        }

        Then("the model at {string} must contain {int} path erasures") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals("Path erasure count mismatch", expectedCount, model.pathErasures.size)
            }
        }

        Then("the model at {string} must contain {int} code comments") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals("Path erasure count mismatch", expectedCount, model.codeComments.size)
            }
        }

        Then("the model at {string} must contain the code comment {string} at index {int}") { path: String, expectedComment: String, expectedIndex: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertCodeCommentExists(MockCodeComment(expectedIndex, expectedComment), model.codeComments)
            }
        }

        Then("the model at {string} must contain the following code comment at index {int}") { path: String, expectedIndex: Int, expectedComment: String ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertCodeCommentExists(MockCodeComment(expectedIndex, expectedComment), model.codeComments)
            }
        }

        Then("the model at {string} must contain the path erasures") { path: String, expectedErasures: DataTable ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                expectedErasures.toMockList<MockPathErasure>().forEach { expectedErasure ->
                    assertPathErasureExists(expectedErasure, model.pathErasures)
                }
            }
        }

        Then("the model at {string} must contain the path erasure {string} at index {int}") { path: String, expectedPath: String, expectedIndex: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertPathErasureExists(MockPathErasure(expectedIndex, expectedPath), model.pathErasures)
            }
        }

        Then("the model at {string} must contain {int} elements") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals("Path configuration count mismatch", expectedCount, model.elementIndex.size)
            }
        }

        Then("the model at {string} must contain the elements") { path: String, expectedElements: DataTable ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                expectedElements.toMockList<MockElement>().forEach { expectedElement ->
                    val actualElement: Any = try {
                        model.getElementAt(expectedElement.codeIndex)
                    } catch (e: Throwable) {
                        fail("No element found for index ${expectedElement.codeIndex}")
                    }
                    assertEquals("element type mismatch", expectedElement.type, actualElement.javaClass.simpleName)
                }
            }
        }

        Then("the element at {string} and index {int} must be of type {string}") { path: String, expectedIndex: Int, expectedType: String ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals(
                    "type mismatch",
                    "${expectedType}Decl",
                    model.getElementAt<Any>(expectedIndex).javaClass.simpleName
                )
            }
        }

        Then("the element at {string} must start in line {int} at char {int} and end in line {int} at char {int}") { path: String, expectedStartLine: Int, expectedStartChar: Int, expectedEndLine: Int, expectedEndChar: Int ->
            assertModel(path, parsedFusionModel) { model: FusionLangElement ->
                assertAstReference(
                    MockAstReference(
                        expectedStartLine,
                        expectedStartChar,
                        expectedEndLine,
                        expectedEndChar
                    ), model
                )
            }
        }

        Then("the path name at {string} must start in line {int} at char {int} and end in line {int} at char {int}") { path: String, expectedStartLine: Int, expectedStartChar: Int, expectedEndLine: Int, expectedEndChar: Int ->
            assertModel(path, parsedFusionModel) { model: FusionPathDecl ->
                assertAstReference(
                    MockAstReference(
                        expectedStartLine,
                        expectedStartChar,
                        expectedEndLine,
                        expectedEndChar
                    ), model.path
                )
            }
        }

        Then("the path name at {string} must have the following segment AST references") { path: String, expectedAstReferences: DataTable ->
            assertModel(path, parsedFusionModel) { model: FusionPathDecl ->
                expectedAstReferences.toMockList<MockPathSegmentAstReference>()
                    .forEachIndexed { index, expectedPathSegmentAstReference ->
                        val actualModel = model.path.pathSegments[index]
                        assertEquals(
                            "path segment name mismatch",
                            expectedPathSegmentAstReference.name,
                            actualModel.segment.segmentAsString
                        )
                        assertEquals(
                            "path segment type mismatch",
                            expectedPathSegmentAstReference.type,
                            actualModel.segment.type.name
                        )
                        assertAstReference(expectedPathSegmentAstReference, actualModel)
                    }
            }
        }

        Then("the assignment body at {string} must start in line {int} at char {int} and end in line {int} at char {int}") { path: String, expectedStartLine: Int, expectedStartChar: Int, expectedEndLine: Int, expectedEndChar: Int ->
            assertModel(path, parsedFusionModel) { model: FusionPathAssignmentDecl ->
                assertAstReference(
                    MockAstReference(
                        expectedStartLine,
                        expectedStartChar,
                        expectedEndLine,
                        expectedEndChar
                    ), model.valueDeclaration.body!!
                )
            }
        }

        Then("the value at {string} must start in line {int} at char {int} and end in line {int} at char {int}") { path: String, expectedStartLine: Int, expectedStartChar: Int, expectedEndLine: Int, expectedEndChar: Int ->
            assertModel(path, parsedFusionModel) { model: FusionValueDecl ->
                assertAstReference(
                    MockAstReference(
                        expectedStartLine,
                        expectedStartChar,
                        expectedEndLine,
                        expectedEndChar
                    ), model
                )
            }
        }

        Then("the block at {string} must start in line {int} at char {int} and end in line {int} at char {int}") { path: String, expectedStartLine: Int, expectedStartChar: Int, expectedEndLine: Int, expectedEndChar: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertAstReference(
                    MockAstReference(
                        expectedStartLine,
                        expectedStartChar,
                        expectedEndLine,
                        expectedEndChar
                    ), model
                )
            }
        }

        Then("the element at {string} and index {int} has one correlated comment") { path: String, expectedIndex: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals(
                    "correlated comment count mismatch",
                    1,
                    model.getCorrelatedCodeCommentsForElementAt(expectedIndex).size
                )
            }
        }

        Then("the element at {string} and index {int} has {int} correlated comments") { path: String, expectedIndex: Int, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals(
                    "correlated comment count mismatch",
                    expectedCount,
                    model.getCorrelatedCodeCommentsForElementAt(expectedIndex).size
                )
            }
        }

        Then("the element at {string} and index {int} has the correlated comment {string} with index {int}") { path: String, expectedIndex: Int, expectedComment: String, expectedCommentIndex: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertCodeCommentExists(
                    MockCodeComment(expectedCommentIndex, expectedComment),
                    model.getCorrelatedCodeCommentsForElementAt(expectedIndex)
                )
            }
        }

        Then("the element at {string} and index {int} has the following correlated comment with index {int}") { path: String, expectedIndex: Int, expectedCommentIndex: Int, expectedComment: String ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertCodeCommentExists(
                    MockCodeComment(expectedCommentIndex, expectedComment),
                    model.getCorrelatedCodeCommentsForElementAt(expectedIndex)
                )
            }
        }

        Then("the model at {string} must contain {int} path configurations") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals("Path configuration count mismatch", expectedCount, model.pathConfigurations.size)
            }
        }

        Then("the model at {string} must contain {int} path assignments") { path: String, expectedCount: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertEquals(
                    "Path assignment count mismatch; actual assignments:\n${model.pathAssignments}",
                    expectedCount,
                    model.pathAssignments.size
                )
            }
        }

        Then("the model at {string} must contain the path assignments") { path: String, expectedAssignments: DataTable ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                expectedAssignments.toMockList<MockPathAssignment>().forEach { expectedAssignment ->
                    assertPathAssignmentsExists(expectedAssignment, model.pathAssignments)
                }
            }
        }

        Then("the model at {string} must contain the path assignment {string} at index {int} with value {string} of type {string}") { path: String, expectedPath: String, expectedCodeIndex: Int, exptectedValue: String, expectedType: String ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertPathAssignmentsExists(
                    MockPathAssignment(
                        expectedCodeIndex,
                        expectedPath,
                        exptectedValue,
                        expectedType
                    ), model.pathAssignments
                )
            }
        }

        Then("the model at {string} must contain the path assignment {string} at index {int} of type {string} with the following value") { path: String, expectedPath: String, expectedCodeIndex: Int, expectedType: String, expectedValue: String ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertPathAssignmentsExists(
                    MockPathAssignment(
                        expectedCodeIndex,
                        expectedPath,
                        expectedValue,
                        expectedType
                    ), model.pathAssignments
                ) {
                    it.replace(Regex("\\s+"), " ")
                }
            }
        }

        Then("the model at {string} must contain the path configuration {string} at index {int}") { path: String, expectedPath: String, expectedCodeIndex: Int ->
            assertModel(path, parsedFusionModel) { model: FusionDecl ->
                assertPathConfigurationExists(
                    MockPathConfiguration(
                        expectedCodeIndex,
                        expectedPath
                    ), model.pathConfigurations
                )
            }
        }

        Then("the prototype declaration at {string} must have no body") { path: String ->
            assertModel(path, parsedFusionModel) { model: PrototypeDecl ->
                assertNull(
                    "root prototype declaration ${model.name} (code index: ${model.codeIndex}) has a body; expected to have no body",
                    model.bodyDeclaration
                )
            }
        }

        Then("the prototype declaration at {string} must have a body") { path: String ->
            assertModel(path, parsedFusionModel) { model: PrototypeDecl ->
                assertNotNull(
                    "root prototype declaration ${model.name} (code index: ${model.codeIndex}) has no body (null); expected to have a body",
                    model.bodyDeclaration
                )
            }
        }

        Then("the path assignment at {string} must have no body") { path: String ->
            assertModel(path, parsedFusionModel) { model: FusionPathAssignmentDecl ->
                assertEquals(
                    "no fusion object type",
                    FusionObjectValueDecl::class.java,
                    model.valueDeclaration.javaClass
                )
                assertNull(
                    "fusion object path assigment ${model.relativePath} (code index: ${model.codeIndex}) has a body; expected to have no body",
                    model.valueDeclaration.body
                )
            }
        }

        Then("the path assignment at {string} must have a body") { path: String ->
            assertModel(path, parsedFusionModel) { model: FusionPathAssignmentDecl ->
                assertNotNull(
                    "fusion object path assigment ${model.relativePath} (code index: ${model.codeIndex}) has no body; expected to have a body",
                    model.valueDeclaration.body
                )
            }
        }

        Then("there must be no parse errors") {
            assertEquals("there were parse errors", 0, getParseResult().count { it.error != null })
        }

        Then("there must be {int} successfully parsed Fusion files") { expectedCount: Int ->
            assertEquals("parsed file count mismatch", expectedCount, getParseResult().count { it.success != null })
        }

        fun expectNoParseErrors(fileName: String, packageName: FusionPackageName) {
            val fileResult =
                getParseResult().singleOrNull {
                    it.input.identifier == InMemoryFusionFile.createSourceIdentifier(
                        packageName,
                        FusionResourceName.fromString(fileName)
                    )
                }
            if (fileResult == null) {
                fail("fusion file $fileName not parsed")
            } else {
                val error = fileResult.error
                if (error != null) {
                    error.printStackTrace()
                    fail("no parse error expected, but found: $error")
                }
            }
        }

        Then("there must be no parse errors in file {string}") { fileName: String ->
            val fusionPackages = FusionCodeDefinitionSteps.getFusionPackages()
            expectNoParseErrors(
                fileName, fusionPackages.singleOrNull()?.packageName
                    ?: throw IllegalStateException("Only single package expected, but got $fusionPackages")
            )
        }

        Then("there must be no parse errors in file {string} for package {string}") { fileName: String, packageName: String ->
            expectNoParseErrors(fileName, FusionPackageName(packageName))
        }

        Then("there must be the following parse errors in file {string}") { fileName: String, expectedParseErrors: DataTable ->
            assertFusionSyntaxErrors(
                expectedParseErrors.toMockList<MockParseError>(),
                fileName,
                FusionSyntaxError.ErrorType.PARSER
            ) { expectedError, actualError ->
                assertEquals("parse error line mismatch", expectedError.line, actualError.codePosition.line)
                assertEquals(
                    "parse error char position in line mismatch",
                    expectedError.char,
                    actualError.codePosition.charPositionInLine
                )
                assertEquals("parse error message mismatch", expectedError.message, actualError.message)
                assertNotNull("parse error must have a offending symbol set", actualError.offendingSymbol)
                assertEquals(
                    "parse error offending symbol text mismatch",
                    expectedError.offendingText,
                    actualError.offendingSymbol!!.text
                )
                assertEquals(
                    "parse error offending symbol line mismatch",
                    expectedError.offendingLine,
                    actualError.offendingSymbol!!.codePosition.line
                )
                assertEquals(
                    "parse error offending symbol char position in line mismatch",
                    expectedError.offendingChar,
                    actualError.offendingSymbol!!.codePosition.charPositionInLine
                )
            }
        }

        Then("there must be the following lexer errors in file {string}") { fileName: String, expectedParseErrors: DataTable ->
            assertFusionSyntaxErrors(
                expectedParseErrors.toMockList<MockLexerError>(),
                fileName,
                FusionSyntaxError.ErrorType.LEXER
            ) { expectedError, actualError ->
                assertEquals("parse error line mismatch", expectedError.line, actualError.codePosition.line)
                assertEquals(
                    "parse error char position in line mismatch",
                    expectedError.char,
                    actualError.codePosition.charPositionInLine
                )
                assertEquals("parse error message mismatch", expectedError.message, actualError.message)
            }
        }

    }

    private fun <TMockError> assertFusionSyntaxErrors(
        expectedErrors: List<TMockError>,
        fileName: String,
        errorType: FusionSyntaxError.ErrorType,
        assertionCode: (TMockError, FusionSyntaxError) -> Unit
    ) {
        val expectedCount = expectedErrors.count()
        val error = getParseResult()
            .singleOrNull { it.input.identifier.resourceName.name == fileName }?.error
        assertNotNull("no $errorType errors found for file $fileName; expected $expectedCount errors", error)
        val parseErrors = error!!.parseErrors.filter { it.errorType == errorType }
        assertEquals("$errorType error count mismatch for file $fileName", expectedCount, parseErrors.size)
        parseErrors.forEachIndexed { index, actualError ->
            val expectedError = expectedErrors[index]
            assertionCode(expectedError, actualError)
        }
    }

    private fun assertAstReference(expected: MockAstReferenceInterface, actualModel: FusionLangElement) {
        assertEquals(
            "start code line position mismatch",
            expected.startLine,
            actualModel.astReference.startPosition.line
        )
        assertEquals(
            "start code char in line position mismatch",
            expected.startChar,
            actualModel.astReference.startPosition.charPositionInLine
        )
        assertEquals("end code line position mismatch", expected.endLine, actualModel.astReference.endPosition.line)
        assertEquals(
            "end code char in line position mismatch",
            expected.endChar,
            actualModel.astReference.endPosition.charPositionInLine
        )
    }

    private fun assertPathAssignmentsExists(
        expected: MockPathAssignment,
        actuals: List<FusionPathAssignmentDecl>,
        sanitizer: (String) -> String = { it -> it }
    ) =
        assertCodeIndexed(expected, actuals) { actual ->
            assertEquals(
                "type mismatch; expected path: $expected - actual: $actual",
                "${expected.type}Decl",
                actual.valueDeclaration.javaClass.simpleName.toString()
            )
            assertEquals("path name mismatch", expected.path, actual.relativePath.pathAsString)
            assertEquals(
                "value mismatch",
                sanitizer(StringValue.restoreSpecialWhitespaceCharacters(expected.value)),
                sanitizer(actual.valueDeclaration.fusionValue.getReadableValue())
            )
        }

    private fun assertPathConfigurationExists(
        expected: MockPathConfiguration,
        actuals: List<FusionPathConfigurationDecl>
    ) =
        assertCodeIndexed(expected, actuals) { actual ->
            assertEquals("path name mismatch", expected.path, actual.relativePath.pathAsString)
        }

    private fun assertPrototypeDeclarationExists(expected: MockPrototypeDeclaration, actuals: List<PrototypeDecl>) =
        assertCodeIndexed(expected, actuals) { actual ->
            assertEquals("prototype name mismatch", expected.prototypeName, actual.qualifiedName.qualifiedName)
            assertEquals(
                "inherit prototype name mismatch",
                expected.inheritPrototypeName,
                actual.inheritPrototype?.qualifiedName?.qualifiedName
            )
            if (expected.hasBody) {
                assertNotNull(
                    "expected prototype ${expected.prototypeName} to have a body; got null",
                    actual.bodyDeclaration
                )
            } else {
                assertNull(
                    "expected prototype ${expected.prototypeName} to have no body; got ${actual.bodyDeclaration}",
                    actual.bodyDeclaration
                )
            }
        }

    private fun assertPathErasureExists(expected: MockPathErasure, actuals: List<FusionPathErasureDecl>) =
        assertCodeIndexed(expected, actuals) { actual ->
            assertEquals("erasure path mismatch", expected.path, actual.relativePath.pathAsString)
        }

    private fun assertCodeCommentExists(expected: MockCodeComment, actuals: List<CodeCommentDecl>) =
        assertCodeIndexed(expected, actuals) { actual ->
            assertEquals("comment mismatch", expected.comment.trim(), actual.comment)
        }

    private fun <TExpected : MockIndexedLangElement, TActual : CodeIndexedElement> assertCodeIndexed(
        expected: TExpected,
        actual: List<TActual>,
        assertionCode: (TActual) -> Unit
    ) {
        val actualAtIndex = actual.find { it.codeIndex == expected.codeIndex }
        if (actualAtIndex == null) {
            fail("no element found for expected $expected at code index in actual elements $actual")
        } else {
            assertionCode(actualAtIndex)
        }
    }

}

data class MockParseError(
    val line: Int,
    val char: Int,
    val message: String,
    val offendingText: String,
    val offendingLine: Int,
    val offendingChar: Int
)

data class MockLexerError(
    val line: Int,
    val char: Int,
    val message: String
)

internal inline fun <reified T> DataTable.toMockList(): List<T> =
    asList(T::class.java as Type)

interface MockIndexedLangElement {
    val codeIndex: Int
}

data class MockPrototypeDeclaration(
    override val codeIndex: Int,
    val prototypeName: String,
    val inheritPrototypeName: String?,
    val hasBody: Boolean
) : MockIndexedLangElement

data class MockPathAssignment(
    override val codeIndex: Int,
    val path: String,
    val value: String,
    val type: String
) : MockIndexedLangElement

data class MockPathErasure(
    override val codeIndex: Int,
    val path: String
) : MockIndexedLangElement

data class MockPathConfiguration(
    override val codeIndex: Int,
    val path: String
) : MockIndexedLangElement

data class MockElement(
    override val codeIndex: Int,
    val type: String
) : MockIndexedLangElement

data class MockCodeComment(
    override val codeIndex: Int,
    val comment: String
) : MockIndexedLangElement

data class MockAstReference(
    override val startLine: Int,
    override val startChar: Int,
    override val endLine: Int,
    override val endChar: Int
) : MockAstReferenceInterface

data class MockStandalonePathSegment(
    val name: String,
    val type: String,
    val quoting: PathNameSegmentQuoting
)

data class MockPathSegmentAstReference(
    val name: String,
    val type: String,
    override val startLine: Int,
    override val startChar: Int,
    override val endLine: Int,
    override val endChar: Int
) : MockAstReferenceInterface

interface MockAstReferenceInterface {
    val startLine: Int
    val startChar: Int
    val endLine: Int
    val endChar: Int
}

internal inline fun <reified T : Any> assertModel(
    path: String,
    allModels: Map<String, RootFusionDecl>,
    assertionCode: (T) -> Unit
) {
    // first segment is file name, followed by path
    val testPath = toTestPathName(path)
    val rootModel = allModels[testPath.fileName]
        ?: throw IllegalArgumentException("fusion model not found for file ${testPath.fileName}")

    if (testPath.segments.isEmpty()) {
        assertionCode(rootModel as T)
        return
    }

    var nestedFusion: FusionDecl = rootModel
    if (testPath.segments.size > 1) {
        for (pathSegmentIdx in 0..testPath.segments.size - 2) {
            val pathSegment = testPath.segments[pathSegmentIdx]
            val child: Any = nestedFusion.getElementAt(pathSegment.index)
            nestedFusion = when (child) {
                is FusionPathConfigurationDecl -> validatePathName(child.body, pathSegment, child.relativePath)
                is FusionPathAssignmentDecl -> if (child.valueDeclaration.body == null)
                    throw IllegalArgumentException("nested path segment $pathSegment (idx: $pathSegmentIdx) must point to a non empty path assignment body") else
                    validatePathName(child.valueDeclaration.body!!, pathSegment, child.relativePath)
                is PrototypeDecl -> validatePathName(
                    child.bodyDeclaration
                        ?: throw IllegalArgumentException("nested path segment $pathSegment (idx: $pathSegmentIdx) must point to a non empty prototype declaration body"),
                    pathSegment,
                    "prototype(${child.qualifiedName})"
                )
                else -> throw IllegalArgumentException("nested path segment $pathSegment (idx: $pathSegmentIdx) must point to a fusion declaration")
            }
        }
    }

    // last segment handling

    val lastSegment = testPath.segments.last()
    var lastElement: Any = nestedFusion.getElementAt(lastSegment.index)
    val genericType = T::class.java
    if (!genericType.isAssignableFrom(lastElement.javaClass)) {
        lastElement = when (lastElement) {
            is FusionPathConfigurationDecl -> validatePathName(lastElement.body, lastSegment, lastElement.relativePath)
            is PrototypeDecl -> when {
                genericType.isAssignableFrom(FusionDecl::class.java) -> validatePathName(
                    lastElement.bodyDeclaration
                        ?: throw IllegalArgumentException("last path segment $lastSegment must point to a non empty prototype declaration body"),
                    lastSegment,
                    "prototype(${lastElement.qualifiedName})"
                )
                else -> TODO("unsupported prototype decl from ${lastElement.javaClass} to $genericType")
            }
            is FusionPathAssignmentDecl -> {
                when {
                    genericType.isAssignableFrom(FusionDecl::class.java) ->
                        if (lastElement.valueDeclaration.body != null)
                            validatePathName(lastElement.valueDeclaration.body!!, lastSegment, lastElement.relativePath)
                        else throw IllegalArgumentException("last path segment $lastSegment must point to a non empty path assignment body")
                    genericType.isAssignableFrom(FusionValueDecl::class.java) -> validatePathName(
                        lastElement.valueDeclaration,
                        lastSegment,
                        lastElement.relativePath
                    )
                    genericType.isAssignableFrom(FusionObjectValueDecl::class.java) -> validatePathName(
                        lastElement.valueDeclaration as FusionObjectValueDecl,
                        lastSegment,
                        lastElement.relativePath
                    )
                    else -> TODO("unsupported required type for path assignment test path")
                }
            }
            else -> TODO("unsupported convert from ${lastElement.javaClass} to $genericType")
        }
    } else {
        lastElement = when (lastElement) {
            is FusionPathDecl -> validatePathName(lastElement, lastSegment, lastElement.relativePath)
            is PrototypeDecl -> validatePathName(lastElement, lastSegment, "prototype(${lastElement.qualifiedName})")
            // code comments must have no path name set
            is CodeCommentDecl -> validateTestPathSegmentHasNoPathName(lastElement, lastSegment)
            is FusionFileIncludeDecl -> validateTestPathSegmentHasNoPathName(lastElement, lastSegment)
            is NamespaceAliasDecl -> validateTestPathSegmentHasNoPathName(lastElement, lastSegment)
            else -> TODO("unsupported name validator: ${lastElement.javaClass}")
        }
    }

    assertionCode(lastElement as T)
}

private fun <T> validatePathName(model: T, pathSegment: TestPathSegment, actualPathName: FusionPathName): T =
    validatePathName(model, pathSegment, actualPathName.pathAsString)

private fun <T> validatePathName(model: T, pathSegment: TestPathSegment, actualPathName: String): T {
    if (pathSegment.pathName != null) {
        assertEquals("Path name must match on segment $pathSegment", pathSegment.pathName, actualPathName)
    }
    return model
}

private fun <T> validateTestPathSegmentHasNoPathName(model: T, pathSegment: TestPathSegment): T {
    if (pathSegment.pathName != null && pathSegment.pathName.isNotEmpty()) {
        fail("the index ${pathSegment.index} references a ${model!!::class.java}. This type may not have a path name set in test path; found: ${pathSegment.pathName}")
    }
    return model
}

private fun toTestPathName(testPath: String): TestPathName {
    val paths = testPath.split("/").filter(String::isNotBlank)
    if (paths.isEmpty()) {
        throw IllegalArgumentException("invalid path $testPath")
    }

    val fileName = paths.first()
    val segments = paths
        .filterIndexed { idx, _ -> idx > 0 }
        .map { pathSegment ->
            val matchResult = "^(.*?)\\[(\\d+)]$".toRegex().matchEntire(pathSegment)
                ?: throw IllegalArgumentException("invalid test fusion path segment $pathSegment")
            val elementIndex = matchResult.groupValues[2].toInt()
            val name = matchResult.groupValues[1]
            return@map TestPathSegment(elementIndex, name)
        }

    return TestPathName(fileName, segments)
}

data class TestPathName(
    val fileName: String,
    val segments: List<TestPathSegment>
)

data class TestPathSegment(
    val index: Int,
    val pathName: String?
)

data class StandalonePathParseResult(
    val pathName: FusionPathName?,
    val error: FusionParseException?
)