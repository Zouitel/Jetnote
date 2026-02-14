#include "string_obfuscator.h"

std::string deobfuscateString(const unsigned char* obfuscated, size_t length) {
    std::string result;
    const unsigned char mask = 0x55; // Simple mask

    for (size_t i = 0; i < length; i++) {
        result += (char)(obfuscated[i] ^ mask);
    }

    return result;
}