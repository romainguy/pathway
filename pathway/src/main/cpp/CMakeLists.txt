cmake_minimum_required(VERSION 3.18.1)
project("pathway")

add_library(
    pathway
    SHARED
    Conic.cpp
    PathIterator.cpp
    pathway.cpp
)

set(VERSION_SCRIPT "${CMAKE_CURRENT_SOURCE_DIR}/libpathway.map")
target_link_options(
    pathway
    PRIVATE
    "-Wl,--version-script=${VERSION_SCRIPT}"
)
