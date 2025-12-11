cmake_minimum_required(VERSION 3.22.1)

project("roleplayai")

# Configuration C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -frtti -fexceptions -O3 -Wall")

# Trouver les bibliothèques Android
find_library(log-lib log)
find_library(android-lib android)

# Ajouter le wrapper JNI
add_library(llama-android SHARED
    app/src/main/cpp/llama-android.cpp
)

# TODO: Pour intégration complète de llama.cpp, décommenter et ajouter:
# 1. Cloner llama.cpp dans app/src/main/cpp/llama.cpp/
# 2. Ajouter les fichiers source llama.cpp:
#
# set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/app/src/main/cpp/llama.cpp)
# 
# add_library(llama-cpp STATIC
#     ${LLAMA_CPP_DIR}/ggml.c
#     ${LLAMA_CPP_DIR}/ggml-alloc.c
#     ${LLAMA_CPP_DIR}/ggml-backend.c
#     ${LLAMA_CPP_DIR}/ggml-quants.c
#     ${LLAMA_CPP_DIR}/llama.cpp
#     ${LLAMA_CPP_DIR}/common/common.cpp
#     ${LLAMA_CPP_DIR}/common/sampling.cpp
# )
#
# target_include_directories(llama-cpp PUBLIC
#     ${LLAMA_CPP_DIR}
#     ${LLAMA_CPP_DIR}/common
# )
#
# # Support ARM NEON (accélération CPU)
# target_compile_options(llama-cpp PRIVATE
#     -march=armv8-a+fp+simd
#     -DGGML_USE_ARM_NEON
# )

# Lier les bibliothèques
target_link_libraries(llama-android
    ${log-lib}
    ${android-lib}
    # llama-cpp  # Décommenter après intégration llama.cpp
)

# Headers publics
target_include_directories(llama-android PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/app/src/main/cpp
)

# Optimisations pour Android
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(llama-android PRIVATE
        GGML_USE_ARM_NEON=1
    )
endif()

# Log de configuration
message(STATUS "Android ABI: ${ANDROID_ABI}")
message(STATUS "Android Platform: ${ANDROID_PLATFORM}")
message(STATUS "Android NDK: ${ANDROID_NDK}")
