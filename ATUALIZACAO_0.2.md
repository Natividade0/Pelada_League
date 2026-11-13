# ⚽ Pelada League 0.2.0-alpha — atualização do app Android

## Regra principal
**O sorteio usa exclusivamente jogadores CONFIRMADOS na sessão atual.**
Quem está pendente, ausente ou na lista de espera não pode entrar em nenhum time.
Todos começam pendentes em cada nova sessão; marcar presença é obrigatório antes de sortear.
Capacidade configurável e promoção automática dos primeiros na espera quando uma vaga abre.

## Novidades implementadas na alfa local
- Presença persistente com sessões, vagas e lista de espera.
- Sorteio de 2, 3 ou 4 equipes, aleatório/equilibrado, com seleção do confronto.
- Equipes sorteadas invalidadas quando as presenças mudam.
- Escalações persistidas para impedir atribuir gols/cartões a quem não jogou.
- Gols contra, assistências opcionais, cartões amarelos/vermelhos e resumo.
- Artilharia sem gols contra; assistências separadas; histórico preservado.
- Transições suaves, resposta visual/háptica ao toque, placar compartilhável.
- Migração automática do SQLite versão 1 para 2 **sem apagar partidas existentes**.

## Como atualizar uma cópia já clonada no Code on the Go
1. No projeto **Pelada_League** abra a opção de sincronizar com repositório / Git Pull, para obter `apply-v02.sh` e `payload/v02/part00.txt`.
2. Abra o terminal na pasta raiz do projeto, a mesma do `settings.gradle`.
3. Execute: `sh apply-v02.sh`.
4. Verifique a mensagem `Pelada League atualizado para 0.2.0-alpha`.
5. Sincronize o Gradle e execute **Build** novamente.

Se `git pull` falhar por alterações locais ou a IDE não disponibilizar terminal, clone novamente o repositório **em uma nova pasta**. Na primeira sincronização, `bootstrap.sh` extrairá e aplicará a atualização automaticamente. Não apague o aplicativo instalado antes de confirmar o comportamento da migração dos dados.

Caso seja necessária importação manual, há um ZIP completo gerado na conversa com o ChatGPT.

## Segurança, limites e testes
O sistema continua **offline/local**. O código de 4 dígitos serve apenas para **simulação**, não verifica a identidade de dois capitães distintos. A sincronização entre usuários, o controle de permissões remoto, os campeonatos eliminatórios completos e o sistema comercial são etapas futuras. As regras Java do core passaram nos testes de seleção, duplicidade, pontos, gols contra e lista de espera. A compilação Android 0.2 ainda requer validação no Code on the Go.

## GitHub
A versão 0.2 está empacotada em `payload/v02/part00.txt`; o script `apply-v02.sh` valida o SHA-256 antes de extrair os arquivos atualizados. O pacote integral contém os arquivos Java e documentação. É intencional manter a estrutura atual compatível com o primeiro clone.

© 2026 Pelada League
