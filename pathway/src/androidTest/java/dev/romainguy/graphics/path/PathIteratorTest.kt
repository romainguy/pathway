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

import android.graphics.*
import android.os.Build
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

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
    fun iteratorStyles() {
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

        val iterator1 = path.iterator(PathIterator.ConicEvaluation.AsConic)
        val iterator2 = path.iterator(PathIterator.ConicEvaluation.AsConic)
        val iterator3 = path.iterator(PathIterator.ConicEvaluation.AsConic)

        val points = FloatArray(8)
        val points2 = FloatArray(16)

        while (iterator1.hasNext() || iterator2.hasNext() || iterator3.hasNext()) {
            val segment = iterator1.next()
            val type = iterator2.next(points)
            val type2 = iterator3.next(points2, 8)

            assertEquals(type, segment.type)
            assertEquals(type2, segment.type)

            when (type) {
                PathSegment.Type.Move -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points2, 4, segment.points[0])
                }
                PathSegment.Type.Line -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points2, 4, segment.points[0])
                    assertPointsEquals(points2, 5, segment.points[1])
                }
                PathSegment.Type.Quadratic -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points, 2, segment.points[2])
                    assertPointsEquals(points2, 4, segment.points[0])
                    assertPointsEquals(points2, 5, segment.points[1])
                    assertPointsEquals(points2, 6, segment.points[2])
                }
                PathSegment.Type.Conic -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points, 2, segment.points[2])
                    // We store the weight twice
                    assertEquals(points[6], segment.weight)
                    assertEquals(points[7], segment.weight)

                    assertPointsEquals(points2, 4, segment.points[0])
                    assertPointsEquals(points2, 5, segment.points[1])
                    assertPointsEquals(points2, 6, segment.points[2])
                    // We store the weight twice
                    assertEquals(points2[14], segment.weight)
                    assertEquals(points2[15], segment.weight)
                }
                PathSegment.Type.Cubic -> {
                    assertPointsEquals(points, 0, segment.points[0])
                    assertPointsEquals(points, 1, segment.points[1])
                    assertPointsEquals(points, 2, segment.points[2])
                    assertPointsEquals(points, 3, segment.points[3])

                    assertPointsEquals(points2, 4, segment.points[0])
                    assertPointsEquals(points2, 5, segment.points[1])
                    assertPointsEquals(points2, 6, segment.points[2])
                    assertPointsEquals(points2, 7, segment.points[3])
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

            val iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)
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

    @Test
    fun conicAsQuadratics() {
        val path = Path().apply {
            addRoundRect(12.0f, 12.0f, 24.0f, 24.0f, 8.0f, 8.0f, Path.Direction.CW)
        }

        for (segment in path) {
            if (segment.type == PathSegment.Type.Conic) fail("Found conic, none expected: $segment")
        }
    }

    @Test
    fun convertedConics() {
        val path1 = Path().apply {
            addRoundRect(RectF(12.0f, 12.0f, 64.0f, 64.0f), 12.0f, 12.0f, Path.Direction.CW)
        }

        val path2 = Path()
        for (segment in path1) {
            when (segment.type) {
                PathSegment.Type.Move -> path2.moveTo(segment.points[0].x, segment.points[0].y)
                PathSegment.Type.Line -> path2.lineTo(segment.points[1].x, segment.points[1].y)
                PathSegment.Type.Quadratic -> path2.quadTo(
                    segment.points[1].x, segment.points[1].y,
                    segment.points[2].x, segment.points[2].y
                )
                PathSegment.Type.Conic -> fail("Unexpected conic! $segment")
                PathSegment.Type.Cubic -> path2.cubicTo(
                    segment.points[1].x, segment.points[1].y,
                    segment.points[2].x, segment.points[2].y,
                    segment.points[3].x, segment.points[3].y
                )
                PathSegment.Type.Close -> path2.close()
                PathSegment.Type.Done ->  { }
            }
        }

        val b1 = createBitmap(76, 76).applyCanvas {
            drawARGB(255, 255, 255, 255)
            drawPath(path1, Paint().apply {
                color = argb(1.0f, 0.0f, 0.0f, 1.0f)
                strokeWidth = 2.0f
                isAntiAlias = true
                style = Paint.Style.STROKE
            })
        }

        val b2 = createBitmap(76, 76).applyCanvas {
            drawARGB(255, 255, 255, 255)
            drawPath(path2, Paint().apply {
                color = argb(1.0f, 0.0f, 0.0f, 1.0f)
                strokeWidth = 2.0f
                isAntiAlias = true
                style = Paint.Style.STROKE
            })
        }

        compareBitmaps(b1, b2)
    }

    @Test
    fun sizes() {
        val path = Path().apply {
            addRoundRect(RectF(12.0f, 12.0f, 64.0f, 64.0f), 8.0f, 8.0f, Path.Direction.CW)
        }

        // Preserve conics and count
        var iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)
        assertEquals(10, iterator.rawSize())
        assertEquals(iterator.rawSize(), iterator.size())

        // Convert conics and count
        iterator = path.iterator(PathIterator.ConicEvaluation.AsQuadratics)
        assertEquals(10, iterator.rawSize())
        assertEquals(14, iterator.size())
    }
}

fun argb(alpha: Float, red: Float, green: Float, blue: Float) =
    ((alpha * 255.0f + 0.5f).toInt() shl 24) or
    ((red   * 255.0f + 0.5f).toInt() shl 16) or
    ((green * 255.0f + 0.5f).toInt() shl 8) or
     (blue  * 255.0f + 0.5f).toInt()
