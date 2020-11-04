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

import io.neos.fusion4j.runtime.FusionRuntime
import org.apache.commons.jexl3.JexlArithmetic
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.internal.introspection.Uberspect
import org.apache.commons.jexl3.introspection.JexlMethod
import org.apache.commons.jexl3.introspection.JexlPropertyGet
import org.apache.commons.jexl3.introspection.JexlPropertySet
import org.apache.commons.jexl3.introspection.JexlUberspect
import org.apache.commons.logging.LogFactory
import java.math.BigDecimal
import java.math.BigInteger

class LazyUberspect(
    private val strict: Boolean
) : Uberspect(LogFactory.getLog(JexlEngine::class.java), JEXL_STRATEGY) {

    override fun getMethod(obj: Any, method: String, vararg args: Any?): JexlMethod? {
        val eagerObj = if (obj is Lazy<*>) obj.value else obj
        val jexlMethod = super.getMethod(eagerObj, method, *args)
        return if (jexlMethod != null) {
            LazyJexlMethod(jexlMethod)
        } else {
            if (strict) {
                throw IllegalStateException("Could not get EEL method; ${obj::class.java.name}#$method is not accessible")
            } else {
                return null
            }
        }
    }

    override fun getPropertyGet(obj: Any, identifier: Any): JexlPropertyGet? {
        return if (obj is Lazy<*>) {
            LazyJexlPropertyGet {
                super.getPropertyGet(it, identifier)
            }
        } else {
            super.getPropertyGet(obj, identifier)
        }
    }

    override fun getPropertyGet(
        resolvers: MutableList<JexlUberspect.PropertyResolver>?,
        obj: Any,
        identifier: Any
    ): JexlPropertyGet? {
        return if (obj is Lazy<*>) {
            LazyJexlPropertyGet {
                val propertyGet = super.getPropertyGet(resolvers, it, identifier)
                propertyGet
            }
        } else {
            super.getPropertyGet(resolvers, obj, identifier)
        }
    }

    override fun getPropertySet(obj: Any?, identifier: Any?, arg: Any?): JexlPropertySet =
        throw UnsupportedOperationException("EEL is immutable")

    override fun getPropertySet(
        resolvers: MutableList<JexlUberspect.PropertyResolver>?,
        obj: Any?,
        identifier: Any?,
        arg: Any?
    ): JexlPropertySet =
        throw UnsupportedOperationException("EEL is immutable")

}

// property accessors
class LazyJexlPropertyGet(
    private val jexlPropertyGetProvider: (Any) -> JexlPropertyGet?
) : JexlPropertyGet {
    override fun invoke(obj: Any): Any {
        val lazyObj = obj as Lazy<*>
        return createLazy {
            val value = FusionRuntime.unwrapLazy(lazyObj)
                ?: throw IllegalStateException("JEXL value must not be null")
            val realGet = jexlPropertyGetProvider(value)
            realGet?.invoke(value)
        }
    }

    override fun tryInvoke(obj: Any?, key: Any?): Any {
        val lazyObj = obj as Lazy<*>
        return createLazy {
            val value = FusionRuntime.unwrapLazy(lazyObj.value)
                ?: throw IllegalStateException("JEXL value must not be null")
            val realGet = jexlPropertyGetProvider(value)
            realGet?.tryInvoke(value, key)
        }
    }

    override fun tryFailed(rval: Any?): Boolean {
        return rval == JexlEngine.TRY_FAILED
    }

    override fun isCacheable(): Boolean =
        false
}

// function calls
class LazyJexlMethod(
    private val eagerMethod: JexlMethod
) : JexlMethod {
    override fun invoke(obj: Any, vararg params: Any?): Any {
        val doLazyEvaluation = obj is Lazy<*> || params.any { it is Lazy<*> }
        return if (doLazyEvaluation) {
            createLazy {
                val eagerParams = params.map { if (it is Lazy<*>) it.value else it }
                eagerMethod.invoke(if (obj is Lazy<*>) obj.value else obj, *eagerParams.toTypedArray())
            }
        } else {
            eagerMethod.invoke(obj, *params)
        }
    }

    override fun tryInvoke(name: String, obj: Any?, vararg params: Any?): Any =
        eagerMethod.tryInvoke(name, obj, *params)

    override fun tryFailed(rval: Any): Boolean =
        eagerMethod.tryFailed(rval)

    override fun isCacheable(): Boolean = eagerMethod.isCacheable

    override fun getReturnType(): Class<*> = eagerMethod.returnType

}

class LazyJexlArithmetic(strict: Boolean) : JexlArithmetic(strict) {

