cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/include/ncnn)

set(ncnn_lib ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/lib/libncnn.a)
add_library (ncnn_lib STATIC IMPORTED)
set_target_properties(ncnn_lib PROPERTIES IMPORTED_LOCATION ${ncnn_lib})

add_library( # Sets the name of the library.
        fastsal
        SHARED
        fastsal.cpp)


find_library(
        log-lib
        log)

target_link_libraries(
        fastsal
        ncnn_lib
        jnigraphics
        ${log-lib})