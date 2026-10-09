#!/usr/bin/env bash
# CGFLIX (Etapa 1B): roda dentro do emulador do workflow "CGFLIX Telas".
# 1) mede a abertura a frio do 1.1.0 e da versão nova (instalada POR CIMA, mesma assinatura);
# 2) tira capturas das telas novas pela tela de demonstração do build de debug (dados falsos).
set -euo pipefail

PKG=br.com.docaio.cgflix.lite
MAIN="$PKG/dev.jdtech.jellyfin.MainActivity"
DEMO="$PKG.debug/dev.jdtech.jellyfin.cgflix.demo.CgflixDemoActivity"
OUT=telas
mkdir -p "$OUT"

# Abertura a frio (am start -W, TotalTime em ms) até a primeira tela, sem servidor configurado.
# A 1ª abertura (logo após instalar) fica de fora; mediana das 5 seguintes.
medir() {
  local nome=$1 tempos=()
  adb shell am start -S -W -n "$MAIN" > /dev/null
  sleep 3
  for _ in 1 2 3 4 5; do
    adb shell am force-stop "$PKG"
    sleep 2
    t=$(adb shell am start -S -W -n "$MAIN" | tr -d '\r' | awk '/TotalTime/ {print $2}')
    tempos+=("$t")
    sleep 3
  done
  local mediana
  mediana=$(printf '%s\n' "${tempos[@]}" | sort -n | sed -n 3p)
  echo "$nome: mediana ${mediana} ms (${tempos[*]})" | tee -a "$OUT/medicoes.txt"
}

adb install antes.apk
medir "1.1.0 (antes)"

# Por cima: só funciona com a mesma chave (prova que atualiza sem desinstalar)
adb install -r depois.apk
medir "1.2.0 (depois)"
adb shell am start -S -W -n "$MAIN" > /dev/null
sleep 4
adb exec-out screencap -p > "$OUT/0-abertura-sem-servidor.png"

adb install -r demo.apk
n=1
for tela in abertura inicio filmes series animes busca voce; do
  adb shell am start -S -W -n "$DEMO" --es tela "$tela" > /dev/null
  sleep 4
  adb exec-out screencap -p > "$OUT/$n-$tela.png"
  n=$((n + 1))
done

ls -l "$OUT"
