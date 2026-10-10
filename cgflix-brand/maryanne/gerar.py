#!/usr/bin/env python3
"""CGFLIX (Modo Maryanne): gera o morango, o ícone e a marca infantil.

Rodar da raiz do repositório (precisa do fontTools só para as letras da marca):
    python3 cgflix-brand/maryanne/gerar.py
Gera cgflix-brand/maryanne/*.svg (para ver e revisar) e, no app, em core/src/main/res/drawable/:
ic_cgflix_morango.xml (morango colorido de 24 dp), ic_launcher_foreground_maryanne.xml e
ic_launcher_monochrome_maryanne.xml (ícone, mipmap-anydpi/ic_launcher_maryanne.xml) e
ic_banner_maryanne.xml (morango + "CGFLIX" na Fredoka, letras saltitantes).

O desenho do morango vive numa caixa de 1000 x 1000 (ocupa x 150..850, y 160..940).
"""
import pathlib

RAIZ = pathlib.Path(__file__).resolve().parents[2]
SAIDA_SVG = RAIZ / "cgflix-brand/maryanne"
DRAWABLE = RAIZ / "core/src/main/res/drawable"
FONTE = RAIZ / "app/phone/src/main/res/font/cgflix_fredoka.ttf"

CORPO = (
    "M500 950C350 910 165 740 155 525C150 360 270 300 400 320C450 328 480 340 500 345"
    "C520 340 550 328 600 320C730 300 850 360 845 525C835 740 650 910 500 950Z"
)
CORPO_DEGRADE = [(0, "#ff8aa3"), (0.45, "#e8395f"), (1, "#b81d47")]
BRILHO = "M262 470C228 545 238 645 292 712C300 650 302 560 326 492C310 470 284 462 262 470Z"
FOLHAS_FORA = [
    "M500 330C420 288 300 298 228 362C320 404 420 392 500 348Z",
    "M500 330C580 288 700 298 772 362C680 404 580 392 500 348Z",
]
FOLHAS_DENTRO = [
    "M502 336C450 362 400 420 378 482C452 462 492 410 512 348Z",
    "M498 336C550 362 600 420 622 482C548 462 508 410 488 348Z",
]
CABINHO = "M500 338C500 272 522 212 572 162"
PLAY = "M446 548L446 712L586 630Z"
SEMENTES = [
    (708, 482), (214, 652), (768, 618), (322, 760), (678, 760),
    (500, 876), (418, 834), (582, 834), (372, 612), (646, 508),
]
SEMENTE = "#ffe7a3"
FOLHA_CLARA = "#43a047"
FOLHA_ESCURA = "#2e7d32"


def oval(cx, cy, rx, ry):
    return f"M{cx - rx} {cy}A{rx} {ry} 0 1 0 {cx + rx} {cy}A{rx} {ry} 0 1 0 {cx - rx} {cy}Z"


def sementes():
    return "".join(oval(x, y, 15, 22) for x, y in SEMENTES)


# ---------- SVG (revisão) ----------

def morango_svg(prefixo="m"):
    stops = "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in CORPO_DEGRADE)
    folhas = "".join(f'<path d="{d}" fill="{FOLHA_CLARA}"/>' for d in FOLHAS_FORA)
    folhas += "".join(f'<path d="{d}" fill="{FOLHA_ESCURA}"/>' for d in FOLHAS_DENTRO)
    return (
        f'<defs><linearGradient id="{prefixo}c" x1="500" y1="320" x2="500" y2="940" '
        f'gradientUnits="userSpaceOnUse">{stops}</linearGradient></defs>'
        f'<path d="{CORPO}" fill="url(#{prefixo}c)"/>'
        f'<path d="{BRILHO}" fill="#ffffff" fill-opacity=".35"/>'
        f'<path d="{sementes()}" fill="{SEMENTE}"/>'
        f'<path d="{PLAY}" fill="#ffffff" stroke="#ffffff" stroke-width="40" stroke-linejoin="round"/>'
        f'<path d="{CABINHO}" fill="none" stroke="{FOLHA_ESCURA}" stroke-width="44" stroke-linecap="round"/>'
        f"{folhas}"
    )


# ---------- VectorDrawable (app) ----------

