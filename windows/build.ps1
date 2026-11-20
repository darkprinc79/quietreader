$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$project = Join-Path $root 'linux/quietreader.pro'
$qmlDir = Join-Path $root 'linux/src'
$buildDir = Join-Path $PSScriptRoot 'build'
$distDir = Join-Path $PSScriptRoot 'dist'
$exe = Join-Path $distDir 'quietreader.exe'
$archive = Join-Path $PSScriptRoot 'quietreader-windows-x64.zip'

if (-not (Get-Command qmake -ErrorAction SilentlyContinue)) { throw 'qmake is not on PATH. Install Qt 6 with the MSVC 2022 x64 kit and open a Developer PowerShell.' }
if (-not (Get-Command nmake -ErrorAction SilentlyContinue)) { throw 'nmake is not on PATH. Run this script from a Visual Studio Developer PowerShell.' }
if (-not (Get-Command windeployqt -ErrorAction SilentlyContinue)) { throw 'windeployqt is not on PATH. Check the Qt installation.' }

Remove-Item $buildDir, $distDir, $archive -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $buildDir, $distDir | Out-Null

Push-Location $buildDir
try {
    & qmake $project 'CONFIG+=release' 'CONFIG-=debug_and_release'
    if ($LASTEXITCODE -ne 0) { throw "qmake failed with exit code $LASTEXITCODE" }
    $makefile = Join-Path $buildDir 'Makefile'
    $makefileText = [System.IO.File]::ReadAllText($makefile)
    $makefileText = $makefileText.Replace('2>NUL >moc_predefs.h', '>moc_predefs.h')
    [System.IO.File]::WriteAllText($makefile, $makefileText)
    & nmake
    if ($LASTEXITCODE -ne 0) { throw "nmake failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$builtExe = Join-Path $buildDir 'quietreader.exe'
if (-not (Test-Path $builtExe)) { throw "Build succeeded but executable was not found at $builtExe" }
Copy-Item $builtExe $exe
& windeployqt --release --qmldir $qmlDir $exe
if ($LASTEXITCODE -ne 0) { throw "windeployqt failed with exit code $LASTEXITCODE" }
Compress-Archive -Path (Join-Path $distDir '*') -DestinationPath $archive -Force
Write-Host "Windows bundle created: $archive"
