# Pelada League · 0.4.0-alpha

Aplicativo Android nativo para organizar peladas, confirmar presenças, sortear equipes e registrar partidas. Esta atualização reconstrói as oito telas da direção visual preto/dourado e mantém o banco SQLite da versão anterior.

**É uma demonstração local funcional.** Não há login online, sincronização entre celulares ou validação oficial de identidades. Jogador cadastrado localmente não é uma conta autenticada. A tabela usa somente súmulas validadas na demonstração; gols contra não entram na artilharia.

## Instalar e atualizar no Code on the Go

O código agora está diretamente em `app/` e `core/`. Não precisa extrair `payload/` nem instalar Python.

Para quem tem a versão antiga extraída e possivelmente arquivos locais não rastreados, abra o terminal **dentro de Pelada_League**:

```sh
git fetch origin
git worktree add --detach ../Pelada_League_0.4 origin/redesign/native-premium-v04
```

Abra a pasta vizinha **Pelada_League_0.4** no Code on the Go, sincronize o Gradle e use a opção de compilar/instalar. A pasta antiga fica intacta. Isso é outro checkout do **mesmo repositório e aplicativo**, não outro produto.

Se a pasta `Pelada_League_0.4` já existe, não repita o comando de criação. Dentro dela, estando sem alterações locais pendentes:

```sh
git fetch origin
git merge --ff-only origin/redesign/native-premium-v04
```

Não desinstale o aplicativo para atualizar. O Android só permite substituir uma instalação quando o APK usa **a mesma chave de assinatura**. Compilar no mesmo Code on the Go mantém a chave local. Um APK de teste gerado em outro ambiente pode ter outra assinatura; nesse caso mantenha a instalação e compile com sua chave original. O `applicationId` permanece `br.com.peladaleague.app`.

## Compilar

- JDK 17, Gradle 9.6.1, Android Gradle Plugin 9.3.1.
- compileSdk/targetSdk 35, minSdk 26.
- AGP usa Build Tools 36.0.0; o SDK Manager precisa disponibilizá-lo.
- Desugaring de biblioteca Java para APIs Java 9+ em Android 8+.

```sh
sh scripts/test-core.sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
No GitHub: **Actions → Android 0.4 · build and device tests → artefato PeladaLeague-0.4-debug**.

## Funcionalidades desta versão

- Oito telas nativas separadas, estádio original, escudo e camisas vetoriais, navegação inferior fixa, formulários que mantêm os dados ao detectar erro.
- Presença por sessão, lotação, espera FIFO e promoção automática ao abrir vagas.
- Sorteio de 2–4 times, seleção apenas de confirmados, equilíbrio por habilidade/goleiros/posições e persistência do sorteio.
- Mudanças na presença invalidam o sorteio. Novas partidas têm escalação persistida e validada.
- Gols, gols contra, assistências, cartões, edição/exclusão confirmada, placar derivado e bloqueio após envio.
- Aprovação demonstrativa com exatamente 4 dígitos, validade de 3 minutos, 3 tentativas e uso único.
- Correção de súmula pendente com invalidação dos códigos e auditoria local.
- Classificação por pontos, artilharia, perfil editável, carreira e histórico derivados dos registros.
- Simulação explícita de papéis em **Informações → Perfil de teste**. Não substitui autorização no servidor.

## Testar no Android

Em um emulador de QA descartável, já iniciado:

```sh
sh scripts/test-android.sh
```

Os testes de banco usam um banco isolado. O teste visual adiciona partidas de demonstração ao banco do app no emulador. Não execute esse teste visual no aparelho com seus dados de uso.

Consulte [arquitetura](docs/ARQUITETURA.md), [notas da versão](docs/CHANGELOG_0.4.md), [validação](docs/VALIDACAO_0.4.md) e [evolução online](docs/PLANO_DE_EVOLUCAO.md).

© 2026 Pelada League. Recursos visuais originais; sem marcas de clubes reais.