def morango_vetor(recuo="    "):
    itens = "\n".join(
        f'{recuo}      <item android:offset="{o}" android:color="{c}"/>' for o, c in CORPO_DEGRADE
    )
    p = []
    p.append(
        f'{recuo}<path android:pathData="{CORPO}">\n'
        f'{recuo}  <aapt:attr name="android:fillColor">\n'
        f'{recuo}    <gradient android:type="linear" android:startX="500" android:startY="320" '
        f'android:endX="500" android:endY="940">\n{itens}\n'
        f"{recuo}    </gradient>\n{recuo}  </aapt:attr>\n{recuo}</path>"
    )
    p.append(
        f'{recuo}<path android:pathData="{BRILHO}" android:fillColor="#ffffff" '
        f'android:fillAlpha="0.35"/>'
    )
    p.append(f'{recuo}<path android:pathData="{sementes()}" android:fillColor="{SEMENTE}"/>')
    p.append(
        f'{recuo}<path android:pathData="{PLAY}" android:fillColor="#ffffff" '
        f'android:strokeColor="#ffffff" android:strokeWidth="40" android:strokeLineJoin="round"/>'
    )
    p.append(
        f'{recuo}<path android:pathData="{CABINHO}" android:strokeColor="{FOLHA_ESCURA}" '
        f'android:strokeWidth="44" android:strokeLineCap="round"/>'
    )
    for d in FOLHAS_FORA:
        p.append(f'{recuo}<path android:pathData="{d}" android:fillColor="{FOLHA_CLARA}"/>')
    for d in FOLHAS_DENTRO:
        p.append(f'{recuo}<path android:pathData="{d}" android:fillColor="{FOLHA_ESCURA}"/>')
    return "\n".join(p)


def vetor(comentario, largura, altura, vw, vh, corpo):
    return (
        f"<!-- CGFLIX: {comentario}, gerado por cgflix-brand/maryanne/gerar.py -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        f'    android:width="{largura}dp"\n    android:height="{altura}dp"\n'
        f'    android:viewportWidth="{vw}"\n    android:viewportHeight="{vh}">\n'
        f"{corpo}\n</vector>\n"
    )


def grupo(escala, tx, ty, corpo, recuo="  "):
    return (
        f'{recuo}<group android:scaleX="{escala:.5f}" android:scaleY="{escala:.5f}" '
        f'android:translateX="{tx:.3f}" android:translateY="{ty:.3f}">\n{corpo}\n{recuo}</group>'
    )


# ---------- letras da marca (Fredoka Bold) ----------

LETRAS = "CGFLIX"
# cada letra pula um pouco (rotação em graus, deslocamento vertical em unidades da fonte)
PULO = [(-6, -20), (4, 18), (-3, -14), (5, 16), (-4, -18), (5, 12)]
LETRA_DEGRADE = [(0, "#e8395f"), (1, "#a3163e")]


def letras():
    """Contornos das letras (y para baixo) e a largura total, em unidades da fonte."""
    from fontTools.pens.svgPathPen import SVGPathPen
    from fontTools.pens.transformPen import TransformPen
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer

    fonte = instancer.instantiateVariableFont(TTFont(FONTE), {"wght": 700})
    glifos = fonte.getGlyphSet()
    cmap = fonte.getBestCmap()
    sobe = fonte["OS/2"].sCapHeight
    x = 0
    saida = []
    for i, ch in enumerate(LETRAS):
        nome = cmap[ord(ch)]
        g = glifos[nome]
        pen = SVGPathPen(glifos)
        # vira o eixo y (fonte cresce para cima) e põe o topo das maiúsculas em 0
        g.draw(TransformPen(pen, (1, 0, 0, -1, x, sobe)))
        largura = g.width
        saida.append((pen.getCommands(), x + largura / 2, sobe / 2, PULO[i]))
        x += largura + 40
    return saida, x, sobe


