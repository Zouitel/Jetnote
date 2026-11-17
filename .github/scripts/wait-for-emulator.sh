#!/usr/bin/env bash
set -euo pipefail

SERIAL="${ANDROID_SERIAL:-emulator-5554}"
adb devices -l
adb reconnect offline || true

READY=false
for attempt in $(seq 1 90); do
    STATE="$(adb -s "$SERIAL" get-state 2>/dev/null || true)"
    if [[ "$STATE" == "device" ]]; then
        BOOT_COMPLETED="$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
        if [[ "$BOOT_COMPLETED" == "1" ]]; then
            READY=true
            break
        fi
    fi
    echo "Waiting for $SERIAL (state=$STATE, boot_completed=${BOOT_COMPLETED:-unknown})"
    sleep 5
done

if [[ "$READY" != "true" ]]; then
    echo "Emulator did not become ready. Final ADB diagnostics:"
    adb devices -l
    adb -s "$SERIAL" get-state || true
    adb -s "$SERIAL" shell getprop sys.boot_completed || true
    exit 1
fi

adb -s "$SERIAL" shell settings put global window_animation_scale 0
adb -s "$SERIAL" shell settings put global transition_animation_scale 0
adb -s "$SERIAL" shell settings put global animator_duration_scale 0
