package br.com.peladaleague.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** A functional local finance desk for a pelada session; never represents a bank gateway. */
final class FinanceScreen extends Screen {
    private final LocalFinance f;
    FinanceScreen(MainActivity a){super(a);f=a.finance;}
    static String money(int cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(cents/100.0);}
    static int cents(String value){
        if(value==null||value.trim().isEmpty())return 0;
        String cleaned=value.trim().replace("R$","").replace(" ","").replace(",",".");
        if(!cleaned.matches("[0-9]+(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("Use um valor como 20,00.");
        try{return new BigDecimal(cleaned).movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).intValueExact();}
        catch(ArithmeticException ex){throw new IllegalArgumentException("Valor fora do limite.");}
    }
    private LinearLayout vertical(){LinearLayout v=Ui.col(a);v.setPadding(Ui.dp(a,15),Ui.dp(a,8),Ui.dp(a,15),Ui.dp(a,10));return v;}
    private EditText field(LinearLayout parent,String label,String initial,boolean numbers){
        TextView title=text(label,12,Ui.MUTED,true);Ui.put(parent,title,9);
        EditText box=Ui.input(a,label);
        box.setText(initial);
        if(numbers)box.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.setSingleLine(true);box.setMinHeight(Ui.dp(a,48));
        Ui.put(parent,box,5);return box;
    }
    private void validatedDialog(String title,LinearLayout form,Runnable save){
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(scroll)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            try{save.run();dialog.dismiss();a.refresh();}
            catch(IllegalArgumentException|IllegalStateException ex){a.toast(ex.getMessage());}
        }));
        dialog.show();
    }
    void render(){
        title("Pagamentos da pelada");
        LocalFinance.Config config=f.config(a.groupId);
        if(!a.canManage()){myPayment(config);return;}
        LocalFinance.Summary total=f.summary(a.groupId);
        add(text(db.group(a.groupId)[0],18,Ui.WHITE,true));
        sub("Controle manual · sessão atual · "+total.people+" confirmados");
        gap(14);

        LinearLayout totals=card();totals.addView(text("Arrecadação da partida",15,Ui.WHITE,true));
        Ui.space(totals,12);
        LinearLayout numbers=Ui.row(a);
        numbers.addView(metric("Previsto",money(total.expected),Ui.WHITE),new LinearLayout.LayoutParams(0,-2,1));
        numbers.addView(metric("Recebido",money(total.received),Ui.GREEN),new LinearLayout.LayoutParams(0,-2,1));
        totals.addView(numbers);Ui.space(totals,12);
        TextView due=text("PENDENTE   "+money(total.pending),14,total.pending>0?Ui.GOLD:Ui.GREEN,true);totals.addView(due);
        Ui.space(totals,5);
        totals.addView(text(total.paid+" pagos · "+total.waived+" isentos · "+(total.people-total.paid-total.waived)+" aguardando pagamento",11,Ui.MUTED,false));
        Ui.space(totals,9);
        totals.addView(text("Custo do campo: "+money(config.fieldCost),12,Ui.MUTED,false));
        totals.addView(text("Diferença prevista: "+money(total.expected-config.fieldCost),12,Ui.MUTED,false));
        add(totals);gap(12);

        if(a.canManage()){
            button("Configurar cobrança",true,()->configure(config));
            gap(8);
        }
        if(config.fee==0){
            LinearLayout notice=card();
            notice.addView(text("Contribuição ainda não definida",14,Ui.GOLD,true));
            Ui.put(notice,text("O organizador pode escolher o valor por jogador e a isenção dos goleiros.",12,Ui.MUTED,false),6);
            add(notice);gap(10);
        }
        if(!config.pixKey.isEmpty()){
            LinearLayout pix=card();
            pix.addView(text("Pix do organizador",14,Ui.WHITE,true));
            Ui.put(pix,text(config.pixKey,12,Ui.MUTED,false),7);
            TextView copy=Ui.chip(a,"Copiar chave Pix",false,()->{
                android.content.ClipboardManager cb=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Chave Pix",config.pixKey));
                a.toast("Chave Pix copiada. Confira os dados antes de transferir.");
            });Ui.put(pix,copy,10);add(pix);gap(12);
        }
        heading("Jogadores confirmados");
        if(total.people==0){
            empty("Ninguém confirmou ainda","Os pagamentos são associados somente aos confirmados da sessão.","Abrir presenças",()->a.open("attendance"));
        }else{
            LinearLayout list=card();
            for(DemoDb.Player player:db.confirmedPlayers(a.groupId)){
                int duePlayer=f.due(a.groupId,player),received=f.paid(a.groupId,player.id);
                LinearLayout item=Ui.row(a);item.setMinimumHeight(Ui.dp(a,65));
                item.addView(Ui.initials(a,player.name,35),Ui.lp(a,35,35));
                LinearLayout labels=Ui.col(a);labels.setPadding(Ui.dp(a,10),0,Ui.dp(a,7),0);
                labels.addView(text(player.name,14,Ui.WHITE,true));
                Ui.put(labels,text(f.position(a.groupId,player)+" · "+f.paymentStatus(a.groupId,player),11,duePlayer==0||received>=duePlayer?Ui.GREEN:Ui.GOLD,false),4);
                item.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
                LinearLayout sum=Ui.col(a);sum.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                TextView value=text(duePlayer==0?"ISENTO":money(received),12,duePlayer==0?Ui.GREEN:Ui.WHITE,true);value.setGravity(Gravity.RIGHT);
                sum.addView(value);
                if(duePlayer>0){TextView smaller=text("de "+money(duePlayer),10,Ui.MUTED,false);smaller.setGravity(Gravity.RIGHT);sum.addView(smaller);}
                item.addView(sum);
                if(a.canManage()){Ui.tappable(item,()->playerActions(player));item.setContentDescription("Gerenciar pagamento e posição de "+player.name);}
                else if(player.id==a.playerId){Ui.tappable(item,()->a.toast("Somente o organizador confirma pagamentos recebidos."));}
                list.addView(item);Ui.divider(list);
            }
            add(list);
        }
        gap(12);
        if(a.canManage()){
            button("Compartilhar resumo no WhatsApp",false,this::shareSummary);
            gap(8);
            TextView audit=Ui.chip(a,"Histórico de alterações",false,this::history);add(audit);
        }
        sub("Os pagamentos são informados manualmente pelo organizador. Esta versão ainda salva as informações apenas neste aparelho; não verifica Pix nem sincroniza com outros celulares.");
    }
    /** Players see their own payment only; other people's debt is private. */
    private void myPayment(LocalFinance.Config config){
        DemoDb.Player player=a.me();
        if(!DemoDb.CONFIRMED.equals(db.attendance(a.groupId).get(player.id))){
            empty("Confirme sua presença","Seu pagamento será exibido aqui depois que você confirmar presença nesta pelada.","Abrir presenças",()->a.open("attendance"));
            return;
        }
        int due=f.due(a.groupId,player),paid=f.paid(a.groupId,player.id);
        LinearLayout card=card();
        card.addView(text("Meu pagamento",17,Ui.WHITE,true));
        Ui.put(card,text(f.paymentStatus(a.groupId,player),17,due==0||paid>=due?Ui.GREEN:Ui.GOLD,true),10);
        Ui.put(card,text("Contribuição: "+money(due),14,Ui.WHITE,false),9);
        Ui.put(card,text("Valor registrado: "+money(paid),14,Ui.WHITE,false),6);
        Ui.put(card,text(due==0?"Você está isento neste evento.":"Saldo pendente: "+money(Math.max(0,due-paid)),13,Ui.MUTED,false),8);
        add(card);gap(12);
        if(!config.pixKey.isEmpty()){
            TextView copy=Ui.button(a,"Copiar Pix do organizador",true,()->{
                android.content.ClipboardManager cb=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Chave Pix",config.pixKey));
                a.toast("Confira o destinatário antes de transferir.");
            });
            add(copy);
        }
        sub("Seu pagamento é confirmado manualmente pelo organizador. Os pagamentos dos demais participantes não são exibidos.");
    }
    private LinearLayout metric(String caption,String value,int color){
        LinearLayout v=Ui.col(a);v.addView(text(caption,11,Ui.MUTED,false));
        TextView t=text(value,19,color,true);Ui.put(v,t,4);return v;
    }
    private void configure(LocalFinance.Config cfg){
        LinearLayout form=vertical();
        EditText amount=field(form,"Contribuição por jogador (R$)",raw(cfg.fee),true);
        EditText cost=field(form,"Aluguel e despesas do campo (R$)",raw(cfg.fieldCost),true);
        CheckBox keeper=new CheckBox(a);
        keeper.setText("Isentar goleiros automaticamente");keeper.setTextColor(Ui.WHITE);
        keeper.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GOLD));
        keeper.setChecked(cfg.keepersExempt);Ui.put(form,keeper,15);
        EditText pix=field(form,"Chave Pix do organizador (opcional)",cfg.pixKey,false);
        validatedDialog("Configurar pagamentos",form,()->f.save(a.groupId,cents(amount.getText().toString()),cents(cost.getText().toString()),keeper.isChecked(),pix.getText().toString()));
    }
    private String raw(int amount){return String.format(Locale.forLanguageTag("pt-BR"),"%.2f",amount/100.0);}
    private void playerActions(DemoDb.Player player){
        String current=f.paymentStatus(a.groupId,player);
        String[] options={"Registrar / corrigir pagamento","Função nesta partida","Isenção individual","Ver histórico financeiro"};
        new AlertDialog.Builder(a).setTitle(player.name+" · "+current).setItems(options,(dialog,index)->{
            switch(index){case 0:payment(player);break;case 1:position(player);break;case 2:exemption(player);break;case 3:history();break;}
        }).setNegativeButton("Cancelar",null).show();
    }
    private void payment(DemoDb.Player player){
        int due=f.due(a.groupId,player);
        if(due==0&&f.paid(a.groupId,player.id)==0){
            a.toast("Jogador isento. Altere a isenção antes de cobrar.");return;
        }
        LinearLayout form=vertical();
        Ui.put(form,text("Valor total já recebido: de R$ 0 até "+money(due)+". Informe o total, não apenas o último pagamento.",12,Ui.MUTED,false),4);
        EditText amount=field(form,"Total já pago (R$)",raw(f.paid(a.groupId,player.id)),true);
        Ui.put(form,text("Forma do último pagamento ou correção",12,Ui.MUTED,true),10);
        Spinner method=new Spinner(a);
        String[] values={"PIX","DINHEIRO","OUTRO","AJUSTE"};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,values);
        method.setAdapter(adapter);Ui.put(form,method,5);
        validatedDialog("Pagamento · "+player.name,form,()->f.setTotalPaid(a.groupId,player,cents(amount.getText().toString()),values[method.getSelectedItemPosition()]));
    }
    private void position(DemoDb.Player player){
        String[] codes={"GOL","ZAG","MEI","ATA","LIN"};
        String[] labels={"Goleiro","Zagueiro","Meio-campista","Atacante","Jogador de linha"};
        int start=Arrays.asList(codes).indexOf(f.position(a.groupId,player));
        new AlertDialog.Builder(a).setTitle("Função nesta partida · "+player.name)
            .setSingleChoiceItems(labels,Math.max(start,0),(dialog,which)->{
                f.setPosition(a.groupId,player.id,codes[which]);dialog.dismiss();a.refresh();
            }).setNegativeButton("Cancelar",null).show();
    }
    private void exemption(DemoDb.Player player){
        new AlertDialog.Builder(a).setTitle("Cobrança de "+player.name)
            .setItems(new String[]{"Automático (regras dos goleiros)","Isentar deste evento","Cobrar normalmente"},(d,which)->{
                f.setExemption(a.groupId,player.id,which==0?null:which==1);a.refresh();
            }).setNegativeButton("Cancelar",null).show();
    }
    private void shareSummary(){
        LocalFinance.Summary s=f.summary(a.groupId);
        String g=db.group(a.groupId)[0];
        share("PELADA LEAGUE · RESUMO FINANCEIRO\n"+g+
            "\nConfirmados: "+s.people+"\nPagamentos completos: "+s.paid+
            "\nIsentos: "+s.waived+"\nPrevisto: "+money(s.expected)+
            "\nRecebido: "+money(s.received)+"\nPendente: "+money(s.pending)+
            "\nRegistro manual e local; não é comprovante de transferência.");
    }
    private void history(){
        List<String[]> items=f.ledger(a.groupId,30);
        if(items.isEmpty()){a.toast("Ainda não há alterações de pagamento.");return;}
        Map<Long,String> names=new HashMap<>();
        for(DemoDb.Player player:db.groupPlayers(a.groupId))names.put(player.id,player.name);
        StringBuilder lines=new StringBuilder();
        SimpleDateFormat fmt=new SimpleDateFormat("dd/MM HH:mm",new Locale("pt","BR"));
        for(String[] item:items){
            int value=Integer.parseInt(item[1]);
            lines.append(names.getOrDefault(Long.parseLong(item[0]),"Jogador "+item[0]))
                .append(": ").append(value>=0?"+":"-").append(money(Math.abs(value)))
                .append(" · ").append(item[2])
                .append(" · ").append(fmt.format(new Date(Long.parseLong(item[3]))))
                .append("\n\n");
        }
        new AlertDialog.Builder(a).setTitle("Histórico financeiro (local)").setMessage(lines.toString())
            .setPositiveButton("Fechar",null).show();
    }
}
