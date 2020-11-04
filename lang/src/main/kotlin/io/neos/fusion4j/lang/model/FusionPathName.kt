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

package io.neos.fusion4j.lang.model

import io.neos.fusion4j.lang.parseStandaloneFusionPath

interface FusionPathName {

    val segments: List<FusionPathNameSegment>
    val absolute: Boolean

    val nested: Boolean get() = segments.size > 1

    val root: Boolean get() = segments.isEmpty()

    val extendingPrototypeName: QualifiedPrototypeName?
        get() {
            val indexOfLastPrototypeSegment = segments.indexOfLast { it is PrototypeCallPathSegment }
            return if (indexOfLastPrototypeSegment > 0 && segments.size > (indexOfLastPrototypeSegment + 1)) {
                (segments[indexOfLastPrototypeSegment] as PrototypeCallPathSegment).prototypeName
            } else {
                null
            }
        }

    val isRootPrototypePath: Boolean get() = segments.isNotEmpty() && segments.first().type == PathSegmentType.PROTOTYPE_CALL

    val propertyPath: Boolean
        get() =
            nested && segments.last() is FusionPropertyPathSegment

    fun pointsToRootPrototype(): Boolean =
        segments.size == 1 && segments.first().type == PathSegmentType.PROTOTYPE_CALL

    fun pointsToRootPrototype(prototypeName: QualifiedPrototypeName): Boolean =
        pointsToRootPrototype() && (segments.first() as PrototypeCallPathSegment).prototypeName == prototypeName

    val prototypeExtensionScopePathSegments: List<FusionPathNameSegment>
        get() {
            val idx = segments.indexOfLast { it is PrototypeCallPathSegment }
            if (idx > 0) {
                return segments.subList(0, idx)
            } else {
                throw IllegalStateException("path $this does not extend any prototype")
            }
        }

    val prototypeExtensionPrototypePathSegments: List<FusionPathNameSegment>
        get() {
            val idx = segments.indexOfLast { it is PrototypeCallPathSegment }
            if (idx > 0) {
                return segments.subList(0, idx + 1)
            } else {
                throw IllegalStateException("path $this does not extend any prototype")
            }
        }

    val prototypeExtensionValuePathSegmentRange: IntRange
        get() {
            val idx = segments.indexOfLast { it is PrototypeCallPathSegment }
            if (idx > 0) {
                return (idx + 1)..segments.lastIndex
            } else {
                throw IllegalStateException("path $this does not extend any prototype")
            }
        }

    fun toReadableString(): String =
        segments.joinToString(".", transform = FusionPathNameSegment::toReadableString)

    fun isAnyChildOf(parentPath: FusionPathName): Boolean =
        parentPath.segments.isEmpty() || nested && segments.size > parentPath.segments.size &&
                parentPath.segments.indices.all { segments[it] == parentPath.segments[it] }

    fun relativeToPrototype(): RelativeFusionPathName {
        val indexOfLastPrototypeCallPathSegment = segments.indexOfLast { it is PrototypeCallPathSegment }
        if (indexOfLastPrototypeCallPathSegment < 0) {
            throw IllegalStateException("Path '${toReadableString()}' is not child of a prototype path")
        }
        return RelativeFusionPathName.fromSegments(
            segments.subList(
                indexOfLastPrototypeCallPathSegment + 1,
                segments.size
            )
        )
    }

    fun relativeToParent(): RelativeFusionPathName =
        if (segments.isEmpty())
            current()
        else
            RelativeFusionPathName.fromSegments(listOf(segments.last()))

    fun endsWith(relativePath: RelativeFusionPathName): Boolean =
        this == relativePath
                || relativePath.segments.isEmpty()
                ||
                (segments.size >= relativePath.segments.size
                        && relativePath.segments.indices
                    .all { idx ->
                        val thisIdx = idx + (segments.size - relativePath.segments.size)
                        segments[thisIdx] == relativePath.segments[idx]
                    })

