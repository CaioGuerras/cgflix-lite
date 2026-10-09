#!/usr/bin/env bash
# CGFLIX: roda dentro do emulador do workflow "CGFLIX Telas".
# 1) mede a abertura a frio da versão anterior e da nova (instalada POR CIMA, mesma assinatura);
# 2) tira capturas das telas pela tela de demonstração do build de debug (dados falsos), nos dois
#    temas: Isis (escuro, roxo) e Heitor (claro, verde).
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
medir "antes ($CGFLIX_ANTES_NOME)"

# Por cima: só funciona com a mesma chave (prova que atualiza sem desinstalar)
adb install -r depois.apk
medir "depois (versão do PR)"
adb shell am start -S -W -n "$MAIN" > /dev/null
sleep 4
adb exec-out screencap -p > "$OUT/0-abertura-sem-servidor.png"

adb install -r demo.apk
for tema in isis heitor; do
  n=1
  for tela in abertura login inicio detalhes configuracoes filmes series animes busca voce apoio; do
    adb shell am start -S -W -n "$DEMO" --es tela "$tela" --es tema "$tema" > /dev/null
    sleep 4
    adb exec-out screencap -p > "$OUT/$tema-$n-$tela.png"
    n=$((n + 1))
  done
done

# Gorjeta no sabor `play` (mesmo pacote e mesma chave de debug: instala por cima do `libre`)
adb install -r demo-play.apk
for tema in isis heitor; do
  adb shell am start -S -W -n "$DEMO" --es tela apoio --es tema "$tema" > /dev/null
  sleep 4
  adb exec-out screencap -p > "$OUT/$tema-play-apoio.png"
done

# Brilho do fundo (0 = preto, 1 = branco) na borda esquerda de cada captura: Heitor deve ficar
# claro (> 0,8) e Isis escuro (< 0,2)
python3 .github/scripts/cgflix-brilho.py "$OUT"/isis-*.png "$OUT"/heitor-*.png \
  | sed 's/^/brilho /' | tee -a "$OUT/medicoes.txt" || true

ls -l "$OUT"
