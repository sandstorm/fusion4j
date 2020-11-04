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

package io.neos.fusion4j.runtime.eel

import io.neos.fusion4j.lang.model.EelHelperContextName
import io.neos.fusion4j.lang.model.PathNameSegmentQuoting
import io.neos.fusion4j.lang.model.PropertyPathSegment
import io.neos.fusion4j.lang.model.values.ExpressionValue
import io.neos.fusion4j.lang.semantic.PathIndex
import io.neos.fusion4j.lang.util.FusionProfiler
import io.neos.fusion4j.runtime.FusionContext
import io.neos.fusion4j.runtime.FusionRuntime
import io.neos.fusion4j.runtime.FusionRuntimeException
import mu.KLogger
import mu.KotlinLogging
import org.apache.commons.jexl3.*

private val log: KLogger = KotlinLogging.logger {}

class JexlEelEvaluator(
    pathIndex: PathIndex,
    private val eelHelperFactory: EelHelperFactory,
    strictMode: Boolean = true,
    private val profiler: FusionProfiler
) : EelEvaluator {

    private val jexl = JexlBuilder()
        .cache(4096)
        .strict(strictMode)
        .silent(false)
        .arithmetic(LazyJexlArithmetic(true))
        .uberspect(LazyUberspect(strictMode))
        .create()

    private val expressionCache: Map<String, Lazy<JexlExpression>> =
        pathIndex.getAllExpressionValues()
            .fold(emptyMap()) { cache, expressionValue ->
                val expressionString = expressionValue.eelExpression
                if (cache.containsKey(expressionString)) {
                    // cache only once
                    cache
                } else {
                    log.debug("Pre-caching JEXL expression '$expressionValue', AST ref: ${expressionValue.astReference}")
                    // this is thread-safe and SYNCHRONIZED by default!
                    val lazyCacheEntry = lazy {
                        log.debug("Lazy initializing JEXL expression '${expressionValue}'")
                        jexl.createExpression(expressionString)
                    }
                    cache + (expressionString to lazyCacheEntry)
                }
            }

    companion object {
        private fun createProblemDescription(
            jexlError: JexlException,
            offendingExpression: String
        ): EelErrorMessage.ProblemDescription {
            val errorChain = collectAllCauses(jexlError)
            return EelErrorMessage.ProblemDescription(
                errorChain
                    .map { error ->
                        when (error) {
                            is JexlException -> {
                                val jexlMsg = error.message
                                jexlMsg
                                    ?.split(" ")
                                    ?.filterNot {
                                        it.contains("io.neos.fusion4j.runtime.eel.JexlEelEvaluator")
                                                || it.contains("org.apache.commons.jexl3")
                                    }
                                    ?.joinToString(" ")
                                    ?: error.toString()
                            }
                            is JexlArithmetic.NullOperand ->
                                "EEL operation '${extractOperatorFromJexlError(jexlError)}' " +
                                        "with null operand '$offendingExpression' is not allowed in strict mode"
                            is ArithmeticException ->
                                "TODO unhandled: " + (error.message ?: error.toString())
                            else -> error.message ?: error.toString()
                        }
                    }
            )
        }

        private fun collectAllCauses(error: Throwable, collectedSoFar: List<Throwable> = emptyList()): List<Throwable> {
            val cause = error.cause
            return listOf(error) +
                    if (cause != null && !collectedSoFar.contains(cause)) {
                        collectAllCauses(cause, collectedSoFar + error)
                    } else emptyList()
        }

        private fun extractOperatorFromJexlError(jexlError: JexlException): String {
            // hacky, but may work
            val msg = jexlError.message!!
            val prefix = "JEXL error : "
            val idx = msg.indexOf(prefix)
            return if (idx < 0) {
                TODO("unhandled JEXL error: $jexlError")
            } else {
                val restMsg = msg.substring(idx + prefix.length)
                val indexOfFirstSymbolChar = restMsg.indexOfFirst { ExpressionValue.SYMBOL_CHARS.contains(it) } - 1
                if (indexOfFirstSymbolChar < 1) {
                    TODO("unhandled JEXL operator error: $jexlError")
                } else {
                    restMsg.substring(0, indexOfFirstSymbolChar)
                }
            }
        }

    }

    override fun evaluateEelExpression(
        runtimeAccess: EelThisPointerRuntimeAccess,
        expression: ExpressionValue,
        context: FusionContext
    ): Any? {
        val expressionString = expression.eelExpression
        val lazyCachedJexlExpression = expressionCache[expressionString]
            ?: throw FusionRuntimeException(
                runtimeAccess.callstack,
                "Could not evaluate EEL expression \${$expressionString}; no JEXL expression cache entry found",
                runtimeAccess.callstack.currentStackItem?.associatedFusionLangElement
            )

        val jexlExpression = try {
            lazyCachedJexlExpression.value
        } catch (jexlParseError: JexlException) {
            val offendingExpression = expression.getOffendingExpressionAround(jexlParseError.info.column)
            throw ExpressionEvaluationException(
                jexlParseError,
                runtimeAccess.callstack,
                EelErrorMessage(
                    expressionValue = expression,
                    offendingExpression = offendingExpression,
                    offendingLine = expression.astReference.startPosition.line + jexlParseError.info.line,
                    offendingCharPositionInLine = expression.astReference.startPosition.charPositionInLine + jexlParseError.info.column,
                    // TODO hide JEXL internals as far as possible
                    problemDescription = EelErrorMessage.ProblemDescription(
                        listOf(
                            "Could not parse expression; cause: ${jexlParseError.message}"
                        )
                    )
                )
            )
        }

        // create context (FusionContext + EEL Helpers + this-pointer)
        val ctx: JexlContext = JexlEelContext(context, eelHelperFactory, runtimeAccess)
        val value = try {
            val result = profiler.profile(
                FusionProfiler.PROFILER_EVALUATE_EEL_EXPRESSION,
                "evaluation of expression $expressionString",
            ) {
                // TODO validate if we want to unwrap here / cleanup a bit
                FusionRuntime.unwrapLazy(jexlExpression.evaluate(ctx))
            }
            result
        } catch (jexlError: JexlException) {
            val offendingExpression = expression.getOffendingSymbol(jexlError.info.column)
            throw ExpressionEvaluationException(
                jexlError,
                runtimeAccess.callstack,
                EelErrorMessage(
                    expressionValue = expression,
                    offendingExpression = offendingExpression,
                    offendingLine = expression.astReference.startPosition.line + jexlError.info.line,
                    offendingCharPositionInLine = expression.astReference.startPosition.charPositionInLine + jexlError.info.column,
                    problemDescription = createProblemDescription(jexlError, offendingExpression)
                )
            )
        }
        return if (value is Lazy<*>) {
            value.value
        } else {
            value
        }
    }
}

