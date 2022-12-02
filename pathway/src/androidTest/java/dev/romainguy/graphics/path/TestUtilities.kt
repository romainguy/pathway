/*
 * Copyright (C) 2022 Romain Guy
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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.abs

fun assertPointsEquals(p1: PointF, p2: PointF) {
    assertEquals(p1.x, p2.x, 1e-6f)
    assertEquals(p1.y, p2.y, 1e-6f)
}

fun assertPointsEquals(p1: FloatArray, offset: Int, p2: PointF) {
    assertEquals(p1[0 + offset * 2], p2.x, 1e-6f)
    assertEquals(p1[1 + offset * 2], p2.y, 1e-6f)
}

fun compareBitmaps(b1: Bitmap, b2: Bitmap, error: Int = 1) {
    assertEquals(b1.width, b2.width)
    assertEquals(b1.height, b2.height)

    val p1 = IntArray(b1.width * b1.height)
    b1.getPixels(p1, 0, b1.width, 0, 0, b1.width, b1.height)

    val p2 = IntArray(b2.width * b2.height)
    b2.getPixels(p2, 0, b2.width, 0, 0, b2.width, b2.height)

    for (x in 0 until b1.width) {
        for (y in 0 until b2.width) {
            val index = y * b1.width + x

            val c1 = p1[index]
            val c2 = p2[index]

            assertTrue(abs(Color.red(c1) - Color.red(c2)) <= error)
            assertTrue(abs(Color.green(c1) - Color.green(c2)) <= error)
            assertTrue(abs(Color.blue(c1) - Color.blue(c2)) <= error)
        }
    }
}

fun assertPathEquals(
    expected: Path,
    actual: Path,
    points1: FloatArray = FloatArray(8),
    points2: FloatArray = FloatArray(8)
) {
    val iterator1 = expected.iterator()
    val iterator2 = actual.iterator()

    assertEquals(iterator1.size(), iterator2.size())

    while (iterator1.hasNext() && iterator2.hasNext()) {
        val type1 = iterator1.next(points1)
        val type2 = iterator2.next(points2)
        assertEquals(type1, type2)
        Assert.assertArrayEquals(points1, points2, 1e-10f)
    }
}
