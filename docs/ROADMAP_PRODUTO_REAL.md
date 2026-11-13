# Pelada League — plano para sair da demonstração e virar um produto real

Atualizado em 13/11/2026. Documento de implementação para continuar no GitHub e no Code on the Go.

## Situação verificável
- 0.4.1-alpha: Android Java + SQLite local, sem autenticação nem sincronização entre aparelhos.
- O aplicativo já consegue cadastrar jogadores locais, criar peladas, registrar presença, formar times, registrar eventos e computar classificação localmente.
- Dados de exemplo são adicionados em uma instalação nova. Eles NÃO são jogadores autenticados.
- Aprovação com PIN de quatro dígitos só demonstra o fluxo em um aparelho. Não pode ser anunciada como aprovação oficial online.
- A interface recebe tour contextual de primeira abertura e controles primários com luz neon animada. A instalação/funcionalidade devem ser verificadas no dispositivo antes de divulgar.

## Experiência-alvo da pessoa comum
1. Instala, vê a apresentação e o tutorial interativo (Próximo, Voltar, Pular).
2. Cria uma conta individual e seu jogador.
3. Participa de uma pelada por convite/solicitação, ou cria sua pelada como organizador.
4. Confirma presença na sessão, acompanha vagas/espera e recebe as alterações nos demais celulares.
5. Organizador sorteia somente os confirmados; atletas recebem seus times.
6. Mesário registra gols, assistências e cartões; responsáveis autorizados revisam a súmula.
7. Dois responsáveis distintos aprovam a versão exata da súmula; classificação e carreira atualizam.
8. Consulta sua história, calendário e conquistas. Cada ação mostra feedback inteligível e estado vazio útil.

## Prioridade A — usabilidade e confiabilidade local
- Concluir e testar: tutorial contextual, repetição em Informações, botões animados sem drenar bateria, botões com área de toque mínima.
- Fluxo de primeiro uso sem se confundir com dados de demonstração; identificação clara do modo local.
- Testes manuais do fluxo completo: cadastrar grupo → cadastrar membro → confirmar presença → sortear → criar partida → registrar gol/cartão → enviar → aprovar na demonstração → consultar tabela e perfil.
- Persistir dados após fechar/reabrir, manter atualização de APK sem desinstalar.
- Corrigir qualquer quebra encontrada antes de implementar assinaturas.

## Prioridade B — contas e sincronização
Stack inicial recomendada para teste: Supabase (Postgres, Auth, RLS, Edge Functions), ou serviço equivalente. Exige criar/configurar um projeto backend; GitHub sozinho não hospeda nem autentica a aplicação.

Modelo mínimo:
- users / profiles / player_profiles
- groups / group_memberships / invitations / group_roles
- sessions / attendance / waitlist_order
- draws / draw_players / games / game_roster
- match_events / reports / report_versions
- approvals / audit_events

Critérios obrigatórios:
- Cada jogador tem conta própria e suas permissões são verificadas NO SERVIDOR.
- Organizador não pode agir como capitão de outro usuário apenas mudando opção no celular.
- Atualização entre celulares em tempo real ou por sincronização confiável, com controle de versão e conflito.
- Operações críticas idempotentes e auditadas.
- Política de privacidade e proteção de dados pessoais; nada de credenciais secretas embutidas no APK.

## Prioridade C — súmula de verdade
- Enviar uma versão imutável da súmula com escalações/eventos.
- Código EXATAMENTE de 4 dígitos, gerado no aplicativo com verificação no servidor (sem SMS ou QR Code).
- Código temporário de uso único, limitado por tempo/tentativas e vinculado à conta designada e à versão da súmula.
- Aprovação por responsáveis distintos e autenticados, vinculados às equipes.
- Mudou a súmula? Invalidar códigos e aprovações anteriores.
- Classificações e estatísticas oficiais somente após aprovações completas.

## Prioridade D — experiências premium e escala
- Estilo grafite/preto/dourado, elementos esportivos autorais, acabamento premium.
- Luz neon dourada percorrendo contornos dos principais botões e microinterações nos cartões.
- Transições suaves entre telas; sorteio com revelação de equipes; gol com animação de placar.
- Respeitar as preferências do Android para redução de movimento e acessibilidade.
- Minimizar animações contínuas, consumo de bateria e uso de memória.
- Campeonato por grupo, temporadas e estatísticas verificáveis; funcionalidades comerciais só depois do fluxo principal consolidado.

## Critérios de aceite para uma versão beta utilizável
- Instalação nova: tutorial aparece uma vez, permite Pular/Voltar/Concluir e reabrir em Informações.
- Nenhum botão faz ação fictícia sem aviso; estados desabilitados explicam o motivo.
- Dois celulares diferentes podem participar do mesmo grupo real e ver a mesma presença.
- Confirmação acima da capacidade produz espera; retirada promove a pessoa correta.
- Sorteio não inclui ausentes/pendentes e sobrevive a reinício.
- Súmula não pode ser publicada por uma única identidade simulando duas pessoas.
- Histórico, ranking e placar são iguais em dois aparelhos e sobrevivem a reinício.
- CI Android verde; instalação debug no Code on the Go; revisão visual em aparelho pequeno/grande.

## Próximo trabalho de engenharia
Primeiro validar 0.4.1-alpha no Code on the Go e verificar o percurso offline. Depois implementar backend e autenticação reais. Não confundir compilar um APK com entregar uma plataforma multiusuário pronta para produção.
