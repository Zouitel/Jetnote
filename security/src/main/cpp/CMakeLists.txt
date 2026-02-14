cmake_minimum_required(VERSION 3.22.1)

project("nativesecurity")

# Add your C++ files
add_library(
        native-lib
        SHARED
        native-lib.cpp
        string_obfuscator.cpp
)

# Link Android logging library
find_library(log-lib log)

target_link_libraries(
        native-lib
        ${log-lib}
)