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

package io.neos.fusion4j.lang.util

import io.neos.fusion4j.lang.model.decl.*
import io.neos.fusion4j.lang.model.decl.values.*

@Suppress("unused")
fun prettyPrintAstReferences(fusionDecl: RootFusionDecl) {
    prettyPrint(
        fusionDecl = fusionDecl,
        includePrinter = astPrinter("fusion file includes"),
        prototypePrinter = astPrinterNested("root prototype declarations"),
        pathAssignmentsPrinter = astPrinterNested("path assignments"),
        pathConfigurationsPrinter = astPrinterNested("path configurations"),
        pathCopyDeclarationsPrinter = astPrinterNested("path copy declarations"),
        pathErasuresPrinter = astPrinter("path erasures"),
        codeCommentsPrinter = astPrinter("code comments")
    )
}

private fun astPrinterNested(description: String): (List<CodeIndexedElement>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit =
    { elements, printIndent, _, innerHandler ->
        printIndent("|* $description")
        elements.forEach {
            internalPrintAst(printIndent, it)
            when (it) {
                is FusionPathAssignmentDecl -> {
                    val body = it.valueDeclaration.body
                    if (body != null) {
                        innerHandler(body)
                    }
                }
                is FusionPathConfigurationDecl -> {
                    innerHandler(it.body)
                }
                is PrototypeDecl -> {
                    if (it.bodyDeclaration != null) {
                        innerHandler(it.bodyDeclaration)
                    }
                }
            }
        }
    }

private fun astPrinter(description: String): (List<CodeIndexedElement>, (String) -> Unit, Int) -> Unit =
    { elements, printIndent, _ ->
        printIndent("|* $description")
        elements.forEach {
            internalPrintAst(printIndent, it)
        }
    }

private fun internalPrintAst(printIndent: (String) -> Unit, it: CodeIndexedElement) {
    printIndent("  |-*[${it.codeIndex}] ${it.elementIdentifier} AST:")
    printIndent("    |- description: ${it.astReference.description}")
    printIndent("    |- code: ${it.astReference.code}")
    printIndent("    |- start position: ${it.astReference.startPosition}")
    printIndent("    |- end position: ${it.astReference.endPosition}")
}

fun prettyPrintFusionModel(fusionDecl: RootFusionDecl) = prettyPrint(
    fusionDecl = fusionDecl,
    includePrinter = { includes, printIndent, _ ->
        printIndent("|* fusion file includes:")
        includes.forEach {
            printIndent("  |-*[${it.codeIndex}] pattern: ${it.includePattern}")
        }
    },
    prototypePrinter = { prototypes, printIndent, _, innerHandler ->
        printIndent("|* root prototype declarations:")
        prototypes.forEach {
            printIndent("  |- ${it.name}")
            printIndent("    |- inherited prototype: ${it.inheritPrototype}")
            if (it.bodyDeclaration != null) {
                printIndent("    |- body:")
                innerHandler(it.bodyDeclaration)
            }
        }
    },
    pathAssignmentsPrinter = { assignments, printIndent, currentLevel, innerHandler ->
        printIndent("|* path assignments:")
        assignments.forEach {
            printIndent(" |-[${it.codeIndex}] ${it.relativePath} ${it.valueDeclaration.fusionValue.getReadableType()}")
            when (val valueDeclaration = it.valueDeclaration) {
                is NullValueDecl -> {
                    printIndent("    |- NULL")
                }
                is PrimitiveValueDecl<*> -> {
                    printIndent("    |- primitive type: ${valueDeclaration.javaClass.simpleName}")
                    printIndent("    |- value: $valueDeclaration")
                }
                is ExpressionValueDecl -> {
                    printIndent("    |- expression: $valueDeclaration")
                }
                is DslDelegateValueDecl -> {
                    printIndent("    |- DSL name: ${valueDeclaration.fusionValue.dslName}")
                    printIndent("    |- code:")
                    printIndent(indent(currentLevel + 1, valueDeclaration.fusionValue.code))
                }
                is FusionObjectValueDecl -> {
                    printIndent("    |- prototype: ${valueDeclaration.prototypeName}")
                }
                else -> printIndent("    !!! unsupported fusion value type: $valueDeclaration")
            }
            val body = it.valueDeclaration.body
            if (body != null) {
                printIndent("      |- body:")
                innerHandler(body)
            }
        }
    },
    pathConfigurationsPrinter = { configurations, printIndent, _, innerHandler ->
        printIndent("|* path configurations:")
        configurations.forEach {
            printIndent(" |-[${it.codeIndex}] ${it.relativePath}")
            if (!it.body.empty) {
                printIndent("    |- body:")
                innerHandler(it.body)
            }
        }
    },
    pathCopyDeclarationsPrinter = { copyDeclarations, printIndent, _, innerHandler ->
        printIndent("|* path copy declarations:")
        copyDeclarations.forEach {
            printIndent(" |-[${it.codeIndex}] ${it.relativePath}")
            val body = it.body
            if (body != null && !body.empty) {
                printIndent("    |- body:")
                innerHandler(body)
            }
        }
    },
    pathErasuresPrinter = { erasures, printIndent, _ ->
        printIndent("|* path erasures:")
        erasures.forEach { pathErasure ->
            printIndent(" |-[${pathErasure.codeIndex}] ${pathErasure.relativePath}")
        }
    },
    codeCommentsPrinter = { comments, printIndent, _ ->
        printIndent("|* code comments:")
        comments.forEach { codeComment ->
            printIndent(" |-[${codeComment.codeIndex}] ${codeComment.comment}")
        }
    }
)

private fun prettyPrint(
    fusionDecl: RootFusionDecl,
    includePrinter: (List<FusionFileIncludeDecl>, (String) -> Unit, Int) -> Unit,
    prototypePrinter: (List<PrototypeDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathAssignmentsPrinter: (List<FusionPathAssignmentDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathConfigurationsPrinter: (List<FusionPathConfigurationDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathCopyDeclarationsPrinter: (List<FusionPathCopyDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathErasuresPrinter: (List<FusionPathErasureDecl>, (String) -> Unit, Int) -> Unit,
    codeCommentsPrinter: (List<CodeCommentDecl>, (String) -> Unit, Int) -> Unit,
) {
    val printIndent: (String) -> Unit = printIndent(0)

    printIndent("########################################")
    printIndent("### Fusion Source: ${fusionDecl.sourceIdentifier}")

    if (fusionDecl.fileIncludes.isNotEmpty()) {
        includePrinter(fusionDecl.fileIncludes, printIndent, 0)
    }

    if (fusionDecl.rootPrototypeDeclarations.isNotEmpty()) {
        prototypePrinter(fusionDecl.rootPrototypeDeclarations, printIndent, 0) {
            prettyPrintInner(
                it,
                2,
                pathAssignmentsPrinter,
                pathConfigurationsPrinter,
                pathCopyDeclarationsPrinter,
                pathErasuresPrinter,
                codeCommentsPrinter
            )
        }
    }

    prettyPrintInner(
        fusionDecl,
        0,
        pathAssignmentsPrinter,
        pathConfigurationsPrinter,
        pathCopyDeclarationsPrinter,
        pathErasuresPrinter,
        codeCommentsPrinter
    )

    printIndent("########################################")
}

private fun prettyPrintInner(
    fusionDecl: FusionDecl,
    currentLevel: Int,
    pathAssignmentsPrinter: (List<FusionPathAssignmentDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathConfigurationsPrinter: (List<FusionPathConfigurationDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathCopyDeclarationsPrinter: (List<FusionPathCopyDecl>, (String) -> Unit, Int, (InnerFusionDecl) -> Unit) -> Unit,
    pathErasuresPrinter: (List<FusionPathErasureDecl>, (String) -> Unit, Int) -> Unit,
    codeCommentsPrinter: (List<CodeCommentDecl>, (String) -> Unit, Int) -> Unit,
) {
    val internalPrintIndent: (String) -> Unit = printIndent(currentLevel)

    if (fusionDecl.pathAssignments.isNotEmpty()) {
        pathAssignmentsPrinter(fusionDecl.pathAssignments, internalPrintIndent, currentLevel) {
            prettyPrintInner(
                it,
                currentLevel + 2,
                pathAssignmentsPrinter,
                pathConfigurationsPrinter,
                pathCopyDeclarationsPrinter,
                pathErasuresPrinter,
                codeCommentsPrinter
            )
        }
    }

    if (fusionDecl.pathConfigurations.isNotEmpty()) {
        pathConfigurationsPrinter(fusionDecl.pathConfigurations, internalPrintIndent, currentLevel) {
            prettyPrintInner(
                it,
                currentLevel + 2,
                pathAssignmentsPrinter,
                pathConfigurationsPrinter,
                pathCopyDeclarationsPrinter,
                pathErasuresPrinter,
                codeCommentsPrinter
            )
        }
    }

    if (fusionDecl.pathCopyDeclarations.isNotEmpty()) {
        pathCopyDeclarationsPrinter(fusionDecl.pathCopyDeclarations, internalPrintIndent, currentLevel) {
            prettyPrintInner(
                it,
                currentLevel + 2,
                pathAssignmentsPrinter,
                pathConfigurationsPrinter,
                pathCopyDeclarationsPrinter,
                pathErasuresPrinter,
                codeCommentsPrinter
            )
        }
    }

    if (fusionDecl.pathErasures.isNotEmpty()) {
        pathErasuresPrinter(fusionDecl.pathErasures, internalPrintIndent, currentLevel)
    }

    if (fusionDecl.codeComments.isNotEmpty()) {
        codeCommentsPrinter(fusionDecl.codeComments, internalPrintIndent, currentLevel)
    }
}

fun printIndent(currentLevel: Int): (String) -> Unit = { message -> println(indent(currentLevel) + message) }
private fun indent(level: Int) = "    ".repeat(level)
fun indent(level: Int, value: String) =
    value
        .replace("^".toRegex(), indent(level))
        .replace("\n".toRegex(), "\n${indent(level)}")
