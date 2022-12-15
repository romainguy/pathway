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

@file:JvmName("Svg")

package dev.romainguy.graphics.path

import android.graphics.Path
import android.graphics.RectF
import dev.romainguy.graphics.path.PathSegment.Type

fun Path.toSvg(document: Boolean = true) = buildString {
    val bounds = RectF()
    this@toSvg.computeBounds(bounds, true)

    if (document) {
        append("""<svg xmlns="http://www.w3.org/2000/svg" """)
        appendLine("""viewBox="${bounds.left} ${bounds.top} ${bounds.width()} ${bounds.height()}">""")
    }

    val iterator = this@toSvg.iterator()
    val points = FloatArray(8)
    var lastType = Type.Done

    if (iterator.hasNext()) {
        if (document) {
            if (this@toSvg.fillType == Path.FillType.EVEN_ODD) {
                append("""  <path fill-rule="evenodd" d="""")
            } else {
                append("""  <path d="""")
            }
        }

        while (iterator.hasNext()) {
            val type = iterator.next(points)
            when (type) {
                Type.Move -> {
                    append("${command(Type.Move, lastType)}${points[0]} ${points[1]}")
                }
                Type.Line -> {
                    append("${command(Type.Line, lastType)}${points[2]} ${points[3]}")
                }
                Type.Quadratic -> {
                    append(command(Type.Quadratic, lastType))
                    append("${points[2]} ${points[3]} ${points[4]} ${points[5]}")
                }
                Type.Conic -> continue // We convert conics to quadratics
                Type.Cubic -> {
                    append(command(Type.Cubic, lastType))
                    append("${points[2]} ${points[3]} ")
                    append("${points[4]} ${points[5]} ")
                    append("${points[6]} ${points[7]}")
                }
                Type.Close -> {
                    append(command(Type.Close, lastType))
                }
                Type.Done -> continue // Won't happen inside this loop
            }
            lastType = type
        }

        if (document) {
            appendLine(""""/>""")
        }
    }
    if (document) {
        appendLine("""</svg>""")
    }
}

private fun command(type: Type, lastType: Type) =
    if (type != lastType) {
        when (type) {
            Type.Move -> "M"
            Type.Line -> "L"
            Type.Quadratic -> "Q"
            Type.Cubic -> "C"
            Type.Close -> "Z"
            else -> ""
        }
    } else " "
