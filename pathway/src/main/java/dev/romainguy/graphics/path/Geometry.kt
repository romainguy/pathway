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

import android.graphics.Path

/**
 * Divides this path into a list of paths. Each contour inside this path is returned as a separate
 * [Path]. For instance the following code snippet creates two rectangular contours:
 *
 * ```
 * val p = Path()
 * p.addRect(…)
 * p.addRect(…)
 * val paths = p.divide()
 * ```
 * The list returned by calling `p.divide()` will contain two `Path` instances, each representing
 * one of the two rectangles.
 *
 * @param paths An optional mutable list of [Path] that will hold the result of the division.
 *
 * @return A list of [Path] representing all the contours in this path. The returned list is either
 * a newly allocated list if the [paths] parameter was left unspecified, or the [paths] parameter.
 */
fun Path.divide(paths: MutableList<Path> = mutableListOf()): List<Path> {
    var path = Path()

    var first = true

    val iterator = iterator()
    val points = FloatArray(8)

    while (iterator.hasNext()) {
        when (iterator.next(points)) {
            PathSegment.Type.Move -> {
                if (!first) {
                    paths.add(path)
                    path = Path()
                }
                first = false
                path.moveTo(points[0], points[1])
            }
            PathSegment.Type.Line -> path.lineTo(points[2], points[3])
            PathSegment.Type.Quadratic -> path.quadTo(
                points[2],
                points[3],
                points[4],
                points[5]
            )
            PathSegment.Type.Conic -> continue // We convert conics to quadratics
            PathSegment.Type.Cubic -> path.cubicTo(
                points[2],
                points[3],
                points[4],
                points[5],
                points[6],
                points[7]
            )
            PathSegment.Type.Close -> path.close()
            PathSegment.Type.Done -> continue // Won't happen inside this loop
        }
    }

    if (!first) paths.add(path)

    return paths
}
