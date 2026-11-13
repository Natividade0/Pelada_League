#!/bin/sh
# Pelada League: local fallback when GitHub Actions has not expanded the project.
set -eu
cd "$(dirname "$0")"
if [ -f app/build.gradle ] && [ -f settings.gradle ]; then
  echo "Projeto Android ja presente. Abra esta pasta no Code on the Go."
  exit 0
fi
if command -v python3 >/dev/null 2>&1; then
  python3 - <<'PY'
import base64, io, lzma, pathlib, tarfile
root = pathlib.Path(".")
parts = sorted(root.glob("payload/part[0-9][0-9].txt"))
assert len(parts) == 5, "Pacote incompleto: esperado 5 partes"
encoded = b"".join(p.read_bytes() for p in parts)
archive = lzma.decompress(base64.b64decode(encoded))
with tarfile.open(fileobj=io.BytesIO(archive), mode="r:") as tf:
    for member in tf.getmembers():
        path = pathlib.PurePosixPath(member.name)
        assert not path.is_absolute() and ".." not in path.parts
    tf.extractall(".")
print("Codigo-fonte extraido.")
PY
else
  command -v base64 >/dev/null 2>&1 || { echo "Instale Python 3 ou base64 + tar + xz."; exit 1; }
  cat payload/part[0-9][0-9].txt | base64 -d | xz -d | tar -xf -
  echo "Codigo-fonte extraido."
fi
if command -v python3 >/dev/null 2>&1 && python3 -c "from PIL import Image" >/dev/null 2>&1; then
  python3 make_assets.py
else
  echo "Pillow indisponivel: aplicando recursos visuais alternativos."
  mkdir -p app/src/main/res/drawable app/src/main/res/mipmap-hdpi
  cat > app/src/main/res/drawable/crest.xml <<'XML'
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="180dp" android:height="180dp" android:viewportWidth="180" android:viewportHeight="180">
 <path android:fillColor="#FFCF00" android:pathData="M33,44 L44,58 59,31 80,58 101,29 120,58 142,44 134,72 42,72 Z"/>
 <path android:fillColor="#FFCF00" android:pathData="M38,77 L142,77 134,128 90,160 46,128 Z"/>
 <path android:fillColor="#090B0F" android:pathData="M47,84 L133,84 126,122 90,151 54,122 Z"/>
 <path android:fillColor="#FFFFFF" android:pathData="M90,94 m-29,0 a29,29 0,1 0,58 0 a29,29 0,1 0,-58 0"/>
 <path android:fillColor="#111111" android:pathData="M90,73 L105,83 100,103 82,109 70,93 77,77 Z"/>
</vector>
XML
  cat > app/src/main/res/drawable/stadium_splash.xml <<'XML'
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
 <gradient android:angle="90" android:startColor="#040609" android:centerColor="#10150F" android:endColor="#403008"/>
</shape>
XML
  cat > app/src/main/res/drawable/stadium_banner.xml <<'XML'
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
 <gradient android:angle="0" android:startColor="#101519" android:centerColor="#1D2516" android:endColor="#55410D"/>
 <corners android:radius="16dp"/>
</shape>
XML
  cp app/src/main/res/drawable/crest.xml app/src/main/res/mipmap-hdpi/ic_launcher.xml
fi
echo "Pronto. Abra a pasta clonada no Code on the Go e sincronize o Gradle."
