@echo off
setlocal
rem Builds MiniOS.jar from the complete src tree. Requires JDK 17+.
pushd "%~dp0"
if errorlevel 1 exit /b 1
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac was not found. Install JDK 17+ and add its bin folder to PATH.
    popd
    exit /b 1
)
where jar >nul 2>nul
if errorlevel 1 (
    echo [ERROR] jar was not found. Install a full JDK 17+.
    popd
    exit /b 1
)
if not exist src\ (
    echo [ERROR] src folder not found. Run this file from the project root.
    popd
    exit /b 1
)
if not exist manifest.txt (
    echo [ERROR] manifest.txt not found.
    popd
    exit /b 1
)
if exist out rmdir /s /q out
mkdir out
if errorlevel 1 goto :failed
set "SOURCE_LIST=%TEMP%\minios-build-sources-%RANDOM%.txt"
for /r src %%F in (*.java) do echo %%F>>"%SOURCE_LIST%"
echo [1/2] Compiling source files...
javac -encoding UTF-8 -d out @"%SOURCE_LIST%"
if errorlevel 1 goto :failed
echo [2/2] Packaging MiniOS.jar...
jar cfm MiniOS.jar manifest.txt -C out .
if errorlevel 1 goto :failed
del "%SOURCE_LIST%" >nul 2>nul
echo [SUCCESS] Built MiniOS.jar. Run it with: java -jar MiniOS.jar
popd
exit /b 0
:failed
echo [FAILED] Build did not complete. Review the errors above.
if defined SOURCE_LIST del "%SOURCE_LIST%" >nul 2>nul
popd
exit /b 1
