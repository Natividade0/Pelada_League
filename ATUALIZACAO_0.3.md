# Pelada League 0.3.0-alpha — redesign visual

## O que mudou
- Interface preto/grafite e dourado mais próxima do mockup de oito telas aprovado
- Splash, início com cartão de estádio, grupo com capa, classificação e perfil revisados
- Navegação inferior mais legível com indicação de aba ativa
- Ícones e camisas desenhados em Canvas, avatar ilustrado original e placar central
- Botões de destaque com gradiente, animações de entrada e feedback de toque
- Sorteio visual: lista compacta de confirmados, caixas de seleção, 2–4 equipes, equilíbrio ou aleatoriedade, revelação animada de times
- Seleção filtrada sempre pela lista de presença confirmada na sessão atual
- Banco de dados preservado; schema permanece v2

## Como aplicar no Code on the Go
Na pasta `Pelada_League`, depois de ter instalado a versão 0.2:

```sh
git pull origin main
sh apply-v03.sh
```

Depois faça a sincronização Gradle e a compilação. Este repositório mantém o código de atualização compactado em `payload/v03/part00.txt`. O script confere o SHA-256 antes de extrair.

## Limites da alfa
Ainda não há login remoto, fotos de usuários, notificações push nem validação segura por capitães. As estatísticas e códigos são demonstração local. Este pacote foi verificado por testes das regras e por instalação local sobre a v0.2, **e o APK debug foi compilado com sucesso pelo GitHub Actions em 13/11/2026; falta validar o visual e o fluxo no celular**. A interface é uma implementação nativa inspirada na referência, não uma reprodução perfeita de cada pixel.
