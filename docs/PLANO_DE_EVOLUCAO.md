# Pelada League — evolução planejada

## Critérios de saída da versão local

- Navegação, telas e banco de dados demonstrados no Android.
- Sorteio de times com jogadores sem duplicação.
- Registro de gols gerando placar correto.
- Tabela calculada de partidas aprovadas.
- Tela de autorização com quatro dígitos como experiência de demonstração.

## Marco 1 — Identidade e permissões reais

- Supabase Auth ou equivalente com contas individuais.
- Modelo `users`, `memberships`, `players`, `group_roles` e `match_officials`.
- Regra de aprovação de contas distintas e representantes previamente designados.
- Política RLS e funções de backend; jamais enviar chaves `service_role` no APK.
- Negar publicação de resultados não autenticados.

## Marco 2 — Súmula confiável

- Report versionado e imutável após envio, hash de eventos canonizados.
- Capitão concorda com a versão na própria sessão autenticada.
- Código de quatro dígitos gerado no servidor somente depois da concordância; 3 minutos; uso único; limite de tentativas por conta, IP e partida.
- São necessários tokens distintos e duas aprovações independentes.
- Em alterações, versão nova invalida credenciais e aprovações anteriores.
- Auditoria de revisão, contestações e mediação.
- Ranking derivado apenas de eventos pertencentes a súmulas oficializadas no servidor.

## Marco 3 — Futebol completo

- Assistências, cartões, gols contra, substituições e minuto de jogo.
- Peladas rotativas, campeonatos por grupos, pontos corridos e mata-mata.
- Fichas de atletas, escalações por equipe, goleiros e balanceamento adaptativo.
- Cards para compartilhar e notificações.

## Marco 4 — Negócio

- Plano free competitivo; assinatura de organizadores e arenas.
- Cobrança e entitlements verificados pelo servidor.
- Painel de administrador comercial, métricas, suporte, LGPD e políticas Play.
- Testes com grupos reais antes de expandir.

## Não prometer

- Autenticidade de gols físicos sem validação humana independente.
- Capacidade antifraude de códigos apenas locais.
- Compilação Android não executada neste ambiente.
- Identidade visual perfeitamente idêntica à imagem sem revisão visual em aparelhos reais.
