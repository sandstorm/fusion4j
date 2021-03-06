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

@file:Suppress("DataClassPrivateConstructor")

package io.neos.fusion4j.lang.model

private const val NAME_PATTERN = "^[a-zA-Z0-9\\-_:]+$"

interface FusionPathNameSegment {

    val type: PathSegmentType

    val segmentAsString: String

}

interface FusionPropertyPathSegment : FusionPathNameSegment {
    val name: String
}

enum class PathSegmentType {
    PROPERTY,
    META_PROPERTY,
    PROTOTYPE_CALL
}

enum class PathNameSegmentQuoting {
    NO_QUOTES,
    SINGLE_QUOTED,
    DOUBLE_QUOTED
}

data class PropertyPathSegment private constructor(
    override val name: String,
    val quoting: PathNameSegmentQuoting,
    override val type: PathSegmentType = PathSegmentType.PROPERTY
) : FusionPropertyPathSegment {
    override val segmentAsString: String =
        when (quoting) {
            PathNameSegmentQuoting.NO_QUOTES -> name
            PathNameSegmentQuoting.DOUBLE_QUOTED -> "\"$name\""
            PathNameSegmentQuoting.SINGLE_QUOTED -> "'$name'"
        }

    companion object {
        fun create(name: String, quoting: PathNameSegmentQuoting): PropertyPathSegment {
            if (quoting == PathNameSegmentQuoting.NO_QUOTES && !name.matches(NAME_PATTERN.toRegex())) {
                throw IllegalArgumentException("invalid fusion path name segment '$name'; must match pattern: $NAME_PATTERN")
            }
            return PropertyPathSegment(name, quoting)
        }
    }
}

data class MetaPropertyPathSegment private constructor(
    override val name: String,
    override val type: PathSegmentType = PathSegmentType.META_PROPERTY
) : FusionPropertyPathSegment {
    override val segmentAsString: String = "@$name"

    companion object {
        fun create(name: String): MetaPropertyPathSegment {
            if (!name.matches(NAME_PATTERN.toRegex())) {
                throw IllegalArgumentException("invalid fusion meta property path name segment '$name'; must match pattern: $NAME_PATTERN")
            }
            return MetaPropertyPathSegment(name)
        }
    }
}

data class PrototypeCallPathSegment private constructor(
    val prototypeName: QualifiedPrototypeName,
    override val type: PathSegmentType = PathSegmentType.PROTOTYPE_CALL
) : FusionPathNameSegment {
    override val segmentAsString: String = "prototype($prototypeName)"

    companion object {
        fun create(prototypeName: QualifiedPrototypeName): PrototypeCallPathSegment =
            PrototypeCallPathSegment(
                prototypeName
            )
    }
}

