# Pathway

[![pathway](https://maven-badges.herokuapp.com/maven-central/dev.romainguy/pathway/badge.svg?subject=pathway)](https://maven-badges.herokuapp.com/maven-central/dev.romainguy/pathway)
[![Android build status](https://github.com/romainguy/pathway/workflows/Android/badge.svg)](https://github.com/romainguy/pathway/actions?query=workflow%3AAndroid)

Pathway is an Android library that provides new functionalities around the graphics
[Path](https://developer.android.com/reference/android/graphics/Path) API.

Pathway is compatible with API 21+.

## Maven

```gradle
repositories {
    // ...
    mavenCentral()
}

dependencies {
    implementation 'dev.romainguy:pathway:0.11.0'
}
```

## Features

- [Paths from images](#paths-from-images)
- [Path division](#path-division)
- [Convert to SVG](#convert-to-svg)
- [Iterating over a Path](#iterating-over-a-path)

## Paths from images

`Bitmap.toPath()` and `Bitmap.toPaths()` can be used to extract vector contours from images, as
`Path` object. `toPath()` extracts all the contours in a single `Path` while `toPaths()` returns
a list of contours as separate `Path` instances. Calling `toPaths()` is equivalent to calling
`toPath().divide()` (see [Path division](#path-division)) but more efficient.

When extracting a path from an image, two parameters can be set:
- `alphaTreshold`: defines the maximum alpha channel value a pixel might have before being
  considered opaque. Transitions from opaque to transparent are used to define the contours
  in the image. The default value is 0.0f (meaning any pixel with an alpha > 0.0 is considered
  to be inside the contour).
- `minAngle`: defines the minimum angle in degrees between two segments in the contour before
  they are collapsed to simplify the final geometry. The default value is 15 degrees. Setting
  this value to 0 will yield an exact vector representation of the contours but will generate
  complex and expensive paths.

## Path division

Path division can be used to generate a list of paths from a source path. Each contour, defined
by a "move" operation, in the source path is extracted as a separate `Path`. In the following
example the `paths` variable contains a list of 2 `Path` instance, each containing one of the
rectangles originally added to the source `path`:

```kotlin
val path = Path().apply {
    addRect(0.0f, 0.0f, 24.0f, 24.0f)
    addRect(32.0f, 32.0f, 64.0f, 64.0f)
}
val paths = path.divide()
```

## Convert to SVG

To convert a `Path` to an SVG document, call `Path.toSvg()`. If you only want the path data instead
of a full SVG document, use `Path.toSvg(document = false)` instead. Exporting a full document will
properly honor the path's fill type.

## Iterating over a Path

> **Note**
> As of Android 14 (tentatively API 34), iterating over a `Path` can be achieved using the new
> platform API [getPathIterator()](https://developer.android.com/reference/android/graphics/Path#getPathIterator()).
> Pathway is however compatible with Android 14, including Developer Preview builds. You can
> also use the new androidx
> [graphics-path library](https://developer.android.com/jetpack/androidx/releases/graphics#graphics-path-1.0.0-alpha01).

With Pathway you can easily iterate over a `Path` object to inspect its segments
(curves or commands):

```kotlin
val path = Path().apply {
    // Build path content
}

for (segment in path) {
    val type = segment.type // The type of segment (move, cubic, quadratic, line, close, etc.)
    val points = segment.points // The points describing the segment geometry
}
```

This type of iteration is easy to use but may create an allocation per segment iterated over.
If you must avoid allocations, Pathway provides a lower-level API to do so:

```kotlin
val path = Path().apply {
    // Build path content
}

val iterator = path.iterator
val points = FloatArray(8)

while (iterator.hasNext()) {
    val type = iterator.next(points) // The type of segment
    // Read the segment geometry from the points array depending on the type
}

```

### Path segments

Each segment in a `Path` can be of one of the following types:

#### Move

Move command. The path segment contains 1 point indicating the move destination.
The weight is set 0.0f and not meaningful.

#### Line

Line curve. The path segment contains 2 points indicating the two extremities of
the line. The weight is set 0.0f and not meaningful.

#### Quadratic

Quadratic curve. The path segment contains 3 points in the following order:
- Start point
- Control point
- End point

The weight is set 0.0f and not meaningful.

#### Conic

Conic curve. The path segment contains 3 points in the following order:
- Start point
- Control point
- End point

The curve is weighted by the `PathSegment.weight` property.

Conic curves are automatically converted to quadratic curves by default, see
[Handling conic segments](#handling-conic-segments) below for more information.

#### Cubic

Cubic curve. The path segment contains 4 points in the following order:
- Start point
- First control point
- Second control point
- End point

The weight is set 0.0f and not meaningful.

#### Close

Close command. Close the current contour by joining the last point added to the
path with the first point of the current contour. The segment does not contain
any point. The weight is set 0.0f and not meaningful.

#### Done

Done command. This optional command indicates that no further segment will be
found in the path. It typically indicates the end of an iteration over a path
and can be ignored.

### Handling conic segments

In some API levels, paths may contain conic curves (weighted quadratics) but the
`Path` API does not offer a way to add conics to a `Path` object. To work around
this, Pathway automatically converts conics into several quadratics by default.

The conic to quadratic conversion is an approximation controlled by a tolerance
threshold, set by default to 0.25f (sub-pixel). If you want to preserve conics
or control the tolerance, you can use the following APIs:

```kotlin
// Preserve conics
val iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)

// Control the tolerance of the conic to quadratic conversion
val iterator = path.iterator(PathIterator.ConicEvaluation.AsQuadratics, 2.0f)

```

## License

Please see LICENSE.
