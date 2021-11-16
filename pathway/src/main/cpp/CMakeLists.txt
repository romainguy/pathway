cmake_minimum_required(VERSION 3.18.1)
project("pathway")

add_library(
    pathway
    SHARED
    PathIterator.cpp
    pathway.cpp
)
