#ifndef NATIVESECURITY_ENCRYPTION_H
#define NATIVESECURITY_ENCRYPTION_H

#include <string>

std::string encryptXOR(const std::string& data);
std::string decryptXOR(const std::string& data);

#endif