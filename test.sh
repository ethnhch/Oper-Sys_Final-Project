#!/bin/sh
# Compiles everything including tests, then runs both test suites (JDK 17+).
set -eu
rm -rf out-test
mkdir out-test
javac -encoding UTF-8 -d out-test $(find src tests -name '*.java')
java -cp out-test ossim.SelfTest > TEST_RESULTS_core.txt
tail -n 3 TEST_RESULTS_core.txt
java -Djava.awt.headless=true -cp out-test ossim.ui.GuiSmokeTest > TEST_RESULTS_gui.txt
tail -n 2 TEST_RESULTS_gui.txt
