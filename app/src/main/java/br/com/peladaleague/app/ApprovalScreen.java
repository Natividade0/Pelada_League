package br.com.peladaleague.app;
import android.app.AlertDialog;
import android.text.*;
import android.view.*;
import android.widget.*;

final class ApprovalScreen extends Screen {
    ApprovalScreen(MainActivity a){super(a);}
    void render(){DemoDb.Game m=db.game(a.gameId);if(m==null)return;title("Aprovar Súmula");add(score(m));gap(12);add(new MatchScreen(a).summary(m));gap(12);
        if(m.status.equals("DEMO_APPROVED")){empty("Súmula validada localmente","As duas confirmações foram registradas nesta demonstração. Isso não comprova identidades independentes.","Ver classificação",()->a.tab("league"));return;}
        LinearLayout confirmation=card();line(confirmation,"shield","Confirmação dos responsáveis",null);Ui.put(confirmation,text("Demonstração local · 4 dígitos\nValidade de 3 minutos e até 3 tentativas.",12,Ui.MUTED,false),5);
        EditText[] pins=new EditText[2];for(int side=0;side<2;side++){final int s=side;boolean ok=db.approved(m.id,side);Ui.put(confirmation,text((side==0?m.a:m.b)+(ok?" · Confirmado":" · Capitão"),13,ok?Ui.GREEN:Ui.WHITE,true),15);
            if(ok)continue;
            if(!a.canApprove()){Ui.put(confirmation,text("Aguardando responsável autorizado.",12,Ui.MUTED,false),7);continue;}
            EditText pin=Ui.input(a,"0000");pin.setContentDescription("Código de quatro dígitos · "+(side==0?m.a:m.b));pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});pin.setGravity(Gravity.CENTER);pin.setLetterSpacing(.6f);pin.setTextSize(23);pin.setMinimumHeight(Ui.dp(a,50));Ui.put(confirmation,pin,8);pins[side]=pin;
            TextView generate=text("Gerar código de demonstração",12,Ui.GOLD,true);generate.setMinHeight(Ui.dp(a,44));Ui.tappable(generate,()->{String code=db.generatePin(m.id,s);if(code==null){a.refresh();return;}new AlertDialog.Builder(a).setTitle("Código local · "+(s==0?m.a:m.b)).setMessage(code+"\n\nEste código é apenas um teste local. No produto online, cada responsável deverá aprovar na própria conta.").setPositiveButton("Entendi",null).show();});Ui.put(confirmation,generate,0);
        }add(confirmation);
        if(a.canApprove())button("Confirmar Súmula",true,()->{for(int i=0;i<2;i++){if(db.approved(m.id,i))continue;if(pins[i]==null||pins[i].getText().length()!=4){a.toast("Preencha exatamente quatro dígitos em cada código pendente.");return;}}boolean accepted=true;for(int i=0;i<2;i++)if(!db.approved(m.id,i))accepted=db.verifyPin(m.id,i,pins[i].getText().toString())&&accepted;a.toast(accepted?"Confirmações registradas.":"Um código está incorreto, expirado ou bloqueado. Gere outro para o responsável pendente.");a.refresh();});
        if(a.canRecord())button("Corrigir súmula",false,()->a.confirm("Reabrir a súmula?","Todos os códigos e aprovações anteriores serão invalidados. A correção ficará registrada no histórico local.",()->a.safely(()->{db.reopen(m.id);a.matchTab=0;a.open("match");})));
    }
}