    companion object {
        fun root(): AbsoluteFusionPathName = AbsoluteFusionPathName.fromSegments(emptyList())
        fun current(): RelativeFusionPathName = RelativeFusionPathName.fromSegments(emptyList())

        // TODO should the name start with an '@' here for better code readability in implementation classes?
        fun metaAttribute(metaPropertyName: String): RelativeFusionPathName =
            RelativeFusionPathName.fromSegments(
                listOf(
                    MetaPropertyPathSegment.create(
                        assertSingleSegmentedNameContainsNoDot(metaPropertyName)
                    )
                )
            )

        fun attribute(
            attributeName: String,
            quoting: PathNameSegmentQuoting = PathNameSegmentQuoting.NO_QUOTES
        ): RelativeFusionPathName =
            RelativeFusionPathName.fromSegments(
                listOf(
                    PropertyPathSegment.create(
                        assertSingleSegmentedNameContainsNoDot(attributeName),
                        quoting
                    )
                )
            )

        fun rootPrototype(prototypeName: QualifiedPrototypeName): AbsoluteFusionPathName =
            AbsoluteFusionPathName.fromSegments(
                listOf(
                    PrototypeCallPathSegment.create(prototypeName)
                )
            )

        fun parse(path: String): FusionPathName = with(path.trim()) {
            if (this.isEmpty()) {
                throw IllegalArgumentException("Fusion path must not be blank or empty")
            } else {
                return parseStandaloneFusionPath(path)
            }
        }

        fun parseSimpleRelative(path: String): RelativeFusionPathName =
            RelativeFusionPathName.fromSegments(
                path
                    .split(".")
                    .map {
                        if (it.startsWith("@")) {
                            MetaPropertyPathSegment.create(it.substring(1))
                        } else {
                            PropertyPathSegment.create(it, PathNameSegmentQuoting.NO_QUOTES)
                        }
                    }
            )

        fun parseRelative(path: String): RelativeFusionPathName = with(parse(path)) {
            when (this) {
                is RelativeFusionPathName -> this
                else -> throw IllegalArgumentException("path $path is not relative")
            }
        }

        fun parseRelativePrependDot(path: String): RelativeFusionPathName =
            parseRelative(".$path")

        fun parseAbsolute(path: String): AbsoluteFusionPathName = with(parse(path)) {
            when (this) {
                is AbsoluteFusionPathName -> this
                else -> throw IllegalArgumentException("path $path is not absolute")
            }
        }

    }
}

fun assertSingleSegmentedNameContainsNoDot(string: String): String =
    if (string.contains("\\."))
        throw IllegalArgumentException("single segmented path name must contain no dots")
    else
        string

class FusionPathNameBuilder(
    private val initialSegments: List<FusionPathNameSegment> = emptyList()
) {
    fun relative(): FusionPathNameSegmentsBuilder<RelativeFusionPathName> =
        FusionPathNameSegmentsBuilder(RelativeFusionPathName::fromSegments, initialSegments)

    fun absolute(): FusionPathNameSegmentsBuilder<AbsoluteFusionPathName> =
        FusionPathNameSegmentsBuilder(AbsoluteFusionPathName::fromSegments, initialSegments)

    companion object {
        fun relative() = FusionPathNameBuilder().relative()
        fun absolute() = FusionPathNameBuilder().absolute()
    }
}

data class FusionPathNameSegmentsBuilder<TPath : FusionPathName>(
    private val factoryFunction: (List<FusionPathNameSegment>) -> TPath,
    private val segments: List<FusionPathNameSegment>
) {
    fun property(
        name: String,
        quoting: PathNameSegmentQuoting = PathNameSegmentQuoting.NO_QUOTES
    ): FusionPathNameSegmentsBuilder<TPath> =
        FusionPathNameSegmentsBuilder(factoryFunction, segments + PropertyPathSegment.create(name, quoting))

    fun meta(name: String): FusionPathNameSegmentsBuilder<TPath> =
        FusionPathNameSegmentsBuilder(factoryFunction, segments + MetaPropertyPathSegment.create(name))

    fun prototypeCall(prototypeName: QualifiedPrototypeName): FusionPathNameSegmentsBuilder<TPath> =
        FusionPathNameSegmentsBuilder(factoryFunction, segments + PrototypeCallPathSegment.create(prototypeName))

    fun build(): TPath = factoryFunction.invoke(segments)
}

data class RelativeFusionPathName private constructor(
    override val segments: List<FusionPathNameSegment>
) : FusionPathName {
    override val absolute: Boolean = false
    val metaAttribute: Boolean
        get() =
            !nested && segments.single() is MetaPropertyPathSegment
    val propertyAttribute: Boolean
        get() =
            !nested && segments.single() is PropertyPathSegment

    val propertyName: String get() = (segments.single() as PropertyPathSegment).name

    fun parent(): RelativeFusionPathName =
        if (nested)
            fromSegments(segments.subList(0, segments.size - 1))
        else
            FusionPathName.current()

    fun relativeTo(basePath: FusionPathName): RelativeFusionPathName =
        when {
            this == basePath -> FusionPathName.current()
            isAnyChildOf(basePath) -> fromSegments(
                segments.subList(
                    basePath.segments.size,
                    segments.size
                )
            )
            else -> throw IllegalArgumentException("Could not relativize path $this to base path; given base path must be a parent path of $basePath")
        }

    fun allParentPaths(): List<RelativeFusionPathName> = when {
        root -> emptyList()
        else -> segments.indices.map { RelativeFusionPathName(segments.subList(0, it)) }
    }

    fun append(relativePath: RelativeFusionPathName): RelativeFusionPathName =
        RelativeFusionPathName(segments + relativePath.segments)

    fun appendSegment(segment: FusionPathNameSegment): RelativeFusionPathName =
        RelativeFusionPathName(segments + segment)

    fun prependSegment(segment: FusionPathNameSegment): RelativeFusionPathName =
        RelativeFusionPathName(listOf(segment) + segments)

    fun toAbsolute(basePath: AbsoluteFusionPathName = FusionPathName.root()) =
        AbsoluteFusionPathName.fromSegments(basePath.segments + segments)

    fun builder(): FusionPathNameSegmentsBuilder<RelativeFusionPathName> =
        FusionPathNameBuilder(segments).relative()

    fun appendPrototypeCallSegment(prototypeName: QualifiedPrototypeName): RelativeFusionPathName =
        builder().prototypeCall(prototypeName).build()

    operator fun plus(appendix: RelativeFusionPathName) = append(appendix)

    override fun toString(): String = ".${toReadableString()}"

    companion object {
        fun fromSegments(segments: List<FusionPathNameSegment>): RelativeFusionPathName =
            RelativeFusionPathName(segments)
    }

}

