#!/bin/sh
set -eu
cd "$(dirname "$0")"
test -f app/src/main/java/br/com/peladaleague/app/MainActivity.java || { echo 'Código incompleto. Atualize esta branch pelo Git.'; exit 1; }
echo 'Pelada League 0.4: código Android já expandido. Não é necessário extrair payloads.'
echo 'Abra esta pasta no Code on the Go, sincronize o Gradle e compile o APK.'
echo 'O banco instalado é atualizado automaticamente, sem excluir registros.'
