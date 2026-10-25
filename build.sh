#!/bin/sh
# Compiles the simulator and packages MiniOS.jar (requires JDK 17+).
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name '*.java')
jar cfm MiniOS.jar manifest.txt -C out .
echo "Built MiniOS.jar  ->  run with: java -jar MiniOS.jar"
