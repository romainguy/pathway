/*
 * Copyright (C) 2021 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.romainguy.graphics.path

import android.graphics.Path
import android.graphics.PointF

/**
 * A path segment represents a curve (line, cubic, quadratic or conic) or a command inside
 * a fully formed [path][android.graphics.Path] object.
 *
 * A segment is identified by a [type][PathSegment.Type] which in turns defines how many
 * [points] are available (from 0 to 3) and whether the [weight] is meaningful. Please refer
 * to the documentation of each [type][PathSegment.Type] for more information.
 *
 * A segment with the [Move][Type.Move] or [Close][Type.Close] is usually represented by
 * the singletons [DoneSegment] and [CloseSegment] respectively.
 *
 * @property type The type that identifies this segment and defines the number of points.
 * @property points An array of points describing this segment, whose size depends on [type].
 * @property weight Conic weight, only valid if [type] is [Type.Conic].
 */
class PathSegment internal constructor(val type: Type, val points: Array<PointF>, val weight: Float) {
    /**
     * Type of a given segment in a [path][android.graphics.Path], either a command
     * ([Type.Move], [Type.Close], [Type.Done]) or a curve ([Type.Line], [Type.Cubic],
     * [Type.Quadratic], [Type.Conic]).
     */
    enum class Type {
        /**
         * Move command, the path segment contains 1 point indicating the move destination.
         * The weight is set 0.0f and not meaningful.
         */
        Move,
        /**
         * Line curve, the path segment contains 2 points indicating the two extremities of
         * the line. The weight is set 0.0f and not meaningful.
         */
        Line,
        /**
         * Quadratic curve, the path segment contains 3 points in the following order:
         * - Start point
         * - Control point
         * - End point
         * The weight is set 0.0f and not meaningful.
         */
        Quadratic,
        /**
         * Conic curve, the path segment contains 3 points in the following order:
         * - Start point
         * - Control point
         * - End point
         * The curve is weighted by the [weight][PathSegment.weight] property.
         */
        Conic,
        /**
         * Cubic curve, the path segment contains 4 points in the following order:
         * - Start point
         * - First control point
         * - Second control point
         * - End point
         * The weight is set 0.0f and not meaningful.
         */
        Cubic,
        /**
         * Close command, close the current contour by joining the last point added to the
         * path with the first point of the current contour. The segment does not contain
         * any point. The weight is set 0.0f and not meaningful.
         */
        Close,
        /**
         * Done command, this optional command indicates that no further segment will be
         * found in the path. It typically indicates the end of an iteration over a path
         * and can be ignored.
         */
        Done
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PathSegment

        if (type != other.type) return false
        if (!points.contentEquals(other.points)) return false
        if (weight != other.weight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + points.contentHashCode()
        result = 31 * result + weight.hashCode()
        return result
    }

    override fun toString(): String {
        return "PathSegment(type=$type, points=${points.contentToString()}, weight=$weight)"
    }
}

/**
 * A [PathSegment] containing the [Done][PathSegment.Type.Done] command.
 */
val DoneSegment = PathSegment(PathSegment.Type.Done, emptyArray(), 0.0f)

/**
 * A [PathSegment] containing the [Close][PathSegment.Type.Close] command.
 */
val CloseSegment = PathSegment(PathSegment.Type.Close, emptyArray(), 0.0f)

/**
 * Cretes a new [PathIterator] for this [path][android.graphics.Path].
 */
operator fun Path.iterator() = PathIterator(this)

/**
 * A path iterator can be used to iterate over all the [segments][PathSegment] that make up
 * a path. Those segments may in turn define multiple contours inside the path.
 */
class PathIterator(path: Path) : Iterator<PathSegment> {
    private companion object {
        init {
            System.loadLibrary("pathway")
        }

        @JvmStatic
        @Suppress("KotlinJniMissingFunction")
        external fun createInternalPathIterator(path: Path): Long

        @JvmStatic
        @Suppress("KotlinJniMissingFunction")
        external fun destroyInternalPathIterator(internalPathIterator: Long)

        @JvmStatic
        @Suppress("KotlinJniMissingFunction")
        external fun internalPathIteratorHasNext(internalPathIterator: Long): Boolean

        @JvmStatic
        @Suppress("KotlinJniMissingFunction")
        external fun internalPathIteratorNext(internalPathIterator: Long, points: FloatArray): Int

        @JvmStatic
        @Suppress("KotlinJniMissingFunction")
        external fun internalPathIteratorPeek(internalPathIterator: Long): Int
    }

