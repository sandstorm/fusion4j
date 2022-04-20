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

package io.neos.fusion4j.lang.model.decl

import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.model.*

interface FusionLangElement : WithAstReference {

    val sourceIdentifier: FusionSourceFileIdentifier
    val elementIdentifier: FusionLangElementIdentifier

    val hintMessage: String get() = sourceIdentifier.buildHintMessage(astReference)

}

interface InnerFusionLangElement {
    val parentElementIdentifier: FusionLangElementIdentifier
}

data class FusionLangElementIdentifier internal constructor(
    val fusionFile: FusionSourceFileIdentifier,
    private val identifier: String
) {
    val fullyQualifiedIdentifier: String = fusionFile.identifierAsString + identifier

    fun append(child: String, type: String, codeIndex: Int) =
        FusionLangElementIdentifier(fusionFile, "$identifier/$child<$type>[$codeIndex]")

    fun appendInner(child: String, type: String) =
        FusionLangElementIdentifier(fusionFile, "$identifier/$child/<$type>")

    fun appendType(type: String) =
        FusionLangElementIdentifier(fusionFile, "$identifier/<$type>")

    override fun toString(): String = fullyQualifiedIdentifier
}

fun identifierForRootFusionDeclaration(source: FusionSourceFileIdentifier): FusionLangElementIdentifier =
    FusionLangElementIdentifier(source, "")

fun identifierForFileInclude(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    includePattern: FusionFileIncludePattern
): FusionLangElementIdentifier =
    parentElementIdentifier.append("'${includePattern.pattern}'", "FileInclude", codeIndex)

fun identifierForFileIncludePattern(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    parentElementIdentifier.appendType("FileIncludePattern")

fun identifierForNamespaceAlias(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    alias: PrototypeNamespace,
    targetNamespace: PrototypeNamespace
): FusionLangElementIdentifier =
    parentElementIdentifier.append("${alias.name}->${targetNamespace.name}", "NamespaceAlias", codeIndex)

enum class NamespaceDeclType(
    val typeAsString: String
) {
    ALIAS_SOURCE("NamespaceAliasSource"),
    ALIAS_TARGET("NamespaceAliasTarget"),
    PROTOTYPE_CALL("NamespacePrototypeCall")
}

fun identifierForNamespaceNameDecl(
    parentElementIdentifier: FusionLangElementIdentifier,
    namespace: PrototypeNamespace,
    type: NamespaceDeclType
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(namespace.name, type.typeAsString)

fun identifierForQualifiedPrototypeName(
    parentElementIdentifier: FusionLangElementIdentifier,
    qualifiedNameAsString: String
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(qualifiedNameAsString, "QualifiedPrototypeName")

fun identifierForSimplePrototypeName(
    parentElementIdentifier: FusionLangElementIdentifier,
    simpleNameAsString: String
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(simpleNameAsString, "SimplePrototypeName")

fun identifierForPathErasure(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    fusionPath: FusionPathName
): FusionLangElementIdentifier =
    parentElementIdentifier.append("$fusionPath", "Erasure", codeIndex)

fun identifierForPathName(
    parentElementIdentifier: FusionLangElementIdentifier,
    fusionPath: String
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(fusionPath, "PathName")

fun identifierForPathNameReference(
    parentElementIdentifier: FusionLangElementIdentifier,
    fusionPath: String,
    absolute: Boolean
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(fusionPath, "PathNameReference|${if (absolute) "absolute" else "relative"}")

fun identifierForPathNameSegmentPrototypeCall(
    parentElementIdentifier: FusionLangElementIdentifier,
    qualifiedPrototypeNameAsString: String
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner("prototype(${qualifiedPrototypeNameAsString})", "PathNameSegmentPrototypeCall")

fun identifierForPathNameSegmentMetaProperty(
    parentElementIdentifier: FusionLangElementIdentifier,
    pathSegmentName: String
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(pathSegmentName, "PathNameSegmentMetaProperty")

fun identifierForPathNameSegment(
    parentElementIdentifier: FusionLangElementIdentifier,
    pathNameSegment: FusionPathNameSegment
): FusionLangElementIdentifier =
    parentElementIdentifier.appendInner(pathNameSegment.segmentAsString, "PathNameSegment")

fun identifierForPathConfiguration(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    fusionPath: RelativeFusionPathName
): FusionLangElementIdentifier =
    parentElementIdentifier.append(fusionPath.pathAsString, "PathConfiguration", codeIndex)

fun identifierForPathConfigurationBody(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    parentElementIdentifier.appendType("PathConfigurationBody")

fun identifierForPathAssignment(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    fusionPath: RelativeFusionPathName
): FusionLangElementIdentifier =
    parentElementIdentifier.append(fusionPath.pathAsString, "PathAssignment", codeIndex)

fun identifierForPathAssignmentValue(
    parentElementIdentifier: FusionLangElementIdentifier,
    dataType: String
): FusionLangElementIdentifier =
    parentElementIdentifier.appendType("PathAssignmentValue|$dataType")

fun identifierForValueBoolean(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "Boolean")

fun identifierForValueString(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "String")

fun identifierForValueInteger(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "Integer")

fun identifierForValueDouble(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "Double")

fun identifierForValueNull(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "Null")

fun identifierForValueDslDelegate(
    parentElementIdentifier: FusionLangElementIdentifier,
    dslName: DslName
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "DslDelegate|$dslName")

fun identifierForValueFusionObject(
    parentElementIdentifier: FusionLangElementIdentifier,
    qualifiedPrototypeNameAsString: String
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "FusionObject|$qualifiedPrototypeNameAsString")

fun identifierForValueBody(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    parentElementIdentifier.appendType("FusionValueBody")

fun identifierForValueExpression(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    identifierForPathAssignmentValue(parentElementIdentifier, "Expression")

fun identifierForCodeComment(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int
): FusionLangElementIdentifier =
    parentElementIdentifier.append("", "Comment", codeIndex)

fun identifierForRootPrototypeDeclaration(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    prototypeName: QualifiedPrototypeName
): FusionLangElementIdentifier =
    parentElementIdentifier.append(prototypeName.qualifiedName, "PrototypeDecl", codeIndex)

fun identifierForRootPrototypeDeclarationBody(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    parentElementIdentifier.appendType("PrototypeDeclBody")

fun identifierForPathCopy(
    parentElementIdentifier: FusionLangElementIdentifier,
    codeIndex: Int,
    fusionPath: FusionPathName
): FusionLangElementIdentifier =
    parentElementIdentifier.append(fusionPath.pathAsString, "PathCopy", codeIndex)

fun identifierForPathCopyBody(
    parentElementIdentifier: FusionLangElementIdentifier
): FusionLangElementIdentifier =
    parentElementIdentifier.appendType("PathCopyBody")
