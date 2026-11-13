#!/bin/sh
# Only run on an emulator reserved for tests; UI fixtures modify the demo database.
set -eu
cd "$(dirname "$0")/.."
mkdir -p qa-output
adb shell wm size 720x1600
adb shell wm density 320
adb shell input keyevent 82
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class br.com.peladaleague.app.DatabaseTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/database-tests.txt
cat qa-output/database-tests.txt
grep -q 'OK (' qa-output/database-tests.txt
adb shell am instrument -w -e class br.com.peladaleague.app.UiTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/ui-tests.txt
cat qa-output/ui-tests.txt
adb pull /sdcard/Android/data/br.com.peladaleague.app/files/qa qa-output/screenshots
grep -q 'OK (' qa-output/ui-tests.txt
adb shell am force-stop br.com.peladaleague.app
adb shell am start -W -n br.com.peladaleague.app/.MainActivity > qa-output/relaunch.txt
adb logcat -d -s AndroidRuntime:E > qa-output/crashes.txt