    private val pointsData = FloatArray(8) // 4 points max -> 8 floats
    private val internalPathIterator: Long = createInternalPathIterator(path)

    /**
     * Returns `true` if the iteration has more elements.
     */
    override fun hasNext(): Boolean = internalPathIteratorHasNext(internalPathIterator)

    /**
     * Returns the type of the current segment in the iteration, or [Done][PathSegment.Type.Done]
     * if the iteration is finished.
     */
    fun peek() = PathSegment.Type.values()[internalPathIteratorPeek(internalPathIterator)]

    /**
     * Returns the [type][PathSegment.Type] of the next [path segment][PathSegment] in the iteration
     * and fills [points] with the points specific to the segment type. Each pair of of floats in
     * the [points] array represents a point for the given segment. The number of pairs of floats
     * depends on the [PathSegment.Type]:
     * - [Move][PathSegment.Type.Move]: 1 pair (indices 0 to 1)
     * - [Move][PathSegment.Type.Line]: 2 pairs (indices 0 to 3)
     * - [Move][PathSegment.Type.Quadratic]: 3 pairs (indices 0 to 5)
     * - [Move][PathSegment.Type.Conic]: 4 pairs (indices 0 to 7), the last pair contains the
     *   [weight][PathSegment.weight] twice
     * - [Move][PathSegment.Type.Cubic]: 4 pairs (indices 0 to 7)
     * - [Close][PathSegment.Type.Close]: 0 pair
     * - [Done][PathSegment.Type.Done]: 0 pair
     * This method does not allocate any memory.
     *
     * @param points *Must* be a [FloatArray] of size 8, throws an [IllegalStateException] otherwise.
     */
    fun next(points: FloatArray): PathSegment.Type {
        check(points.size >= 8) { "The points array must contain at least 8 floats" }
        val typeValue = internalPathIteratorNext(internalPathIterator, points)
        return PathSegment.Type.values()[typeValue]
    }

    /**
     * Returns the next [path segment][PathSegment] in the iteration, or [DoneSegment] if
     * the iteration is finished. If no allocation is desirable, please use the alternative
     * [next] method.
     */
    override fun next(): PathSegment {
        val typeValue = internalPathIteratorNext(internalPathIterator, pointsData)
        val type = PathSegment.Type.values()[typeValue]

        if (type == PathSegment.Type.Done) return DoneSegment
        if (type == PathSegment.Type.Close) return CloseSegment

        val points = when (type) {
            PathSegment.Type.Move -> {
                arrayOf(PointF(pointsData[0], pointsData[1]))
            }
            PathSegment.Type.Line -> {
                arrayOf(
                    PointF(pointsData[0], pointsData[1]),
                    PointF(pointsData[2], pointsData[3])
                )
            }
            PathSegment.Type.Quadratic,
            PathSegment.Type.Conic -> {
                arrayOf(
                    PointF(pointsData[0], pointsData[1]),
                    PointF(pointsData[2], pointsData[3]),
                    PointF(pointsData[4], pointsData[5])
                )
            }
            PathSegment.Type.Cubic -> {
                arrayOf(
                    PointF(pointsData[0], pointsData[1]),
                    PointF(pointsData[2], pointsData[3]),
                    PointF(pointsData[4], pointsData[5]),
                    PointF(pointsData[6], pointsData[7])
                )
            }
            // This should not happen because of the early returns above
            else -> emptyArray()
        }

        val weight = if (type == PathSegment.Type.Conic) pointsData[7] else 0.0f

        return PathSegment(type, points, weight)
    }

    protected fun finalize() {
        destroyInternalPathIterator(internalPathIterator)
    }
}
