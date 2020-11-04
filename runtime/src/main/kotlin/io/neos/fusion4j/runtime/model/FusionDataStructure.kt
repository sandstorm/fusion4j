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

package io.neos.fusion4j.runtime.model

class FusionDataStructure<T>(
    val data: List<Pair<String, T>>
) : List<Pair<String, T>>, Map<String, T> {
    val valueList: List<T> by lazy { data.map { it.second } }

    // collection
    override val size: Int get() = data.size

    // list
    override fun contains(element: Pair<String, T>): Boolean = data.contains(element)
    override fun containsAll(elements: Collection<Pair<String, T>>): Boolean = data.containsAll(elements)
    override fun get(index: Int): Pair<String, T> = data[index]
    override fun indexOf(element: Pair<String, T>): Int = data.indexOf(element)
    override fun isEmpty(): Boolean = data.isEmpty()
    fun isNotEmpty(): Boolean = !isEmpty()
    override fun iterator(): Iterator<Pair<String, T>> = data.iterator()
    override fun lastIndexOf(element: Pair<String, T>): Int = data.lastIndexOf(element)
    override fun listIterator(): ListIterator<Pair<String, T>> = data.listIterator()
    override fun listIterator(index: Int): ListIterator<Pair<String, T>> = data.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<Pair<String, T>> = data.subList(fromIndex, toIndex)

    // map
    val dataMap by lazy { data.toMap() }

    //val dataMap get() = data.toMap()
    override val keys: Set<String> get() = dataMap.keys
    override val values: Collection<T> get() = dataMap.values
    override val entries: Set<Map.Entry<String, T>> get() = dataMap.entries
    override fun containsKey(key: String): Boolean = dataMap.containsKey(key)
    override fun containsValue(value: T): Boolean = dataMap.containsValue(value)
    override fun get(key: String): T? = dataMap[key]

    override fun toString(): String = "FusionDataStructure"


    companion object {
        fun <T> fromList(data: List<T>): FusionDataStructure<T> =
            FusionDataStructure(
                data.mapIndexed { idx, item ->
                    val key = when (item) {
                        is Pair<*, *> -> item.first?.toString() ?: idx.toString()
                        else -> idx.toString()
                    }

                    @Suppress("UNCHECKED_CAST")
                    val value = when (item) {
                        is Pair<*, *> -> item.second as T
                        else -> item
                    }
                    key to value
                }
            )

        fun <T> fromSet(data: Set<T>): FusionDataStructure<T> =
            fromList(data.toList())

        fun <T> fromMap(data: Map<String, T>): FusionDataStructure<T> =
            FusionDataStructure(data.toList())

        fun <T> fromCollection(data: Collection<T>?): FusionDataStructure<T>? =
            @Suppress("UNCHECKED_CAST")
            when (data) {
                null -> null
                is FusionDataStructure<*> -> data as FusionDataStructure<T>
                is List<*> -> fromList(data as List<T>)
                is Set<*> -> fromSet(data as Set<T>)
                is Map<*, *> -> fromMap(data as Map<String, T>)
                else -> throw IllegalArgumentException(
                    "Could not create FusionDataStructure from collection; " +
                            "unhandled type ${data::class.java.name} with value: $data"
                )
            }

        inline fun <reified T> fromAny(data: Any?): FusionDataStructure<T>? =
            @Suppress("UNCHECKED_CAST")
            when (data) {
                null -> null
                is Collection<*> -> fromCollection(data as Collection<T>)
                is Array<*> -> fromList(data.asList() as List<T>)
                is Map<*, *> -> fromMap(data as Map<String, T>)
                else -> throw IllegalArgumentException(
                    "Could not create FusionDataStructure; " +
                            "unhandled type ${data::class.java.name} with value: $data"
                )
            }

    }
}