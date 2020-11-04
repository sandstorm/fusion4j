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

package io.neos.fusion4j.lang

import io.neos.fusion4j.lang.antlr.FusionLexer
import io.neos.fusion4j.lang.antlr.FusionParser
import io.neos.fusion4j.lang.file.FusionFile
import io.neos.fusion4j.lang.file.FusionPackageDefinition
import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.file.StandalonePathSourceIdentifier
import io.neos.fusion4j.lang.model.DslName
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.decl.AstReference
import io.neos.fusion4j.lang.model.decl.RootFusionDecl
import io.neos.fusion4j.lang.parser.*
import io.neos.fusion4j.lang.parser.afx.AfxDslParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.TerminalNode
import java.nio.file.Path

val DEFAULT_DSL_PARSERS = mapOf(
    AfxDslParser.AFX_DSL_NAME to AfxDslParser()
)

/**
 * Main Entry point to parse a set of fusion packages.
 *
 * @param fusionPackageDefinitions Set<FusionPackage> contains [FusionFile] instances for abstracting over the actual source code.
 */
fun parseFusionPackages(
    fusionPackageDefinitions: Set<FusionPackageDefinition>,
    dslParsers: Map<DslName, DslParser> = DEFAULT_DSL_PARSERS
): Set<ParseResult> = fusionPackageDefinitions.flatMap { parseFusionPackage(it, dslParsers) }.toSet()

/**
 * Entry point if you want to parse a single package.
 */
fun parseFusionPackage(
    fusionPackageDefinition: FusionPackageDefinition,
    dslParsers: Map<DslName, DslParser>
): Set<ParseResult> = parseEntrypointAndIncludes(
    fusionPackageDefinition,
    fusionPackageDefinition.entrypointFile,
    dslParsers,
    fusionPackageDefinition.fusionFiles - fusionPackageDefinition.entrypointFile
)

private fun parseEntrypointAndIncludes(
    fusionPackageDefinition: FusionPackageDefinition,
    currentFile: FusionFile,
    dslParsers: Map<DslName, DslParser>,
    restFiles: Set<FusionFile>
): Set<ParseResult> {
    // parse
    val parseResult = parseFusionFile(currentFile, dslParsers)
    val fusionDecl = parseResult.success
    // get file includes
    return if (fusionDecl != null) {
        val includePatterns = fusionDecl.fileIncludes
            .map { it.patternAsRegex }

        val filesToInclude = restFiles.filter { file ->
            includePatterns.any { includePattern ->
                includePattern.matches(file.identifier.resourceName.name)
            }
        }

        filesToInclude
            .flatMap {
                parseEntrypointAndIncludes(
                    fusionPackageDefinition,
                    it,
                    dslParsers,
                    restFiles - filesToInclude.toSet()
                )
            }
            .toSet() + parseResult
    } else {
        setOf(parseResult)
    }
}


private fun parseFusionFile(fusionFile: FusionFile, dslParsers: Map<DslName, DslParser>): ParseResult =
    try {
        ParseResult.success(
            fusionFile,
            parseFusion(
                fusionFile.identifier,
                CharStreams.fromStream(fusionFile.getInputStream()),
                dslParsers
            )
        )
    } catch (error: FusionParseException) {
        ParseResult.parseError(fusionFile, error)
    }


class FusionErrorListener(
    private val errorType: FusionSyntaxError.ErrorType
) : BaseErrorListener() {
    val errors: MutableList<FusionSyntaxError> = mutableListOf()
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        errors.add(
            FusionSyntaxError(
                codePosition = AstReference.CodePosition(line, charPositionInLine),
                errorType = errorType,
                message = msg!!,
                offendingSymbol = when (offendingSymbol) {
                    is TerminalNode -> FusionSyntaxError.OffendingSymbol(
                        text = offendingSymbol.text,
                        codePosition = AstReference.CodePosition(
                            offendingSymbol.symbol.line,
                            offendingSymbol.symbol.charPositionInLine
                        )
                    )
                    is Token -> FusionSyntaxError.OffendingSymbol(
                        text = offendingSymbol.text,
                        codePosition = AstReference.CodePosition(
                            offendingSymbol.line,
                            offendingSymbol.charPositionInLine
                        )
                    )
                    else -> null
                },
                parserClass = recognizer!!::class.java,
                cause = e
            )
        )
    }

}

private fun parseFusion(
    source: FusionSourceFileIdentifier,
    charStream: CharStream,
    dslParsers: Map<DslName, DslParser>
): RootFusionDecl =
    withFusionParser(
        charStream,
        // entry rule
        FusionParser::fusionFile,
        { internalModel -> buildFusionMetaModel(source, internalModel, dslParsers) },
        { inputSource, allErrors -> FusionParseException(source, inputSource, allErrors) })

fun parseStandaloneFusionPath(pathString: String): FusionPathName =
    withFusionParser(
        CharStreams.fromString(pathString),
        // entry rule
        FusionParser::fusionPath,
        { internalModel -> buildFusionPathMetaModel(internalModel) },
        { inputSource, allErrors ->
            FusionParseException(
                StandalonePathSourceIdentifier(pathString),
                inputSource,
                allErrors
            )
        })

private fun <T, R> withFusionParser(
    charStream: CharStream,
    parserCode: (FusionParser) -> T,
    mapperCode: (T) -> R,
    errorHandler: (String, List<FusionSyntaxError>) -> RuntimeException
): R {
    val lexerErrorListener = FusionErrorListener(FusionSyntaxError.ErrorType.LEXER)
    val parserErrorListener = FusionErrorListener(FusionSyntaxError.ErrorType.PARSER)

    val lexer = FusionLexer(charStream)
    lexer.removeErrorListeners()
    lexer.addErrorListener(lexerErrorListener)

    val tokenStream = CommonTokenStream(lexer)
    val parser = FusionParser(tokenStream)
    parser.removeErrorListeners()
    parser.addErrorListener(parserErrorListener)

    val internalModel = parserCode.invoke(parser)

    val allErrors = lexerErrorListener.errors + parserErrorListener.errors
    if (allErrors.isNotEmpty()) {
        throw errorHandler(charStream.getText(Interval(0, charStream.size() - 1)), allErrors)
    }

    return mapperCode.invoke(internalModel)
}

@Suppress("unused")
fun printTokenStreamFromPath(path: Path) {
    printTokenStream(CharStreams.fromPath(path))
}

private fun printTokenStream(charStream: CharStream) {
    val lexer = FusionLexer(charStream)
    do {
        val token: Token = lexer.nextToken()
        println("######## ${FusionLexer.VOCABULARY.getSymbolicName(token.type)}")
        println("'${token.text}'")
    } while (token.type != Token.EOF)
}
