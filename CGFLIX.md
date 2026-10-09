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

## O que mudou (Etapa 1B, versão 1.2.0 (40))

Redesenho **leve** (aparelho fraco, internet ruim), na direção do app completo 1.3.0. Código novo isolado em
`app/phone/src/main/java/dev/jdtech/jellyfin/cgflix/` (regras sem Android em `cgflix/logic/`, com testes em
`app/phone/src/test/.../cgflix/logic/`). Nos arquivos do Findroid, só ganchos curtos marcados com `CGFLIX`.

| Área | Mudança | Arquivos |
|---|---|---|
| Cores | Todas as cores da marca numa paleta só (`CgflixPalette`, tema escuro "Isis": fundo `#07060a`, destaque `#a855f7`, texto ≥ 4,5:1). O esquema escuro do Material sai dela; as telas usam `MaterialTheme.cgflix` para selos, número do Em alta, esqueleto e corações. Tema claro "Heitor" = outra paleta, sem mexer nas telas | `core/.../theme/CgflixPalette.kt`, `core/.../theme/Color.kt`, `presentation/theme/CgflixTheme.kt`, `Theme.kt`, `CgflixButtons.kt`, `AboutScreen.kt` |
| Tema fixo | Sempre o tema Isis (escuro), inclusive no Android 12+, que seguia o sistema e ficava claro; o grupo Aparência saiu das Configurações (commit do servidor CGSERVER em 08/10). **Na 1.3.0** volta como escolha Isis/Heitor/Automático (ver abaixo) | `BaseApplication.kt`, `presentation/theme/Theme.kt`, `SettingsViewModel.kt` |
| Ícones | Material Symbols Rounded só como vetores dos ícones usados (contorno no inativo, preenchido no ativo). Licença Apache 2.0 (Google) | `core/src/main/res/drawable/ic_cgflix_*.xml` |
| A. Navegação | Barra inferior Início · Buscar · Baixados · Você (Material 3: 80 dp, pílula 64×32, rótulos sempre visíveis). "Você": Meus pedidos, Bibliotecas, Favoritos, Trocar usuário, Configurações, Sobre | `NavigationRoot.kt`, `cgflix/you/CgflixYouScreen.kt`, `core/.../values/cgflix.xml` |
| A/B. Categorias | Chips Filmes · Séries · Animes no topo da Início. Categoria = **biblioteca** (Id achado pelo nome "Séries"/"Animes"), nunca o tipo do item; toda linha filtra por `ParentId`. Sem biblioteca "Animes", o chip some | `cgflix/logic/CgflixCategories.kt`, `cgflix/home/CgflixCategory*.kt` |
| C. Início | Continuar assistindo → Em alta no Brasil → Filmes/Séries/Animes recentes. Sem banner, sem "Novos episódios" soltos. Cada linha carrega sozinha (em paralelo), esqueleto parado, volta mantém a rolagem, sem recarregar se < 60 s. A Início do Findroid (`HomeScreen.kt`/`HomeViewModel`) segue no código, só não é usada | `cgflix/home/CgflixHome*.kt`, `cgflix/ui/CgflixComponents.kt` |
| C. Em alta | `GET <servidor>/cgflix/emalta.json` (Início usa `itens`; categoria usa `porBiblioteca`, some com < 3; sem o campo, filtra `itens` por `biblioteca`). Se falhar: coleção "Em alta no Brasil"; se nada, a linha some. Número grande vazado desenhado em Compose. Cache de 1 h (memória e disco) | `cgflix/logic/CgflixTrending.kt`, `cgflix/CgflixRepository.kt` |
| D. Busca e pedidos | Uma busca só: primeiro o que temos, depois "Disponível para pedir" (Seerr, `language=pt-BR`; 1/ausente = Pedir, 2 = Pedido, 3 = Baixando, 4/5 não aparecem). Filme pede direto, série pede todas as temporadas. Entrada automática pelo Quick Connect (initiate → `/QuickConnect/Authorize` com o token da pessoa → authenticate → cookie `connect.sid`), sem formulário nem WebView. Seerr achado ao lado do Jellyfin (`netflix.` → `pedidos.`). Cookie cifrado com chave AES-GCM do Android Keystore e renovado sozinho em 401/403. Fora do ar: "Pedidos indisponíveis agora" | `cgflix/logic/CgflixSeerr.kt`, `cgflix/logic/CgflixOkHttp.kt`, `cgflix/CgflixSecureStore.kt`, `cgflix/search/*` |
| D. Meus pedidos | Lista nativa (título, ano, situação). Pôster só com caminho que passa na regra do site, pelo proxy do Seerr | `cgflix/you/CgflixMyRequestsScreen.kt` |
| E. Título | Botão principal grande (52 dp) "Assistir"/"Continuar"/"Continuar S01E03"; selos Dublado (áudio `por`) e Legendado (legenda `por`/`pob`); temporadas em chips com os episódios embaixo e o próximo já selecionado | `PlayButton.kt`, `ItemButtonsBar.kt`, `ShowScreen.kt`, `MovieScreen.kt`, `EpisodeScreen.kt`, `cgflix/title/CgflixShowExtras.kt`, `cgflix/logic/CgflixLanguageBadges.kt` |
| F. Player | Áudio e legenda num menu pequeno preso ao botão (o vídeo continua tocando), em vez da janela por cima | `PlayerActivity.kt`, `cgflix/player/CgflixTrackMenu.kt` |
| G. Música tema | `<servidor>/__tema/<tvdb>.mp3` na página da série, **desligada por padrão** (Configurações > Interface). 404 = silêncio. Para ao sair da tela | `AppPreferences.kt`, `SettingsViewModel.kt`, `settings/.../values/cgflix.xml`, `CgflixShowExtras.kt` |
| G. Nome | O app só leva os textos em português do Brasil e inglês (`localeFilters`): as traduções do Findroid nos outros idiomas ainda diziam "Findroid" na tela. Atribuição GPL-3.0 continua no Sobre | `app/phone/build.gradle.kts` |
| Slogan | "Aperte o play", parado, em lilás, no login e na abertura (nunca dedicatória fora do Sobre) | `LoginScreen.kt`, `WelcomeScreen.kt`, `MainActivity.kt` |
| H. Acessibilidade | Descrição em português em todo botão só com ícone; "Remover animações" do sistema vira corte seco entre telas | vários `presentation/**` (marcados), `NavigationRoot.kt` |
| CI | Testes de unidade das regras (`testLibreDebugUnitTest`) no "CGFLIX Android". Novo "CGFLIX Telas": emulador com capturas (tela de demonstração só do build de debug, dados falsos) e abertura a frio 1.1.0 × 1.2.0, instalando por cima | `.github/workflows/cgflix-*.yml`, `.github/scripts/cgflix-telas.sh`, `app/phone/src/debug/` |

