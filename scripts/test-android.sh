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
adb logcat -c
if adb shell am instrument -w -e class br.com.peladaleague.app.DatabaseTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/database-tests.txt; then
  :
else
  echo "Database instrument terminated with nonzero code."
fi
cat qa-output/database-tests.txt
if ! grep -q 'OK (' qa-output/database-tests.txt; then
  echo "Database instrumentation failure; Android crash log:"
  adb logcat -d -s AndroidRuntime:E > qa-output/database-crashes.txt || true
  cat qa-output/database-crashes.txt
  exit 1
fi
adb shell am instrument -w -e class br.com.peladaleague.app.FinanceTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/finance-tests.txt
cat qa-output/finance-tests.txt
grep -q 'OK (' qa-output/finance-tests.txt
adb shell am instrument -w -e class br.com.peladaleague.app.ClockTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/clock-tests.txt
cat qa-output/clock-tests.txt
grep -q 'OK (' qa-output/clock-tests.txt
adb shell am instrument -w -e class br.com.peladaleague.app.ReminderTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/reminder-tests.txt
cat qa-output/reminder-tests.txt
grep -q 'OK (' qa-output/reminder-tests.txt
adb logcat -c
if adb shell am instrument -w -e class br.com.peladaleague.app.UiTest br.com.peladaleague.app.test/android.test.InstrumentationTestRunner > qa-output/ui-tests.txt; then
  instrument_ok=1
else
  instrument_ok=0
fi
adb logcat -d -s AndroidRuntime:E > qa-output/crashes.txt || true
cat qa-output/ui-tests.txt
if [ "$instrument_ok" -ne 1 ] || ! grep -q 'OK (' qa-output/ui-tests.txt; then
  echo "Android UI test failed; crash trace follows:"
  cat qa-output/crashes.txt
  exit 1
fi
adb exec-out run-as br.com.peladaleague.app tar -C files -cf - qa > qa-output/screenshots.tar
mkdir -p qa-output/screenshots
tar -xf qa-output/screenshots.tar -C qa-output/screenshots --strip-components=1
adb shell am force-stop br.com.peladaleague.app
adb shell am start -W -n br.com.peladaleague.app/.MainActivity > qa-output/relaunch.txt
adb logcat -d -s AndroidRuntime:E > qa-output/crashes.txt || true
if [ -s qa-output/crashes.txt ]; then cat qa-output/crashes.txt; exit 1; fi
