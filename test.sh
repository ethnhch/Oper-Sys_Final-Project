#!/bin/sh
# Compiles everything including tests, then runs both test suites.
set -e
rm -rf out-test && mkdir out-test
javac -encoding UTF-8 -d out-test $(find src tests -name '*.java')
java -cp out-test ossim.SelfTest | tail -n 3
java -Djava.awt.headless=true -cp out-test ossim.ui.GuiSmokeTest | tail -n 2
