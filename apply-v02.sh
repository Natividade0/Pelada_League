#!/system/bin/sh
# Pelada League v0.2 updater; execute with: sh apply-v02.sh
set -eu
cd "$(dirname "$0")"
if [ ! -f app/build.gradle ]; then
  echo "Projeto base nao foi extraido. Primeiro execute: sh bootstrap.sh"
  exit 1
fi
EXPECTED='5d7f6b638daac931287ee0430f2656fc7fd060e1075b18f416a0715b76125ebf'
if [ -f .pelada-league-release ] && grep -q "$EXPECTED" .pelada-league-release && grep -q '0.2.0-alpha' app/build.gradle; then
  echo "Pelada League 0.2 ja instalado neste projeto."
  exit 0
fi
TEMP_FILE=".pelada-v02-package.tar.xz"
trap 'rm -f "$TEMP_FILE"' EXIT
if command -v base64 >/dev/null 2>&1; then
  cat payload/v02/part00.txt | tr -d '\r\n' | base64 -d > "$TEMP_FILE"
else
  echo "Ferramenta base64 nao encontrada; atualize pelo ZIP do projeto."
  exit 1
fi
if command -v sha256sum >/dev/null 2>&1; then
  GOT="$(sha256sum "$TEMP_FILE" | cut -d ' ' -f1)"
  if [ "$GOT" != "$EXPECTED" ]; then
    echo "Falha de integridade: dados incompletos. Nenhum arquivo alterado."
    exit 1
  fi
fi
if command -v xz >/dev/null 2>&1; then
  xz -dc "$TEMP_FILE" | tar -xf -
else
  echo "Ferramenta xz indisponivel. Execute com Termux ou use o ZIP completo."
  exit 1
fi
if [ -f repair-gradle.sh ]; then
  sh repair-gradle.sh
fi
printf "0.2.0-alpha %s\n" "$EXPECTED" > .pelada-league-release
echo "Pelada League atualizado para 0.2.0-alpha: presencas, sorteio e sumulas."
echo "Abra o projeto no Code on the Go e recompile."
