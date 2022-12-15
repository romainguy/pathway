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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SvgTest {
    @Test
    fun emptyPath() {
        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 0.0 0.0">
            </svg>

            """.trimIndent(),
            Path().toSvg()
        )

        assertTrue(Path().toSvg(document = false).isEmpty())
    }

    @Test
    fun singleMove() {
        val svg = Path().apply { moveTo(10.0f, 10.0f) }.toSvg()
        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="10.0 10.0 0.0 0.0">
              <path d="M10.0 10.0"/>
            </svg>

            """.trimIndent(),
            svg
        )
    }

    @Test
    fun twoPaths() {
        val svg = Path().apply {
            addRect(0.0f, 0.0f, 10.0f, 10.0f, Path.Direction.CW)
            addRect(20.0f, 20.0f, 50.0f, 50.0f, Path.Direction.CW)
        }.toSvg()

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 50.0 50.0">
              <path d="M0.0 0.0L10.0 0.0 10.0 10.0 0.0 10.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z"/>
            </svg>

            """.trimIndent(),
            svg
        )
    }

    @Test
    fun dataOnly() {
        val svg = Path().apply {
            addRect(0.0f, 0.0f, 10.0f, 10.0f, Path.Direction.CW)
            addRect(20.0f, 20.0f, 50.0f, 50.0f, Path.Direction.CW)
        }.toSvg(document = false)

        assertEquals(
            "M0.0 0.0L10.0 0.0 10.0 10.0 0.0 10.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z",
            svg
        )
    }

    @Test
    fun curves() {
        val svg = Path().apply {
            addCircle(36.0f, 36.0f, 16.0f, Path.Direction.CW)
        }.toSvg()

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="20.0 20.0 32.0 32.0">
              <path d="M52.0 36.0Q51.999992 42.62741 47.3137 47.3137 42.62741 51.999992 36.0 52.0 29.372581 51.999992 24.68629 47.3137 19.999998 42.62741 20.0 36.0 19.999998 29.372581 24.68629 24.68629 29.372581 20.0 36.0 20.0 42.62741 20.0 47.3137 24.68629 51.999992 29.372581 52.0 36.0Z"/>
            </svg>

            """.trimIndent(),
            svg
        )
    }

    @Test
    fun donuts() {
        val donut = Path().apply {
            addCircle(36.0f, 36.0f, 18.0f, Path.Direction.CW)
            addCircle(36.0f, 36.0f, 8.0f, Path.Direction.CW)
        }

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="18.0 18.0 36.0 36.0">
              <path d="M54.0 36.0Q53.999992 39.580418 52.62982 42.888294 51.25965 46.19617 48.727917 48.727917 46.19617 51.25965 42.888294 52.62982 39.580418 53.999992 36.0 54.0 32.419575 53.999992 29.111696 52.62982 25.803814 51.25965 23.272076 48.727917 20.740335 46.19617 19.370167 42.888294 17.999998 39.580418 18.0 36.0 17.999998 32.419575 19.370167 29.111696 20.740335 25.803814 23.272076 23.272076 25.803814 20.740335 29.111694 19.370167 32.419575 18.0 36.0 18.0 39.580418 18.0 42.888294 19.370167 46.19617 20.740335 48.727917 23.272076 51.25965 25.803814 52.62982 29.111694 53.999992 32.419575 54.0 36.0ZM44.0 36.0Q44.0 39.31371 41.656853 41.656853 39.31371 44.0 36.0 44.0 32.686287 44.0 30.343143 41.656853 27.999998 39.31371 28.0 36.0 27.999998 32.686287 30.343143 30.343143 32.686287 28.0 36.0 28.0 39.31371 28.0 41.656853 30.343143 44.0 32.686287 44.0 36.0Z"/>
            </svg>

            """.trimIndent(),
            donut.toSvg()
        )

        donut.fillType = Path.FillType.EVEN_ODD

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="18.0 18.0 36.0 36.0">
              <path fill-rule="evenodd" d="M54.0 36.0Q53.999992 39.580418 52.62982 42.888294 51.25965 46.19617 48.727917 48.727917 46.19617 51.25965 42.888294 52.62982 39.580418 53.999992 36.0 54.0 32.419575 53.999992 29.111696 52.62982 25.803814 51.25965 23.272076 48.727917 20.740335 46.19617 19.370167 42.888294 17.999998 39.580418 18.0 36.0 17.999998 32.419575 19.370167 29.111696 20.740335 25.803814 23.272076 23.272076 25.803814 20.740335 29.111694 19.370167 32.419575 18.0 36.0 18.0 39.580418 18.0 42.888294 19.370167 46.19617 20.740335 48.727917 23.272076 51.25965 25.803814 52.62982 29.111694 53.999992 32.419575 54.0 36.0ZM44.0 36.0Q44.0 39.31371 41.656853 41.656853 39.31371 44.0 36.0 44.0 32.686287 44.0 30.343143 41.656853 27.999998 39.31371 28.0 36.0 27.999998 32.686287 30.343143 30.343143 32.686287 28.0 36.0 28.0 39.31371 28.0 41.656853 30.343143 44.0 32.686287 44.0 36.0Z"/>
            </svg>

            """.trimIndent(),
            donut.toSvg()
        )
    }
}
