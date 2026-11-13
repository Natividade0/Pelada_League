# Pelada League 0.6.0-alpha — continuidade sem gastos

**Status:** funcionalidades locais implementadas no GitHub; sincronização online depende de criar/configurar o servidor gratuito. Não foi contratado serviço algum nesta etapa.

## Mudanças desta atualização

### No celular, mesmo sem internet/servidor
- Temporadas por pelada, com histórico preservado em migração do SQLite da versão 4 para 5.
- Organizador pode encerrar a temporada anterior e iniciar uma nova.
- Premiações automáticas locais somente com súmulas `DEMO_APPROVED`: artilharia, assistências e maior número de partidas.
- Tabela e artilharia com filtro por temporada. Premiações podem ser compartilhadas.
- Módulos anteriores da 0.5 permanecem: financeiro manual, Pix informativo, goleiros isentos, sorteio tático animado 2.5D, cronômetro e substituições.

### Depois de conectar um projeto Supabase Free
- Criar uma partida única ou 4/8 eventos semanais antecipadamente.
- Convites com código exclusivo por evento; confirmação individual e lista de espera no servidor.
- Lembretes opcionais neste aparelho, 24h e 2h antes do evento. Não são push para outros celulares. O Android pode atrasar alarmes inexatos, especialmente com economia de energia.
- Central de placar online provisório: organizador inicia, pausa e encerra; registra gols, cartões e substituições. Jogadores inscritos visualizam o mesmo placar consultando o servidor a cada 15 segundos com a tela aberta (45 segundos antes/depois).
- O cronômetro online é calculado a partir do relógio do servidor. Estatísticas online ainda não são validadas por súmula de árbitro/capitães.

## Como habilitar o online sem pagar

1. Crie um projeto gratuito em https://supabase.com/dashboard.
2. Em SQL Editor, execute, **nesta ordem**, os arquivos:
   - `backend/supabase/001_core.sql`
   - `backend/supabase/002_recurring_events.sql`
   - `backend/supabase/003_live_match.sql`
3. Em Supabase Auth, mantenha a confirmação de e-mail e configure SMTP adequado. O SMTP padrão do Supabase serve para teste, com restrições; não é indicado para muitos cadastros.
4. No arquivo **local.properties** na raiz do projeto do Code on the Go (ignorado pelo Git), adicione as propriedades abaixo sem colocar chaves secretas:
```properties
supabase.url=https://SEU_PROJETO.supabase.co
supabase.anonKey=CHAVE_PUBLICAVEL_OU_ANON_DO_PROJETO
invite.enabled=false
```
5. Compile e teste cadastro e login com duas contas separadas. Para convite WhatsApp, publique a função `backend/supabase/functions/invite/index.ts` como função pública (Verify JWT desligado), teste a URL e então mude `invite.enabled=true` e recompile.
6. Em **Eventos e convites online**, confirme presença, consulte a central e autorize os lembretes se quiser. O Android 13+ poderá solicitar permissão de notificações.

**Nunca** cole chaves `service_role`, `secret`, senhas ou tokens no GitHub ou em capturas.

## Testes essenciais

### Testar local
1. Abra **Ligas → Temporadas**.
2. Confira a `Temporada inicial` com as partidas antigas ainda visíveis.
3. Crie `Temporada 2027` e confirme que a artilharia inicia limpa.
4. Registre uma partida e aprove localmente a súmula. Confirme que artilharia e premiações aparecem **apenas** na temporada nova.
5. Reinicie o app para validar persistência, sem desinstalá-lo.

### Testar online depois de configurar servidor
1. Conta A cria pelada e quatro semanas de eventos.
2. Conta B abre convite, confirma posição e presença, sem entrar indevidamente na lista de outra semana.
3. Conta A configura pagamentos; somente autorizados conseguem registrar recebimentos.
4. Conta B autoriza lembretes no Android (sem automatismo em celulares que nunca abriram o aplicativo).
5. Em evento com quatro confirmados, Conta A inicia cronômetro online e registra gol/cartão.
6. Conta B abre a central em outro aparelho e observa os mesmos resultados após atualização do servidor.
7. Teste negar acesso a pessoa de fora do evento e jogador tentando escrever lances.

### Regressões Android
- `DatabaseTest`: migração V2→V5, temporadas, persistência e substituições.
- `FinanceTest`: pagamento manual e isenção por goleiro.
- `ClockTest`: pausa, continuidade e persistência do relógio local.
- `ReminderTest`: fuso horário e revogação de consentimento.
- `UiTest`: navegação, telas, sorteio, placar e aprovação da demo.

## Escopo ainda não concluído
- Notificação **push instantânea** aos demais jogadores quando o organizador cria eventos ou marca gols; os lembretes desta versão dependem de abertura prévia e permissão no dispositivo.
- Classificação e temporadas com dados oficiais sincronizados pelo servidor, escalação exclusiva do técnico em **campeonatos**, permissão de árbitro, relatórios definitivos e aprovação segura em duas contas.
- Campo de futebol em **3D real**. A apresentação atual é 2.5D Canvas, não OpenGL/Unity/Godot.
- Sincronização offline robusta, recuperação de conflitos, edição/cancelamento dos eventos recorrentes, correção de lances online e comprovação bancária Pix.
- Testes de backend em projeto Supabase real, testes de carga e segurança, LGPD, publicação Play Store.

## Custo

**Nenhuma compra, plano pago, domínio ou serviço foi contratado.** Mantenha todas as configurações dentro das faixas gratuitas; nunca faça upgrade sem autorização. Seu limite total autorizado continua no máximo R$ 50, e ele não foi utilizado nesta etapa.
