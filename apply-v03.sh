#!/bin/sh
# Pelada League v0.3: update the already expanded 0.2 project without touching SQLite.
set -eu
cd "$(dirname "$0")"
if [ ! -f app/build.gradle ]; then
  echo "Projeto Android nao extraido. Execute sh bootstrap.sh e sh apply-v02.sh primeiro."
  exit 1
fi
if grep -q "versionName '0.3.0-alpha'" app/build.gradle; then
  echo "Pelada League 0.3 ja instalado."
  exit 0
fi
if ! grep -q "versionName '0.2.0-alpha'" app/build.gradle; then
  echo "Esta atualizacao requer a versao 0.2. Execute sh apply-v02.sh primeiro."
  exit 1
fi
for cmd in base64 xz tar sha256sum; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "Ferramenta necessaria indisponivel: $cmd"; exit 1; }
done
TEMP_FILE=".pelada-v03-package.tar.xz"
trap 'rm -f "$TEMP_FILE"' EXIT
cat payload/v03/part00.txt | tr -d '\r\n' | base64 -d > "$TEMP_FILE"
EXPECTED='88ae61b0ad64a1c583aa84a68efa631c23663dca49f5541fab5ab5fd7e4da192'
GOT="$(sha256sum "$TEMP_FILE" | cut -d ' ' -f1)"
if [ "$GOT" != "$EXPECTED" ]; then
  echo "Falha de integridade. Nenhum arquivo alterado."
  exit 1
fi
# Existing app data is stored outside the source tree; no database migration needed.
xz -dc "$TEMP_FILE" | tar -xf -
printf '0.3.0-alpha %s\n' "$EXPECTED" > .pelada-league-release
printf 'Pelada League 0.3 aplicado: layout premium, animacoes e sorteio visual.\n'
printf 'Sincronize o Gradle e compile pelo Code on the Go.\n'
