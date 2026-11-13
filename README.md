# Pelada League — atualização visual 0.3.0-alpha

**Interface premium refeita com base no mockup preto e dourado.** A v0.3 inclui navegação e cartões redesenhados, iconografia esportiva original, animações e sorteador visual de jogadores confirmados. Para atualizar no Code on the Go: `git pull origin main` e `sh apply-v03.sh`. Leia [ATUALIZACAO_0.3.md](ATUALIZACAO_0.3.md).

**Importante:** ainda é uma demonstração local. O APK precisa passar pela compilação e pelo teste visual no aparelho; não há autenticação remota.

---

# ⚽ Pelada League — alfa Android 0.1.0

Projeto Android nativo (Java + Gradle) com identidade visual **preto, dourado e amarelo** inspirada na prévia aprovada pelo criador.

O aplicativo inclui telas de início, peladas, sorteio de equipes, registro de partidas, súmula local demonstrativa com código de **4 dígitos**, classificação, artilharia e perfil.

## Clonar no Code on the Go

URL: `https://github.com/Natividade0/Pelada_League.git`

**Como a primeira importação do código está em um pacote comprimido**, há duas formas de abrir:

1. Faça o clone privado na sua conta autenticada, abra a pasta pelo Code on the Go e sincronize o Gradle. O `settings.gradle` tenta extrair o projeto automaticamente na primeira sincronização.
2. Se a sincronização não conseguir extrair (por falta de Python 3 ou xz), execute `sh bootstrap.sh` no terminal da pasta clonada e sincronize novamente. Sem Pillow, o script usa recursos vetoriais alternativos. Para gerar as imagens completas, use `python3 -m pip install pillow && python3 make_assets.py`.
3. Alternativamente, no GitHub, abra **Actions → Import Pelada League Android source → Run workflow**. Esse workflow expande o projeto, gera os recursos visuais e tenta compilar um APK de teste.

Configuração: Java 17, AGP 8.11, Gradle 8.13, compileSdk 35. O Android SDK é necessário para compilar.

## Estado do produto

**Alfa local:** contém dados de exemplo e banco SQLite no aparelho. Não há login real, backend, pagamentos ou aprovação antifraude remota. Os códigos de quatro dígitos são demonstração, não prova de aprovação por duas contas independentes. O objetivo da primeira entrega é validar navegação, aparência e regras locais antes de implementar contas e sincronização.

## Segurança e direitos

- O código não contém chaves privadas nem credenciais de backend.
- Os rankings locais não representam resultados oficiais verificados.
- Repositório privado; sem licença pública de redistribuição.

© 2026 Pelada League.
