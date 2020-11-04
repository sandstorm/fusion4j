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

package io.neos.fusion4j.lang.parser

import io.neos.fusion4j.lang.antlr.FusionParser
import io.neos.fusion4j.lang.antlr.FusionParserBaseVisitor
import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.model.*
import io.neos.fusion4j.lang.model.decl.*
import io.neos.fusion4j.lang.model.decl.values.*
import io.neos.fusion4j.lang.model.values.*
import mu.KLogger
import mu.KotlinLogging
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*
import java.util.regex.Pattern

private val log: KLogger = KotlinLogging.logger {}

fun buildFusionMetaModel(
    source: FusionSourceFileIdentifier,
    fusionFileContext: FusionParser.FusionFileContext,
    dslParsers: Map<DslName, DslParser>
): RootFusionDecl =
    fusionFileContext.accept(RootFusionDeclVisitor(source, dslParsers))

fun buildFusionPathMetaModel(
    fusionPathContext: FusionParser.FusionPathContext
): FusionPathName =
    fusionPathContext.accept(StandaloneFusionPathVisitor())

private fun createIndexer(): () -> Int {
    var nextIndex = 0
    return {
        nextIndex++
    }
}

// public T visitErrorNode(ErrorNode node)
private open class ErrorHandlingFusionParserBaseVisitor<T> : FusionParserBaseVisitor<T>() {
    override fun visitErrorNode(node: ErrorNode?): T {
        TODO("implement error node handling")
    }
}

private class StandaloneFusionPathVisitor : ErrorHandlingFusionParserBaseVisitor<FusionPathName>() {
    override fun visitFusionPath(ctx: FusionParser.FusionPathContext?): FusionPathName {
        val context = checkNotNull(ctx)
        val segmentVisitor = StandaloneFusionPathSegmentVisitor()
        val reference = context.rootFusionConfigurationPathReference()
        // a leading dot makes a path relative '.'
        val relative = reference.ROOT_FUSION_PATH_NESTING_SEPARATOR() != null
        val segments = reference.rootFusionConfigurationPath()
            .children.mapNotNull { parseTree ->
                parseTree.accept(segmentVisitor)
            }
        return if (relative)
            RelativeFusionPathName.fromSegments(segments)
        else
            AbsoluteFusionPathName.fromSegments(segments)
    }
}

private class StandaloneFusionPathSegmentVisitor : ErrorHandlingFusionParserBaseVisitor<FusionPathNameSegment>() {
    override fun visitRootFusionPathSegment(ctx: FusionParser.RootFusionPathSegmentContext?): FusionPathNameSegment {
        val context = checkNotNull(ctx)
        return pathSegmentThatMayBeQuoted(context.ROOT_FUSION_PATH_SEGMENT())
    }

    override fun visitRootFusionMetaPropPathSegment(ctx: FusionParser.RootFusionMetaPropPathSegmentContext?): FusionPathNameSegment {
        val context = checkNotNull(ctx)
        return MetaPropertyPathSegment.create(context.ROOT_FUSION_PATH_SEGMENT().text)
    }

    override fun visitRootPrototypeCall(ctx: FusionParser.RootPrototypeCallContext?): FusionPathNameSegment {
        val context = checkNotNull(ctx)
        return PrototypeCallPathSegment.create(
            QualifiedPrototypeName.fromString(context.PROTOTYPE_NAME().text)
        )
    }
}

