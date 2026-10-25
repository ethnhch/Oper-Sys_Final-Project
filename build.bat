@echo off
rem Compiles the simulator and packages MiniOS.jar (requires JDK 17+).
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
del sources.txt
jar cfm MiniOS.jar manifest.txt -C out .
echo Built MiniOS.jar - run with: java -jar MiniOS.jar
