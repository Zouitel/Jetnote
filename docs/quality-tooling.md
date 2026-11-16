# Android quality tooling

Jetnote uses Baseline Profiles, Macrobenchmark and Roborazzi to protect runtime performance and Compose layouts.

## Baseline Profiles

Generate the profile on a physical device or API 35 emulator:

```bash
./gradlew :app:generateBaselineProfile
```

The profile journey launches Jetnote and exercises the home screen. The generated profile is saved under `app/src/release/generated/baselineProfiles`; commit that generated file so release builds package it. ProfileInstaller installs it on supported devices. Keep the journey representative of the most common startup and navigation paths.

## Macrobenchmarks

Run on a physical device or emulator. Use a release-like, non-debuggable target build:

```bash
./gradlew :benchmark:connectedCheck
```

The benchmark suite records cold startup, warm startup and frame timing while swiping the home screen. Results are written by AndroidX Benchmark under the benchmark module's outputs. Compare results on the same device/API level; do not compare timings across unrelated emulator hosts.

## Compose screenshot regression tests

The feedback form has a Robolectric/Roborazzi screenshot test, so it runs on the JVM without an emulator.

Record or intentionally update reference screenshots:

```bash
./gradlew :feedback:recordRoborazziDebug
```

Verify screenshots without changing the references:

```bash
./gradlew :feedback:verifyRoborazziDebug
```

Review comparison output under `feedback/build/outputs/roborazzi` and the Roborazzi report under `feedback/build/reports/roborazzi`. Only update references when the visual change is intentional.

GitHub Actions records the current master screenshots as an artifact and downloads them for pull-request verification. The first run bootstraps the artifact; subsequent pull requests compare against the latest master baseline.