class JexlEelContext(
    fusionContext: FusionContext,
    private val eelHelperFactory: EelHelperFactory,
    private val runtimeAccess: EelThisPointerRuntimeAccess
) : JexlContext {

    private val delegate: MapContext by lazy {
        MapContext(
            fusionContext.currentContextMap +
                    // this pointer
                    (EelEvaluator.THIS_POINTER_CONTEXT_VARIABLE_NAME to JexlThisPointerContext(runtimeAccess))
        )
    }

    override fun get(name: String): Any? {
        val varResult = if (name.matches(EelHelperContextName.VALID_PATTERN)) {
            // TODO how to hold / cache EEL helper instance?
            val eelHelperContextName = EelHelperContextName(name)
            if (eelHelperFactory.exists(eelHelperContextName)) {
                val eelHelperInstance = eelHelperFactory.createEelHelperInstance(eelHelperContextName)
                eelHelperInstance
            } else {
                // non-strict mode
                throw FusionRuntimeException(
                    runtimeAccess.callstack,
                    "EEL Helper not found for context variable name $eelHelperContextName",
                    runtimeAccess.callstack.currentStackItem?.associatedFusionLangElement
                )
            }
        } else {
            evaluateValue(name)
        }

        return varResult
    }

    private fun evaluateValue(name: String): Any? {
        return if (delegate.has(name)) {
            val value = delegate.get(name)
            /*if (value is Lazy<*>) {
                val lazyResolvedValue = value.value
                if (lazyResolvedValue is Lazy<*>) {
                    // we intentionally do NOT unpack nested Lazy values
                    // in case you want to render a Lazy by yourself
                    log.warn("nested lazy attribute: $name")
                }
                lazyResolvedValue
            } else {
            *
             */
            value
        } else {
            // non-strict mode
            log.debug("EEL Value '$name' evaluated to null")
            null
        }
    }

    override fun has(name: String): Boolean =
        if (name.matches(EelHelperContextName.VALID_PATTERN)) {
            eelHelperFactory.exists(EelHelperContextName(name))
        } else {
            delegate.has(name)
        }

    override fun set(name: String?, value: Any?) =
        throw UnsupportedOperationException("The Fusion context is immutable; cannot set $name = $value")
}

class JexlThisPointerContext(
    private val runtimeAccess: EelThisPointerRuntimeAccess
) : JexlContext {

    companion object {
        private fun contextVariableNameToPathSegment(name: String): PropertyPathSegment =
            PropertyPathSegment.create(name, PathNameSegmentQuoting.NO_QUOTES)
    }

    override fun get(name: String): Any? {
        // validate incorrect usages of the this-pointer
        //runtimeAccess.callstack.

        val varNamePathSegment = contextVariableNameToPathSegment(name)
        val varResult = if (runtimeAccess.hasThisPointerAttribute(varNamePathSegment)) {
            runtimeAccess.evaluateThisPointer(varNamePathSegment)
        } else {
            null
        }

        return varResult
    }


    override fun has(name: String): Boolean =
        runtimeAccess.hasThisPointerAttribute(contextVariableNameToPathSegment(name))

    override fun set(name: String, value: Any?) =
        throw UnsupportedOperationException("The Fusion this pointer context is immutable; cannot set $name = $value")

}