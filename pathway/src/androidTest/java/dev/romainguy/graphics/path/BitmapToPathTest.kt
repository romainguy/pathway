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

import android.graphics.*
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapToPathTest {
    @Test
    fun transparentBitmap() {
        assertTrue(createBitmap(10, 10).toPath().isEmpty)
    }

    @Test
    fun opaqueBitmap() {
        assertPathEquals(
            Path().apply { addRect(0.0f, 0.0f, 10.0f, 10.0f, Path.Direction.CCW) },
            createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { setHasAlpha(false) }.toPath()
        )
    }

    @Test
    fun opaqueByDrawBitmap() {
        val bitmap = createBitmap(10, 10).applyCanvas {
            drawColor(0xffff0000.toInt())
        }

        val result = bitmap.toPath()

        var count = 0
        var lines = 0
        var moves = 0

        for (segment in result) {
            count++
            when (segment.type) {
                PathSegment.Type.Move -> moves++
                PathSegment.Type.Line -> lines++
                else -> fail()
            }
        }

        assertEquals(5, count)
        assertEquals(4, lines)
        assertEquals(1, moves)
    }

    @Test
    fun multiplePaths() {
        val bitmap = createBitmap(50, 50).applyCanvas {
            drawRect(2.0f, 2.0f, 8.0f, 8.0f, Paint())
            drawRect(20.0f, 20.0f, 28.0f, 28.0f, Paint())
        }

        val paths = bitmap.toPaths()
        assertEquals(2, paths.size)
    }
}
