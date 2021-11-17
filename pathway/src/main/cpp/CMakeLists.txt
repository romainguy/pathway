cmake_minimum_required(VERSION 3.18.1)
project("pathway")

add_library(
    pathway
    SHARED
    Conic.cpp
    PathIterator.cpp
    pathway.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    pathway
    ${log-lib}
)
