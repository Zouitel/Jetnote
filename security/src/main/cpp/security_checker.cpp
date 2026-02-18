#include "security_checker.h"
#include <string>
#include <fstream>
#include <sys/stat.h>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "SecurityChecker"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ========================================
// EMULATOR DETECTION
// ========================================

bool checkEmulatorFiles() {
    const char* emulatorFiles[] = {
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
    };

    for (const char* file : emulatorFiles) {
        if (access(file, F_OK) == 0) {
            return true;
        }
    }

    return false;
}

bool checkSystemProperties() {
    // Check build properties stored in system
    std::ifstream propFile("/system/build.prop");
    if (propFile.is_open()) {
        std::string line;
        while (std::getline(propFile, line)) {
            if (line.find("ro.hardware=goldfish") != std::string::npos ||
                line.find("ro.hardware=ranchu") != std::string::npos ||
                line.find("ro.kernel.qemu=1") != std::string::npos) {
                propFile.close();
                return true;
            }
        }
        propFile.close();
    }

    return false;
}

bool isEmulatorNative() {
    return checkEmulatorFiles() || checkSystemProperties();
}

// ========================================
// DEBUGGER DETECTION
// ========================================

bool checkTracerPid() {
    std::ifstream statusFile("/proc/self/status");
    if (statusFile.is_open()) {
        std::string line;
        while (std::getline(statusFile, line)) {
            if (line.find("TracerPid:") == 0) {
                int tracerPid = std::stoi(line.substr(10));
                statusFile.close();
                return tracerPid != 0;
            }
        }
        statusFile.close();
    }
    return false;
}

bool checkWchan() {
    std::ifstream wchanFile("/proc/self/wchan");
    if (wchanFile.is_open()) {
        std::string wchan;
        std::getline(wchanFile, wchan);
        wchanFile.close();

        return wchan.find("ptrace") != std::string::npos ||
               wchan.find("sys_ptrace") != std::string::npos;
    }
    return false;
}

bool isDebuggerAttachedNative() {
    return checkTracerPid() || checkWchan();
}

// ========================================
// ROOT DETECTION
// ========================================

bool checkSuBinaries() {
    const char* suPaths[] = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
    };

    for (const char* path : suPaths) {
        if (access(path, F_OK) == 0) {
            return true;
        }
    }

    return false;
}

bool checkMagisk() {
    const char* magiskPaths[] = {
            "/sbin/.magisk",
            "/sbin/magisk",
            "/data/adb/magisk"
    };

    for (const char* path : magiskPaths) {
        if (access(path, F_OK) == 0) {
            return true;
        }
    }

    return false;
}

bool isRootedNative() {
    return checkSuBinaries() || checkMagisk();
}

// ========================================
// JNI EXPORTS
// ========================================

extern "C" {

JNIEXPORT jboolean JNICALL
Java_zouitel_neighborhood_security_NativeSecurityManager_isEmulatorNative(
        JNIEnv* env,
        jobject) {
    return isEmulatorNative();
}

JNIEXPORT jboolean JNICALL
Java_zouitel_neighborhood_security_NativeSecurityManager_isDebuggerAttachedNative(
        JNIEnv* env,
        jobject) {
    return isDebuggerAttachedNative();
}

JNIEXPORT jboolean JNICALL
Java_zouitel_neighborhood_security_NativeSecurityManager_isRootedNative(
        JNIEnv* env,
        jobject) {
    return isRootedNative();
}

} // extern "C"