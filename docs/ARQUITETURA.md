# Arquitetura 0.4

## Separação de responsabilidades

| Unidade | Responsabilidade |
|---|---|
| MainActivity | Lifecycle, navegação/back stack, estado selecionado, executor do sorteio |
| Welcome/Home/Group/Draw/Match/Approval/League/ProfileScreen | Composição nativa de cada fluxo |
| Screen | Componentes comuns de tela, placar, tabela e banner |
| Forms / EventForm | Formulários com validação e edição de eventos |
| Ui / SportArt / ArenaBackdrop | Tema, tipografia, componentes, ícones e arte vetorial original |
| Motion | Transições e microinterações respeitando o Android Animator Duration Scale |
| DemoDb | Repositório SQLite local, transações, migrações e validações de persistência |
| core | Regras Java independentes de Android; sorteio, presença e classificação |

A escolha continua sendo Java e Android View nativos, sem WebView, servidor web embutido ou migração para outro framework. Telas programáticas usam dp/sp e altura de texto adaptável. Os desenhos são gravados em Picture uma vez; animações alteram alpha/scale/translation/rotation no compositor. O cálculo do sorteio ocorre em executor fora da thread visual.

A navegação salva grupo, partida, abas, seleção de participantes, histórico de navegação e rolagem na recriação. Preferências locais guardam o jogador e papel de teste; o banco guarda cadastros, presenças, partidas, eventos, aprovações e sorteios.

## Migração

O nome `pelada_league_demo.db` e o applicationId são preservados. Schema v1→v2 existente continua ativo; v2→v3 adiciona `saved_draws` e `audit`, sem remover tabelas de usuários. Não são recriados jogadores ou partidas durante upgrade.

`saved_draws` referencia a sessão e uma impressão determinística do conjunto confirmado/capacidade. Mudança de presença, nova sessão ou ajuste de capacidade invalida o sorteio. As escalações de partidas já criadas permanecem como registro histórico. Nunca se remove retroativamente alguém da súmula só porque faltou a uma sessão posterior.

Súmulas antigas sem escalação continuam disponíveis para consulta; não aceitam novos eventos em nome de qualquer pessoa. Uma nova partida exige escalação válida. Histórico antigo permanece preservado, inclusive as estatísticas já registradas na demonstração.

## Limites e próximos passos

- O aparelho ainda é um ambiente local de demonstração; não é uma fronteira de segurança contra seu proprietário.
- Papéis locais escondem/bloqueiam ações na interface, mas não comprovam identidade. O backend deverá autorizar toda mutação por usuário e vínculo.
- Convites compartilham informação da pelada; não simulam links de ingresso ou sincronização.
- A tabela atual agrega as equipes da pelada por pontos. Entidades próprias de competição, temporadas, grupos e mata-mata são a próxima etapa; não há títulos fictícios.
- Substituições, notificações push, login Google e fotos online não foram simulados como recursos prontos.
- Ainda é necessário testar desempenho e consumo em aparelhos físicos. Um emulador sem aceleração não comprova 60 FPS.

## Contrato para evolução online

Criar identidades `users` separadas de `players`; vínculos `memberships` com papéis por grupo e `match_officials` por partida. Autorizações executadas no servidor. Cada aprovação vincula responsável distinto, versão imutável da súmula e nonce de uso único com quatro dígitos. Guardar expiração, tentativas, hash/salt e auditoria. Uma correção cria nova revisão e invalida aprovações da anterior. Nunca publicar rankings oficiais a partir do status `DEMO_APPROVED` local.
