package br.com.peladaleague.app;
import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import br.com.peladaleague.core.TeamBalancer;
import java.util.*;

final class DrawScreen extends Screen {
    DrawScreen(MainActivity a){super(a);}
    void render(){title("Sortear Times");List<DemoDb.Player> eligible=db.confirmedPlayers(a.groupId);String fingerprint=db.attendanceFingerprint(a.groupId);
        if(!fingerprint.equals(a.selectionFingerprint)){a.selected.clear();for(DemoDb.Player p:eligible)a.selected.add(p.id);a.selectionFingerprint=fingerprint;}
        tabs(new String[]{"Participantes","Opções"},a.drawTab,i->{a.drawTab=i;a.refresh();});
        if(a.drawTab==0){
            LinearLayout header=Ui.row(a);header.addView(text("Confirmados ("+eligible.size()+")",14,Ui.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));TextView all=Ui.chip(a,"Todos",a.selected.size()==eligible.size(),()->{if(a.selected.size()==eligible.size())a.selected.clear();else for(DemoDb.Player p:eligible)a.selected.add(p.id);a.refresh();});header.addView(all);add(header);gap(5);
            if(eligible.isEmpty()){empty("Primeiro, confirme as presenças","Só entram no sorteio os jogadores confirmados nesta sessão.","Ver presenças",()->a.open("attendance"));return;}
            LinearLayout list=card();List<DemoDb.Player> visible=eligible.subList(0,Math.min(6,eligible.size()));for(DemoDb.Player p:visible)playerRow(list,p);add(list);
            if(eligible.size()>6)button("Selecionar todos os "+eligible.size()+" jogadores",false,()->selectionDialog(eligible));
            sub(a.selected.size()+" selecionados · "+a.teamCount+" times · "+(a.balanced?"Equilibrado":"Aleatório"));
        }else{
            heading("Quantidade de times");LinearLayout qty=Ui.row(a);for(int i=2;i<=4;i++){final int n=i;TextView item=Ui.chip(a,i+" times",a.teamCount==i,()->{a.teamCount=n;db.clearDraw(a.groupId);a.refresh();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(Ui.dp(a,3),0,Ui.dp(a,3),0);qty.addView(item,p);}add(qty);gap(20);heading("Como distribuir os jogadores?");
            pair(page,Ui.button(a,"Equilibrado",a.balanced,()->{a.balanced=true;db.clearDraw(a.groupId);a.refresh();}),Ui.button(a,"Aleatório",!a.balanced,()->{a.balanced=false;db.clearDraw(a.groupId);a.refresh();}));sub("No equilibrado, habilidade, goleiros e posições entram no cálculo. O número de jogadores por time varia no máximo em um.");gap(10);
        }
        if(a.selected.size()<a.teamCount*2){sub("São necessários pelo menos "+(a.teamCount*2)+" confirmados para formar "+a.teamCount+" times.");button("Voltar às presenças",false,()->a.open("attendance"));}
        TextView draw=Ui.button(a,a.drawBusy?"Distribuindo jogadores…":"Sortear Times",true,this::perform);draw.setEnabled(!a.drawBusy&&a.selected.size()>=a.teamCount*2&&a.canManage());draw.setAlpha(draw.isEnabled()?1:.5f);Ui.put(page,draw,12);
        if(!a.canManage())sub("O organizador pode salvar os sorteios oficiais da sessão.");
        List<List<DemoDb.Player>> saved=db.savedDraw(a.groupId);if(saved.isEmpty())return;gap(16);heading("Times sorteados");LinearLayout result=Ui.col(a);
        for(int i=0;i<saved.size();i+=2){LinearLayout left=team(i,saved.get(i));View right=i+1<saved.size()?team(i+1,saved.get(i+1)):new View(a);pair(result,left,right);Ui.space(result,10);}add(result);
        sub("Escalações salvas neste aparelho. Alterar a presença invalida o sorteio.");
        if(a.canManage())button("Criar partida com os times",true,()->matchup(saved));
        if(a.revealTeams){a.revealTeams=false;Motion.reveal(result,0);result.post(()->{if(a.scroll!=null)a.scroll.smoothScrollTo(0,result.getTop()-Ui.dp(a,28));});}
    }
    private void playerRow(LinearLayout list,DemoDb.Player p){LinearLayout r=Ui.row(a);r.setMinimumHeight(Ui.dp(a,45));r.addView(Ui.initials(a,p.name,28),Ui.lp(a,28,28));TextView t=text(p.name+" · "+p.pos,13,Ui.WHITE,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=Ui.dp(a,9);r.addView(t,lp);CheckBox check=new CheckBox(a);check.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GOLD));check.setChecked(a.selected.contains(p.id));check.setContentDescription("Selecionar "+p.name);check.setOnCheckedChangeListener((b,on)->{if(on)a.selected.add(p.id);else a.selected.remove(p.id);});r.addView(check,Ui.lp(a,48,45));Ui.tappable(r,()->{if(a.selected.contains(p.id))a.selected.remove(p.id);else a.selected.add(p.id);a.refresh();});check.setOnCheckedChangeListener((b,on)->{if(on)a.selected.add(p.id);else a.selected.remove(p.id);a.refresh();});list.addView(r);Ui.divider(list);}
    private void selectionDialog(List<DemoDb.Player> people){String[] names=people.stream().map(p->p.name+" · "+p.pos).toArray(String[]::new);boolean[] checked=new boolean[people.size()];for(int i=0;i<people.size();i++)checked[i]=a.selected.contains(people.get(i).id);new AlertDialog.Builder(a).setTitle("Participantes confirmados").setMultiChoiceItems(names,checked,(d,i,on)->{if(on)a.selected.add(people.get(i).id);else a.selected.remove(people.get(i).id);}).setPositiveButton("Concluir",(d,w)->a.refresh()).show();}
    private LinearLayout team(int index,List<DemoDb.Player> people){LinearLayout box=card();LinearLayout h=Ui.row(a);h.addView(new SportArt(a,"shirt",a.teamColor(a.teamName(index))),Ui.lp(a,30,34));TextView name=text(a.teamName(index),12,Ui.WHITE,true);h.addView(name,new LinearLayout.LayoutParams(0,-2,1));box.addView(h);Ui.space(box,8);int sum=0;for(DemoDb.Player p:people){LinearLayout r=Ui.row(a);r.addView(text(p.name,12,Ui.WHITE,false),new LinearLayout.LayoutParams(0,-2,1));r.addView(text(p.pos,10,Ui.MUTED,false));Ui.put(box,r,6);sum+=p.skill;}Ui.space(box,9);box.addView(text("FORÇA "+sum/people.size(),10,Ui.GOLD,true));return box;}
    private void perform(){if(a.drawBusy||!a.canManage())return;long group=a.groupId;String fingerprint=db.attendanceFingerprint(group);List<TeamBalancer.Player> players=new ArrayList<>();Map<Integer,DemoDb.Player> source=new HashMap<>();for(DemoDb.Player p:db.confirmedPlayers(group))if(a.selected.contains(p.id)){players.add(new TeamBalancer.Player((int)p.id,p.name,p.pos,p.skill));source.put((int)p.id,p);}int count=a.teamCount;boolean balanced=a.balanced;
        if(players.size()<count*2){a.toast("Confirme e selecione pelo menos "+count*2+" jogadores.");return;}a.drawBusy=true;a.refresh();
        a.worker.execute(()->{try{TeamBalancer.MultiDraw draw=TeamBalancer.drawTeams(players,count,balanced,new Random());List<List<DemoDb.Player>> squads=new ArrayList<>();for(List<TeamBalancer.Player> team:draw.teams()){List<DemoDb.Player> result=new ArrayList<>();for(TeamBalancer.Player p:team)result.add(source.get(p.id()));squads.add(result);}a.runOnUiThread(()->{if(a.isDestroyed())return;a.drawBusy=false;a.safely(()->{if(!fingerprint.equals(db.attendanceFingerprint(group))){a.toast("A presença mudou. Faça um novo sorteio.");return;}db.saveDraw(group,squads);if(a.route.equals("draw")&&a.groupId==group){a.revealTeams=true;a.refresh();}});});}catch(RuntimeException e){a.runOnUiThread(()->{if(a.isDestroyed())return;a.drawBusy=false;a.toast("Não foi possível sortear. Confira os participantes.");a.refresh();});}});
    }
    private void matchup(List<List<DemoDb.Player>> saved){List<int[]> pairs=new ArrayList<>();List<String> labels=new ArrayList<>();for(int i=0;i<saved.size();i++)for(int j=i+1;j<saved.size();j++){pairs.add(new int[]{i,j});labels.add(a.teamName(i)+" × "+a.teamName(j));}if(pairs.size()==1){create(saved,0,1);return;}new AlertDialog.Builder(a).setTitle("Escolha o confronto").setItems(labels.toArray(new String[0]),(d,i)->create(saved,pairs.get(i)[0],pairs.get(i)[1])).show();}
    private void create(List<List<DemoDb.Player>> teams,int x,int y){a.safely(()->{if(db.savedDraw(a.groupId).isEmpty())throw new IllegalStateException("A presença mudou. Sorteie novamente.");a.gameId=db.addGameWithLineup(a.groupId,a.teamName(x),a.teamName(y),teams.get(x),teams.get(y));a.matchTab=0;a.open("match");});}
}
