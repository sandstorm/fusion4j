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

import io.neos.fusion4j.lang.model.FusionPathNameBuilder
import io.neos.fusion4j.lang.model.RelativeFusionPathName

interface KeyPosition {
    val subject: String

    companion object {
        private val NUMERIC_PATTERN = Regex("^\\d+$")
        private val BEFORE_PATTERN = Regex("^before\\s+(?<key>[a-zA-Z0-9_\\-:]+)(\\s+(?<weight>\\d+))?$")
        private val AFTER_PATTERN = Regex("^after\\s+(?<key>[a-zA-Z0-9_\\-:]+)(\\s+(?<weight>\\d+))?$")
        private val START_PATTERN = Regex("^start(\\s+(?<weight>\\d+))?$")
        private val END_PATTERN = Regex("^end(\\s+(?<weight>\\d+))?$")

        fun isNumericKey(relativePath: RelativeFusionPathName): Boolean =
            NUMERIC_PATTERN.matches(relativePath.propertyName)

        fun parseFromString(subjectRaw: String): KeyPosition {
            val subject = subjectRaw.trim()
            if (subject.isEmpty()) {
                throw KeyPositionParseException(subject, "subject must not be blank")
            }
            // TODO are meta properties position sortable?

            // numeric subject
            if (subject.matches(NUMERIC_PATTERN)) {
                return MiddlePosition(subject, subject.toInt())
            }

            // before
            val beforeMatch = BEFORE_PATTERN.matchEntire(subject)
            if (beforeMatch != null) {
                val weight = beforeMatch.groups["weight"]?.value?.toInt()
                    ?: 0
                val keyString = beforeMatch.groups["key"]?.value
                    ?: throw KeyPositionParseException(subject, "could not parse before key")
                return BeforePosition(
                    subject,
                    weight,
                    FusionPathNameBuilder.relative().property(keyString).build()
                )
            }
            // after
            val afterMatch = AFTER_PATTERN.matchEntire(subject)
            if (afterMatch != null) {
                val weight = afterMatch.groups["weight"]?.value?.toInt()
                    ?: 0
                val keyString = afterMatch.groups["key"]?.value
                    ?: throw KeyPositionParseException(subject, "could not parse after key")
                return AfterPosition(
                    subject,
                    weight,
                    FusionPathNameBuilder.relative().property(keyString).build()
                )
            }

            // start
            val startMatch = START_PATTERN.matchEntire(subject)
            if (startMatch != null) {
                val weight = startMatch.groups["weight"]?.value?.toInt()
                    ?: 0
                return StartPosition(
                    subject,
                    weight
                )
            }

            // end
            val endMatch = END_PATTERN.matchEntire(subject)
            if (endMatch != null) {
                val weight = endMatch.groups["weight"]?.value?.toInt()
                    ?: 0
                return EndPosition(
                    subject,
                    weight
                )
            }

            throw KeyPositionParseException(subject, "unknown subject")
        }
    }

}

data class BeforePosition(
    override val subject: String,
    val weight: Int,
    val beforeKey: RelativeFusionPathName
) : KeyPosition

data class AfterPosition(
    override val subject: String,
    val weight: Int,
    val afterKey: RelativeFusionPathName
) : KeyPosition

data class StartPosition(
    override val subject: String,
    val weight: Int
) : KeyPosition

data class EndPosition(
    override val subject: String,
    val weight: Int
) : KeyPosition

data class MiddlePosition(
    override val subject: String,
    val numericIndex: Int
) : KeyPosition

class KeyPositionParseException(
    subject: String,
    message: String,
    cause: Throwable? = null
) :
    IllegalArgumentException("Could not parse key position '$subject'; $message", cause)