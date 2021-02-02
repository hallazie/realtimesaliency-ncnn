project(fastsal)

cmake_minimum_required(VERSION 3.4.1)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(fastsal SHARED fastsal.cpp)


find_library(
        log-lib
        log)

target_link_libraries(
        fastsal
        ncnn
        jnigraphics
        ${log-lib})