def marca(svg):
    """Marca 1536 x 512: morango à esquerda e "CGFLIX" à direita."""
    contornos, largura, altura = letras()
    # morango: 400 de altura (780 no desenho), centrado em y 256
    em = 400 / 780
    mx, my = 40 - 150 * em, 256 - 550 * em
    # letras: 230 de altura de maiúscula, começando em x 470
    el = min(230 / altura, (1536 - 470 - 40) / largura)
    lx, ly = 470, 256 - altura * el / 2
    if svg:
        stops = "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in LETRA_DEGRADE)
        corpo = (
            f'<defs><linearGradient id="l" x1="0" y1="0" x2="0" y2="{altura}" '
            f'gradientUnits="userSpaceOnUse">{stops}</linearGradient></defs>'
            f'<g transform="translate({mx:.2f} {my:.2f}) scale({em:.5f})">{morango_svg("b")}</g>'
            f'<g transform="translate({lx} {ly:.2f}) scale({el:.5f})">'
        )
        for d, cx, cy, (rot, dy) in contornos:
            corpo += (
                f'<path d="{d}" fill="url(#l)" '
                f'transform="translate(0 {dy}) rotate({rot} {cx:.1f} {cy:.1f})"/>'
            )
        return (
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1536 512" width="1536" '
            f'height="512">{corpo}</g></svg>\n'
        )
    itens = "\n".join(
        f'            <item android:offset="{o}" android:color="{c}"/>' for o, c in LETRA_DEGRADE
    )
    partes = [grupo(em, mx, my, morango_vetor("    "))]
    letras_xml = []
    for d, cx, cy, (rot, dy) in contornos:
        letras_xml.append(
            f'    <group android:rotation="{rot}" android:pivotX="{cx:.1f}" '
            f'android:pivotY="{cy:.1f}" android:translateY="{dy}">\n'
            f'      <path android:pathData="{d}">\n'
            '        <aapt:attr name="android:fillColor">\n'
            f'          <gradient android:type="linear" android:startX="0" android:startY="0" '
            f'android:endX="0" android:endY="{altura}">\n{itens}\n'
            "          </gradient>\n        </aapt:attr>\n      </path>\n    </group>"
        )
    partes.append(grupo(el, lx, ly, "\n".join(letras_xml)))
    return vetor("marca do Modo Maryanne", 1536, 512, 1536, 512, "\n".join(partes))


def main():
    SAIDA_SVG.mkdir(parents=True, exist_ok=True)
    (SAIDA_SVG / "morango.svg").write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 1000" width="1000" '
        f'height="1000">{morango_svg()}</svg>\n'
    )
    # ícone: fundo creme e morango com 50 dp de altura, centrado (zona segura de 66 dp)
    e = 50 / 780
    tx, ty = 54 - 500 * e, 54 - 550 * e
    (SAIDA_SVG / "icone.svg").write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="432" height="432">'
        '<rect width="108" height="108" rx="24" fill="#fff7ee"/>'
        f'<g transform="translate({tx:.3f} {ty:.3f}) scale({e:.5f})">{morango_svg()}</g></svg>\n'
    )
    (DRAWABLE / "ic_launcher_foreground_maryanne.xml").write_text(
        vetor("ícone do app no Modo Maryanne", 108, 108, 108, 108, grupo(e, tx, ty, morango_vetor()))
    )
    # monocromático (ícones temáticos do Android 13+): corpo com o play vazado, folhas e cabinho
    mono = (
        f'    <path android:pathData="{CORPO}{PLAY}" android:fillColor="#ffffff" '
        'android:fillType="evenOdd"/>\n'
        f'    <path android:pathData="{CABINHO}" android:strokeColor="#ffffff" '
        'android:strokeWidth="44" android:strokeLineCap="round"/>\n'
        + "\n".join(
            f'    <path android:pathData="{d}" android:fillColor="#ffffff"/>' for d in FOLHAS_FORA
        )
    )
    (DRAWABLE / "ic_launcher_monochrome_maryanne.xml").write_text(
        vetor("ícone monocromático do Modo Maryanne", 108, 108, 108, 108, grupo(e, tx, ty, mono))
    )
    # morango de 24 dp, colorido (usar com tint = Color.Unspecified)
    p = 22 / 800
    (DRAWABLE / "ic_cgflix_morango.xml").write_text(
        vetor(
            "morango do Modo Maryanne (colorido, sem tint)",
            24, 24, 24, 24, grupo(p, 12 - 500 * p, 12 - 550 * p, morango_vetor()),
        )
    )
    (SAIDA_SVG / "marca.svg").write_text(marca(svg=True))
    (DRAWABLE / "ic_banner_maryanne.xml").write_text(marca(svg=False))


if __name__ == "__main__":
    main()
