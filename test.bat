
@echo off
title Mini OS Simulator - Test Runner
setlocal

echo ========================================
echo       MINI OS SIMULATOR TEST RUNNER
echo ========================================
echo.

if not exist "src" (
    echo [ERROR] Cannot find the src folder.
    echo Make sure this file is in your project root.
    pause
    exit /b 1
)

if not exist "tests" (
    echo [ERROR] Cannot find the tests folder.
    pause
    exit /b 1
)

if exist "out-test" rmdir /s /q "out-test"
mkdir "out-test"

echo [1/3] Compiling source and test files...

powershell -NoProfile -Command "$files = Get-ChildItem -Path 'src','tests' -Recurse -Filter '*.java' | ForEach-Object { '"' + $_.FullName + '"' }; if ($files.Count -eq 0) { exit 1 }; Set-Content -Path 'test-sources.txt' -Value $files -Encoding ASCII"

if errorlevel 1 (
    echo [ERROR] Could not create the source file list.
    goto failed
)

javac -encoding UTF-8 -d "out-test" @"test-sources.txt"

if errorlevel 1 goto failed

del "test-sources.txt"

echo.
echo [2/3] Running SelfTest...
java -cp "out-test" ossim.SelfTest

if errorlevel 1 goto failed

echo.
echo [3/3] Running GuiSmokeTest...
java -Djava.awt.headless=true -cp "out-test" ossim.ui.GuiSmokeTest

if errorlevel 1 goto failed

echo.
echo ========================================
echo [SUCCESS] ALL TESTS PASSED
echo ========================================
pause
exit /b 0

:failed
if exist "test-sources.txt" del "test-sources.txt"
echo.
echo [FAILED] Please check the errors above.
pause
exit /b 1