private class RootFusionDeclVisitor(
    private val sourceIdentifier: FusionSourceFileIdentifier,
    private val dslParsers: Map<DslName, DslParser>
) : ErrorHandlingFusionParserBaseVisitor<RootFusionDecl>() {

    private val rootFusionIdentifier: FusionLangElementIdentifier = identifierForRootFusionDeclaration(sourceIdentifier)

    override fun visitFusionFile(ctx: FusionParser.FusionFileContext?): RootFusionDecl {
        val context = checkNotNull(ctx)
        val elementVisitor = FusionElementVisitor(rootFusionIdentifier, FusionPathName.root())
        val allElements = context.rootFragment()
            .mapNotNull { it.accept(elementVisitor) }

        return RootFusionDecl(
            elementIdentifier = rootFusionIdentifier,
            pathAssignments = allElements.filterIsInstance<FusionPathAssignmentDecl>(),
            pathConfigurations = allElements.filterIsInstance<FusionPathConfigurationDecl>(),
            pathCopyDeclarations = allElements.filterIsInstance<FusionPathCopyDecl>(),
            pathErasures = allElements.filterIsInstance<FusionPathErasureDecl>(),
            codeComments = allElements.filterIsInstance<CodeCommentDecl>(),
            rootPrototypeDeclarations = allElements.filterIsInstance<PrototypeDecl>(),
            fileIncludes = allElements.filterIsInstance<FusionFileIncludeDecl>(),
            namespaceAliases = allElements.filterIsInstance<NamespaceAliasDecl>(),
            sourceIdentifier = sourceIdentifier,
            astReference = context.toAstReference(rootFusionIdentifier)
        )
    }

    private inner class InnerFusionDeclVisitor(
        private val elementIdentifier: FusionLangElementIdentifier,
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<InnerFusionDecl>() {


        override fun visitFusionConfiguration(ctx: FusionParser.FusionConfigurationContext?): InnerFusionDecl =
            fusionDecl(
                ctx,
                { context -> context.fusionConfigurationBody().fusionFragment() },
                FusionParser.FusionConfigurationContext::toAstReferenceBody
            )

        override fun visitRootFusionConfiguration(ctx: FusionParser.RootFusionConfigurationContext?): InnerFusionDecl =
            fusionDecl(
                ctx,
                { context -> context.rootFusionConfigurationBody().fusionFragment() },
                FusionParser.RootFusionConfigurationContext::toAstReferenceBody
            )

        override fun visitPrototypeBody(ctx: FusionParser.PrototypeBodyContext?): InnerFusionDecl =
            fusionDecl(
                ctx,
                FusionParser.PrototypeBodyContext::fusionFragment,
                FusionParser.PrototypeBodyContext::toAstReference
            )

        override fun visitFusionValueBody(ctx: FusionParser.FusionValueBodyContext?): InnerFusionDecl =
            fusionDecl(
                ctx,
                { context -> context.fusionFragment() },
                FusionParser.FusionValueBodyContext::toAstReference
            )

        override fun visitRootFusionValueBody(ctx: FusionParser.RootFusionValueBodyContext?): InnerFusionDecl =
            fusionDecl(
                ctx,
                { context -> context.fusionFragment() },
                FusionParser.RootFusionValueBodyContext::toAstReference
            )

        private fun <TContext : ParserRuleContext> fusionDecl(
            ctx: TContext?,
            childrenMapper: (TContext) -> List<ParseTree>,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference
        ): InnerFusionDecl {
            val context = checkNotNull(ctx)

            val elementVisitor = FusionElementVisitor(elementIdentifier, currentPath)
            val allContexts = childrenMapper(context)
            val allElements = allContexts.mapNotNull { it.accept(elementVisitor) }

            return InnerFusionDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                pathAssignments = allElements.filterIsInstance<FusionPathAssignmentDecl>(),
                pathConfigurations = allElements.filterIsInstance<FusionPathConfigurationDecl>(),
                pathCopyDeclarations = allElements.filterIsInstance<FusionPathCopyDecl>(),
                pathErasures = allElements.filterIsInstance<FusionPathErasureDecl>(),
                codeComments = allElements.filterIsInstance<CodeCommentDecl>(),
                sourceIdentifier = sourceIdentifier,
                astReference = astReferenceMapper(context, elementIdentifier)
            )
        }

    }

    private inner class FusionElementVisitor(
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<Any>() {
        private val indexer = createIndexer()

        override fun visitRootFragment(ctx: FusionParser.RootFragmentContext?): Any? =
            with(ctx?.getChild(0)) {
                when (this) {
                    null -> super.visitRootFragment(ctx)
                    // root prototype declarations
                    is FusionParser.RootPrototypeDeclContext ->
                        accept(RootPrototypeDeclVisitor(indexer(), parentElementIdentifier))
                    // root path assignments
                    is FusionParser.RootFusionAssignContext ->
                        accept(FusionPathAssignVisitor(indexer(), parentElementIdentifier, currentPath))
                    // root path configurations
                    is FusionParser.RootFusionConfigurationContext ->
                        accept(FusionPathConfigurationVisitor(indexer(), parentElementIdentifier, currentPath))
                    // root path copy
                    is FusionParser.RootFusionConfigurationCopyContext ->
                        accept(FusionPathCopyVisitor(indexer(), parentElementIdentifier, currentPath))
                    // root path erasures
                    is FusionParser.RootFusionConfigurationErasureContext ->
                        accept(FusionPathErasureVisitor(indexer(), parentElementIdentifier, currentPath))
                    is FusionParser.RootPrototypeErasureContext ->
                        accept(FusionPathErasureVisitor(indexer(), parentElementIdentifier, currentPath))
                    // root code comments
                    is FusionParser.CodeCommentContext ->
                        accept(CodeCommentVisitor(indexer(), parentElementIdentifier))
                    // namespace alias
                    is FusionParser.NamespaceAliasContext ->
                        accept(NamespaceAliasVisitor(indexer(), parentElementIdentifier))
                    // file includes
                    is FusionParser.FileIncludeContext ->
                        accept(FileIncludeVisitor(indexer(), parentElementIdentifier))
                    else -> super.visitRootFragment(ctx)
                }
            }

        override fun visitFusionFragment(ctx: FusionParser.FusionFragmentContext?): Any? =
            with(ctx?.getChild(0)) {
                when (this) {
                    null -> super.visitFusionFragment(ctx)
                    // inner path assignments
                    is FusionParser.FusionAssignContext ->
                        accept(FusionPathAssignVisitor(indexer(), parentElementIdentifier, currentPath))
                    // inner path configurations
                    is FusionParser.FusionConfigurationContext ->
                        accept(FusionPathConfigurationVisitor(indexer(), parentElementIdentifier, currentPath))
                    // inner path copy
                    is FusionParser.FusionConfigurationCopyContext ->
                        accept(FusionPathCopyVisitor(indexer(), parentElementIdentifier, currentPath))
                    // inner path erasures
                    is FusionParser.FusionConfigurationErasureContext ->
                        accept(FusionPathErasureVisitor(indexer(), parentElementIdentifier, currentPath))
                    // inner code comments
                    is FusionParser.CodeCommentContext ->
                        accept(CodeCommentVisitor(indexer(), parentElementIdentifier))
                    else -> super.visitFusionFragment(ctx)
                }
            }
    }

    private inner class FusionPathCopyVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<FusionPathCopyDecl>() {

        override fun visitRootFusionConfigurationCopy(ctx: FusionParser.RootFusionConfigurationCopyContext?): FusionPathCopyDecl =
            pathCopy(
                ctx,
                FusionParser.RootFusionConfigurationCopyContext::rootFusionConfigurationPathReference,
                FusionParser.RootFusionConfigurationPathReferenceContext::rootFusionConfigurationPath,
                FusionParser.RootFusionConfigurationPathReferenceContext::ROOT_FUSION_PATH_NESTING_SEPARATOR,
                FusionParser.RootFusionConfigurationPathReferenceContext::toAstReference,
                FusionParser.RootFusionConfigurationCopyContext::rootFusionConfigurationBody,
                FusionParser.RootFusionConfigurationCopyContext::toAstReference
            ) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.RootFusionConfigurationCopyContext::rootFusionConfigurationPath,
                    FusionParser.RootFusionConfigurationCopyContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        override fun visitFusionConfigurationCopy(ctx: FusionParser.FusionConfigurationCopyContext?): FusionPathCopyDecl =
            pathCopy(
                ctx,
                FusionParser.FusionConfigurationCopyContext::fusionConfigurationPathReference,
                FusionParser.FusionConfigurationPathReferenceContext::fusionConfigurationPath,
                FusionParser.FusionConfigurationPathReferenceContext::FUSION_PATH_NESTING_SEPARATOR,
                FusionParser.FusionConfigurationPathReferenceContext::toAstReference,
                FusionParser.FusionConfigurationCopyContext::fusionConfigurationBody,
                FusionParser.FusionConfigurationCopyContext::toAstReference
            ) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.FusionConfigurationCopyContext::fusionConfigurationPath,
                    FusionParser.FusionConfigurationCopyContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        private fun <TContext : ParserRuleContext, TPathNameContext : ParserRuleContext> pathCopy(
            ctx: TContext?,
            pathToCopyReferenceMapper: (TContext) -> TPathNameContext,
            pathNameMapper: (TPathNameContext) -> ParserRuleContext,
            relativePathPrefixMapper: (TPathNameContext) -> TerminalNode?,
            pathAstReferenceMapper: (TPathNameContext, FusionLangElementIdentifier) -> AstReference,
            bodyMapper: (TContext) -> ParseTree?,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
            fusionPathNameDeclMapper: (TContext) -> FusionPathNameDecl
        ): FusionPathCopyDecl {
            val context = checkNotNull(ctx)
            val fusionPath = fusionPathNameDeclMapper(context)
            val absoluteFusionPath = currentPath + fusionPath.relativePathName
            val elementIdentifier =
                identifierForPathCopy(parentElementIdentifier, codeIndex, fusionPath.relativePathName)
            val bodyContext = bodyMapper(context)
            val body = if (bodyContext != null) {
                val elementBodyIdentifier = identifierForPathCopyBody(elementIdentifier)
                val bodyVisitor = InnerFusionDeclVisitor(elementBodyIdentifier, elementIdentifier, currentPath)
                context.accept(bodyVisitor)
            } else null
            val pathToCopyReferenceContext = pathToCopyReferenceMapper(context)
            return FusionPathCopyDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                path = fusionPath,
                absolutePath = absoluteFusionPath,
                pathToCopy = createFusionPathReference(
                    pathToCopyReferenceContext,
                    pathNameMapper,
                    relativePathPrefixMapper,
                    pathAstReferenceMapper,
                    elementIdentifier
                ),
                body = body,
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = astReferenceMapper(context, elementIdentifier)
            )
        }

        private fun <TContext : ParserRuleContext> createFusionPathReference(
            context: TContext,
            pathNameMapper: (TContext) -> ParserRuleContext,
            relativePathPrefixMapper: (TContext) -> TerminalNode?,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
            parentElementIdentifier: FusionLangElementIdentifier
        ): FusionPathNameReferenceDecl {
            val pathNameContext = pathNameMapper(context)
            val pathNameText = pathNameContext.text
            val relativePathPrefix = relativePathPrefixMapper(context)
            val absolute = relativePathPrefix == null
            val pathNameDeclIdentifier = identifierForPathNameReference(parentElementIdentifier, pathNameText, absolute)
            return FusionPathNameReferenceDecl(
                elementIdentifier = pathNameDeclIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                pathSegments = pathNameContext.children
                    .mapNotNull { it.accept(FusionPathNameSegmentVisitor(pathNameDeclIdentifier)) },
                absolute = absolute,
                sourceIdentifier = sourceIdentifier,
                astReference = astReferenceMapper(context, pathNameDeclIdentifier)
            )
        }

    }

    private fun namespaceNameDecl(
        parentElementIdentifier: FusionLangElementIdentifier,
        namespace: PrototypeNamespace,
        type: NamespaceDeclType,
        astReference: AstReference
    ) = PrototypeNamespaceDecl(
        elementIdentifier = identifierForNamespaceNameDecl(parentElementIdentifier, namespace, type),
        parentElementIdentifier = parentElementIdentifier,
        name = namespace,
        sourceIdentifier = sourceIdentifier,
        astReference = astReference
    )

    private inner class NamespaceAliasVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier
    ) : ErrorHandlingFusionParserBaseVisitor<NamespaceAliasDecl>() {

        override fun visitNamespaceAlias(ctx: FusionParser.NamespaceAliasContext?): NamespaceAliasDecl {
            val context = checkNotNull(ctx)
            val alias = PrototypeNamespace(context.NAMESPACE_ALIAS_NAMESPACE().text.trim())
            val targetNamespace = PrototypeNamespace(context.NAMESPACE_ALIAS_TARGET_NAMESPACE().text.trim())
            val elementIdentifier =
                identifierForNamespaceAlias(parentElementIdentifier, codeIndex, alias, targetNamespace)

            return NamespaceAliasDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                alias = namespaceNameDecl(
                    elementIdentifier,
                    alias,
                    NamespaceDeclType.ALIAS_SOURCE,
                    context.NAMESPACE_ALIAS_NAMESPACE().toAstReferenceNamespaceNameAliasSource(elementIdentifier)
                ),
                targetNamespace = namespaceNameDecl(
                    elementIdentifier,
                    targetNamespace,
                    NamespaceDeclType.ALIAS_TARGET,
                    context.NAMESPACE_ALIAS_TARGET_NAMESPACE().toAstReferenceNamespaceNameAliasTarget(elementIdentifier)
                ),
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }

    }

    private inner class FileIncludeVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier
    ) : ErrorHandlingFusionParserBaseVisitor<FusionFileIncludeDecl>() {

        override fun visitFileInclude(ctx: FusionParser.FileIncludeContext?): FusionFileIncludeDecl {
            val context = checkNotNull(ctx)
            val includePattern = FusionFileIncludePattern(
                // TODO what to do with quotes?
                context.fileIncludePattern().FILE_INCLUDE_FILE_PATTERN().text.trim().trim('"', '\'')
            )
            val elementIdentifier = identifierForFileInclude(parentElementIdentifier, codeIndex, includePattern)
            return FusionFileIncludeDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                includePattern = FusionFileIncludePatternDecl(
                    elementIdentifier = identifierForFileIncludePattern(elementIdentifier),
                    parentElementIdentifier = elementIdentifier,
                    pattern = includePattern,
                    sourceIdentifier = sourceIdentifier,
                    astReference = context.fileIncludePattern().FILE_INCLUDE_FILE_PATTERN()
                        .toAstReferenceFileIncludePattern(elementIdentifier)
                ),
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }

    }

    private inner class FusionPathErasureVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<FusionPathErasureDecl>() {

        override fun visitRootPrototypeErasure(ctx: FusionParser.RootPrototypeErasureContext?): FusionPathErasureDecl =
            pathErasure(ctx, FusionParser.RootPrototypeErasureContext::toAstReference) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    { it },
                    FusionParser.RootPrototypeErasureContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        override fun visitRootFusionConfigurationErasure(ctx: FusionParser.RootFusionConfigurationErasureContext?): FusionPathErasureDecl =
            pathErasure(ctx, FusionParser.RootFusionConfigurationErasureContext::toAstReference) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.RootFusionConfigurationErasureContext::rootFusionConfigurationPath,
                    FusionParser.RootFusionConfigurationErasureContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }


        override fun visitFusionConfigurationErasure(ctx: FusionParser.FusionConfigurationErasureContext?): FusionPathErasureDecl =
            pathErasure(ctx, FusionParser.FusionConfigurationErasureContext::toAstReference) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.FusionConfigurationErasureContext::fusionConfigurationPath,
                    FusionParser.FusionConfigurationErasureContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        private fun <TContext : ParserRuleContext> pathErasure(
            ctx: TContext?,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
            pathNameDeclarationNameMapper: (TContext) -> FusionPathNameDecl
        ): FusionPathErasureDecl {
            val context = checkNotNull(ctx)
            val fusionPath = pathNameDeclarationNameMapper(context)
            val identifierForPathErasure = identifierForPathErasure(
                parentElementIdentifier,
                codeIndex,
                fusionPath.relativePathName
            )
            return FusionPathErasureDecl(
                elementIdentifier = identifierForPathErasure,
                parentElementIdentifier = parentElementIdentifier,
                path = fusionPath,
                absolutePath = currentPath + fusionPath.relativePathName,
                sourceIdentifier = sourceIdentifier,
                codeIndex = codeIndex,
                astReference = astReferenceMapper(context, identifierForPathErasure)
            )

        }

    }

    private inner class CodeCommentVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier
    ) : ErrorHandlingFusionParserBaseVisitor<CodeCommentDecl>() {

        override fun visitCodeComment(ctx: FusionParser.CodeCommentContext?): CodeCommentDecl {
            val context = checkNotNull(ctx)
            val terminalNode = context.ROOT_CODE_COMMENT() ?: context.CODE_COMMENT()
            val commentString = Optional.ofNullable(terminalNode)
                .map { it.text }
                .map(CodeCommentDecl::sanitizeComment)
                .orElseThrow { IllegalArgumentException("No code comment found on context: $context") }

            val identifierForCodeComment = identifierForCodeComment(parentElementIdentifier, codeIndex)
            return CodeCommentDecl(
                elementIdentifier = identifierForCodeComment,
                parentElementIdentifier = parentElementIdentifier,
                comment = commentString,
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(identifierForCodeComment)
            )
        }

    }

    private fun qualifiedPrototypeNameDecl(
        parentElementIdentifier: FusionLangElementIdentifier,
        qualifiedNameTerminalNode: TerminalNode
    ): QualifiedPrototypeNameDecl {
        val qualifiedNameText = qualifiedNameTerminalNode.text.trim()
        val qualifiedName = QualifiedPrototypeName.fromString(qualifiedNameText)
        val qualifiedNameIdentifier = identifierForQualifiedPrototypeName(parentElementIdentifier, qualifiedNameText)
        val identifierForSimplePrototypeName = identifierForSimplePrototypeName(
            qualifiedNameIdentifier,
            qualifiedName.simpleName.name
        )
        return QualifiedPrototypeNameDecl(
            elementIdentifier = qualifiedNameIdentifier,
            parentElementIdentifier = parentElementIdentifier,
            simpleName = SimplePrototypeNameDecl(
                elementIdentifier = identifierForSimplePrototypeName,
                parentElementIdentifier = qualifiedNameIdentifier,
                name = qualifiedName.simpleName,
                sourceIdentifier = sourceIdentifier,
                astReference = qualifiedNameTerminalNode.toAstReferenceSimplePrototypeName(
                    identifierForSimplePrototypeName
                )
            ),
            namespace = if (qualifiedName.namespace.isEmpty()) null else namespaceNameDecl(
                parentElementIdentifier = qualifiedNameIdentifier,
                namespace = qualifiedName.namespace,
                type = NamespaceDeclType.PROTOTYPE_CALL,
                astReference = qualifiedNameTerminalNode.toAstReferencePrototypeNamespace(qualifiedNameIdentifier)
            ),
            sourceIdentifier = sourceIdentifier,
            astReference = qualifiedNameTerminalNode.toAstReferenceQualifiedPrototypeName(qualifiedNameIdentifier)
        )
    }

    private inner class RootPrototypeDeclVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier
    ) : ErrorHandlingFusionParserBaseVisitor<PrototypeDecl>() {

        override fun visitRootPrototypeDecl(ctx: FusionParser.RootPrototypeDeclContext?): PrototypeDecl {
            val context = checkNotNull(ctx)
            val prototypeNameDecl =
                qualifiedPrototypeNameDecl(parentElementIdentifier, context.rootPrototypeCall().PROTOTYPE_NAME())

            val elementIdentifier = identifierForRootPrototypeDeclaration(
                parentElementIdentifier,
                codeIndex,
                prototypeNameDecl.qualifiedName
            )
            val elementBodyIdentifier = identifierForRootPrototypeDeclarationBody(elementIdentifier)

            val inheritanceContext = context.prototypeInheritance()
            val inheritPrototypeName = if (inheritanceContext != null)
                qualifiedPrototypeNameDecl(
                    parentElementIdentifier,
                    inheritanceContext.rootPrototypeCall().PROTOTYPE_NAME()
                )
            else null
            val prototypeBody = context.prototypeBody()
                ?.accept(
                    InnerFusionDeclVisitor(
                        elementBodyIdentifier,
                        elementIdentifier,
                        FusionPathName.rootPrototype(prototypeNameDecl.qualifiedName)
                    )
                )

            return PrototypeDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                name = prototypeNameDecl,
                inheritPrototype = inheritPrototypeName,
                bodyDeclaration = prototypeBody,
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }
    }

    private inner class FusionPathConfigurationVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<FusionPathConfigurationDecl>() {

        override fun visitFusionConfiguration(ctx: FusionParser.FusionConfigurationContext?): FusionPathConfigurationDecl =
            pathConfiguration(ctx, FusionParser.FusionConfigurationContext::toAstReference) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.FusionConfigurationContext::fusionConfigurationPath,
                    FusionParser.FusionConfigurationContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        override fun visitRootFusionConfiguration(ctx: FusionParser.RootFusionConfigurationContext?): FusionPathConfigurationDecl =
            pathConfiguration(ctx, FusionParser.RootFusionConfigurationContext::toAstReference) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.RootFusionConfigurationContext::rootFusionConfigurationPath,
                    FusionParser.RootFusionConfigurationContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        private fun <TContext : ParserRuleContext> pathConfiguration(
            ctx: TContext?,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
            fusionPathNameDeclMapper: (TContext) -> FusionPathNameDecl
        ): FusionPathConfigurationDecl {
            val context = checkNotNull(ctx)
            val fusionPath = fusionPathNameDeclMapper(context)

            val elementIdentifier =
                identifierForPathConfiguration(parentElementIdentifier, codeIndex, fusionPath.relativePathName)
            val elementBodyIdentifier = identifierForPathConfigurationBody(elementIdentifier)

            val absoluteFusionPath = currentPath + fusionPath.relativePathName
            return FusionPathConfigurationDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                path = fusionPath,
                absolutePath = absoluteFusionPath,
                body = context.accept(
                    InnerFusionDeclVisitor(
                        elementBodyIdentifier,
                        elementIdentifier,
                        absoluteFusionPath
                    )
                ),
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = astReferenceMapper(context, elementIdentifier)
            )
        }

    }

    private inner class FusionPathAssignVisitor(
        private val codeIndex: Int,
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<FusionPathAssignmentDecl>() {

        override fun visitFusionAssign(ctx: FusionParser.FusionAssignContext?): FusionPathAssignmentDecl {
            return pathAssignment(
                ctx,
                FusionParser.FusionAssignContext::fusionValueDecl,
                FusionParser.FusionValueDeclContext::fusionValueLiteralDecl,
                FusionParser.FusionValueDeclContext::fusionValueBody,
                FusionParser.FusionAssignContext::toAstReference
            ) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.FusionAssignContext::fusionAssignPath,
                    FusionParser.FusionAssignContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }
        }

        override fun visitRootFusionAssign(ctx: FusionParser.RootFusionAssignContext?): FusionPathAssignmentDecl =
            pathAssignment(
                ctx,
                FusionParser.RootFusionAssignContext::rootFusionValueDecl,
                FusionParser.RootFusionValueDeclContext::fusionValueLiteralDecl,
                FusionParser.RootFusionValueDeclContext::rootFusionValueBody,
                FusionParser.RootFusionAssignContext::toAstReference
            ) { context ->
                createFusionPath(
                    currentPath,
                    context,
                    FusionParser.RootFusionAssignContext::rootFusionAssignPath,
                    FusionParser.RootFusionAssignContext::toAstReferenceFusionPath,
                    parentElementIdentifier
                )
            }

        private fun <TContext : ParserRuleContext, TValueContext : ParserRuleContext> pathAssignment(
            ctx: TContext?,
            valueMapper: (TContext) -> TValueContext,
            valueLiteralMapper: (TValueContext) -> ParseTree,
            valueBodyMapper: (TValueContext) -> ParseTree?,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
            fusionPathNameDeclMapper: (TContext) -> FusionPathNameDecl
        ): FusionPathAssignmentDecl {
            val context = checkNotNull(ctx)
            val valueContext = valueMapper(context)
            val valueLiteralContext = valueLiteralMapper(valueContext)
            val bodyContext = valueBodyMapper(valueContext)
            val fusionPath = fusionPathNameDeclMapper(context)
            val absoluteFusionPath = currentPath + fusionPath.relativePathName
            val elementIdentifier =
                identifierForPathAssignment(parentElementIdentifier, codeIndex, fusionPath.relativePathName)

            val body = if (bodyContext != null) {
                val elementBodyIdentifier = identifierForValueBody(elementIdentifier)
                val bodyVisitor = InnerFusionDeclVisitor(elementBodyIdentifier, elementIdentifier, absoluteFusionPath)
                bodyContext.accept(bodyVisitor)
            } else null

            val valueDeclaration =
                valueLiteralContext.accept(FusionValueDeclVisitor(elementIdentifier, body, absoluteFusionPath))
            return FusionPathAssignmentDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                path = fusionPath,
                absolutePath = absoluteFusionPath,
                valueDeclaration = valueDeclaration,
                codeIndex = codeIndex,
                sourceIdentifier = sourceIdentifier,
                astReference = astReferenceMapper(context, elementIdentifier)
            )
        }
    }

    private inner class FusionValueDeclVisitor(
        private val parentElementIdentifier: FusionLangElementIdentifier,
        private val body: InnerFusionDecl?,
        private val currentPath: AbsoluteFusionPathName
    ) : ErrorHandlingFusionParserBaseVisitor<FusionValueDecl>() {

        override fun visitFusionValueNull(ctx: FusionParser.FusionValueNullContext?): FusionValueDecl {
            val context = checkNotNull(ctx)
            val elementIdentifier = identifierForValueNull(parentElementIdentifier)
            val astReference = context.toAstReference(elementIdentifier)
            return NullValueDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                fusionValue = NullValue(astReference),
                body = body,
                sourceIdentifier = sourceIdentifier,
                astReference = astReference
            )
        }

        override fun visitFusionValueBoolean(ctx: FusionParser.FusionValueBooleanContext?): FusionValueDecl {
            val context = checkNotNull(ctx)
            val elementIdentifier = identifierForValueBoolean(parentElementIdentifier)
            val astReference = context.toAstReference(elementIdentifier)
            return BooleanValueDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                fusionValue = BooleanValue(
                    context.FUSION_VALUE_BOOLEAN().text.trim().toBoolean(),
                    astReference
                ),
                body = body,
                sourceIdentifier = sourceIdentifier,
                astReference = astReference
            )
        }

        override fun visitFusionValueNumber(ctx: FusionParser.FusionValueNumberContext?): FusionValueDecl {
            val context = checkNotNull(ctx)
            val numberValueAsString = context.FUSION_VALUE_NUMBER().text
            return if (numberValueAsString.contains('.')) {
                val elementIdentifier = identifierForValueDouble(parentElementIdentifier)
                val astReference = context.toAstReference(elementIdentifier)
                DoubleValueDecl(
                    elementIdentifier = elementIdentifier,
                    parentElementIdentifier = parentElementIdentifier,
                    fusionValue = DoubleValue(
                        numberValueAsString.trim().toDouble(),
                        astReference
                    ),
                    body = body,
                    sourceIdentifier = sourceIdentifier,
                    astReference = astReference
                )
            } else {
                val elementIdentifier = identifierForValueInteger(parentElementIdentifier)
                val astReference = context.toAstReference(elementIdentifier)
                IntegerValueDecl(
                    elementIdentifier = elementIdentifier,
                    parentElementIdentifier = parentElementIdentifier,
                    fusionValue = IntegerValue(
                        numberValueAsString.trim().toInt(),
                        astReference
                    ),
                    body = body,
                    sourceIdentifier = sourceIdentifier,
                    astReference = astReference
                )
            }
        }

        override fun visitFusionValueStringSingleQuote(ctx: FusionParser.FusionValueStringSingleQuoteContext?): FusionValueDecl =
            unquoteString(
                ctx,
                '\'',
                FusionParser.FusionValueStringSingleQuoteContext::toAstReference,
                FusionParser.FusionValueStringSingleQuoteContext::FUSION_VALUE_STRING_SQUOTE
            )

        override fun visitFusionValueStringDoubleQuote(ctx: FusionParser.FusionValueStringDoubleQuoteContext?): FusionValueDecl =
            unquoteString(
                ctx,
                '"',
                FusionParser.FusionValueStringDoubleQuoteContext::toAstReference,
                FusionParser.FusionValueStringDoubleQuoteContext::FUSION_VALUE_STRING_DQUOTE
            )

        override fun visitFusionValueExpression(ctx: FusionParser.FusionValueExpressionContext?): FusionValueDecl {
            val context = checkNotNull(ctx)
            val rawExpressionValue = context.text.trim()
            val expressionValue = rawExpressionValue
                .replace("^\\$\\{".toRegex(), "")
                .replace("}$".toRegex(), "")

            val elementIdentifier = identifierForValueExpression(parentElementIdentifier)
            val astReference = context.toAstReference(elementIdentifier)
            return ExpressionValueDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                fusionValue = ExpressionValue(expressionValue, astReference),
                body = body,
                sourceIdentifier = sourceIdentifier,
                astReference = astReference
            )
        }

        override fun visitFusionValueDslDelegate(ctx: FusionParser.FusionValueDslDelegateContext?): FusionValueDecl {
            val context = checkNotNull(ctx)
            val fullDslDelegateValueRaw = context.FUSION_VALUE_DSL_DELEGATE().text

            val openCodeIndex = fullDslDelegateValueRaw.indexOfFirst { it == '`' }
            val dslName = DslName(fullDslDelegateValueRaw.substring(0 until openCodeIndex))
            val code = fullDslDelegateValueRaw.substring(openCodeIndex).trim(' ', '\t', '\n', '\r', '`')

            val elementIdentifier = identifierForValueDslDelegate(parentElementIdentifier, dslName)
            val astReference = context.toAstReference(elementIdentifier)
            val dslValueDecl = DslDelegateValueDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                fusionValue = DslDelegateValue(
                    dslName = dslName,
                    code = code,
                    astReference = astReference
                ),
                body = body,
                sourceIdentifier = sourceIdentifier,
                astReference = astReference
            )

            // DSL parsing
            val dslParser = dslParsers[dslName]
            return if (dslParser != null) {
                dslParser.parse(dslValueDecl, currentPath)
            } else {
                log.warn("Unrecognized DSL '$dslName'; no DSL parser registered")
                dslValueDecl
            }
        }

        override fun visitFusionValueObject(ctx: FusionParser.FusionValueObjectContext?): FusionValueDecl {
            val context = checkNotNull(ctx)
            val prototypeNameContext = context.getChild(0) as TerminalNode
            val elementIdentifier =
                identifierForValueFusionObject(parentElementIdentifier, prototypeNameContext.text.trim())
            val prototypeNameDecl = qualifiedPrototypeNameDecl(elementIdentifier, prototypeNameContext)
            val astReference = context.toAstReference(elementIdentifier)
            return FusionObjectValueDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                prototypeName = prototypeNameDecl,
                fusionValue = FusionObjectValue(prototypeNameDecl.qualifiedName, astReference),
                body = body,
                sourceIdentifier = sourceIdentifier,
                astReference = astReference
            )
        }

        private fun <TContext : ParserRuleContext> unquoteString(
            ctx: TContext?,
            quoteChar: Char,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
            valueMapper: (TContext) -> TerminalNode
        ): StringValueDecl {
            val context = checkNotNull(ctx)
            val terminalNode = valueMapper(context)
            val rawStringValue = terminalNode.text
            val quoteCharRegexEscaped = Pattern.quote(quoteChar.toString())
            val stringValue = rawStringValue
                // remove trailing / leading quotes
                .replace("^$quoteCharRegexEscaped".toRegex(), "")
                .replace("$quoteCharRegexEscaped$".toRegex(), "")
                // remove escape char from quote
                .replace("\\\\$quoteCharRegexEscaped".toRegex(), quoteChar.toString())
                // remove escape char from escape char
                .replace("\\\\\\\\".toRegex(), "\\\\")
            val elementIdentifier = identifierForValueString(parentElementIdentifier)
            val astReference = astReferenceMapper(context, elementIdentifier)
            return StringValueDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = parentElementIdentifier,
                fusionValue = StringValue.fromRawString(stringValue, astReference),
                body = body,
                sourceIdentifier = sourceIdentifier,
                astReference = astReference
            )
        }

    }

    private fun <TContext : ParserRuleContext> createFusionPath(
        parentPathName: AbsoluteFusionPathName,
        context: TContext,
        pathNameMapper: (TContext) -> ParserRuleContext,
        astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
        parentElementIdentifier: FusionLangElementIdentifier
    ): FusionPathNameDecl {
        val pathNameContext = pathNameMapper(context)
        val pathNameText = pathNameContext.text
        val pathNameDeclIdentifier = identifierForPathName(parentElementIdentifier, pathNameText)
        return FusionPathNameDecl(
            elementIdentifier = pathNameDeclIdentifier,
            parentElementIdentifier = parentElementIdentifier,
            pathSegments = pathNameContext.children
                .mapNotNull { it.accept(FusionPathNameSegmentVisitor(pathNameDeclIdentifier)) },
            parentPathName = parentPathName,
            sourceIdentifier = sourceIdentifier,
            astReference = astReferenceMapper(context, pathNameDeclIdentifier)
        )
    }

    private inner class FusionPathNameSegmentVisitor(
        private val pathNameDeclParentIdentifier: FusionLangElementIdentifier
    ) : ErrorHandlingFusionParserBaseVisitor<FusionPathNameSegmentDecl<out FusionPathNameSegment>>() {


        override fun visitPrototypeCall(ctx: FusionParser.PrototypeCallContext?): FusionPathNameSegmentDecl<out FusionPathNameSegment> =
            prototypeCall(
                ctx,
                FusionParser.PrototypeCallContext::PROTOTYPE_NAME,
                FusionParser.PrototypeCallContext::toAstReferencePathNameSegment
            )

        override fun visitRootPrototypeCall(ctx: FusionParser.RootPrototypeCallContext?): FusionPathNameSegmentDecl<out FusionPathNameSegment> =
            prototypeCall(
                ctx,
                FusionParser.RootPrototypeCallContext::PROTOTYPE_NAME,
                FusionParser.RootPrototypeCallContext::toAstReferencePathNameSegment
            )

        private fun <TContext : ParserRuleContext> prototypeCall(
            ctx: TContext?,
            prototypeNameMapper: (TContext) -> TerminalNode,
            astReferenceMapper: (TContext, FusionLangElementIdentifier) -> AstReference,
        ): PathNameSegmentPrototypeCallDecl {
            val context = checkNotNull(ctx)
            val prototypeNameContext = prototypeNameMapper(context)
            val elementIdentifier = identifierForPathNameSegmentPrototypeCall(
                pathNameDeclParentIdentifier,
                prototypeNameContext.text.trim()
            )
            val prototypeNameDecl = qualifiedPrototypeNameDecl(elementIdentifier, prototypeNameContext)
            return PathNameSegmentPrototypeCallDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = pathNameDeclParentIdentifier,
                segment = PrototypeCallPathSegment.create(
                    prototypeNameDecl.qualifiedName
                ),
                qualifiedPrototypeNameDecl = prototypeNameDecl,
                sourceIdentifier = sourceIdentifier,
                astReference = astReferenceMapper(context, elementIdentifier)
            )
        }

        override fun visitFusionMetaPropPathSegment(ctx: FusionParser.FusionMetaPropPathSegmentContext?): FusionPathNameSegmentDecl<out FusionPathNameSegment> {
            val context = checkNotNull(ctx)
            val pathSegmentName = context.FUSION_PATH_SEGMENT().text
            val elementIdentifier = identifierForPathNameSegmentMetaProperty(
                pathNameDeclParentIdentifier,
                pathSegmentName
            )
            return PathNameSegmentMetaPropertyDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = pathNameDeclParentIdentifier,
                segment = MetaPropertyPathSegment.create(pathSegmentName),
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }

        override fun visitRootFusionMetaPropPathSegment(ctx: FusionParser.RootFusionMetaPropPathSegmentContext?): FusionPathNameSegmentDecl<out FusionPathNameSegment> {
            val context = checkNotNull(ctx)
            val pathSegmentName = context.ROOT_FUSION_PATH_SEGMENT().text
            val elementIdentifier = identifierForPathNameSegmentMetaProperty(
                pathNameDeclParentIdentifier,
                pathSegmentName
            )
            return PathNameSegmentMetaPropertyDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = pathNameDeclParentIdentifier,
                segment = MetaPropertyPathSegment.create(pathSegmentName),
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }

        override fun visitFusionPathSegment(ctx: FusionParser.FusionPathSegmentContext?): FusionPathNameSegmentDecl<out FusionPathNameSegment> {
            val context = checkNotNull(ctx)
            val pathSegmentName = pathSegmentThatMayBeQuoted(context.FUSION_PATH_SEGMENT())
            val elementIdentifier = identifierForPathNameSegment(pathNameDeclParentIdentifier, pathSegmentName)
            return PathNameSegmentPropertyDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = pathNameDeclParentIdentifier,
                segment = pathSegmentName,
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }

        override fun visitRootFusionPathSegment(ctx: FusionParser.RootFusionPathSegmentContext?): FusionPathNameSegmentDecl<out FusionPathNameSegment> {
            val context = checkNotNull(ctx)
            val pathSegmentName = pathSegmentThatMayBeQuoted(context.ROOT_FUSION_PATH_SEGMENT())
            val elementIdentifier = identifierForPathNameSegment(pathNameDeclParentIdentifier, pathSegmentName)
            return PathNameSegmentPropertyDecl(
                elementIdentifier = elementIdentifier,
                parentElementIdentifier = pathNameDeclParentIdentifier,
                segment = pathSegmentName,
                sourceIdentifier = sourceIdentifier,
                astReference = context.toAstReference(elementIdentifier)
            )
        }

    }
}

private fun pathSegmentThatMayBeQuoted(
    context: TerminalNode
): PropertyPathSegment {
    val rawSegmentString = context.text
    val quoting: PathNameSegmentQuoting = when {
        rawSegmentString.startsWith('\'') && rawSegmentString.endsWith('\'') ->
            PathNameSegmentQuoting.SINGLE_QUOTED
        rawSegmentString.startsWith('"') && rawSegmentString.endsWith('"') ->
            PathNameSegmentQuoting.DOUBLE_QUOTED
        else -> PathNameSegmentQuoting.NO_QUOTES
    }
    return when (quoting) {
        PathNameSegmentQuoting.NO_QUOTES -> PropertyPathSegment.create(rawSegmentString, quoting)
        PathNameSegmentQuoting.SINGLE_QUOTED,
        PathNameSegmentQuoting.DOUBLE_QUOTED ->
            PropertyPathSegment.create(rawSegmentString.substring(1 until rawSegmentString.length - 1), quoting)
    }
}
