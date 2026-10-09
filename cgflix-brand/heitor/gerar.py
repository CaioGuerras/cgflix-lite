#!/usr/bin/env python3
"""CGFLIX: gera a marca do tema Heitor (verde) a partir da marca Isis (roxa).

Só troca cores (tabela TROCA, da ordem de serviço do tema Heitor). Rodar da raiz do repositório:
    python3 cgflix-brand/heitor/gerar.py
Gera cgflix-brand/heitor/*.svg e core/src/main/res/drawable/ic_banner_heitor.xml (vetor do app).
"""
import pathlib

TROCA = {
    "#f3e8ff": "#8be3a1", "#c084fc": "#34c759", "#9333ea": "#1a9443", "#581c87": "#00531d",
    'stop-color="#ffffff"/><stop offset=".55" stop-color="#ede9fe"/><stop offset="1" stop-color="#c4b5fd"':
        'stop-color="#2b3a2a"/><stop offset=".55" stop-color="#1c261c"/><stop offset="1" stop-color="#161d16"',
    'flood-color="#a855f7" flood-opacity=".6"': 'flood-color="#34c759" flood-opacity=".28"',
    'stop-color="#1e1030"': 'stop-color="#ffffff"', 'stop-color="#07060a"': 'stop-color="#e2ebde"',
}

# A mesma troca no formato do VectorDrawable (o degradê das letras vira <item>s)
TROCA_VETOR = {
    "#f3e8ff": "#8be3a1", "#c084fc": "#34c759", "#9333ea": "#1a9443", "#581c87": "#00531d",
    'android:offset="0" android:color="#ffffff"': 'android:offset="0" android:color="#2b3a2a"',
    'android:offset="0.55" android:color="#ede9fe"': 'android:offset="0.55" android:color="#1c261c"',
    'android:offset="1" android:color="#c4b5fd"': 'android:offset="1" android:color="#161d16"',
}


def trocar(texto, tabela):
    for de, para in tabela.items():
        texto = texto.replace(de, para)
    return texto


raiz = pathlib.Path(__file__).resolve().parents[2]
marca = raiz / "cgflix-brand"
for svg in sorted(marca.glob("*.svg")):
    (marca / "heitor" / svg.name).write_text(trocar(svg.read_text(), TROCA))

drawable = raiz / "core/src/main/res/drawable"
vetor = trocar((drawable / "ic_banner.xml").read_text(), TROCA_VETOR)
(drawable / "ic_banner_heitor.xml").write_text(
    "<!-- CGFLIX: marca do tema Heitor, gerada por cgflix-brand/heitor/gerar.py -->\n" + vetor
)
