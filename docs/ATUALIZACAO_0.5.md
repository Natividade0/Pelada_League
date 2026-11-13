# Pelada League 0.5.0-alpha — implantação com orçamento R$ 0

O código deste marco está preparado para duas experiências honestamente separadas:

- **Local, utilizável sem servidor:** presenças, sorteios, súmulas, cronômetro, substituições, pagamentos MANUAIS, Pix para copiar e isenção de goleiros. Dados ficam no celular.
- **Online, opcional:** conta real por e-mail, posição do jogador, grupos, eventos, convites, presença com espera e pagamentos manuais autorizados. Funciona entre celulares SOMENTE após configurar um projeto Supabase e aplicar as migrações incluídas neste repositório.

A versão 0.5.0-alpha NÃO é lançamento público. Não há FCM/push automático, cronômetro sincronizado entre telefones, recorrência automática, emissão de comprovante Pix, gateway de pagamento, campeonato completo por temporada ou campo 3D com motor gráfico. O campo é uma visualização tática **2.5D** animada, de autoria própria.

## 1. Zero custo até o limite gratuito

Supabase Free: projeto gratuito, quota de banco e autenticação; pode pausar após inatividade. Nunca escolher plano pago sem autorização do dono.

**E-mails:** o SMTP padrão do Supabase é de teste, limitado a 2 e-mails/hora e endereços autorizados no projeto. Para cadastros reais, configurar SMTP próprio, por exemplo Brevo Free (sujeito a cadastro, validação de remetente e limites). **Não desabilitar confirmação de e-mail para uma versão pública.** Não há garantia de envio para todos sem um provedor configurado. Não é necessário pagar nada para compilar ou testar o modo local.

Fontes oficiais:
- https://supabase.com/pricing
- https://supabase.com/docs/guides/auth/auth-smtp
- https://supabase.com/docs/guides/auth/passwords
- https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan

## 2. Conectar contas online gratuitamente

1. Entre em https://supabase.com/dashboard e crie um projeto **Free**.
2. Abra **SQL Editor** e execute o conteúdo de `backend/supabase/001_core.sql`. Não aplique um segundo vez num banco com outras tabelas chamadas `groups`/`events` sem revisar.
3. Em **Authentication → Providers → Email**, mantenha confirmação de e-mail ativada. Configure seu provedor SMTP gratuito antes de convidar usuários reais.
4. Em **Project Settings / API Keys**, copie a **Project URL** e a **publishable key / anon key**, NUNCA a `service_role` ou `secret`.
5. No **Code on the Go**, abra o arquivo `local.properties` da raiz `Pelada_League` (ele é ignorado pelo Git) e acrescente SEM remover configurações de SDK já existentes:

```properties
supabase.url=https://SEU-PROJETO.supabase.co
supabase.anonKey=sb_publishable_SUA_CHAVE_PUBLICA
invite.enabled=false
```

Se o painel oferecer a chave pública antiga `eyJ...` do tipo `anon`, também pode usá-la. Só use as chaves publicáveis. O APK não deve conter tokens admin.

6. Compile novamente pelo botão **Build** do Code on the Go. Se ainda não configurar, a tela de contas online avisará claramente que não há servidor conectado.

## 3. Publicar convites do WhatsApp, sem pagar hospedagem

O repositório inclui `backend/supabase/functions/invite/index.ts`, função pública de página de convite. Ela gera uma página HTML gratuita no endereço:

`https://SEU-PROJETO.supabase.co/functions/v1/invite?code=CODIGO`

No painel Supabase Edge Functions, crie a função `invite`, cole o código do arquivo e publique com **Verify JWT desativado**, porque convidados ainda sem conta precisam abrir a página. Essa função contém apenas HTML; ela não confirma presença nem consulta dados bancários.

Depois de testar uma URL válida e o botão que abre o APK, altere `invite.enabled=true` em `local.properties`, recompile e teste o link pelo WhatsApp em OUTRO telefone.

Sem a função publicada, o botão de compartilhar link online permanece desativado para não distribuir URLs quebradas.

O aplicativo Android inclui o deep link `peladaleague://convite?code=CODIGO`. O navegador abre o app instalado; antes da publicação na Play Store, cada testador precisará instalar o APK por um meio confiável, por exemplo enviado diretamente pelo organizador.

## 4. Garantias e limites de segurança

- O backend usa contas autenticadas, RLS, convites aleatórios de 128 bits e operações críticas em funções com validação de papéis.
- A lista de espera usa bloqueio transacional do evento para evitar exceder vagas em confirmações simultâneas.
- O registro de Pix é apenas anotação manual do organizador: **não comprova recebimento**.
- O jogador não ganha permissão de administrador por escolher um papel na demonstração local.
- A conta online não importa automaticamente o SQLite da demo. Os dados locais continuam preservados e separados do banco online.
- Confirme o funcionamento em dois telefones e revise as políticas RLS/privacidade antes de usar com dados reais.
- Não registrar senhas em mensagens, capturas ou GitHub.

## 5. Testar o aplicativo

No projeto aberto na pasta `Pelada_League`, na branch `redesign/native-premium-v04`, rode:

```sh
git pull --ff-only
```

Reabra o projeto e compile com o **botão Build**, sem apagar o aplicativo anterior. A versão usa `versionCode=6` e `versionName=0.5.0-alpha`; ao instalar pelo mesmo Code on the Go o Android poderá preservar o banco anterior se a assinatura do APK for a mesma.

Teste o fluxo local:
1. Abra uma pelada → **Pagamentos da pelada**.
2. Configure R$ 20/jogador, custo R$ 250, e **goleiros isentos**.
3. Confirme quatro ou mais jogadores na sessão.
4. Marque um pagamento parcial; reinicie o app e confira a persistência.
5. Sorteie os times e visualize o campo animado.
6. Registre uma partida, inicie/pare o cronômetro, adicione um substituto confirmado que não estava em campo e registre um gol.
7. Feche/reabra o APK; verifique o histórico e a escalação.

**Testes automatizados:** `FinanceTest`, `ClockTest`, `DatabaseTest` (incluindo substituições/migração V4), `UiTest`. O GitHub Actions executa a compilação e os testes em emulador, mas é importante conferir a interface no Motorola e em outros tamanhos.

## 6. Ainda necessário antes de versão 1.0

- Recorrência real de partidas, lembretes e notificações push (FCM, consentimento, servidor agendador).
- Cronômetro e atualização de lances em tempo real entre dispositivos com servidor e resolução de conflitos.
- Controle avançado de substituições e versões de súmula em servidor, dupla aprovação autenticada.
- Temporadas, campeonatos completos, escalação manual pelo técnico e premiações.
- Campo cinematográfico em motor gráfico 3D verdadeiro (a visualização atual usa Canvas 2.5D).
- Auditoria de segurança, UX acessível, política de privacidade e testes com grupo real.

**Orçamento comprometido nesta implantação: R$ 0.** Não foi realizada nenhuma compra, assinatura ou operação financeira.
