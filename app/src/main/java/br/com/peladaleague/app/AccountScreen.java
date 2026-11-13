package br.com.peladaleague.app;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

/** Authentic online account UI; never presents a local demonstration as a genuine account. */
final class AccountScreen extends Screen {
    AccountScreen(MainActivity a){super(a);}
    void render(){
        title("Conta Pelada League");
        if(!a.auth.enabled()){
            LinearLayout note=card();
            note.addView(text("Acesso por e-mail em preparação",18,Ui.GOLD,true));
            Ui.put(note,text("O cadastro online já foi implementado no código, mas só poderá ser utilizado após configurar um projeto Supabase gratuito e a entrega de e-mails de confirmação. Ainda não existem contas conectadas neste aplicativo.",13,Ui.WHITE,false),10);
            Ui.put(note,text("Continue utilizando o controle local de peladas e pagamentos sem criar conta fictícia.",12,Ui.MUTED,false),10);
            add(note);gap(12);
            button("Voltar ao aplicativo local",true,()->a.tab("home"));
            return;
        }
        if(a.auth.loggedIn())signedIn();else signIn();
    }
    private void signedIn(){
        LinearLayout card=card();
        card.addView(text("Conta conectada",18,Ui.GREEN,true));
        Ui.put(card,text(a.auth.email(),14,Ui.WHITE,true),8);
        Ui.put(card,text("Sessão guardada de forma criptografada no aparelho.",11,Ui.MUTED,false),9);
        add(card);gap(12);
        button("Atualizar meus dados",true,this::editProfile);
        button("Verificar conexão",false,()->a.auth.renew((ok,msg)->a.toast(msg)));
        button("Sair desta conta",false,()->a.confirm("Sair da conta online?","Seus dados locais não serão apagados.",()->{a.auth.logout();a.refresh();}));
        sub("A conta online não sincroniza automaticamente as peladas locais antigas. O módulo de eventos compartilhados precisa ser ativado no servidor.");
    }
    private void signIn(){
        LinearLayout intro=card();
        intro.addView(text("Seu futebol tem identidade",18,Ui.WHITE,true));
        Ui.put(intro,text("Acesse por e-mail e senha. Depois do primeiro login, você permanece conectado até encerrar a sessão.",12,Ui.MUTED,false),7);
        add(intro);gap(14);

        TextView emailTitle=text("E-mail",12,Ui.WHITE,true);add(emailTitle);
        EditText email=Ui.input(a,"seuemail@exemplo.com");email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);Ui.put(page,email,7);
        Ui.space(page,11);
        add(text("Senha",12,Ui.WHITE,true));
        EditText pass=Ui.input(a,"Sua senha");
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Ui.put(page,pass,7);
        Ui.space(page,10);
        button("Entrar na minha conta",true,()->{
            a.auth.signIn(email.getText().toString(),pass.getText().toString(),(ok,msg)->{
                a.toast(msg);if(ok)a.refresh();
            });
        });
        TextView recovery=Ui.chip(a,"Esqueci minha senha",false,()->a.auth.recover(email.getText().toString(),(ok,msg)->a.toast(msg)));
        add(recovery);gap(20);

        LinearLayout registration=card();
        registration.addView(text("Ainda não tem uma conta?",16,Ui.GOLD,true));
        Ui.put(registration,text("Crie uma conta com sua posição principal. Você pode trocar a posição ao participar de uma partida.",12,Ui.MUTED,false),6);
        Ui.put(registration,Ui.button(a,"Criar minha conta",false,this::signUp),12);
        add(registration);
    }
    private void signUp(){
        LinearLayout form=Ui.col(a);form.setPadding(Ui.dp(a,18),Ui.dp(a,8),Ui.dp(a,18),Ui.dp(a,12));
        EditText name=input(form,"Nome do jogador",false);
        EditText email=input(form,"E-mail",false);
        email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password=input(form,"Senha (mínimo de 8 caracteres)",true);
        Ui.put(form,text("Posição principal",12,Ui.WHITE,true),14);
        final String[] positions={"GOL","ZAG","MEI","ATA","LIN"};
        Spinner position=new Spinner(a);
        String[] choices={"Goleiro","Zagueiro","Meio-campo","Atacante","Jogador de linha"};
        position.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,choices));
        Ui.put(form,position,6);
        Ui.put(form,text("Você poderá mudar sua função em cada evento. O goleiro pode ser isento de pagamento quando o organizador habilitar essa opção.",11,Ui.MUTED,false),12);
        ScrollView sc=new ScrollView(a);sc.addView(form);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Criar conta").setView(sc).setNegativeButton("Cancelar",null).setPositiveButton("Cadastrar",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            String n=name.getText().toString(),mail=email.getText().toString(),pass=password.getText().toString();
            String pos=positions[position.getSelectedItemPosition()];
            d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            a.auth.signUp(n,pos,mail,pass,(ok,msg)->{
                a.toast(msg);
                if(ok){d.dismiss();a.refresh();}
                else d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            });
        }));
        d.show();
    }
    private EditText input(LinearLayout parent,String label,boolean masked){
        Ui.put(parent,text(label,12,Ui.WHITE,true),10);
        EditText v=Ui.input(a,label);if(masked)v.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Ui.put(parent,v,6);return v;
    }
    private void editProfile(){
        LinearLayout form=Ui.col(a);
        form.setPadding(Ui.dp(a,18),Ui.dp(a,8),Ui.dp(a,18),Ui.dp(a,12));
        EditText name=input(form,"Nome do jogador",false);
        String[] codes={"GOL","ZAG","MEI","ATA","LIN"},labels={"Goleiro","Zagueiro","Meio-campo","Atacante","Jogador de linha"};
        Ui.put(form,text("Posição principal",12,Ui.WHITE,true),12);
        Spinner pos=new Spinner(a);pos.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels));Ui.put(form,pos,6);
        ScrollView sc=new ScrollView(a);sc.addView(form);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Atualizar perfil online").setView(sc).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            String n=name.getText().toString().trim();
            if(n.length()<2){a.toast("Informe seu nome completo ou apelido.");return;}
            final String uid=a.auth.userId();
            if(uid.isEmpty()){a.toast("Identidade não carregada. Entre novamente.");return;}
            d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            final String selection=codes[pos.getSelectedItemPosition()];
            a.auth.runOnline(()->{
                JSONObject body=new JSONObject();
                body.put("display_name",n);body.put("primary_position",selection);
                a.auth.authorized("PATCH","/rest/v1/profiles?id=eq."+uid,body);
                return "Seu perfil foi atualizado.";
            },(ok,msg)->{
                a.toast(msg);if(ok)d.dismiss();else d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            });
        }));
        d.show();
    }
}
