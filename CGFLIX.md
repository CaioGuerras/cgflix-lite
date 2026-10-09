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
| Servidor | (Etapa 1A) o campo de endereço começa **vazio**, com dica neutra; nenhum endereço é sugerido nem embutido | `AddServerScreen.kt` (phone), `setup/.../strings.xml` |
| PT-BR | `values-pt-rBR` já estava completa em todos os módulos (core, setup, settings, player/local, modes/film); só ajustamos nome e descrição. Fica pronta para contribuir ao upstream | `core/src/main/res/values-pt-rBR/strings.xml` |
| CI | `cgflix-android.yml` gera o APK de release (artifact) em push no `main` e PRs; em tags `v*` cria Release. `build.yaml` e `publish.yaml` do upstream ficaram só manuais (`publish` desativado: usa keystore e Google Play do Findroid) | `.github/workflows/` |

## O que mudou (Etapa 1A)

Tudo marcado com `CGFLIX` no código, para achar na hora de sincronizar com o upstream.

| Área | Mudança | Arquivos |
|---|---|---|
| Marca nas telas | Boas-vindas e dica do campo de servidor em CGFLIX Lite (créditos do Findroid só em Sobre e README) | `setup/src/main/res/values*/strings.xml` (padrão e pt-BR; outros idiomas seguem o original) |
| Conexão rápida | Botão escondido no login (continua ligado no servidor). Para voltar: `CGFLIX_SHOW_QUICK_CONNECT = true` | `LoginScreen.kt` |
| Início | Não recarrega se a última carga tem menos de 60 s; recargas em segundo plano sem círculo (só 1ª carga, erro ou puxar para atualizar); seções e bibliotecas carregam em paralelo | `HomeViewModel.kt` |
| Botões | Principal em pílula com gradiente roxo; secundário com contorno roxo; foco lilás | `presentation/theme/CgflixButtons.kt` + usos em `PlayButton`, `LoadingButton`, `WelcomeScreen`, `LoginScreen`, `ServerSelectionBottomSheet` |
| Imagens | Pedidas ao servidor com `maxWidth` da tela e `quality=80`; cache de disco 256 MB (era 20); cache de memória 15%; timeouts de 4G | `CgflixImageSizeInterceptor.kt`, `BaseApplication.kt`, `AppPreferences.kt`, `Constants.kt` |
| Sobre | Dedicatória "Feito com amor, para Isis e Heitor" com dois corações (roxo e verde) | `AboutScreen.kt`, `core/.../values/cgflix.xml`, `ic_cgflix_heart.xml` |
| CI | Confere e imprime a assinatura (SHA-256 do certificado, público) e o tamanho do APK | `cgflix-android.yml` |

Não mexemos no módulo `app/tv` (o Findroid não tem interface de Android TV de verdade).

## Assinatura do APK

Os quatro secrets já existem neste repositório (keystore PKCS12, alias `cgflix`) e o workflow assina com eles em push e PRs
deste repositório (PRs de fork não recebem secrets e caem na chave de debug). Sem configuração, o release é assinado com a chave de **debug** (instala, mas não serve para loja e muda a cada runner —
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
- Redesenho visual da Início e das páginas de título (Etapa 1B, depois da pesquisa de interface).
- mpv por conteúdo: o Findroid só deixa escolher o player globalmente (Configurações > Player); trocar sozinho para anime/ASS exige código novo.
