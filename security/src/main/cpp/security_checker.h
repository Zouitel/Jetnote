#ifndef SECURITY_CHECKER_H
#define SECURITY_CHECKER_H

#include <jni.h>

// Emulator detection
bool isEmulatorNative();

// Debugger detection
bool isDebuggerAttachedNative();

// Root detection
bool isRootedNative();

#endif