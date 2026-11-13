package br.com.peladaleague.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Real Supabase email/password REST auth when the owner supplies a FREE project.
 * No local dummy accounts or embedded service_role credentials.
 * Refresh tokens are encrypted at rest with Android Keystore AES/GCM.
 */
final class CloudAuth {
    interface Callback { void complete(boolean success,String message); }
    private static final String PREFERENCES="online_auth_secure";
    private static final String ALIAS="pelada_league_session_aes";
    private final MainActivity a;
    private JSONObject session;
    private String email;
    CloudAuth(MainActivity activity){a=activity;restore();}
    boolean enabled(){
        return BuildConfig.SUPABASE_URL.startsWith("https://")
            && BuildConfig.SUPABASE_ANON_KEY.length()>16
            && !BuildConfig.SUPABASE_URL.contains("example");
    }
    boolean loggedIn(){return enabled()&&session!=null&&session.optString("refresh_token").length()>10;}
    String email(){return email==null?"":email;}
    void logout(){
        session=null;email=null;
        a.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).edit().clear().apply();
    }
    void signUp(String name,String position,String address,String password,Callback done){
        if(!enabled()){done.complete(false,"Ainda falta configurar o servidor gratuito.");return;}
        final String n=name.trim(),mail=address.trim().toLowerCase(java.util.Locale.ROOT);
        if(n.length()<2){done.complete(false,"Informe seu nome de jogador.");return;}
        if(!Arrays.asList("GOL","ZAG","MEI","ATA","LIN").contains(position)){done.complete(false,"Selecione sua posição.");return;}
        if(!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")){done.complete(false,"Informe um e-mail válido.");return;}
        if(password.length()<8){done.complete(false,"A senha precisa ter pelo menos 8 caracteres.");return;}
        submit(()->{
            JSONObject data=new JSONObject();data.put("display_name",n);data.put("primary_position",position);
            JSONObject body=new JSONObject();body.put("email",mail);body.put("password",password);body.put("data",data);
            JSONObject response=api("POST","/auth/v1/signup",body,null);
            if(response.has("access_token")){saveSession(response,mail);return "Conta criada e conectada.";}
            return "Cadastro solicitado. Confirme o e-mail antes de entrar; verifique também a pasta de spam.";
        },done);
    }
    void signIn(String address,String password,Callback done){
        if(!enabled()){done.complete(false,"O servidor online ainda não foi configurado.");return;}
        final String mail=address.trim().toLowerCase(java.util.Locale.ROOT);
        if(mail.isEmpty()||password.isEmpty()){done.complete(false,"Informe e-mail e senha.");return;}
        submit(()->{
            JSONObject body=new JSONObject();body.put("email",mail);body.put("password",password);
            JSONObject result=api("POST","/auth/v1/token?grant_type=password",body,null);
            if(result.optString("access_token").isEmpty())throw new IOException("Acesso não confirmado.");
            saveSession(result,mail);
            return "Você entrou com sua conta.";
        },done);
    }
    void recover(String address,Callback done){
        if(!enabled()){done.complete(false,"O servidor online ainda não foi configurado.");return;}
        final String mail=address.trim().toLowerCase(java.util.Locale.ROOT);
        if(!mail.contains("@")){done.complete(false,"Informe seu e-mail para recuperar o acesso.");return;}
        submit(()->{
            JSONObject body=new JSONObject();body.put("email",mail);
            api("POST","/auth/v1/recover",body,null);
            return "Se o e-mail estiver cadastrado, as instruções de recuperação serão enviadas.";
        },done);
    }
    void renew(Callback done){
        if(!loggedIn()){done.complete(false,"Entre com e-mail e senha.");return;}
        if(session.optLong("expires_at")>System.currentTimeMillis()/1000L+90L){
            done.complete(true,"Sessão ativa.");return;
        }
        final String token=session.optString("refresh_token");
        submit(()->{
            JSONObject body=new JSONObject();body.put("refresh_token",token);
            JSONObject response=api("POST","/auth/v1/token?grant_type=refresh_token",body,null);
            saveSession(response,email);
            return "Sessão renovada.";
        },done);
    }
    /** PostgREST request for the signed-in user; caller runs on worker thread. */
    JSONObject authorized(String method,String endpoint,JSONObject body)throws Exception{
        if(!loggedIn())throw new IOException("É necessário entrar com sua conta.");
        if(session.optLong("expires_at")<=System.currentTimeMillis()/1000L+90L){
            JSONObject req=new JSONObject();req.put("refresh_token",session.optString("refresh_token"));
            JSONObject refreshed=api("POST","/auth/v1/token?grant_type=refresh_token",req,null);
            saveSession(refreshed,email);
        }
        return api(method,endpoint,body,session.getString("access_token"));
    }
    void runOnline(OnlineTask work,Callback callback){submit(work,callback);}
    interface OnlineTask {String execute()throws Exception;}

    private void submit(OnlineTask task,Callback callback){
        a.worker.execute(()->{
            boolean ok;String message;
            try{message=task.execute();ok=true;}
            catch(Exception ex){message=ex.getMessage()==null?"Falha ao comunicar com o servidor.":ex.getMessage();ok=false;}
            final boolean success=ok;final String info=message;
            a.runOnUiThread(()->{
                if(!a.isDestroyed()&&!a.isFinishing())callback.complete(success,info);
            });
        });
    }
    JSONObject api(String method,String path,JSONObject payload,String token)throws Exception{
        if(!enabled())throw new IOException("Servidor online não configurado.");
        String base=BuildConfig.SUPABASE_URL.replaceAll("/+$","");
        if(!path.startsWith("/auth/v1/")&&!path.startsWith("/rest/v1/"))
            throw new IOException("Rota remota não permitida.");
        HttpURLConnection conn=(HttpURLConnection)new URL(base+path).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(15000);
        conn.setRequestMethod(method);
        conn.setRequestProperty("apikey",BuildConfig.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization","Bearer "+(token==null?BuildConfig.SUPABASE_ANON_KEY:token));
        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestProperty("Accept","application/json");
        conn.setRequestProperty("Prefer","return=representation");
        if(payload!=null){
            conn.setDoOutput(true);
            byte[] bytes=payload.toString().getBytes(StandardCharsets.UTF_8);
            try(OutputStream out=conn.getOutputStream()){out.write(bytes);}
        }
        try{
            int status=conn.getResponseCode();
            InputStream stream=status<400?conn.getInputStream():conn.getErrorStream();
            String raw;
            try(InputStream input=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
                if(input!=null){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>300000)throw new IOException("Resposta muito extensa.");out.write(buffer,0,n);}}
                raw=out.toString("UTF-8");
            }
            if(status>=400){
                String reason="Solicitação recusada ("+status+").";
                try{JSONObject error=new JSONObject(raw);reason=error.optString("msg",
                    error.optString("error_description",error.optString("message",reason)));}catch(Exception ignored){}
                throw new IOException(reason.length()>220?reason.substring(0,220):reason);
            }
            if(raw.trim().isEmpty())return new JSONObject();
            if(raw.trim().startsWith("[")){
                JSONObject wrapped=new JSONObject();wrapped.put("rows",new org.json.JSONArray(raw));return wrapped;
            }
            return new JSONObject(raw);
        }finally{conn.disconnect();}
    }
    private void saveSession(JSONObject payload,String mail)throws Exception {
        String token=payload.optString("access_token"),refresh=payload.optString("refresh_token");
        if(token.isEmpty()||refresh.isEmpty())throw new IOException("Sessão incompleta. Entre novamente.");
        JSONObject record=new JSONObject();
        record.put("access_token",token);record.put("refresh_token",refresh);
        record.put("expires_at",System.currentTimeMillis()/1000L+Math.max(120,payload.optLong("expires_in",3600)));
        record.put("email",mail==null?"":mail);
        byte[] encrypted=encrypt(record.toString());
        a.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).edit()
            .putString("session",Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
        session=record;email=mail;
    }
    private void restore(){
        String stored=a.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).getString("session","");
        if(stored.isEmpty())return;
        try{
            session=new JSONObject(decrypt(Base64.decode(stored,Base64.NO_WRAP)));
            email=session.optString("email");
        }catch(Exception ex){logout();}
    }
    private SecretKey key()throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)store.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    private byte[] encrypt(String text)throws Exception{
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] iv=cipher.getIV(),body=cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        byte[] combined=new byte[1+iv.length+body.length];combined[0]=(byte)iv.length;
        System.arraycopy(iv,0,combined,1,iv.length);
        System.arraycopy(body,0,combined,1+iv.length,body.length);
        return combined;
    }
    private String decrypt(byte[] all)throws Exception{
        if(all.length<30)throw new IOException("Sessão inválida.");
        int n=all[0]&0xff;if(n!=12||all.length<=n+17)throw new IOException("Sessão inválida.");
        byte[] iv=Arrays.copyOfRange(all,1,1+n);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
        return new String(cipher.doFinal(all,1+n,all.length-1-n),StandardCharsets.UTF_8);
    }
}
