# Quiet Reader

A simple tablet-oriented Android text reader. It uses Android's system folder picker, shows folders and readable files in a left sidebar, and displays the selected file in a scrollable reading pane. Supported files are `.txt` files and names without an extension. Reading appearance includes adjustable text size and dim white, white, warm, and green text on black.

## Android compatibility

- Minimum Android version: 5.0 (API 21)
- The two-pane layout is intended for tablets and can also run on phones.
- Folder access is granted through Android's Storage Access Framework; the app does not request broad storage permissions.

## Build

Open this directory in Android Studio with Android SDK 34 installed, or build with Gradle 8.2+ and a Java 17 JDK:

    gradle assembleDebug

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Linux desktop version

The native Linux app is in `linux/`. It uses Qt 6 and qmake, with a two-pane library tree and reader, and adjustable text size and color. It starts with the home folder in the tree, accepts folder and file paths directly, detects text by file contents regardless of filename extension, and supports UTF-8 and UTF-16. Build it with:

    cd linux
    ./bin/build

Run `linux/build/quietreader`. You can pass a file path to open it immediately. Shortcuts: Ctrl+O opens a text file, Ctrl+Shift+O chooses a library folder, Ctrl+Plus/Ctrl+Minus adjust the text size, and Ctrl+Q quits.
