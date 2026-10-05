# CGFLIX Lite — fork do Findroid

Base: [Findroid](https://github.com/jarnedemeulemeester/findroid) (Kotlin nativo, GPL-3.0; Jellyfin; mpv + ExoPlayer).
Este é o app **leve** do CGFLIX, para celular simples e TV box. Irmão completo: `CaioGuerras/cgflix-app` (fork do Plezy).
Atenção: o Findroid NÃO tem interface de Android TV (pedido nº 1 dele, issue #927); TV fica para uma etapa futura.


## Contexto (leia antes de tudo)
- **CGFLIX** é um servidor de mídia particular de família e amigos no Brasil (Jellyfin principal; Plex também). Endereço público
  sugerido do Jellyfin: `https://netflix.docaio.com.br`. Público: brasileiros, pouco técnicos, celulares de todo tipo e TV box.
- O dono é o **Caio**. Comente código novo e escreva commits, PRs e docs em **português do Brasil**.
- O servidor **não transcodifica vídeo** (`EnableVideoPlaybackTranscoding=false` nos usuários): o app tem de tocar direto
  (direct play). Anime tem legenda **ASS** com estilo (precisa de libass/mpv). Preferência de áudio e legenda: `por`.
  Legendas externas nossas: `<vídeo>.por.srt`.
- Login: usuário + senha do Jellyfin, ou **Quick Connect** (ligado no servidor). Usuários ficam ocultos na tela de login.
- Limite de **2 telas** por pessoa (plugin StreamLimiter): quando estoura, o servidor recusa o play; mostrar mensagem clara em PT.

## Regras do fork
1. **Diferença mínima do original** (para puxar as atualizações do upstream toda semana sem conflito): marca, endereço sugerido
   e padrões em arquivos próprios/isolados; não reformatar nem renomear o que não precisa.
2. **Sem servidor embutido**: `https://netflix.docaio.com.br` vem SUGERIDO/pré-preenchido e é editável ("Outro servidor").
3. Sem segredos, tokens, chaves ou telemetria no código, nos logs e nos workflows.
4. Manter a licença e os créditos do projeto original (tela "Sobre" + README). Nome e logo do app passam a ser **CGFLIX**;
   não usar o nome/logo do upstream como marca do app.
5. Identidade visual: preto OLED `#07060a`, superfícies `#120e1a`, roxo `#9333ea` (destaque `#a855f7`, lilás `#c084fc`),
   fonte Inter. Marca em `cgflix-brand/` (emblema "C com play", ícone quadrado, marca horizontal, PNG 512).
6. Trabalhe num ramo e abra **Pull Request para o `main` deste repositório** (nunca para o upstream), com descrição em PT-BR:
   o que mudou, como testar, riscos. Não faça merge sozinho.

## Etapa 0 (esta sessão): APK do CGFLIX Lite compilando sozinho
1. Ler o projeto (README, Gradle, módulos, workflows existentes) e entender como o upstream gera o APK.
2. Marca: nome exibido "CGFLIX Lite", `applicationId` `br.com.docaio.cgflix.lite` (sem conflitar com o Findroid instalado),
   ícone adaptativo a partir de `cgflix-brand/`, tema escuro com o roxo do CGFLIX onde o app já tem cor de destaque configurável.
3. Tela de adicionar servidor: `https://netflix.docaio.com.br` **sugerido** (pré-preenchido, editável). PT-BR: completar as strings
   em `values-pt-rBR` se estiverem faltando (aproveitar e preparar a contribuição dessa tradução para o upstream).
4. **GitHub Actions** `.github/workflows/cgflix-android.yml`: APK de release a cada push no `main` e em PRs (assinatura de debug
   por enquanto, pronta para keystore por secrets), artifact; em tags `v*`, Release com o APK. Neutralizar workflows do upstream
   que dependam de segredos que não temos.
5. Rodar `./gradlew assembleRelease` (ou o equivalente do projeto) no contêiner se for viável.
6. `CGFLIX.md`: mudanças em relação ao upstream, como sincronizar, próximos passos (interface de TV, selos Dublado/Legendado,
   aviso das 2 telas, mpv como padrão para anime, PRs úteis do upstream: #1228 temporada inteira, #1285 offline automático,
   #1293 download no app, #1253 autoplay do servidor).
7. PR para o `main` deste fork.
Pronto quando: PR aberto, workflow verde (ou erro explicado no PR), APK como artifact.
