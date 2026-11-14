# Pelada League 0.2.0-alpha — evolução local

## Entregue nesta iteração

- **Sessões de pelada e presença persistente**: todos começam pendentes; organizador marca CONFIRMADO, AUSENTE ou aguarda lista de espera; novas sessões reiniciam presença sem apagar jogos anteriores.
- **Sorteio com presenças confirmadas apenas**: jogadores pendentes/ausentes/em espera nunca entram; conferência novamente antes de criar partida; mudança de presença invalida equipes sorteadas na tela.
- **Capacidade e lista de espera**: limite configurável; com lotação, tentativa de confirmar entra em lista de espera; quando um confirmado sai, o primeiro na espera é promovido automaticamente.
- **Sorteio melhorado**: 2, 3 ou 4 equipes (mínimo de dois jogadores por equipe), modos equilibrado e aleatório, força média e distribuição de goleiros, confronto selecionável.
- **Escalações persistidas**: partidas novas vinculam jogadores às equipes; o aplicativo impede lançar gol/cartão em nome de atleta não escalado.
- **Súmula ampliada**: gols, gols contra, assistências opcionais, cartões amarelos/vermelhos, resumo, exclusão em rascunho e bloqueio após envio.
- **Estatísticas mais consistentes**: gols contra contam no placar mas não na artilharia; assistências contabilizadas separadamente; perfil mostra participações apenas com escalação registrada.
- **Movimento e refinamento**: entrada suave de páginas, resposta ao toque, relevo discreto dos cartões, respeito à escala de animação do Android e rotas de retorno.
- **Compartilhamento**: placar em texto com identificação de demonstração para WhatsApp e outros aplicativos.
- **SQLite v1→v2**: migração sem exclusão dos cadastros, partidas ou gols existentes. Novas partidas seguem regras de presença; partidas antigas continuam no histórico.
- **Teste automatizado do core**: verificação de duplicidades, capacidade, elegibilidade, escalações, pontos e gols contra.

## Limitações declaradas

Esta é uma **alfa local**, não o produto final. Não há contas reais, sincronização entre aparelhos, premiações oficiais, controle de acesso por usuário, notificações online ou cobrança. Os códigos de quatro dígitos continuam apenas uma simulação local: não provam validação independente entre dois capitães. Campeonatos de mata-mata, grupos e temporada com regulamento avançado exigem uma etapa própria; a tela atual funciona como ranking/tabela da pelada.

## Orientação de testes

1. Entre na Pelada dos Amigos → Lista de presença. Todos começam pendentes na sessão corrente.
2. Marque pelo menos 4 presentes e teste o sorteio com 2 equipes. Altere uma presença e confirme que o sorteio anterior não é reutilizado.
3. Experimente limite de vagas, lista de espera e promoção automática.
4. Crie uma partida a partir dos times sorteados, registre um gol com assistência, um gol contra e cartões.
5. Confira resumo, placar e artilharia. Envie a súmula e teste o fluxo local de quatro dígitos.
6. Crie uma nova sessão de pelada e verifique que o histórico antigo continua disponível.

© 2026 Pelada League