    override fun add(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.add(l, r) }
    override fun subtract(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.subtract(l, r) }
    override fun multiply(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.multiply(l, r) }
    override fun and(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.and(l, r) }
    override fun or(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.or(l, r) }
    override fun xor(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.xor(l, r) }
    override fun divide(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.divide(l, r) }
    override fun mod(left: Any?, right: Any?): Any? = lazyOperation(left, right) { l, r -> super.mod(l, r) }
    override fun compare(left: Any?, right: Any?, operator: String?): Int =
        eagerOperation(left, right) { l, r -> super.compare(l, r, operator) }

    override fun toBoolean(value: Any): Boolean = eagerUnaryOperation(value) { v -> super.toBoolean(v) }
    override fun toInteger(value: Any): Int = eagerUnaryOperation(value) { v -> super.toInteger(v) }
    override fun toString(value: Any): String = eagerUnaryOperation(value) { v -> super.toString(v) }
    override fun toLong(value: Any): Long = eagerUnaryOperation(value) { v -> super.toLong(v) }
    override fun toDouble(value: Any): Double = eagerUnaryOperation(value) { v -> super.toDouble(v) }
    override fun toBigDecimal(value: Any?): BigDecimal? = eagerUnaryOperation(value) { v -> super.toBigDecimal(v) }
    override fun toBigInteger(value: Any?): BigInteger? = eagerUnaryOperation(value) { v -> super.toBigInteger(v) }

    override fun not(value: Any?): Any? = lazyUnaryOperation(value) { v -> super.not(v) }
    override fun equals(left: Any?, right: Any?): Boolean = eagerOperation(left, right) { l, r -> super.equals(l, r) }

    override fun isFloatingPointNumber(value: Any?): Boolean =
        eagerUnaryOperation(value) { v -> super.isFloatingPointNumber(v) }

    override fun isFloatingPoint(value: Any?): Boolean = eagerUnaryOperation(value) { v -> super.isFloatingPoint(v) }
    override fun isNumberable(value: Any?): Boolean = eagerUnaryOperation(value) { v -> super.isNumberable(v) }

    override fun isEmpty(value: Any?): Boolean = eagerUnaryOperation(value) { v -> super.isEmpty(v) }
    override fun isEmpty(value: Any?, def: Boolean?): Boolean =
        eagerUnaryOperation(value) { v -> super.isEmpty(v, def) }

    override fun negate(value: Any?): Any? = lazyUnaryOperation(value) { v -> super.negate(v) }
    override fun positivize(value: Any?): Any? = lazyUnaryOperation(value) { v -> super.positivize(v) }
    override fun asLongNumber(value: Any?): Number? = eagerUnaryOperation(value) { v -> super.asLongNumber(v) }
    override fun contains(container: Any?, value: Any?): Boolean = eagerOperation(container, value) { c, v ->
        super.contains(c, v)
    }

    override fun endsWith(left: Any?, right: Any?): Boolean =
        eagerOperation(left, right) { l, r -> super.endsWith(l, r) }

    override fun startsWith(left: Any?, right: Any?): Boolean =
        eagerOperation(left, right) { l, r -> super.startsWith(l, r) }

    override fun empty(value: Any?): Boolean = eagerUnaryOperation(value) { v -> super.empty(v) }
    override fun size(value: Any?): Int = eagerUnaryOperation(value) { v -> super.size(v) }
    override fun size(value: Any?, def: Int?): Int = eagerUnaryOperation(value) { v -> super.size(v, def) }

    private fun <TResult> eagerOperation(left: Any?, right: Any?, operation: (Any?, Any?) -> TResult): TResult {
        val unpackLeft = FusionRuntime.unwrapLazy(left)
        val unpackRight = FusionRuntime.unwrapLazy(right)
        return operation.invoke(unpackLeft, unpackRight)
    }

    private fun <TResult> eagerUnaryOperation(value: Any?, operation: (Any?) -> TResult): TResult {
        val unpackValue = FusionRuntime.unwrapLazy(value)
        return operation.invoke(unpackValue)
    }

    private fun lazyOperation(left: Any?, right: Any?, operation: (Any?, Any?) -> Any?): Any? {
        return if (left is Lazy<*> || right is Lazy<*>) {
            createLazy {
                val unpackLeft = FusionRuntime.unwrapLazy(left)
                val unpackRight = FusionRuntime.unwrapLazy(right)
                operation.invoke(unpackLeft, unpackRight)
            }
        } else {
            operation.invoke(left, right)
        }
    }

    private fun lazyUnaryOperation(value: Any?, operation: (Any?) -> Any?): Any? {
        return if (value is Lazy<*>) {
            createLazy {
                val unpackValue = FusionRuntime.unwrapLazy(value)
                operation.invoke(unpackValue)
            }
        } else {
            operation.invoke(value)
        }
    }

}

private fun <T> createLazy(initializer: () -> T): Lazy<T> =
    lazy(initializer)