Ajustes de Início do Findroid em Configurações: "Sugestões" e "Próximos" não valem mais (a Início do Lite não tem essas
linhas); "Continuar assistindo" e "Recentes" continuam valendo.

Não mexemos no módulo `app/tv` (o Findroid não tem interface de Android TV de verdade).

## O que mudou (tema Heitor, versão 1.3.0 (50))

Segundo tema, **Heitor** (claro, verde; semente `#34C759`, Material 3 `SchemeContent`), ao lado do **Isis** (escuro, roxo,
que não muda e continua o padrão). As telas não mudaram de código: leem as cores do tema ativo (`MaterialTheme.colorScheme`
e `MaterialTheme.cgflix`). Ganchos nos arquivos do Findroid marcados com `CGFLIX`.

| Item | Mudança | Arquivos |
|---|---|---|
| A. Esquema | `lightColorScheme` do Heitor com a paleta oficial (fundo `#f4fcee`, cartões `#ffffff`, barras/campos `#e8f0e3`/`#e2ebde`, texto `#161d16`/`#3d4a3c`, `primary` `#006e28`, item ativo `#b0efb0`, bordas `#6d7b6b`/`#bccbb8`). `CgflixPalette` ganhou a variante Heitor e a paleta da marca segue o tema ativo; botões próprios seguem o esquema. Fundo da janela (antes do Compose) por tema, sem piscar | `core/.../theme/CgflixPalette.kt`, `core/.../theme/Color.kt`, `presentation/theme/CgflixTheme.kt`, `Theme.kt`, `CgflixButtons.kt`, `core/src/main/res/values{,-night}/themes.xml`, `core/.../values/cgflix.xml` |
| B. Camadas | Véu claro e degradê que clareia mais cedo sobre a foto de fundo (título escuro legível); sombra suave nos pôsteres do Heitor (Isis sem sombra); ícones das barras do sistema escuros no Heitor; item ativo em `primary`. **Player sempre escuro** nos dois temas | `ItemHeader.kt`, `ItemCard.kt`, `MovieScreen.kt`, `MainActivity.kt`, `NavigationRoot.kt`, `cgflix/home/CgflixHomeScreen.kt`, `cgflix/ui/CgflixComponents.kt`, `PlayerActivity.kt` |
| C. Escolha | Configurações → Aparência: "Tema: Isis (escuro, roxo) / Heitor (claro, verde) / Automático (segue o aparelho)", padrão Isis, guardado no aparelho (`pref_cgflix_theme`), vale na hora. Substitui a opção claro/escuro do Findroid (não há duas); cores dinâmicas desligadas (trocariam a marca) | `core/.../theme/CgflixThemeChoice.kt`, `cgflix/CgflixThemeMode.kt`, `BaseApplication.kt`, `MainViewModel.kt`, `AppPreferences.kt`, `SettingsViewModel.kt`, `settings/.../values/cgflix.xml`, `SettingsScreen.kt` |
| D. Logo Heitor | `cgflix-brand/heitor/*.svg` gerados de `cgflix-brand/*.svg` só com a troca de cores da ordem (`gerar.py`), e o vetor `ic_banner_heitor.xml` usado no login, no Sobre, na abertura e na escolha de servidor/usuário quando o tema é Heitor (vetor, não PNG) | `cgflix-brand/heitor/*`, `core/src/main/res/drawable/ic_banner_heitor.xml`, `LoginScreen.kt`, `AboutScreen.kt`, `WelcomeScreen.kt`, `AddServerScreen.kt`, `ServersScreen.kt`, `UsersScreen.kt` |
| E. Ícone | Glifo "C com play" a ~70% da zona segura (46 de 66 dp) e centrado, fundo `#07060a`; camada **monocromática** própria (ícones temáticos do Android 13+). Ícone único (Isis), não troca por tema | `core/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `core/src/main/res/mipmap-anydpi/ic_launcher.xml` |
| F. Testes | Contraste WCAG dos pares principais do Isis e do Heitor (texto ≥ 4,5:1, bordas/ícones ≥ 3:1) e regra da escolha de tema (testes de unidade). Capturas de Abertura, Login, Início, Detalhes e Configurações nos dois temas no workflow "CGFLIX Telas" (1.2.0 × 1.3.0) | `app/phone/src/test/.../cgflix/theme/*Test.kt`, `app/phone/src/debug/.../CgflixDemoActivity.kt`, `.github/scripts/cgflix-telas.sh`, `.github/workflows/cgflix-telas.yml` |
| Versão | 1.3.0 (50) | `buildSrc/src/main/kotlin/Versions.kt` |

Para regerar a marca Heitor depois de mudar a marca Isis: `python3 cgflix-brand/heitor/gerar.py` (da raiz).

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
- Selos **Dublado** / **Legendado** também nos cartões (hoje só na página do título).
- Aviso claro em PT quando o limite de **2 telas** (StreamLimiter) recusar o play.
- **mpv como player padrão** para anime (legenda ASS com estilo).
- Preferência de áudio e legenda `por` por padrão.
- PRs úteis do upstream: #1228 (temporada inteira), #1285 (offline automático), #1293 (download no app),
  #1253 (autoplay do servidor).
- Interface de TV com a mesma Início por categorias.
- mpv por conteúdo: o Findroid só deixa escolher o player globalmente (Configurações > Player); trocar sozinho para anime/ASS exige código novo.
