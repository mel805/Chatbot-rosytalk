cmake_minimum_required(VERSION 3.22.1)

project("roleplayai")

# Configuration C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -frtti -fexceptions -O3 -Wall")

# Trouver les bibliothèques Android
find_library(log-lib log)
find_library(android-lib android)

# Configuration llama.cpp
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/app/src/main/cpp/llama.cpp)

# Vérifier si llama.cpp existe
if(EXISTS "${LLAMA_CPP_DIR}/ggml.c")
    message(STATUS "✅ llama.cpp trouvé dans: ${LLAMA_CPP_DIR}")
    
    # Ajouter les sources principales de llama.cpp
    file(GLOB LLAMA_SOURCES
        "${LLAMA_CPP_DIR}/ggml.c"
        "${LLAMA_CPP_DIR}/ggml-alloc.c"
        "${LLAMA_CPP_DIR}/ggml-backend.c"
        "${LLAMA_CPP_DIR}/ggml-quants.c"
        "${LLAMA_CPP_DIR}/llama.cpp"
    )
    
    # Créer une bibliothèque statique llama
    add_library(llama STATIC ${LLAMA_SOURCES})
    
    # Configuration pour Android ARM
    target_compile_definitions(llama PRIVATE
        GGML_USE_CPU
    )
    
    # Optimisations ARM NEON pour ARM64
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        target_compile_definitions(llama PRIVATE
            GGML_USE_ARM_NEON=1
        )
        target_compile_options(llama PRIVATE
            -march=armv8-a+fp+simd
        )
    endif()
    
    # Inclure les headers llama.cpp
    target_include_directories(llama PUBLIC
        ${LLAMA_CPP_DIR}
    )
    
    set(LLAMA_AVAILABLE TRUE)
else()
    message(WARNING "⚠️ llama.cpp non trouvé - compilation sans llama.cpp")
    set(LLAMA_AVAILABLE FALSE)
endif()

# Ajouter le wrapper JNI
add_library(llama-android SHARED
    app/src/main/cpp/llama-android.cpp
)

# Lier les bibliothèques
if(LLAMA_AVAILABLE)
    target_link_libraries(llama-android
        llama
        ${log-lib}
        ${android-lib}
    )
    
    # Inclure les headers dans notre wrapper
    target_include_directories(llama-android PRIVATE
        ${LLAMA_CPP_DIR}
    )
    
    target_compile_definitions(llama-android PRIVATE
        LLAMA_CPP_AVAILABLE=1
    )
else()
    target_link_libraries(llama-android
        ${log-lib}
        ${android-lib}
    )
endif()

# Headers publics
target_include_directories(llama-android PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/app/src/main/cpp
)

# Log de configuration
message(STATUS "Android ABI: ${ANDROID_ABI}")
message(STATUS "Android Platform: ${ANDROID_PLATFORM}")
message(STATUS "Android NDK: ${ANDROID_NDK}")
message(STATUS "llama.cpp disponible: ${LLAMA_AVAILABLE}")
