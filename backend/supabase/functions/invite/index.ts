/** Public, token-only invitation landing page for WhatsApp.
 * Deploy at Supabase Edge Functions /invite with verify_jwt = false.
 * No payment flow, cookies, credentials or participant data is exposed here.
 */
Deno.serve((request: Request): Response => {
  const url = new URL(request.url);
  const code = (url.searchParams.get("code") || "").toLowerCase();
  if (!/^[a-f0-9]{32}$/.test(code)) {
    return new Response("Convite inválido.", {
      status: 400,
      headers: {"content-type": "text/plain; charset=utf-8"}
    });
  }
  const appLink = "peladaleague://convite?code=" + code;
  const html = `<!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#090C0E">
<title>Convite • Pelada League</title>
<style>
*{box-sizing:border-box}body{margin:0;min-height:100dvh;display:grid;place-items:center;
background:radial-gradient(circle at 50% -10%,#3d3210,#0b0d0f 60%);color:#f5f5f5;
font:16px system-ui,sans-serif;padding:22px}
main{max-width:430px;width:100%;padding:36px 26px;border:1px solid #6c5720;
border-radius:23px;background:#141719;box-shadow:0 18px 70px #000a;text-align:center}
h1{margin:0;color:#ffd000;letter-spacing:.04em;font-size:28px}h2{font-size:19px;line-height:1.3}
p{font-size:14px;color:#c2c5c8;line-height:1.6}
a{display:block;margin-top:20px;padding:17px;border-radius:12px;background:#ffd000;color:#0d1113;
font-weight:800;text-decoration:none;font-size:15px}
small{display:block;color:#888;padding-top:18px;font-size:12px}
</style></head><body><main>
<h1>PELADA LEAGUE</h1><h2>Você recebeu um convite para jogar!</h2>
<p>Abra o aplicativo, entre com seu e-mail e confirme se vai participar. Se as vagas acabarem, você poderá entrar na lista de espera.</p>
<a href="${appLink}">Abrir convite no aplicativo</a>
<p>Ainda não instalou? Peça o aplicativo ao organizador. O Pelada League ainda está em testes.</p>
<small>O link apenas abre o convite. Nenhuma inscrição ou pagamento é feito automaticamente.</small>
</main></body></html>`;
  return new Response(html, {
    status: 200,
    headers: {"content-type":"text/html; charset=utf-8",
              "cache-control":"no-store","x-content-type-options":"nosniff",
              "content-security-policy":"default-src 'none'; style-src 'unsafe-inline';"}
  });
});
