#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .local-test
find core/src/main/java core/src/test/java -name '*.java' > .local-test/sources.txt
javac -encoding UTF-8 -d .local-test @.local-test/sources.txt
java -cp .local-test br.com.peladaleague.core.SmokeTest

java -cp .local-test br.com.peladaleague.core.RegressionTest
