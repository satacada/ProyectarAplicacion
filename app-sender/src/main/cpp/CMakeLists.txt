cmake_minimum_required(VERSION 3.22.1)
project("stream_classifier" CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(stream_classifier SHARED
    stream_classifier.cpp
    dean_edwards_unpacker.cpp
    ad_pattern_matcher.cpp
)

find_library(log-lib log)

target_link_libraries(stream_classifier
    ${log-lib}
)
