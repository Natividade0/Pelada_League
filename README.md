# ⚽ Pelada League — alfa Android 0.1.0

Projeto Android nativo (Java + Gradle) com identidade visual **preto, dourado e amarelo** inspirada na prévia aprovada pelo criador.

O aplicativo inclui telas de início, peladas, sorteio de equipes, registro de partidas, súmula local demonstrativa com código de **4 dígitos**, classificação, artilharia e perfil.

## Clonar no Code on the Go

URL: `https://github.com/Natividade0/Pelada_League.git`

**Como a primeira importação do código está em um pacote comprimido**, há duas formas de abrir:

1. Se o repositório já mostrar pastas `app/` e `core/`, clone, abra a raiz do projeto no Code on the Go e sincronize o Gradle.
2. Se ainda aparecer apenas a pasta `payload/`, clone o repositório e execute `sh bootstrap.sh` no terminal da pasta clonada **antes** de abrir o projeto. O script expande o código e prepara os recursos visuais. Ele aceita Python 3; sem Pillow, usa fundos vetoriais alternativos. Para imagens mais próximas do conceito aprovado, execute `python3 -m pip install pillow && python3 make_assets.py`.
3. Alternativamente, no GitHub, abra **Actions → Import Pelada League Android source → Run workflow**. Esse workflow expande o projeto, gera os recursos visuais e tenta compilar um APK de teste.

Configuração: Java 17, AGP 8.11, Gradle 8.13, compileSdk 35. O Android SDK é necessário para compilar.

## Estado do produto

**Alfa local:** contém dados de exemplo e banco SQLite no aparelho. Não há login real, backend, pagamentos ou aprovação antifraude remota. Os códigos de quatro dígitos são demonstração, não prova de aprovação por duas contas independentes. O objetivo da primeira entrega é validar navegação, aparência e regras locais antes de implementar contas e sincronização.

## Segurança e direitos

- O código não contém chaves privadas nem credenciais de backend.
- Os rankings locais não representam resultados oficiais verificados.
- Repositório privado; sem licença pública de redistribuição.

© 2026 Pelada League.
