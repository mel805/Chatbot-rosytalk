# CMake configuration for llama.cpp integration
# This file will be used to build native libraries for the app

cmake_minimum_required(VERSION 3.22.1)

project("roleplay-ai-native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add llama.cpp as a subdirectory (when integrated)
# add_subdirectory(src/main/cpp/llama.cpp)

# Create our JNI wrapper library
add_library(roleplay-ai-native SHARED
    # JNI interface files
    src/main/cpp/jni_interface.cpp
    # Add more source files as needed
)

# Link libraries
target_link_libraries(roleplay-ai-native
    # Android libraries
    android
    log
    # llama.cpp library (when integrated)
    # llama
)

# Include directories
target_include_directories(roleplay-ai-native PRIVATE
    src/main/cpp
    # src/main/cpp/llama.cpp
)

# Compiler flags for optimization
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(roleplay-ai-native PRIVATE
        -O3
        -DNDEBUG
        -ffast-math
        -funroll-loops
    )
endif()

# ARM NEON optimizations
if(ANDROID_ABI STREQUAL "armeabi-v7a" OR ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(roleplay-ai-native PRIVATE
        -mfpu=neon  # For ARMv7
    )
endif()
