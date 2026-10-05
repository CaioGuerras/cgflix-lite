# CGFLIX Lite — mudanças em relação ao Findroid

O CGFLIX Lite é um fork do [Findroid](https://github.com/jarnedemeulemeester/findroid) (GPL-3.0), cliente Jellyfin nativo
em Kotlin, para celulares simples. O irmão completo é o `CaioGuerras/cgflix-app` (fork do Plezy).
A licença (GPL-3.0) e os créditos do projeto original são mantidos (`LICENSE`, `README.md` e tela **Sobre** do app).

## O que mudou (Etapa 0)

| Área | Mudança | Arquivos |
|---|---|---|
| Identidade | `applicationId` `br.com.docaio.cgflix.lite`; nome "CGFLIX Lite" (debug/staging com sufixo) | `app/phone/build.gradle.kts`, `core/src/*/res/values/strings.xml` |
| Ícone e logo | Ícone adaptativo (fundo `#07060a` + emblema "C com play") e marca horizontal no lugar do logo do Findroid, gerados de `cgflix-brand/` | `core/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_banner*.xml`, `values/ic_*_background.xml` |
| Cores | Roxo `#9333ea`/`#a855f7`/`#c084fc`, preto OLED `#07060a`, superfícies `#120e1a`; tema escuro e "cores dinâmicas" desligadas por padrão (o usuário ainda pode mudar nas configurações) | `core/.../theme/Color.kt`, `core/src/main/res/values/colors.xml`, `settings/.../AppPreferences.kt` |
| Servidor | `https://netflix.docaio.com.br` vem **sugerido** (pré-preenchido e editável) na tela de adicionar servidor; nada embutido | `core/src/main/res/values/cgflix.xml`, `AddServerScreen.kt` (phone) |
| PT-BR | `values-pt-rBR` já estava completa em todos os módulos (core, setup, settings, player/local, modes/film); só ajustamos nome e descrição. Fica pronta para contribuir ao upstream | `core/src/main/res/values-pt-rBR/strings.xml` |
| CI | `cgflix-android.yml` gera o APK de release (artifact) em push no `main` e PRs; em tags `v*` cria Release. `build.yaml` e `publish.yaml` do upstream ficaram só manuais (`publish` desativado: usa keystore e Google Play do Findroid) | `.github/workflows/` |

Não mexemos no módulo `app/tv` (o Findroid não tem interface de Android TV de verdade).

## Assinatura do APK

Sem configuração, o release é assinado com a chave de **debug** (instala, mas não serve para loja e muda a cada runner —
atualizar por cima exige a mesma chave). Para assinar de verdade, crie estes *secrets* no GitHub:
`CGFLIX_KEYSTORE_BASE64` (keystore em base64), `CGFLIX_KEYSTORE_PASSWORD`, `CGFLIX_KEY_ALIAS`, `CGFLIX_KEY_PASSWORD`.
Nada de segredo fica no repositório.

## Como sincronizar com o upstream

```bash
git remote add upstream https://github.com/jarnedemeulemeester/findroid.git   # só na primeira vez
git fetch upstream
git checkout -b sync/upstream-AAAA-MM-DD main
git merge upstream/main        # resolver conflitos; os pontos de atrito são os arquivos da tabela acima
```

Depois abra PR para o `main` **deste** repositório (nunca para o upstream). Para manter os conflitos pequenos, as mudanças
próprias ficam em poucos arquivos e as demais telas seguem como no original.

## Próximos passos

- Interface de Android TV (o Findroid não tem; issue #927 do upstream).
- Selos **Dublado** / **Legendado** nos itens.
- Aviso claro em PT quando o limite de **2 telas** (StreamLimiter) recusar o play.
- **mpv como player padrão** para anime (legenda ASS com estilo).
- Preferência de áudio e legenda `por` por padrão.
- PRs úteis do upstream: #1228 (temporada inteira), #1285 (offline automático), #1293 (download no app),
  #1253 (autoplay do servidor).
- Keystore própria do CGFLIX nos secrets, para atualizações por cima.
