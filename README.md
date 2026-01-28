# Jetnote
Jetnote is modern note android application written by kotlin language and [jetpack-compose](https://github.com/JetBrains/compose-jb) toolkit. The app dealing with Room for Database, Koin dependency injection, Coroutine the non-blocking programming, Navigation-Compose, DataStore, ExoPlayer to play the recorded media in the app, CameraX to take and save picture, And more to make it good app that inspired by google app **keep Notes**.

[![Android Platform](https://img.shields.io/badge/Android-27+-34A853?logo=android)](https://www.android.com/)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Kotlin Version](https://img.shields.io/badge/coroutines-1.9.0-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/coroutines-overview.html)
[![Jetpack Compose](https://img.shields.io/badge/Compose-1.5.14-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/androidx/releases/compose)
[![Gradle](https://img.shields.io/badge/Gradle-8.5.2-02303A?logo=gradle)](https://gradle.org)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://github.com/City-Zouitel/JetNote/edit/main/LICENSE)
[![Android CI](https://github.com/City-Zouitel/JetNote/actions/workflows/android.yml/badge.svg)](https://github.com/City-Zouitel/Jetnote/actions/workflows/gradle.yml)
[![wakatime](https://wakatime.com/badge/user/218fee08-512f-4a26-b56d-104925a888b1/project/69a8b8aa-bef4-403e-9f71-8ca21ac8ed73.svg)](https://wakatime.com/badge/user/218fee08-512f-4a26-b56d-104925a888b1/project/69a8b8aa-bef4-403e-9f71-8ca21ac8ed73)

## Screenshots

<p float="left">
  <img src="screenshots/Screenshot_20221206-134054_JetNote.jpg" width="32%" />
  <img src="screenshots/Screenshot_20221206-134218_JetNote.png" width="32%" />
  <img src="screenshots/Screenshot_20221206-134300_JetNote.png" width="32%" />
</p>


## Libraries
### [Kotlinx-coroutines](https://github.com/Kotlin/kotlinx.coroutines) 
A coroutine is a concurrency design pattern that you can use on Android to simplify code that executes asynchronously.

### [Voyager](https://voyager.adriel.cafe/) 
A multiplatform navigation and viewmodel library built for, and seamlessly integrated with, Jetpack Compose.

### [Room](https://developer.android.com/jetpack/androidx/releases/room) 
The Room persistence library provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite.

### [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
Jetpack DataStore is a data storage solution that allows you to store key-value pairs or typed objects with protocol buffers. DataStore uses Kotlin coroutines and Flow to store data asynchronously, consistently, and transactionally.

### [Koin](https://github.com/InsertKoinIO/koin)
Koin is a pragmatic lightweight dependency injection framework for Kotlin developers.

### [Sketchbook](https://github.com/GetStream/sketchbook-compose) 
Jetpack Compose canvas library that helps you draw paths, images on canvas with color pickers and palettes.

### [ExoPlayer](https://exoplayer.dev/)
ExoPlayer is an application level media player for Android. It provides an alternative to Android’s MediaPlayer API for playing audio and video both locally and over the Internet.

### [CameraX](https://developer.android.com/jetpack/androidx/releases/camera)
CameraX is an addition to Jetpack that makes it easier to add camera capabilities to your app. The library provides a number of compatibility fixes and workarounds to help make the developer experience consistent across many devices.

### [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
LiveData is an observable data holder class. Unlike a regular observable, LiveData is lifecycle-aware, meaning it respects the lifecycle of other app components, such as activities, fragments, or services. This awareness ensures LiveData only updates app component observers that are in an active lifecycle state.

### [Coil](https://coil-kt.github.io/coil/)
An image loading library for Android backed by Kotlin Coroutines. 

### [AboutLibraries](https://github.com/mikepenz/AboutLibraries)
Collects all dependency details including licenses at compile time, and offers simple APIs to visualize these in the app.

### [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
WorkManager is the recommended solution for persistent work. Work is persistent when it remains scheduled through app restarts and system reboots.

## The App Hierarchy

```
                          *****************************
                          |        UI Controller      |
                          *****************************
                                        |
                                        |
                          *****************************
                          |        View Module        |
                          *****************************
                                        |
                                        |
                          *****************************
                          |         Repository        |
                          *****************************
                                        |
                      --------------------------------------
                      |                                     |
          *****************************      *****************************
          |            Room           |      |        ...........        |
          *****************************      *****************************
```

## License
```text
Copyright 2022 The Android Open Source Project
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
