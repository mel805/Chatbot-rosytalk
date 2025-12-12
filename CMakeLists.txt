cmake_minimum_required(VERSION 3.22.1)

project("roleplayai")

# Configuration C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -frtti -fexceptions -O3 -Wall")

# Trouver les bibliothèques Android
find_library(log-lib log)
find_library(android-lib android)

# llama.cpp (optionnel)
# - La source n'est pas versionnée dans ce dépôt pour éviter un énorme diff.
# - Le CI la récupère et la place dans app/src/main/cpp/llama.cpp (voir workflow).
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/app/src/main/cpp/llama.cpp)

if(EXISTS "${LLAMA_CPP_DIR}/CMakeLists.txt")
    message(STATUS "✅ llama.cpp trouvé dans: ${LLAMA_CPP_DIR}")

    # Désactiver tout ce qui est inutile sur Android (on ne veut que la lib)
    set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
    set(LLAMA_HTTPLIB OFF CACHE BOOL "" FORCE)

    # Ajouter llama.cpp comme sous-projet
    add_subdirectory(${LLAMA_CPP_DIR} llama_cpp_build)

    set(LLAMA_AVAILABLE TRUE)
else()
    message(WARNING "⚠️ llama.cpp non trouvé - compilation sans llama.cpp (fallback)")
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
    
    # Headers nécessaires pour inclure llama.h
    target_include_directories(llama-android PRIVATE
        ${LLAMA_CPP_DIR}/include
        ${LLAMA_CPP_DIR}/ggml/include
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
