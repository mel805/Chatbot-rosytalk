cmake_minimum_required(VERSION 3.22.1)

project("roleplay-ai-native")

# Configuration pour Android
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

# Désactiver les warnings pour llama.cpp
add_compile_options(-w)

# Flags d'optimisation pour Android (AVEC exceptions pour llama.cpp)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fexceptions -frtti")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2")

# Support ARM NEON sur ARM
if(${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfpu=neon")
endif()

# Dossiers llama.cpp
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/llama.cpp)
set(GGML_DIR ${LLAMA_DIR}/ggml)

# Sources GGML (moteur de calcul + backends)
file(GLOB GGML_SOURCES
    ${GGML_DIR}/src/ggml.c
    ${GGML_DIR}/src/ggml-alloc.c
    ${GGML_DIR}/src/ggml-quants.c
    ${GGML_DIR}/src/ggml-backend.cpp
    ${GGML_DIR}/src/ggml-backend-reg.cpp
    ${GGML_DIR}/src/ggml-cpu/ggml-cpu.cpp
    ${GGML_DIR}/src/ggml-threading.cpp
)

# Sources llama.cpp principales (compilées sélectivement)
file(GLOB LLAMA_CORE_SOURCES
    ${LLAMA_DIR}/src/llama.cpp
    ${LLAMA_DIR}/src/llama-vocab.cpp
    ${LLAMA_DIR}/src/llama-grammar.cpp
    ${LLAMA_DIR}/src/llama-sampling.cpp
    ${LLAMA_DIR}/src/llama-context.cpp
    ${LLAMA_DIR}/src/llama-batch.cpp
    ${LLAMA_DIR}/src/llama-model.cpp
    ${LLAMA_DIR}/src/llama-model-loader.cpp
    ${LLAMA_DIR}/src/llama-io.cpp
    ${LLAMA_DIR}/src/llama-impl.cpp
    ${LLAMA_DIR}/src/llama-arch.cpp
    ${LLAMA_DIR}/src/llama-kv-cache.cpp
    ${LLAMA_DIR}/src/llama-memory.cpp
    ${LLAMA_DIR}/src/llama-mmap.cpp
    ${LLAMA_DIR}/src/llama-hparams.cpp
    ${LLAMA_DIR}/src/llama-cparams.cpp
    ${LLAMA_DIR}/src/unicode.cpp
    ${LLAMA_DIR}/src/unicode-data.cpp
)

# Sources JNI
set(JNI_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/jni_interface.cpp
)

# Inclure les headers
include_directories(
    ${LLAMA_DIR}/src
    ${LLAMA_DIR}/include
    ${GGML_DIR}/include
    ${GGML_DIR}/src
    ${GGML_DIR}/src/ggml-cpu
    ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp
)

# Créer la bibliothèque native
add_library(${CMAKE_PROJECT_NAME} SHARED
    ${GGML_SOURCES}
    ${LLAMA_CORE_SOURCES}
    ${JNI_SOURCES}
)

# Lier avec les bibliothèques Android
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(${CMAKE_PROJECT_NAME}
    ${log-lib}
    ${android-lib}
)

# Définitions pour llama.cpp
target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
    GGML_USE_CPU
    NDEBUG
    GGML_VERSION="1.0.0"
    GGML_COMMIT="android-build"
)

# Exporter les symboles JNI
set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
    LINK_FLAGS "-Wl,--export-dynamic"
)
