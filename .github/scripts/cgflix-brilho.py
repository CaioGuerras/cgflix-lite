#!/usr/bin/env python3
"""CGFLIX: brilho do fundo de capturas PNG (0 = preto, 1 = branco), só com a biblioteca padrão.

Média da luminância de uma faixa de 16 px na borda esquerda, do 1/4 aos 3/4 da altura (fundo da
tela, sem barra de status). Lê só as primeiras colunas de cada linha: os filtros do PNG dependem
apenas de pixels à esquerda e acima. Uso: cgflix-brilho.py <arquivo.png>...
"""
import struct
import sys
import zlib

FAIXA = 16


def brilho(caminho):
    with open(caminho, "rb") as f:
        dados = f.read()
    pos, idat = 8, b""
    while pos < len(dados):
        tam, tipo = struct.unpack(">I4s", dados[pos : pos + 8])
        corpo = dados[pos + 8 : pos + 8 + tam]
        if tipo == b"IHDR":
            larg, alt, prof, cor, _, _, entrel = struct.unpack(">IIBBBBB", corpo)
        elif tipo == b"IDAT":
            idat += corpo
        pos += 12 + tam
    if prof != 8 or cor not in (2, 6) or entrel:
        raise ValueError("PNG não suportado")
    bpp = 4 if cor == 6 else 3
    linha = larg * bpp
    cru = zlib.decompress(idat)
    n = FAIXA * bpp
    ant = bytearray(n)
    soma = conta = 0
    for y in range(alt):
        base = y * (linha + 1)
        filtro = cru[base]
        atual = bytearray(cru[base + 1 : base + 1 + n])
        for i in range(n):
            a = atual[i - bpp] if i >= bpp else 0
            b = ant[i]
            c = ant[i - bpp] if i >= bpp else 0
            if filtro == 1:
                atual[i] = (atual[i] + a) & 255
            elif filtro == 2:
                atual[i] = (atual[i] + b) & 255
            elif filtro == 3:
                atual[i] = (atual[i] + (a + b) // 2) & 255
            elif filtro == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                atual[i] = (atual[i] + pred) & 255
        if alt // 4 <= y < 3 * alt // 4:
            for x in range(FAIXA):
                r, g, bl = atual[x * bpp : x * bpp + 3]
                soma += 0.2126 * r + 0.7152 * g + 0.0722 * bl
                conta += 1
        ant = atual
    return soma / conta / 255


for arquivo in sys.argv[1:]:
    print(f"{arquivo.rsplit('/', 1)[-1].removesuffix('.png')}: {brilho(arquivo):.3f}")