data class AbsoluteFusionPathName private constructor(
    override val segments: List<FusionPathNameSegment>
) : FusionPathName {

    override val absolute: Boolean = true
    val prototypeExtensionScopePath: AbsoluteFusionPathName get() = fromSegments(prototypeExtensionScopePathSegments)
    val prototypeExtensionPrototypePath: AbsoluteFusionPathName
        get() = fromSegments(
            prototypeExtensionPrototypePathSegments
        )

    val allPrototypeExtensionVariants: Set<AbsoluteFusionPathName>
        get() =
            prototypeExtensionValuePathSegmentRange.fold(emptySet()) { result, idx ->
                result + fromSegments(segments.subList(0, idx))
            }

    fun isDirectChildOf(parentPath: FusionPathName): Boolean =
        nested && segments.size == parentPath.segments.size + 1 &&
                parentPath.segments.indices.all { segments[it] == parentPath.segments[it] }

    fun isDirectChildOfRelative(parentPath: RelativeFusionPathName): Boolean =
        nested && parentPath.segments.indices.all { segments[segments.size - it - 2] == parentPath.segments[it] }

    fun relativeToRoot(): RelativeFusionPathName = RelativeFusionPathName.fromSegments(segments)

    fun relativeTo(basePath: AbsoluteFusionPathName): RelativeFusionPathName =
        when {
            this == basePath -> FusionPathName.current()
            isAnyChildOf(basePath) -> RelativeFusionPathName.fromSegments(
                segments.subList(
                    basePath.segments.size,
                    segments.size
                )
            )
            else -> throw IllegalArgumentException("Could not relativize path $this to base path; given base path must be a parent path of $basePath")
        }

    fun cutTail(segmentCountToCut: Int): AbsoluteFusionPathName =
        if (segmentCountToCut > segments.size) {
            throw IllegalArgumentException("Could not cut $segmentCountToCut tail segments; too less segments in path $this")
        } else {
            fromSegments(segments.subList(0, segments.size - segmentCountToCut))
        }

    fun parent(): AbsoluteFusionPathName =
        if (nested)
            fromSegments(segments.subList(0, segments.size - 1))
        else
            FusionPathName.root()

    fun allParentPaths(): List<AbsoluteFusionPathName> = when {
        root -> emptyList()
        else -> segments.indices
            .map { AbsoluteFusionPathName(segments.subList(0, it)) }
            .reversed()
    }

    fun allVariants(): List<AbsoluteFusionPathName> =
        listOf(this) + allParentPaths()

    fun append(relativePath: RelativeFusionPathName): AbsoluteFusionPathName =
        AbsoluteFusionPathName(segments + relativePath.segments)

    fun appendSegment(segment: FusionPathNameSegment): AbsoluteFusionPathName =
        AbsoluteFusionPathName(segments + segment)

    fun prependSegment(segment: FusionPathNameSegment): AbsoluteFusionPathName =
        AbsoluteFusionPathName(listOf(segment) + segments)

    fun builder(): FusionPathNameSegmentsBuilder<AbsoluteFusionPathName> =
        FusionPathNameBuilder(segments).absolute()

    fun appendPrototypeCallSegment(prototypeName: QualifiedPrototypeName): AbsoluteFusionPathName =
        builder().prototypeCall(prototypeName).build()

    operator fun plus(appendix: RelativeFusionPathName) = append(appendix)

    override fun toString(): String = "/${toReadableString()}"

    companion object {
        fun fromSegments(segments: List<FusionPathNameSegment>) =
            AbsoluteFusionPathName(segments)
    }


}

inline fun <reified T : FusionPathNameSegment> FusionPathName.segmentsOfType() = segments.filterIsInstance<T>()
