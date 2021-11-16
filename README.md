# Pathway

[![pathway](https://maven-badges.herokuapp.com/maven-central/dev.romainguy/pathway/badge.svg?subject=pathway)](https://maven-badges.herokuapp.com/maven-central/dev.romainguy/pathway)

Pathway is an Android library that provides new functionalities around the graphics
[Path](https://developer.android.com/reference/android/graphics/Path) API.

## Maven

```gradle
repositories {
    // ...
    mavenCentral()
}

dependencies {
    implementation 'dev.romainguy:pathway:0.1.0'
}
```

## Roadmap

- Automatically convert conic segments to quadratics

## Iterating over a Path

With Pathway you can easily iterate over a `Path` object to inspect its segments
(curves or commands):

```kotlin
val path = Path().apply {
    // Build path content
}

for (segment in path) {
    val type = path.type // The type of segment (move, cubic, quadratic, line, close, etc.)
    val points = path.points // The points describing the segment geometry
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
