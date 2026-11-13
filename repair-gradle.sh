#!/bin/sh
# Pelada League - Gradle compatibility with Code on the Go (Gradle 9.6.1).
set -eu
cd "$(dirname "$0")"
cat > build.gradle <<'GRADLE'
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21'
    }
}

plugins {
    id 'com.android.application' version '9.3.1' apply false
}
GRADLE
if [ -f gradle/wrapper/gradle-wrapper.properties ]; then
    sed -i 's/gradle-8\.13-bin\.zip/gradle-9.6.1-bin.zip/g' gradle/wrapper/gradle-wrapper.properties
fi
echo "Pelada League: Gradle 9.6.1 / Android Gradle Plugin 9.3.1 configurados."
