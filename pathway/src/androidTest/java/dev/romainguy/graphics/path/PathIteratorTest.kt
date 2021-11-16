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
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

private fun assertPointsEquals(p1: PointF, p2: PointF) {
    assertEquals(p1.x, p2.x, 1e-6f)
    assertEquals(p1.y, p2.y, 1e-6f)
}

private fun assertPointsEquals(p1: FloatArray, offset: Int, p2: PointF) {
    assertEquals(p1[0 + offset * 2], p2.x, 1e-6f)
    assertEquals(p1[1 + offset * 2], p2.y, 1e-6f)
}

@RunWith(AndroidJUnit4::class)
class PathIteratorTest {
    @Test
    fun emptyIterator() {
        val path = Path()

        val iterator = path.iterator()
        assertFalse(iterator.hasNext())


        var count = 0
        for (segment in path) {
            assertEquals(PathSegment.Type.Done, segment.type)
            count++
        }

        assertEquals(0, count)
    }

    @Test
    fun emptyPeek() {
        val path = Path()
        val iterator = path.iterator()
        assertEquals(PathSegment.Type.Done, iterator.peek())
    }

    @Test
    fun nonEmptyIterator() {
        val path = Path().apply {
            moveTo(1.0f, 1.0f)
            lineTo(2.0f, 2.0f)
            close()
        }

        val iterator = path.iterator()
        assertTrue(iterator.hasNext())

        val types = arrayOf(
            PathSegment.Type.Move,
            PathSegment.Type.Line,
            PathSegment.Type.Close
        )
        val points = arrayOf(
            PointF(1.0f, 1.0f),
            PointF(2.0f, 2.0f)
        )

        var count = 0
        for (segment in path) {
            assertEquals(types[count], segment.type)
            when (segment.type) {
                PathSegment.Type.Move -> {
                    assertEquals(points[count], segment.points[0])
                }
                PathSegment.Type.Line -> {
                    assertEquals(points[count - 1], segment.points[0])
                    assertEquals(points[count],     segment.points[1])
                }
                else -> { }
            }
            count++
        }

        assertEquals(3, count)
    }

    @Test
    fun peek() {
        val path = Path().apply {
            moveTo(1.0f, 1.0f)
            lineTo(2.0f, 2.0f)
            close()
        }

        val iterator = path.iterator()
        assertEquals(PathSegment.Type.Move, iterator.peek())
    }

    @Test
    fun peekBeyond() {
        val path = Path()
        assertEquals(PathSegment.Type.Done, path.iterator().peek())

        path.apply {
            moveTo(1.0f, 1.0f)
            lineTo(2.0f, 2.0f)
            close()
        }

        val iterator = path.iterator()
        while (iterator.hasNext()) iterator.next()
        assertEquals(PathSegment.Type.Done, iterator.peek())
    }

    @Test
    fun iteratorLevels() {
        val path = Path().apply {
            moveTo(1.0f, 1.0f)
            lineTo(2.0f, 2.0f)
            cubicTo(3.0f, 3.0f, 4.0f, 4.0f, 5.0f, 5.0f)
            quadTo(7.0f, 7.0f, 8.0f, 8.0f)
            moveTo(10.0f, 10.0f)
            // addRoundRect() will generate conic curves on certain API levels
            addRoundRect(12.0f, 12.0f, 36.0f, 36.0f, 8.0f, 8.0f, Path.Direction.CW)
            close()
        }

        val iterator1 = path.iterator()
        val iterator2 = path.iterator()

        val points = FloatArray(8)

        while (iterator1.hasNext() || iterator2.hasNext()) {
            val segment = iterator1.next()
            val type = iterator2.next(points)

            assertEquals(type, segment.type)

            when (type) {
                PathSegment.Type.Move -> {
                    assertPointsEquals(points, 0, segment.points[0])
                }
                PathSegment.Type.Line -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                }
                PathSegment.Type.Quadratic -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points, 2, segment.points[2])
                }
                PathSegment.Type.Conic -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points, 2, segment.points[2])
                    // We store the weight twice
                    assertEquals(points[6], segment.weight)
                    assertEquals(points[7], segment.weight)
                }
                PathSegment.Type.Cubic -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points, 2, segment.points[2])
                    assertPointsEquals(points, 3, segment.points[3])
                }
                PathSegment.Type.Close -> { }
                PathSegment.Type.Done -> { }
            }
        }
    }

    @Test
    fun done() {
        val path = Path().apply {
            close()
        }

        val segment = path.iterator().next()

        assertEquals(PathSegment.Type.Done, segment.type)
        assertEquals(0, segment.points.size)
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun close() {
        val path = Path().apply {
            lineTo(10.0f, 12.0f)
            close()
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()
        // Swallow the line
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Close, segment.type)
        assertEquals(0, segment.points.size)
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun moveTo() {
        val path = Path().apply {
            moveTo(10.0f, 12.0f)
        }

        val segment = path.iterator().next()

        assertEquals(PathSegment.Type.Move, segment.type)
        assertEquals(1, segment.points.size)
        assertPointsEquals(PointF(10.0f, 12.0f), segment.points[0])
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun lineTo() {
        val path = Path().apply {
            moveTo(4.0f, 6.0f)
            lineTo(10.0f, 12.0f)
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Line, segment.type)
        assertEquals(2, segment.points.size)
        assertPointsEquals(PointF(4.0f, 6.0f), segment.points[0])
        assertPointsEquals(PointF(10.0f, 12.0f), segment.points[1])
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun quadraticTo() {
        val path = Path().apply {
            moveTo(4.0f, 6.0f)
            quadTo(10.0f, 12.0f, 20.0f, 24.0f)
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Quadratic, segment.type)
        assertEquals(3, segment.points.size)
        assertPointsEquals(PointF(4.0f, 6.0f), segment.points[0])
        assertPointsEquals(PointF(10.0f, 12.0f), segment.points[1])
        assertPointsEquals(PointF(20.0f, 24.0f), segment.points[2])
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun cubicTo() {
        val path = Path().apply {
            moveTo(4.0f, 6.0f)
            cubicTo(10.0f, 12.0f, 20.0f, 24.0f, 30.0f, 36.0f)
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Cubic, segment.type)
        assertEquals(4, segment.points.size)
        assertPointsEquals(PointF(4.0f, 6.0f), segment.points[0])
        assertPointsEquals(PointF(10.0f, 12.0f), segment.points[1])
        assertPointsEquals(PointF(20.0f, 24.0f), segment.points[2])
        assertPointsEquals(PointF(30.0f, 36.0f), segment.points[3])
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun conicTo() {
        if (Build.VERSION.SDK_INT >= 25) {
            val path = Path().apply {
                addRoundRect(12.0f, 12.0f, 24.0f, 24.0f, 8.0f, 8.0f, Path.Direction.CW)
            }

            val iterator = path.iterator()
            // Swallow the move
            iterator.next()

            val segment = iterator.next()

            assertEquals(PathSegment.Type.Conic, segment.type)
            assertEquals(3, segment.points.size)

            assertPointsEquals(PointF(12.0f, 18.0f), segment.points[0])
            assertPointsEquals(PointF(12.0f, 12.0f), segment.points[1])
            assertPointsEquals(PointF(18.0f, 12.0f), segment.points[2])
            assertEquals(0.70710677f, segment.weight)
        }
    }
}
