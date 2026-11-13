package br.com.peladaleague.app;
import android.view.*;
import android.widget.*;
import java.util.*;

final class MatchScreen extends Screen {
    MatchScreen(MainActivity a){super(a);}
    void render(){DemoDb.Game m=db.game(a.gameId);if(m==null){empty("Partida não encontrada","Volte à pelada e selecione uma partida.","Minhas peladas",()->a.tab("groups"));return;}title("Registrar Partida");add(score(m));sub(gameStatus(m));gap(10);clockPanel(m);gap(12);
        tabs(new String[]{"Gols","Cartões","Substituições","Resumo"},a.matchTab,i->{a.matchTab=i;a.refresh();});boolean edit=m.status.equals("DRAFT")&&a.canRecord();boolean roster=db.roster(m.id,0).size()>=2&&db.roster(m.id,1).size()>=2;
        if(a.matchTab==0){LinearLayout events=card();if(db.goals(m.id).isEmpty()){events.addView(text("A bola ainda não entrou.",15,Ui.WHITE,true));Ui.put(events,text("Adicione o primeiro gol para atualizar o placar.",12,Ui.MUTED,false),6);}
            for(DemoDb.Goal g:db.goals(m.id)){LinearLayout row=eventRow("goal",g.name+(g.ownGoal?" · contra":""),g.minute+"' · "+(g.team==0?m.a:m.b)+(g.assistId>0?"\nAssistência: "+g.assistName:""));if(edit){row.addView(iconButton("edit","Editar gol de "+g.name,()->new EventForm(a).goal(g)));row.addView(iconButton("delete","Excluir gol de "+g.name,()->a.confirm("Excluir este gol?","O placar e as estatísticas provisórias serão recalculados.",()->a.safely(()->{db.deleteGoal(g.id,m.id);a.highlightScore=true;a.refresh();}))));}events.addView(row);Ui.divider(events);}add(events);
            if(edit&&roster)button("Adicionar gol",false,()->new EventForm(a).goal(null));
        }else if(a.matchTab==1){LinearLayout events=card();if(db.cards(m.id).isEmpty())events.addView(text("Nenhum cartão registrado.",13,Ui.MUTED,false));for(DemoDb.Card c:db.cards(m.id)){LinearLayout row=eventRow("shield",c.name,c.minute+"' · "+(c.color.equals("YELLOW")?"Amarelo":"Vermelho")+" · "+(c.team==0?m.a:m.b));if(edit){row.addView(iconButton("edit","Editar cartão de "+c.name,()->new EventForm(a).card(c)));row.addView(iconButton("delete","Excluir cartão de "+c.name,()->a.confirm("Excluir este cartão?","O cartão será removido da súmula em edição.",()->{db.deleteCard(c.id,m.id);a.refresh();})));}events.addView(row);Ui.divider(events);}add(events);if(edit&&roster)button("Registrar cartão",false,()->new EventForm(a).card(null));
        }else if(a.matchTab==2){
            LinearLayout subs=card();
            subs.addView(text("Trocas de jogadores",16,Ui.WHITE,true));
            Ui.put(subs,text("Somente jogadores confirmados podem entrar. O histórico da partida preserva quem participou.",12,Ui.MUTED,false),7);
            List<DemoDb.Substitution> history=db.substitutions(m.id);
            if(history.isEmpty())Ui.put(subs,text("Nenhuma substituição registrada.",13,Ui.MUTED,false),12);
            for(DemoDb.Substitution sub:history){
                Ui.put(subs,text(sub.minute+"' · "+(sub.team==0?m.a:m.b),12,Ui.GOLD,true),13);
                Ui.put(subs,text("Sai: "+sub.outName+"  →  Entra: "+sub.inName,12,Ui.WHITE,false),5);
                Ui.divider(subs);
            }
            add(subs);
            if(edit){
                button("Registrar substituição",true,()->new SubstitutionForm(a).open());
                if(!history.isEmpty())button("Desfazer a última substituição",false,()->a.confirm(
                    "Desfazer a última substituição?",
                    "O substituto voltará ao banco e o jogador anterior retornará ao campo. Não será possível caso o substituto tenha participado de outros lances.",
                    ()->a.safely(()->{db.undoLastSubstitution(m.id);a.refresh();})));
            }
        }else{add(summary(m));gap(12);LinearLayout rosters=card();for(int side=0;side<2;side++){rosters.addView(text(side==0?m.a:m.b,14,Ui.GOLD,true));for(DemoDb.Player p:db.roster(m.id,side))Ui.put(rosters,text(p.name+" · "+p.pos,12,Ui.WHITE,false),5);Ui.space(rosters,10);}add(rosters);}
        if(edit&&roster)button("Avançar para aprovação",true,()->a.confirm("Enviar a súmula?","Após o envio, os lances ficam bloqueados. Para corrigir, será necessário reabrir a súmula e gerar novos códigos.",()->a.safely(()->{a.matchClock.pause(m.id);db.submit(m.id);a.open("approve");})));
        else if(m.status.equals("PENDING"))button("Aprovar súmula",true,()->a.open("approve"));
        else if(m.status.equals("DEMO_APPROVED"))button("Compartilhar resultado",false,()->share("PELADA LEAGUE\n"+m.a+" "+db.goals(m.id,0)+" × "+db.goals(m.id,1)+" "+m.b+"\n"+db.group(m.groupId)[0]+"\nResultado validado na demonstração local."));
        if(!roster&&m.status.equals("DRAFT")){sub("Partida antiga sem escalação: preservada para consulta. Para registrar lances, crie uma partida pelo sorteador.");button("Criar partida com escalação",true,()->a.open("draw"));}
    }
    private void clockPanel(DemoDb.Game game){
        LocalMatchClock clock=a.matchClock;
        if(!"DRAFT".equals(game.status)&&clock.running(game.id))clock.pause(game.id);
        LinearLayout box=card();
        LinearLayout header=Ui.row(a);
        header.addView(new Ui.Icon(a,"timer",Ui.GOLD),Ui.lp(a,24,24));
        TextView title=text("CRONÔMETRO DA PARTIDA",11,Ui.MUTED,true);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(0,-2,1);
        titleParams.leftMargin=Ui.dp(a,8);header.addView(title,titleParams);
        TextView timer=text(LocalMatchClock.display(clock.elapsed(game.id)),24,Ui.GOLD,true);
        timer.setGravity(Gravity.RIGHT);header.addView(timer);
        box.addView(header);
        if("DRAFT".equals(game.status)&&a.canRecord()){
            LinearLayout controls=Ui.row(a);
            boolean running=clock.running(game.id);
            TextView toggle=Ui.chip(a,running?"Pausar":"Iniciar / continuar",running,()->{
                if(clock.running(game.id))clock.pause(game.id);else clock.start(game.id);
                a.refresh();
            });
            controls.addView(toggle,new LinearLayout.LayoutParams(0,-2,1));
            TextView reset=Ui.chip(a,"Zerar",false,()->a.confirm("Zerar cronômetro?","O tempo registrado nesta partida será reiniciado.",()->{
                clock.reset(game.id);a.refresh();
            }));
            LinearLayout.LayoutParams resetParams=Ui.lp(a,82,-2);
            resetParams.leftMargin=Ui.dp(a,8);controls.addView(reset,resetParams);
            Ui.put(box,controls,9);
        }else{
            Ui.put(box,text("Tempo registrado localmente. Apenas o responsável pode controlá-lo durante a edição da súmula.",10,Ui.MUTED,false),6);
        }
        add(box);
        if(clock.running(game.id)){
            timer.post(new Runnable(){
                @Override public void run(){
                    if(!timer.isAttachedToWindow()||a.isDestroyed()||!a.route.equals("match")||a.gameId!=game.id)return;
                    timer.setText(LocalMatchClock.display(clock.elapsed(game.id)));
                    if(clock.running(game.id))timer.postDelayed(this,1000L);
                }
            });
        }
    }
    private LinearLayout eventRow(String icon,String title,String detail){LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,56));row.addView(new Ui.Icon(a,icon,Ui.GOLD),Ui.lp(a,24,24));LinearLayout t=Ui.col(a);t.setPadding(Ui.dp(a,8),Ui.dp(a,5),0,Ui.dp(a,5));t.addView(text(title,13,Ui.WHITE,true));Ui.put(t,text(detail,11,Ui.MUTED,false),5);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));return row;}
    LinearLayout summary(DemoDb.Game m){LinearLayout s=card();s.addView(text("Resumo da partida",16,Ui.WHITE,true));Ui.space(s,8);line(s,"goal","Gols",db.goals(m.id,0)+" × "+db.goals(m.id,1));line(s,"shield","Cartões amarelos",db.cardCount(m.id,0,"YELLOW")+" × "+db.cardCount(m.id,1,"YELLOW"));line(s,"shield","Cartões vermelhos",db.cardCount(m.id,0,"RED")+" × "+db.cardCount(m.id,1,"RED"));int assist=0,own=0;for(DemoDb.Goal g:db.goals(m.id)){if(g.assistId>0)assist++;if(g.ownGoal)own++;}line(s,"stats","Assistências",""+assist);line(s,"shuffle","Substituições",""+db.substitutions(m.id).size());if(own>0)line(s,"goal","Gols contra",""+own);return s;}
}
