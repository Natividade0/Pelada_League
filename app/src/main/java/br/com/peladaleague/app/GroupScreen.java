package br.com.peladaleague.app;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

final class GroupScreen extends Screen {
    GroupScreen(MainActivity a){super(a);}
    void render(){if(a.route.equals("groups")){groups();return;}if(a.route.equals("attendance")){attendance();return;}group();}
    private void groups(){
        LinearLayout h=Ui.row(a);h.addView(text("Minhas peladas",23,Ui.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));h.addView(iconButton("settings","Informações",a::info));add(h);sub("Seu grupo. Seu futebol.");gap(12);
        for(String[] g:db.groups()){long id=Long.parseLong(g[0]);LinearLayout c=Ui.col(a);c.addView(text(g[1],18,Ui.WHITE,true));Ui.space(c,6);line(c,"location",g[2],null);line(c,"calendar",g[3],null);Ui.put(c,text(db.groupPlayers(id).size()+" membros · "+db.confirmedCount(id)+" confirmados",12,Ui.GOLD,true),7);FrameLayout cover=stadium(c,138,false);if(!a.tutorialTargets.containsKey("groups_first"))a.mark("groups_first",cover);Ui.tappable(cover,()->a.selectGroup(id));add(cover);gap(10);}
        if(a.canManage())button("Criar pelada",true,()->new Forms(a).createGroup());
    }
    private void group(){title("Minha pelada");String[] g=db.group(a.groupId);LinearLayout overlay=Ui.row(a);overlay.addView(new SportArt(a,"crest",Ui.GOLD),Ui.lp(a,48,54));LinearLayout info=Ui.col(a);info.setPadding(Ui.dp(a,10),0,0,0);info.addView(text(g[0],19,Ui.WHITE,true));Ui.put(info,text("Grupo · "+db.groupPlayers(a.groupId).size()+" membros",12,Ui.MUTED,false),6);overlay.addView(info,new LinearLayout.LayoutParams(0,-2,1));LinearLayout content=Ui.col(a);content.addView(overlay);add(stadium(content,100,false));gap(12);
        tabs(new String[]{"Geral","Partidas","Membros","Estatísticas"},a.groupTab,i->{a.groupTab=i;a.positions.put("group:"+a.groupId,0);a.refresh();});
        if(a.groupTab==1){matches();return;}if(a.groupTab==2){members();return;}if(a.groupTab==3){statistics();return;}
        LinearLayout next=card();LinearLayout header=Ui.row(a);header.addView(text("Próxima partida",16,Ui.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));header.addView(Ui.badge(a,db.confirmedCount(a.groupId)+" / "+db.capacity(a.groupId),false));next.addView(header);Ui.space(next,7);line(next,"calendar",g[2],null);line(next,"location",g[1],null);
        boolean member=db.attendance(a.groupId).containsKey(a.playerId);String status=db.attendance(a.groupId).getOrDefault(a.playerId,DemoDb.PENDING);
        if(member){String label=status.equals(DemoDb.CONFIRMED)?"Presença confirmada · cancelar":status.equals(DemoDb.WAITLIST)?"Na lista de espera · cancelar":"Confirmar minha presença";
            Ui.put(next,Ui.button(a,label,true,()->a.safely(()->{String now=db.markAttendance(a.groupId,a.playerId,status.equals(DemoDb.CONFIRMED)||status.equals(DemoDb.WAITLIST)?DemoDb.DECLINED:DemoDb.CONFIRMED);a.toast(now.equals(DemoDb.WAITLIST)?"Você entrou na lista de espera.":now.equals(DemoDb.CONFIRMED)?"Presença confirmada.":"Presença cancelada.");a.refresh();Motion.score(a.page);})),8);}
        else Ui.put(next,text("Seu perfil local ainda não faz parte deste grupo.",12,Ui.MUTED,false),8);
        add(a.mark("group_presence",next));gap(12);
        LocalFinance.Summary finance=a.finance.summary(a.groupId);
        if(finance.people>0){
            TextView summary=text("Pagamentos · "+FinanceScreen.money(finance.received)+" recebido · "+FinanceScreen.money(finance.pending)+" pendente",12,Ui.GOLD,true);
            summary.setMinHeight(Ui.dp(a,44));Ui.tappable(summary,()->a.open("finance"));add(summary);
        }
        gap(15);heading("Ações do grupo");LinearLayout actions=card();
        if(a.canManage()){actions.addView(Ui.action(a,"calendar","Criar partida",null,()->a.open("draw")));Ui.divider(actions);}
        actions.addView(Ui.action(a,"shuffle","Sortear times",null,()->a.open("draw")));Ui.divider(actions);
        actions.addView(Ui.action(a,"users",a.canManage()?"Gerenciar presenças":"Ver presenças",db.waitCount(a.groupId)+" na lista de espera",()->a.open("attendance")));Ui.divider(actions);
        actions.addView(Ui.action(a,"wallet","Pagamentos da pelada","Pix manual, contribuições e isenção de goleiros",()->a.open("finance")));Ui.divider(actions);
        actions.addView(Ui.action(a,"stats","Estatísticas do grupo",null,()->{a.groupTab=3;a.refresh();}));
        if(a.canManage()){Ui.divider(actions);actions.addView(Ui.action(a,"plus","Convidar jogadores",null,()->share("Vamos jogar na "+g[0]+"?\n"+g[2]+" · "+g[1]+"\nCombine a participação com o organizador. Convite informativo do Pelada League; esta versão não permite entrar por link.")));}
        add(actions);gap(14);heading("Sessões da pelada");LinearLayout sessions=card();int count=0;for(String[] s:db.sessions(a.groupId)){if(count++==3)break;String date=new java.text.SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR")).format(new Date(Long.parseLong(s[1])));line(sessions,"calendar",date+" · "+(s[2].equals("1")?"Atual":"Arquivada"),s[3]+" presentes");}add(sessions);
    }
    private void matches(){if(a.canManage())button("Criar partida com os confirmados",true,()->a.open("draw"));gap(10);if(db.games(a.groupId).isEmpty()){empty("Nenhuma partida ainda","Confirme os jogadores e sorteie os times para começar.","Ver presenças",()->a.open("attendance"));return;}LinearLayout c=card();for(DemoDb.Game m:db.games(a.groupId)){gameRow(c,m);Ui.divider(c);}add(c);}
    private void members(){if(a.canManage())button("Adicionar jogador",true,()->new Forms(a).member());gap(10);LinearLayout c=card();for(DemoDb.Player p:db.groupPlayers(a.groupId)){LinearLayout r=Ui.row(a);r.setMinimumHeight(Ui.dp(a,54));r.addView(Ui.initials(a,p.name,34),Ui.lp(a,34,34));LinearLayout t=Ui.col(a);t.setPadding(Ui.dp(a,9),0,0,0);t.addView(text(p.name,14,Ui.WHITE,true));Ui.put(t,text(p.pos+" · habilidade "+p.skill,11,Ui.MUTED,false),4);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(a.canManage())r.addView(iconButton("edit","Editar "+p.name,()->new Forms(a).editPlayer(p)));c.addView(r);Ui.divider(c);}if(db.groupPlayers(a.groupId).isEmpty())c.addView(text("Adicione os primeiros jogadores do grupo.",13,Ui.MUTED,false));add(c);}
    private void statistics(){int matches=0,goals=0;for(DemoDb.Game m:db.games(a.groupId))if(m.status.equals("DEMO_APPROVED")){matches++;goals+=db.goals(m.id,0)+db.goals(m.id,1);}pair(page,Ui.stat(a,""+matches,"Partidas validadas"),Ui.stat(a,""+goals,"Gols registrados"));gap(12);new LeagueScreen(a).scorers(10);button("Ver classificação e jogos",true,()->a.open("league"));sub("Resultados validados na demonstração local.");}
    private void attendance(){title("Presenças");int count=db.confirmedCount(a.groupId);add(text(count+" confirmados · "+Math.max(0,db.capacity(a.groupId)-count)+" vagas",18,Ui.WHITE,true));sub(db.waitCount(a.groupId)+" na espera · promoção automática por ordem de entrada");gap(12);
        Map<Long,String> statuses=db.attendance(a.groupId);LinearLayout list=card();for(DemoDb.Player p:db.groupPlayers(a.groupId)){String status=statuses.get(p.id);LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,57));row.addView(Ui.initials(a,p.name,32),Ui.lp(a,32,32));LinearLayout t=Ui.col(a);t.setPadding(Ui.dp(a,8),0,Ui.dp(a,4),0);t.addView(text(p.name,14,Ui.WHITE,true));Ui.put(t,text(a.status(status),11,status.equals(DemoDb.CONFIRMED)?Ui.GOLD:Ui.MUTED,false),4);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(a.canManage()||p.id==a.playerId){TextView change=Ui.chip(a,status.equals(DemoDb.CONFIRMED)||status.equals(DemoDb.WAITLIST)?"Cancelar":"Confirmar",status.equals(DemoDb.CONFIRMED),()->a.safely(()->{db.markAttendance(a.groupId,p.id,status.equals(DemoDb.CONFIRMED)||status.equals(DemoDb.WAITLIST)?DemoDb.DECLINED:DemoDb.CONFIRMED);a.refresh();}));change.setMinWidth(Ui.dp(a,92));row.addView(change);}list.addView(row);Ui.divider(list);}add(a.mark("attendance_roster",list));button("Sortear times",true,()->a.open("draw"));
        if(a.canManage()){button("Ajustar vagas · "+db.capacity(a.groupId),false,()->new Forms(a).capacity());button("Iniciar próxima sessão",false,()->a.confirm("Nova sessão?","As presenças atuais serão arquivadas e o sorteio será invalidado. As partidas e os jogadores serão preservados.",()->{db.newSession(a.groupId);a.refresh();}));}
    }
}
