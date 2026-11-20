# Quiet Reader for Windows

Quiet Reader shares its Qt 6 source with the Linux desktop app. The Windows build creates a portable x64 folder containing the executable and the Qt libraries, plugins, and QML modules it needs.

## Build

Install Qt 6.8.3 with the MSVC 2022 64-bit desktop kit and install Visual Studio 2022 Build Tools with the C++ desktop workload. Open **Developer PowerShell for VS 2022** at the project root and run:

    .\windows\build.ps1

The resulting bundle is `windows\quietreader-windows-x64.zip`. Extract it and run `quietreader.exe`. A GitHub Actions workflow builds the same bundle on each push to `main`; download it from the run's **Artifacts** section.
