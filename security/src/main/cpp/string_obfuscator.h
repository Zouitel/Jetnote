#ifndef NATIVESECURITY_STRING_OBFUSCATOR_H
#define NATIVESECURITY_STRING_OBFUSCATOR_H

#include <string>

std::string deobfuscateString(const unsigned char* obfuscated, size_t length);

#endif