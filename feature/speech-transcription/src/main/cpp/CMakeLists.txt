cmake_minimum_required(VERSION 3.22.1)
project(jetnote_whisper_jni LANGUAGES C CXX)

# Keep the Android build CPU-only and focused on the Whisper inference library.
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)

include(FetchContent)
FetchContent_Declare(
    whisper_cpp
    URL https://github.com/ggml-org/whisper.cpp/archive/d1be6fde11ac6e0407606b4e42fe72d34add8037.tar.gz
)
FetchContent_MakeAvailable(whisper_cpp)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_compile_features(whisper_jni PRIVATE cxx_std_17)
target_link_libraries(whisper_jni PRIVATE whisper log android)
