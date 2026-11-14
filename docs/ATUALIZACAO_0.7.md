# Pelada League 0.7.0-alpha · Redesign AMOLED

Atualização real do aplicativo Android nativo existente. Nenhuma funcionalidade, tabela SQLite, identificador Android ou contrato de servidor foi removido. **Custo adicional: R$ 0.**

## Direção visual implementada

- Preto AMOLED real em toda a janela e barras Android: \`#000000\`.
- Âmbar quente \`#FFB52E\`, textos claros \`#F6F5F1\`, cartões grafite \`#0D0D0F\` e bordas discretas \`#2C2B2E\`.
- Botões sem neon giratório. O principal é âmbar fixo com **ripple preto** ao toque, compressão discreta e resposta tátil; o secundário é preto com borda âmbar.
- Cartões arredondados, ícones vetoriais e tipografia mais consistente em todas as telas por meio de \`Ui.java\`.
- Navegação inferior de quatro áreas com seleção suave e alvo de toque de 48dp ou mais.
- Introdução e dashboard da pelada com hierarquia visual esportiva, atalhos rápidos, cartão do próximo jogo e último placar.
- Placar de jogo com nomes das equipes, camisetas vetoriais, gols centralizados e situação da súmula.
- Campo tático **vetorial 2.5D original**: distribuição por GOL/ZAG/MEI/ATA, camisetas nas cores dos times e nome real de cada jogador sorteado. Sem números ou reservas inventados.
- Cartões de estádio deixam de usar imagem fotográfica e passam a usar traços vetoriais originais em fundo preto.
- Tutorial interativo já existente foi preservado e simplificado para **seis etapas**, apenas na primeira entrada ou quando reaberto em Informações. Pular, Voltar, Próximo e Concluir continuam funcionais.
- Transições curtas com \`Motion.java\`; animações respeitam as configurações de redução/desativação de movimento do Android.

As imagens de referência enviadas pelo usuário orientam a composição, não são incorporadas nem copiadas como recursos protegidos. O objetivo é uma identidade própria.

## Funções preservadas

1. Peladas, grupos, presença, fila de espera e sorteio equilibrado.
2. Histórico local, súmulas, gols, cartões, substituições e relógio.
3. Financeiro por sessão, confirmação manual e isenção de goleiro.
4. Temporadas locais, classificação, artilharia e premiações.
5. Telas online de contas, convites, eventos semanais, pagamentos e placar provisório, **dependentes de Supabase Free configurado**.

## Testes

- \`DesignTest\`: fundo preto exato, âmbar, botão clicável com ripple e desenho da formação.
- \`UiTest\`: tutorial, navegação, sorteio, partida, súmula, ligas, perfil e persistência.
- \`DatabaseTest\`: migrações existentes SQLite, histórico, times e temporadas.
- \`FinanceTest\`, \`ClockTest\`, \`ReminderTest\`: regressões anteriores.
- \`scripts/check-supabase-sql.py\` inspeciona padrões básicos nos SQL; **não substitui teste real em servidor**.

## Atualizar pelo Code on the Go

Na pasta real do Git \`/storage/emulated/0/CodeOnTheGoProjects/PeladaLeague/Pelada_League\`:

\`\`\`sh
git pull --ff-only
./gradlew :app:assembleDebug --no-daemon
\`\`\`

APK esperado: \`app/build/outputs/apk/debug/app-debug.apk\`. Para preservar dados, **não desinstale** a versão anterior; atualize com o mesmo certificado de assinatura.

## Ainda não finalizado

- Campo 3D verdadeiro com câmera e iluminação volumétrica; o atual é Canvas 2.5D.
- Validação de súmula por duas contas autenticadas e placar oficial de campeonato.
- Push instantâneo de gols/convites para celulares fechados.
- Backend Supabase, SMTP e publicação da função de convite não configurados nem testados em duas contas reais.
- Testes de uso com público amplo, otimização fina de acessibilidade, LGPD e eventual lançamento.

O redesign não depende de pacote pago nem de APIs de clubes reais.
