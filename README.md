# Quiet Reader

![Quiet Reader running on Android](Screenshot_20261122_162618.jpg)

Quiet Reader is a text reader for Android 5.0 and later, Windows, and Linux. The Android app uses the system folder picker to open a directory, lists its folders and readable files in a compact sidebar, and displays the selected file in a scrollable reading pane. It supports `.txt` files and files without an extension, with adjustable text size and dim white, white, warm, or green text on black.

## Android compatibility

- Minimum Android version: 5.0 (API 21)
- The two-pane layout is intended for tablets and can also run on phones. Android shows the selected folder's immediate subfolders and readable text files; it does not recursively expand the whole directory tree.
- Android folder scans run in the background and the sidebar adds up to 200 rows at a time. A previously selected folder is not scanned automatically at startup; tap Open and choose “Resume last folder” to load it.
- Folder access is granted through Android's Storage Access Framework; the app does not request broad storage permissions.

## Build

Open this directory in Android Studio with Android SDK 34 installed, or build with Gradle 8.2+ and a Java 17 JDK:

    gradle assembleDebug

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Windows desktop version

The Linux and Windows desktops use the same Qt source. Windows builds require Qt 6.8.3 for MSVC 2022 x64 and Visual Studio Build Tools. Run `windows/build.ps1` from a Visual Studio Developer PowerShell to create `windows/quietreader-windows-x64.zip`. The GitHub Actions workflow also builds this bundle on pushes to `main` and publishes it as a downloadable workflow artifact.

## Linux desktop version

The native Linux app is in `linux/`. It uses Qt 6 and qmake, with a two-pane library tree and reader, and adjustable text size and color. It starts with the home folder in the tree, accepts folder and file paths directly, detects text by file contents regardless of filename extension, and supports UTF-8 and UTF-16. Build it with:

    cd linux
    ./bin/build

Run `linux/build/quietreader`. You can pass a file path to open it immediately. Shortcuts: Ctrl+O opens a text file, Ctrl+Shift+O chooses a library folder, Ctrl+Plus/Ctrl+Minus adjust the text size, and Ctrl+Q quits.
