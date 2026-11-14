package br.com.peladaleague.app;

import android.app.AlertDialog;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.util.*;

/**
 * Provisional online match centre. Owner writes events via Supabase RPC;
 * other participants see the same server snapshot, refreshed while this screen is open.
 * This is not an official approved match report, and it does not send background push.
 */
final class OnlineLiveScreen extends Screen {
    static final class State {
        String eventId="";
        JSONObject snapshot;
        boolean loading=false,busy=false;
        String error="";
        int lastTotal=-1;
    }
    private final State state;
    OnlineLiveScreen(MainActivity a){super(a);state=a.liveState;}
    void render(){
        title("Partida ao vivo");
        if(!a.auth.enabled()||!a.auth.loggedIn()){
            empty("Entre com sua conta","Para acompanhar o mesmo placar em diferentes celulares é necessário ativar o servidor gratuito e entrar na sua conta.","Ir para conta",()->a.open("account"));
            return;
        }
        if(state.eventId.isEmpty()){
            empty("Nenhuma partida selecionada","Abra um evento online para acompanhar o placar.","Meus eventos",()->a.open("online"));
            return;
        }
        if(state.snapshot==null&&!state.loading)a.shell.post(this::load);
        if(!state.error.isEmpty())sub(state.error);
        if(state.snapshot==null){sub(state.loading?"Carregando placar do servidor…":"Não foi possível carregar o placar.");return;}

        JSONObject data=state.snapshot;
        boolean owner=data.optBoolean("owner",false);
        String status=data.optString("state","READY");
        add(text(data.optString("title","Evento"),17,Ui.WHITE,true));gap(12);

        LinearLayout scoreboard=card();
        LinearLayout teams=Ui.row(a);
        LinearLayout left=Ui.col(a);left.setGravity(Gravity.CENTER);
        left.addView(new SportArt(a,"shirt",Ui.GOLD),Ui.lp(a,46,49));
        TextView leftName=text(data.optString("team_a","Time Amarelo"),12,Ui.WHITE,true);
        leftName.setGravity(Gravity.CENTER);Ui.put(left,leftName,5);
        teams.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout middle=Ui.col(a);middle.setGravity(Gravity.CENTER);
        int goals=data.optInt("score_a")+data.optInt("score_b");
        TextView score=text(data.optInt("score_a")+"  :  "+data.optInt("score_b"),28,Ui.GOLD,true);
        score.setGravity(Gravity.CENTER);middle.addView(score);
        if(state.lastTotal>=0&&state.lastTotal!=goals)Motion.score(score);
        state.lastTotal=goals;
        TextView clock=text(LocalMatchClock.display(data.optLong("elapsed_ms")),18,Ui.WHITE,true);
        clock.setGravity(Gravity.CENTER);Ui.put(middle,clock,6);
        teams.addView(middle,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout right=Ui.col(a);right.setGravity(Gravity.CENTER);
        right.addView(new SportArt(a,"shirt",0xFFB9C0C8),Ui.lp(a,46,49));
        TextView rightName=text(data.optString("team_b","Time Preto"),12,Ui.WHITE,true);
        rightName.setGravity(Gravity.CENTER);Ui.put(right,rightName,5);
        teams.addView(right,new LinearLayout.LayoutParams(0,-2,1));
        scoreboard.addView(teams);
        Ui.put(scoreboard,text("Status: "+statusLabel(status),12,Ui.MUTED,true),10);
        add(scoreboard);gap(10);
        if("LIVE".equals(status))animateClock(clock,data.optLong("elapsed_ms"));
        if(owner)ownerActions(status);
        else sub("Acompanhamento somente leitura. Os lances são registrados pelo organizador.");

        gap(12);heading("Lances registrados");
        JSONArray timeline=data.optJSONArray("timeline");
        LinearLayout box=card();
        if(timeline==null||timeline.length()==0){
            box.addView(text("Ainda não existem lances neste evento.",13,Ui.MUTED,false));
        }else for(int i=0;i<timeline.length();i++){
            JSONObject entry=timeline.optJSONObject(i);if(entry==null)continue;
            String action=entry.optString("action");
            String label="GOAL".equals(action)?"GOL!":"YELLOW".equals(action)?"Cartão amarelo":
                "RED".equals(action)?"Cartão vermelho":"Substituição";
            int color="YELLOW".equals(action)?Ui.GOLD:"RED".equals(action)?0xFFEF6B6B:Ui.WHITE;
            LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,53));
            TextView minute=text(entry.optInt("minute")+"'",12,Ui.GOLD,true);
            row.addView(minute,Ui.lp(a,38,-2));
            LinearLayout info=Ui.col(a);
            info.addView(text(label+" · "+entry.optString("player"),13,color,true));
            if("SUB".equals(action))Ui.put(info,text("Entra: "+entry.optString("other"),11,Ui.MUTED,false),4);
            else Ui.put(info,text(entry.optInt("side")==0?data.optString("team_a"):data.optString("team_b"),11,Ui.MUTED,false),3);
            row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            box.addView(row);Ui.divider(box);
        }
        add(box);gap(10);
        sub("Este placar online é provisório, sem validação oficial de súmula. A tela consulta o servidor periodicamente enquanto estiver aberta; não são notificações push.");
        poll(status);
    }
    private String statusLabel(String status){
        switch(status){case "LIVE":return "Em andamento";case "PAUSED":return "Pausada";
            case "FINISHED":return "Encerrada";default:return "Aguardando início";}
    }
    private void animateClock(TextView clock,long elapsed){
        final long base=SystemClock.elapsedRealtime(),id=state.eventId.hashCode();
        final LinearLayout host=a.shell;
        clock.post(new Runnable(){
            @Override public void run(){
                if(a.shell!=host||!host.isAttachedToWindow()||!a.route.equals("live")
                    ||state.eventId.hashCode()!=id)return;
                clock.setText(LocalMatchClock.display(elapsed+SystemClock.elapsedRealtime()-base));
                clock.postDelayed(this,1000);
            }
        });
    }
    private void poll(String status){
        final LinearLayout host=a.shell;
        long period="LIVE".equals(status)?15000L:45000L;
        host.postDelayed(()->{
            if(a.shell==host&&a.route.equals("live")&&host.isAttachedToWindow())load();
        },period);
    }
    void load(){
        if(state.loading||state.eventId.isEmpty()||!a.auth.loggedIn())return;
        state.loading=true;
        final String eventId=state.eventId;
        try{
            JSONObject request=new JSONObject().put("p_event",eventId);
            a.auth.runOnline(()->{
                JSONObject response=a.auth.authorized("POST","/rest/v1/rpc/live_match_snapshot",request);
                a.runOnUiThread(()->{if(eventId.equals(state.eventId))state.snapshot=response;});
                return "Placar atualizado.";
            },(ok,msg)->{
                state.loading=false;state.error=ok?"":msg;
                if(eventId.equals(state.eventId)&&a.route.equals("live"))a.refresh();
            });
        }catch(Exception ex){state.loading=false;state.error="Evento inválido.";a.refresh();}
    }
    private void ownerActions(String status){
        if(state.busy){sub("Salvando alteração no servidor…");return;}
        if("READY".equals(status))button("Iniciar partida",true,()->setState("LIVE"));
        else if("LIVE".equals(status)){
            button("Pausar cronômetro",false,()->setState("PAUSED"));
            button("Encerrar partida",false,()->confirmFinish());
        }else if("PAUSED".equals(status)){
            button("Continuar partida",true,()->setState("LIVE"));
            button("Encerrar partida",false,this::confirmFinish);
        }
        if("LIVE".equals(status)||"PAUSED".equals(status)){
            LinearLayout action=Ui.row(a);
            TextView goal=Ui.button(a,"Registrar gol",true,()->register("GOAL"));
            TextView card=Ui.button(a,"Cartão",false,this::chooseCard);
            action.addView(goal,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
            p.leftMargin=Ui.dp(a,8);action.addView(card,p);
            add(action);
            button("Substituição",false,()->register("SUB"));
        }
    }
    private void confirmFinish(){
        a.confirm("Encerrar o placar?","Depois do encerramento, não será possível registrar novos lances nesta versão.",
            ()->setState("FINISHED"));
    }
    private void setState(String next){
        if(state.busy)return;
        state.busy=true;
        try{
            JSONObject body=new JSONObject().put("p_event",state.eventId).put("p_action",next);
            a.auth.runOnline(()->{
                a.auth.authorized("POST","/rest/v1/rpc/live_match_state",body);
                return "Partida atualizada.";
            },(ok,msg)->{
                state.busy=false;a.toast(msg);if(ok)load();else a.refresh();
            });
        }catch(Exception ex){state.busy=false;a.toast("Ação inválida.");}
    }
    private void chooseCard(){
        new AlertDialog.Builder(a).setTitle("Registrar cartão")
            .setItems(new String[]{"Amarelo","Vermelho"},(d,i)->register(i==0?"YELLOW":"RED"))
            .setNegativeButton("Cancelar",null).show();
    }
    private void register(String action){
        JSONArray roster=state.snapshot.optJSONArray("players");
        if(roster==null||roster.length()==0){a.toast("Primeiro confirme os jogadores neste evento.");return;}
        String[] teams={state.snapshot.optString("team_a","Amarelo"),state.snapshot.optString("team_b","Preto")};
        new AlertDialog.Builder(a).setTitle("Qual equipe?")
            .setItems(teams,(dialog,side)->choosePlayer(action,side,roster))
            .setNegativeButton("Cancelar",null).show();
    }
    private void choosePlayer(String action,int side,JSONArray roster){
        String[] names=new String[roster.length()];
        for(int i=0;i<roster.length();i++){
            JSONObject p=roster.optJSONObject(i);
            names[i]=p==null?"Participante":p.optString("name")+" · "+p.optString("position");
        }
        new AlertDialog.Builder(a).setTitle("Selecione o jogador")
            .setItems(names,(dialog,chosen)->{
                JSONObject first=roster.optJSONObject(chosen);
                if("SUB".equals(action))chooseSubstitute(side,first,roster);
                else enterMinute(action,side,first,null);
            }).setNegativeButton("Cancelar",null).show();
    }
    private void chooseSubstitute(int side,JSONObject outgoing,JSONArray roster){
        List<JSONObject> options=new ArrayList<>();
        for(int i=0;i<roster.length();i++){
            JSONObject player=roster.optJSONObject(i);
            if(player!=null&&!player.optString("id").equals(outgoing.optString("id")))options.add(player);
        }
        if(options.isEmpty()){a.toast("Não há outro jogador confirmado.");return;}
        String[] labels=new String[options.size()];
        for(int i=0;i<options.size();i++)labels[i]=options.get(i).optString("name");
        new AlertDialog.Builder(a).setTitle("Quem entra em campo?")
            .setItems(labels,(d,i)->enterMinute("SUB",side,outgoing,options.get(i)))
            .setNegativeButton("Cancelar",null).show();
    }
    private void enterMinute(String action,int side,JSONObject player,JSONObject other){
        EditText minute=Ui.input(a,"Minuto do lance");
        minute.setInputType(InputType.TYPE_CLASS_NUMBER);
        minute.setText(""+Math.min(240,state.snapshot.optLong("elapsed_ms",0)/60000L));
        new AlertDialog.Builder(a).setTitle("Confirmar lance")
            .setMessage("O registro será enviado ao servidor. Confira a equipe e o jogador.")
            .setView(minute).setNegativeButton("Cancelar",null)
            .setPositiveButton("Registrar",(dialog,index)->{
                try{
                    int when=Integer.parseInt(minute.getText().toString().trim());
                    if(when<0||when>240)throw new IllegalArgumentException("Informe um minuto entre 0 e 240.");
                    if(state.busy)return;
                    state.busy=true;
                    JSONObject body=new JSONObject().put("p_event",state.eventId)
                        .put("p_action",action).put("p_side",side)
                        .put("p_player",player.getString("id"))
                        .put("p_second_player",other==null?JSONObject.NULL:other.getString("id"))
                        .put("p_minute",when).put("p_nonce",UUID.randomUUID().toString());
                    a.auth.runOnline(()->{
                        a.auth.authorized("POST","/rest/v1/rpc/record_live_event",body);
                        return "Lance registrado.";
                    },(ok,msg)->{
                        state.busy=false;a.toast(msg);if(ok)load();else a.refresh();
                    });
                }catch(Exception ex){state.busy=false;a.toast("Confira o minuto e o jogador.");}
            }).show();
    }
}
