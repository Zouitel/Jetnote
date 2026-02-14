#include <jni.h>
#include <string>
#include <android/log.h>
#include "encryption.h"
#include "string_obfuscator.h"

#define LOG_TAG "NativeSecurity"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

// Simple XOR encryption
//JNIEXPORT jstring JNICALL
//
//Java_zouitel_neighborhood_security_NativeSecurityManager_encryptData(
//        JNIEnv* env,
//        jobject /* this */,
//        jstring data) {
//
//    const char* nativeData = env->GetStringUTFChars(data, nullptr);
//    std::string encrypted = encryptXOR(nativeData);
//    env->ReleaseStringUTFChars(data, nativeData);
//
//    return env->NewStringUTF(encrypted.c_str());
//}

// Decrypt data
//JNIEXPORT jstring JNICALL
//Java_zouitel_neighborhood_security_NativeSecurityManager_decryptData(
//        JNIEnv* env,
//        jobject /* this */,
//        jstring encryptedData) {
//
//    const char* nativeEncrypted = env->GetStringUTFChars(encryptedData, nullptr);
//    std::string decrypted = decryptXOR(nativeEncrypted);
//    env->ReleaseStringUTFChars(encryptedData, nativeEncrypted);
//
//    return env->NewStringUTF(decrypted.c_str());
//}

// Get obfuscated API key (stored in native code)
JNIEXPORT jstring JNICALL
Java_zouitel_neighborhood_security_NativeSecurityManager_googleClientId(
        JNIEnv* env,
        jobject /* this */) {

    // Obfuscated key stored as byte array
    unsigned char obfuscated[] = {
            0x6d, 0x63, 0x63, 0x62, 0x64, 0x67, 0x6d, 0x62, 0x6c, 0x66, 0x61, 0x63, 0x78, 0x39,
            0x26, 0x24, 0x3c, 0x21, 0x61, 0x21, 0x3d, 0x20, 0x33, 0x3b, 0x3e, 0x36, 0x6d, 0x39,
            0x31, 0x3b, 0x63, 0x21, 0x20, 0x27, 0x60, 0x33, 0x24, 0x39, 0x3b, 0x25, 0x3d, 0x39,
            0x24, 0x36, 0x63, 0x7b, 0x34, 0x25, 0x25, 0x26, 0x7b, 0x32, 0x3a, 0x3a, 0x32, 0x39,
            0x30, 0x20, 0x26, 0x30, 0x27, 0x36, 0x3a, 0x3b, 0x21, 0x30, 0x3b, 0x21, 0x7b, 0x36,
            0x3a, 0x38
    };

    std::string key = deobfuscateString(obfuscated, sizeof(obfuscated));

    return env->NewStringUTF(key.c_str());
}

// Validate app signature (anti-tampering)
//JNIEXPORT jboolean JNICALL
//Java_zouitel_neighborhood_security_NativeSecurityManager_validateSignature(
//        JNIEnv* env,
//        jobject /* this */,
//        jstring signature) {
//
//    const char* sig = env->GetStringUTFChars(signature, nullptr);
//
//    // Your actual app signature hash (hardcoded and obfuscated)
//    const char* expectedSig = " 78:1B:62:FC:71:18:76:3C:7A:15:9C:4C:20:7A:9E:85:9A:44:89:E0:4E:AC:CB:50:DE:41:63:E0:B5:DA:13:51";
//    bool isValid = (strcmp(sig, expectedSig) == 0);
//
//    env->ReleaseStringUTFChars(signature, sig);
//    return isValid;
//}

// Complex calculation that you want to protect
//JNIEXPORT jint JNICALL
//Java_zouitel_neighborhood_security_NativeSecurityManager_calculateLicenseKey(
//        JNIEnv* env,
//        jobject /* this */,
//        jstring deviceId,
//        jlong timestamp) {
//
//    const char* device = env->GetStringUTFChars(deviceId, nullptr);
//
//    // Your secret algorithm here
//    int hash = 0;
//    for (int i = 0; device[i] != '\0'; i++) {
//        hash = ((hash << 5) + hash) + device[i];
//    }
//    hash ^= (int)(timestamp & 0xFFFFFFFF);
//
//    env->ReleaseStringUTFChars(deviceId, device);
//    return hash;
//}

} // extern "C"