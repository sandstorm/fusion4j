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

package io.neos.fusion4j.lang.semantic

import io.neos.fusion4j.lang.model.RelativeFusionPathName
import kotlin.math.min

class PositionalArraySorter(
    val order: Map<RelativeFusionPathName, Int>
) : Comparator<RelativeFusionPathName> {
    override fun compare(o1: RelativeFusionPathName?, o2: RelativeFusionPathName?): Int {
        checkNotNull(o1)
        checkNotNull(o2)

        val index1 = order[o1]
            ?: throw IllegalStateException("Could not compare ${o1.pathAsString}, relative path not listed in order $order")
        val index2 = order[o2]
            ?: throw IllegalStateException("Could not compare ${o2.pathAsString}, relative path not listed in order $order")
        return compareValues(index1, index2)
    }

    companion object {
        fun createSorter(
            allKeys: Set<RelativeFusionPathName>,
            declaredKeyPositions: Map<RelativeFusionPathName, KeyPosition>
        ): PositionalArraySorter {

            val builder = allKeys
                .filter { it.propertyAttribute }
                .fold(Builder()) { result, current ->
                    when (val declaredPosition = declaredKeyPositions[current]) {
                        is StartPosition -> result.addStart(current, declaredPosition)
                        is EndPosition -> result.addEnd(current, declaredPosition)
                        is MiddlePosition -> result.addMiddle(current, declaredPosition)
                        is BeforePosition -> result.addBefore(current, declaredPosition)
                        is AfterPosition -> result.addAfter(current, declaredPosition)
                        null -> result.addUnspecified(current)
                        else -> throw IllegalArgumentException("Unknown key position declaration $declaredPosition")
                    }
                }

            val preOrdered: List<RelativeFusionPathName> =
                // start list sorted by weight (reversed)
                builder.start
                    .sortedBy { it.second.weight }
                    .reversed()
                    .map { it.first } +
                        // middle list plus unspecified numeric keys
                        (builder.middle.map { it.first to it.second.numericIndex } + builder.unspecifiedNumeric)
                            .sortedBy { it.second }
                            .map { it.first } +
                        // unspecified
                        builder.unspecified
                            .sortedBy { it.propertyName } +
                        // end list sorted by weight
                        builder.end
                            .sortedBy { it.second.weight }
                            .map { it.first }

            val beforeByRefKey: Map<RelativeFusionPathName, List<Pair<RelativeFusionPathName, BeforePosition>>> =
                builder.before
                    .groupBy { it.second.beforeKey }
                    .mapValues { before ->
                        before.value
                            .sortedBy { it.second.weight }
                            .reversed()
                    }
            // before cycle detection
            beforeByRefKey.forEach { groupByRef ->
                groupByRef.value.forEach { checkForCycles(it.first, beforeByRefKey, emptySet()) }
            }
            val afterByRefKey: Map<RelativeFusionPathName, List<Pair<RelativeFusionPathName, AfterPosition>>> =
                builder.after
                    .groupBy { it.second.afterKey }
                    .mapValues { after ->
                        after.value
                            .sortedBy { it.second.weight }
                    }
            // after cycle detection
            afterByRefKey.forEach { groupByRef ->
                groupByRef.value.forEach { checkForCycles(it.first, afterByRefKey, emptySet()) }
            }

            val insertKeys: Map<RelativeFusionPathName, InsertKeys> = (beforeByRefKey.keys + afterByRefKey.keys)
                .associateWith {
                    InsertKeys(it, beforeByRefKey[it], afterByRefKey[it])
                }

            // recursive inserting of before / after keys
            val inserted = if (insertKeys.isNotEmpty())
                insertKeys(
                    insertKeys,
                    preOrdered
                )
            else {
                preOrdered
            }
            return PositionalArraySorter(inserted.mapIndexed { idx, key -> key to idx }.toMap())
        }

        private fun insertKeys(
            insertKeys: Map<RelativeFusionPathName, InsertKeys>,
            finished: List<RelativeFusionPathName>
        ): List<RelativeFusionPathName> {
            val currentIteration = insertKeys.keys.filter { finished.contains(it) }.toSet()

            val currentIterationFinished = currentIteration.fold(finished) { result, insertTargetKey ->
                val insertableKeysSorted = insertKeys[insertTargetKey]
                    ?: throw FusionSemanticError("No insertable keys found for '$insertTargetKey'; target key not found in: $insertKeys")

                val targetIndex = result.indexOf(insertTargetKey)
                if (targetIndex < 0) {
                    throw FusionSemanticError("Could not insert keys at '$insertTargetKey'; target key not found in: $result")
                }
                val startList = result.subList(0, targetIndex)
                val endList = result.subList(min(targetIndex + 1, result.size), result.size)
                val targetKey = result[targetIndex]
                val beforeKeys = insertableKeysSorted.beforeKeys?.map { it.first }
                    ?: emptyList()
                val afterKeys = insertableKeysSorted.afterKeys?.map { it.first }
                    ?: emptyList()
                startList + beforeKeys + targetKey + afterKeys + endList
            }

            val notInsertableJet = insertKeys.filter { !finished.contains(it.key) }
            return if (notInsertableJet.isNotEmpty()) {
                insertKeys(notInsertableJet, currentIterationFinished)
            } else {
                currentIterationFinished
            }
        }

        private fun checkForCycles(
            entry: RelativeFusionPathName,
            beforeByRefKey: Map<RelativeFusionPathName, List<Pair<RelativeFusionPathName, *>>>,
            foundSoFar: Set<Pair<RelativeFusionPathName, RelativeFusionPathName>>
        ) {
            if (foundSoFar.any { it.first == entry }) {
                throw FusionSemanticError("Cycle detected in key position before reference: " +
                        "${entry.pathAsString}, cycle: ${foundSoFar.map { it.first.pathAsString to it.second.pathAsString }}")
            }
            val allRefTargets = beforeByRefKey
                .filter { it.value.any { pair -> pair.first == entry } }

            allRefTargets.forEach { refTarget ->
                checkForCycles(refTarget.key, beforeByRefKey, foundSoFar + (entry to refTarget.key))
            }
        }

    }

    internal data class InsertKeys(
        val targetKey: RelativeFusionPathName,
        val beforeKeys: List<Pair<RelativeFusionPathName, BeforePosition>>?,
        val afterKeys: List<Pair<RelativeFusionPathName, AfterPosition>>?,
    ) {
        init {
            if (beforeKeys == null && afterKeys == null) {
                throw FusionSemanticError("Before or after keys must not be null for target key $targetKey")
            }
        }
    }

    internal class Builder(
        val start: List<Pair<RelativeFusionPathName, StartPosition>> = listOf(),
        val middle: List<Pair<RelativeFusionPathName, MiddlePosition>> = listOf(),
        val end: List<Pair<RelativeFusionPathName, EndPosition>> = listOf(),
        val before: List<Pair<RelativeFusionPathName, BeforePosition>> = listOf(),
        val after: List<Pair<RelativeFusionPathName, AfterPosition>> = listOf(),
        val unspecified: Set<RelativeFusionPathName> = setOf(),
        val unspecifiedNumeric: List<Pair<RelativeFusionPathName, Int>> = listOf(),
    ) {

        fun addStart(key: RelativeFusionPathName, keyPosition: StartPosition): Builder =
            Builder(
                start + (key to keyPosition),
                middle, end, before, after, unspecified, unspecifiedNumeric
            )

        fun addMiddle(key: RelativeFusionPathName, keyPosition: MiddlePosition): Builder =
            Builder(
                start,
                middle + (key to keyPosition),
                end, before, after, unspecified, unspecifiedNumeric
            )

        fun addEnd(key: RelativeFusionPathName, keyPosition: EndPosition): Builder =
            Builder(
                start, middle,
                end + (key to keyPosition),
                before, after, unspecified, unspecifiedNumeric
            )

        fun addBefore(key: RelativeFusionPathName, keyPosition: BeforePosition): Builder =
            Builder(
                start, middle, end,
                before + (key to keyPosition),
                after, unspecified, unspecifiedNumeric
            )

        fun addAfter(key: RelativeFusionPathName, keyPosition: AfterPosition): Builder =
            Builder(
                start, middle, end, before,
                after + (key to keyPosition),
                unspecified, unspecifiedNumeric
            )

        fun addUnspecified(key: RelativeFusionPathName): Builder {
            val numericKey = KeyPosition.isNumericKey(key)
            return Builder(
                start, middle, end, before, after,
                if (!numericKey) unspecified + key else unspecified,
                if (numericKey) unspecifiedNumeric + (key to key.propertyName.toInt()) else unspecifiedNumeric
            )
        }

    }
}