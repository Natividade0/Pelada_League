#!/bin/sh
# v0.4 keeps reviewable Android sources in Git. Historical archives remain for recovery.
set -eu
cd "$(dirname "$0")"
if [ -f app/build.gradle ]; then
  echo 'Projeto Android completo. Abra esta pasta e sincronize o Gradle.'
else
  echo 'Código Android ausente. Faça checkout da branch redesign/native-premium-v04.'
  exit 1
fi
