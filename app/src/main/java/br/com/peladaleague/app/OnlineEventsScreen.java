package br.com.peladaleague.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

/**
 * Actual multiuser event UI backed by secure Supabase RPCs, activated only once
 * the owner configures a project. Offline demo remains an independent mode.
 */
final class OnlineEventsScreen extends Screen {
    static final class State {
        JSONArray groups=new JSONArray(),events=new JSONArray();
        Map<String,String> personalStatuses=new HashMap<>();
        JSONObject preview,finance;
        String code="",lastError="";
        boolean loading=false,loaded=false,previewLoading=false;
    }
    private final State state;
    OnlineEventsScreen(MainActivity a){super(a);state=a.onlineState;}
    private String date(String iso){
        try{SimpleDateFormat parser=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.ROOT);
            Date parsed=parser.parse(iso);return new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("pt","BR")).format(parsed);}
        catch(Exception ex){return iso==null?"":iso.replace("T"," "); }
    }
    void render(){
        title("Eventos e convites online");
        if(!a.auth.enabled()){
            empty("Servidor ainda não configurado","O módulo online foi preparado, mas precisa de um projeto Supabase gratuito e do banco de dados. Nenhum convite pela internet funciona sem isso.","Usar pelada local",()->a.tab("groups"));
            return;
        }
        if(!a.auth.loggedIn()){
            empty("Entre com seu e-mail","Crie sua conta, escolha sua posição e entre para participar de convites reais.","Entrar ou criar conta",()->a.open("account"));
            return;
        }
        if(!state.loaded&&!state.loading)a.shell.post(this::load);
        if(!state.code.isEmpty()&&state.preview==null&&!state.previewLoading)a.shell.post(()->open(state.code));
        LinearLayout intro=card();
        intro.addView(text("Peladas compartilhadas",17,Ui.GOLD,true));
        Ui.put(intro,text("Organize um evento, envie um convite e receba confirmações de pessoas diferentes. As inscrições ficam no servidor.",12,Ui.MUTED,false),6);
        add(intro);gap(12);

        if(!state.code.isEmpty()&&state.preview!=null)preview();
        else if(!state.code.isEmpty()&&state.previewLoading)sub("Carregando convite…");

        if(!state.lastError.isEmpty())sub("Aviso: "+state.lastError);
        if(state.loading)sub("Consultando eventos online…");
        if(!state.loading){
            if(state.groups.length()==0)button("Criar meu primeiro grupo online",true,this::createGroup);
            else{
                button("Criar evento e gerar convite",true,this::createEvent);
                button("Adicionar outro grupo",false,this::createGroup);
            }
            button("Abrir convite por código",false,this::askCode);
            gap(8);
            if(EventReminders.enabled(a)){
                button("Desativar lembretes neste aparelho",false,()->{
                    EventReminders.setEnabled(a,false);a.refresh();
                });
            }else button("Ativar lembretes de partidas",false,this::enableReminders);
            TextView refresh=Ui.chip(a,"Atualizar eventos",false,this::load);
            add(refresh);
        }
        gap(16);heading("Meus próximos eventos");
        if(state.events.length()==0){sub("Nenhum evento online encontrado na sua conta.");return;}
        for(int i=0;i<state.events.length();i++){
            JSONObject e=state.events.optJSONObject(i);if(e==null)continue;
            String code=e.optString("invite_code"),id=e.optString("id");
            LinearLayout item=card();
            item.addView(text(e.optString("title","Evento"),16,Ui.WHITE,true));
            Ui.put(item,text(date(e.optString("starts_at"))+" · "+e.optString("location"),12,Ui.MUTED,false),5);
            Ui.put(item,text("Status: "+e.optString("status","OPEN"),11,Ui.GOLD,true),6);
            if(!code.isEmpty()){
                Ui.put(item,Ui.button(a,"Abrir evento",false,()->open(code)),10);
                Ui.put(item,Ui.chip(a,"Compartilhar convite",false,()->shareInvitation(e.optString("title"),code)),7);
            }
            Ui.put(item,Ui.chip(a,"Central de partida online",false,()->openLive(id)),7);
            Ui.put(item,Ui.chip(a,"Pagamentos online",false,()->onlineFinance(id)),7);
            add(item);gap(9);
        }
        sub("Os avisos são lembretes agendados neste aparelho, depois que você autoriza notificações e abre os eventos. O aplicativo ainda não envia push automático para todos os convidados.");
        sub("As peladas da demonstração local não são importadas automaticamente.");
    }
    private void load(){
        if(!a.auth.loggedIn()||state.loading)return;
        state.loading=true;state.lastError="";
        a.auth.runOnline(()->{
            JSONArray g=a.auth.authorized("GET","/rest/v1/groups?select=id,name&order=created_at.desc",null).getJSONArray("rows");
            JSONArray events=a.auth.authorized("GET","/rest/v1/events?select=id,group_id,title,location,starts_at,status,invite_code&order=created_at.desc&limit=30",null).getJSONArray("rows");
            Map<String,String> self=new HashMap<>();
            String user=a.auth.userId();
            if(user.matches("[a-fA-F0-9\\-]{36}")){
                JSONArray own=a.auth.authorized("GET","/rest/v1/event_attendance?select=event_id,status&user_id=eq."+user,null).getJSONArray("rows");
                for(int i=0;i<own.length();i++){
                    JSONObject entry=own.optJSONObject(i);
                    if(entry!=null)self.put(entry.optString("event_id"),entry.optString("status"));
                }
            }
            a.runOnUiThread(()->{state.groups=g;state.events=events;state.personalStatuses=self;state.loaded=true;});
            return "Eventos atualizados.";
        },(ok,msg)->{
            state.loading=false;
            if(!ok)state.lastError=msg;
            else syncReminders();
            if(a.route.equals("online"))a.refresh();
        });
    }
    /** Native opt-in: no prompts before the player explicitly requests reminders. */
    private void enableReminders(){
        if(!EventReminders.allowed(a)){
            if(android.os.Build.VERSION.SDK_INT>=33)
                a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    EventReminders.PERMISSION_REQUEST);
            else a.toast("Ative as notificações deste aplicativo nas configurações do Android.");
            return;
        }
        EventReminders.setEnabled(a,true);
        syncReminders();
        a.toast("Avisos ativados neste aparelho.");
        a.refresh();
    }
    /** Only the organizer or a confirmed player receives event reminders on this device. */
    void syncReminders(){
        if(!EventReminders.enabled(a)||!EventReminders.allowed(a))return;
        EventReminders.clear(a);
        Set<String> ownedGroups=new HashSet<>();
        for(int i=0;i<state.groups.length();i++){
            JSONObject group=state.groups.optJSONObject(i);
            if(group!=null)ownedGroups.add(group.optString("id"));
        }
        for(int i=0;i<state.events.length();i++){
            JSONObject event=state.events.optJSONObject(i);
            if(event==null)continue;
            boolean organizer=ownedGroups.contains(event.optString("group_id"));
            boolean playing=DemoDb.CONFIRMED.equals(state.personalStatuses.get(event.optString("id")));
            if(organizer||playing)EventReminders.schedule(a,event);
        }
    }
    private void askCode(){
        EditText code=Ui.input(a,"Cole o código do convite");
        code.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(a).setTitle("Abrir convite").setView(code).setNegativeButton("Cancelar",null)
            .setPositiveButton("Abrir",(dialog,id)->{
                String value=code.getText().toString().trim();
                if(value.contains("code="))value=value.substring(value.indexOf("code=")+5).split("&")[0];
                open(value);
            }).show();
    }
    void open(String token){
        if(token==null||!token.matches("[a-fA-F0-9]{32}")){
            a.toast("O convite precisa ter um código válido de 32 caracteres.");return;
        }
        state.code=token.toLowerCase(Locale.ROOT);state.preview=null;state.finance=null;
        if(state.previewLoading)return;
        state.previewLoading=true;
        JSONObject body=new JSONObject();
        try{body.put("p_code",state.code);}catch(Exception ignored){}
        a.auth.runOnline(()->{
            JSONObject preview=a.auth.authorized("POST","/rest/v1/rpc/event_preview",body);
            a.runOnUiThread(()->state.preview=preview);
            return "Convite carregado.";
        },(ok,msg)->{
            state.previewLoading=false;
            if(!ok){state.lastError=msg;state.code="";}
            else state.lastError="";
            if(a.route.equals("online"))a.refresh();
        });
    }
    private void preview(){
        JSONObject ev=state.preview;
        LinearLayout card=card();
        card.addView(text(ev.optString("title","Convite"),18,Ui.GOLD,true));
        Ui.put(card,text(ev.optString("group","Grupo")+" · "+date(ev.optString("starts_at")),12,Ui.WHITE,false),8);
        Ui.put(card,text("Local: "+ev.optString("location"),12,Ui.MUTED,false),6);
        Ui.put(card,text(ev.optInt("confirmed")+" / "+ev.optInt("capacity")+" confirmados · "+ev.optInt("waiting")+" na espera",12,Ui.WHITE,true),7);
        Ui.put(card,text("Contribuição por pessoa: "+FinanceScreen.money(ev.optInt("fee_cents"))+(ev.optBoolean("goalkeeper_exempt")?" · goleiros podem ser isentos":""),12,Ui.MUTED,false),5);
        Ui.put(card,text("Minha presença: "+ev.optString("my_status","PENDING"),12,Ui.GOLD,true),8);
        add(card);gap(10);
        button("Ver placar da partida",false,()->openLive(ev.optString("id")));
        if(ev.optString("status").equals("OPEN")){
            button("Vou jogar · confirmar presença",true,()->respond(true));
            button("Não vou jogar",false,()->respond(false));
        }else sub("As inscrições deste evento estão encerradas.");
        TextView close=Ui.chip(a,"Fechar convite",false,()->{state.code="";state.preview=null;a.getPreferences(0).edit().remove("pending_invite").apply();a.refresh();});add(close);gap(14);
    }
    private void respond(boolean going){
        if(!going){sendResponse(false,"LIN");return;}
        String[] labels={"Goleiro","Zagueiro","Meio-campo","Atacante","Jogador de linha"};
        String[] codes={"GOL","ZAG","MEI","ATA","LIN"};
        new AlertDialog.Builder(a).setTitle("Qual função você vai jogar nesta partida?")
            .setItems(labels,(d,i)->sendResponse(true,codes[i]))
            .setNegativeButton("Cancelar",null).show();
    }
    private void sendResponse(boolean going,String position){
        try{
            JSONObject body=new JSONObject().put("p_code",state.code).put("p_going",going);
            body.put("p_position",position);
            a.auth.runOnline(()->{
                JSONObject resp=a.auth.authorized("POST","/rest/v1/rpc/event_respond",body);
                return resp.optString("status","Registro salvo no servidor.");
            },(ok,msg)->{
                if(ok){a.toast("Presença: "+msg);open(state.code);load();}
                else a.toast(msg);
            });
        }catch(Exception ex){a.toast("Não foi possível preparar a confirmação.");}
    }
    private EditText field(LinearLayout form,String label,String initial){
        Ui.put(form,text(label,12,Ui.WHITE,true),10);
        EditText entry=Ui.input(a,label);entry.setText(initial);Ui.put(form,entry,5);return entry;
    }
    private void createGroup(){
        LinearLayout form=Ui.col(a);form.setPadding(Ui.dp(a,16),Ui.dp(a,6),Ui.dp(a,16),Ui.dp(a,10));
        EditText name=field(form,"Nome da pelada","");name.setSingleLine(true);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Novo grupo online").setView(form)
            .setNegativeButton("Cancelar",null).setPositiveButton("Criar",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            String n=name.getText().toString().trim();
            if(n.length()<3){a.toast("Informe um nome com pelo menos 3 caracteres.");return;}
            d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            try{
                JSONObject body=new JSONObject().put("p_name",n);
                a.auth.runOnline(()->{
                    JSONObject result=a.auth.authorized("POST","/rest/v1/rpc/create_group",body);
                    if(result.optString("id").isEmpty())throw new IllegalStateException("Grupo não retornado.");
                    return "Grupo criado no servidor.";
                },(ok,msg)->{a.toast(msg);if(ok){d.dismiss();load();}else d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);});
            }catch(Exception e){a.toast("Dados do grupo inválidos.");}
        }));
        d.show();
    }
    private void createEvent(){
        if(state.groups.length()==0){createGroup();return;}
        LinearLayout form=Ui.col(a);form.setPadding(Ui.dp(a,15),Ui.dp(a,6),Ui.dp(a,15),Ui.dp(a,12));
        ArrayList<String> names=new ArrayList<>();
        for(int i=0;i<state.groups.length();i++)names.add(state.groups.optJSONObject(i).optString("name"));
        Ui.put(form,text("Grupo",12,Ui.WHITE,true),8);
        Spinner group=new Spinner(a);group.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));Ui.put(form,group,5);
        EditText title=field(form,"Nome do evento","Pelada da semana");
        EditText location=field(form,"Nome do campo ou endereço","");
        SimpleDateFormat df=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.forLanguageTag("pt-BR"));
        EditText date=field(form,"Dia e horário (dd/MM/aaaa HH:mm)",df.format(new Date(System.currentTimeMillis()+86400000L)));
        EditText capacity=field(form,"Quantidade de vagas","14");capacity.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText price=field(form,"Valor por jogador (R$)","20,00");price.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText cost=field(form,"Custo do campo (R$)","250,00");cost.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        CheckBox keeper=new CheckBox(a);keeper.setText("Goleiros isentos de contribuição");keeper.setTextColor(Ui.WHITE);keeper.setChecked(true);Ui.put(form,keeper,12);
        EditText pix=field(form,"Chave Pix (opcional)","");
        Ui.put(form,text("Repetição semanal",12,Ui.WHITE,true),12);
        Spinner recurrence=new Spinner(a);
        recurrence.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Somente esta partida","4 semanas (4 eventos)","8 semanas (8 eventos)"}));
        Ui.put(form,recurrence,6);
        Ui.put(form,text("Cada semana terá uma lista de presença e um link de convite próprios.",11,Ui.MUTED,false),7);
        ScrollView sc=new ScrollView(a);sc.addView(form);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Criar evento online").setView(sc)
            .setNegativeButton("Cancelar",null).setPositiveButton("Criar evento",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            try{
                df.setLenient(false);Date when=df.parse(date.getText().toString().trim());
                if(when==null||when.before(new Date()))throw new IllegalArgumentException("Informe uma data futura.");
                String begins=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.ROOT).format(when);
                JSONObject selectedGroup=state.groups.getJSONObject(group.getSelectedItemPosition());
                JSONObject body=new JSONObject();
                body.put("p_group",selectedGroup.getString("id"));
                body.put("p_title",title.getText().toString().trim());
                body.put("p_location",location.getText().toString().trim());
                body.put("p_starts_at",begins);
                body.put("p_capacity",Integer.parseInt(capacity.getText().toString().trim()));
                body.put("p_fee_cents",FinanceScreen.cents(price.getText().toString()));
                body.put("p_field_cost_cents",FinanceScreen.cents(cost.getText().toString()));
                body.put("p_keeper_exempt",keeper.isChecked());
                body.put("p_pix_key",pix.getText().toString().trim());
                final int weeks=recurrence.getSelectedItemPosition()==0?1:
                    recurrence.getSelectedItemPosition()==1?4:8;
                if(weeks>1)body.put("p_weeks",weeks);
                d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                a.auth.runOnline(()->{
                    String route=weeks==1?"create_event":"create_weekly_events";
                    JSONObject result=a.auth.authorized("POST","/rest/v1/rpc/"+route,body);
                    final String code=result.getString("code");
                    a.runOnUiThread(()->state.code=code);
                    return weeks==1?"Evento criado no servidor.":weeks+" eventos semanais criados no servidor.";
                },(ok,msg)->{
                    a.toast(msg);
                    if(ok){d.dismiss();load();a.shell.postDelayed(()->open(state.code),250);}
                    else d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                });
            }catch(Exception ex){a.toast(ex.getMessage()==null?"Revise os dados do evento.":ex.getMessage());}
        }));
        d.show();
    }
    private void shareInvitation(String name,String code){
        if(!BuildConfig.INVITE_LINK_ENABLED){
            new AlertDialog.Builder(a).setTitle("Link ainda não publicado")
                .setMessage("O evento existe no servidor, mas falta publicar a página gratuita que recebe convites do WhatsApp. Compartilhamento ficará disponível após essa configuração.")
                .setPositiveButton("Entendi",null).show();
            return;
        }
        try{
            String url=BuildConfig.SUPABASE_URL.replaceAll("/+$","")+
                "/functions/v1/invite?code="+URLEncoder.encode(code,"UTF-8");
            share("PELADA LEAGUE · "+name+"\nConfirme sua presença pelo link:\n"+url);
        }catch(Exception ex){a.toast("Não foi possível preparar o convite.");}
    }
    private void openLive(String eventId){
        if(eventId==null||!eventId.matches("[a-fA-F0-9-]{36}")){
            a.toast("Evento inválido.");return;
        }
        a.liveState.eventId=eventId;
        a.liveState.snapshot=null;
        a.liveState.loading=false;
        a.liveState.error="";
        a.liveState.lastTotal=-1;
        a.open("live");
    }
    private void onlineFinance(String eventId){
        if(eventId==null||eventId.isEmpty())return;
        try{
            JSONObject body=new JSONObject().put("p_event",eventId);
            a.auth.runOnline(()->{
                JSONObject data=a.auth.authorized("POST","/rest/v1/rpc/event_finance",body);
                a.runOnUiThread(()->state.finance=data);
                return "Financeiro carregado.";
            },(ok,msg)->{
                if(!ok)a.toast(msg);
                else showFinance(eventId);
            });
        }catch(Exception ex){a.toast("Evento inválido.");}
    }
    private void showFinance(String eventId){
        JSONObject data=state.finance;if(data==null)return;
        JSONArray players=data.optJSONArray("players");if(players==null)players=new JSONArray();
        int total=0,received=0;
        String[] labels=new String[players.length()];
        for(int i=0;i<players.length();i++){
            JSONObject p=players.optJSONObject(i);
            int due=p.optInt("due_cents"),paid=p.optInt("paid_cents");
            total+=due;received+=paid;
            labels[i]=p.optString("name")+" · "+(due==0?"ISENTO":FinanceScreen.money(paid)+" / "+FinanceScreen.money(due));
        }
        final JSONArray roster=players;
        new AlertDialog.Builder(a).setTitle("Recebido "+FinanceScreen.money(received)+" · pendente "+FinanceScreen.money(Math.max(0,total-received)))
            .setItems(labels,(dialog,index)->onlinePayment(eventId,roster.optJSONObject(index)))
            .setPositiveButton("Fechar",null).show();
    }
    private void onlinePayment(String eventId,JSONObject player){
        if(player==null)return;
        int due=player.optInt("due_cents");
        if(due==0){a.toast("Este participante está isento.");return;}
        EditText amount=Ui.input(a,"Total já recebido");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(String.format(Locale.forLanguageTag("pt-BR"),"%.2f",player.optInt("paid_cents")/100.0));
        new AlertDialog.Builder(a).setTitle("Total recebido de "+player.optString("name")+" (máx. "+FinanceScreen.money(due)+")")
            .setView(amount).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,which)->{
                try{
                    int cents=FinanceScreen.cents(amount.getText().toString());
                    JSONObject body=new JSONObject().put("p_event",eventId).put("p_user",player.getString("user_id"))
                        .put("p_total_cents",cents).put("p_method","PIX");
                    a.auth.runOnline(()->{
                        a.auth.authorized("POST","/rest/v1/rpc/mark_event_payment",body);
                        return "Pagamento registrado manualmente.";
                    },(ok,msg)->{
                        a.toast(msg);if(ok)onlineFinance(eventId);
                    });
                }catch(Exception ex){a.toast(ex.getMessage());}
            }).show();
    }
}
