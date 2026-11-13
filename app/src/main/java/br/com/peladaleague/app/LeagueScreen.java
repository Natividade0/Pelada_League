package br.com.peladaleague.app;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import br.com.peladaleague.core.LeagueRules;
import java.util.*;

/** Season-aware local league, lifetime-safe historical awards and honest sample standings. */
final class LeagueScreen extends Screen {
    LeagueScreen(MainActivity a){super(a);}
    private DemoDb.Season chosenSeason(){
        List<DemoDb.Season> items=db.seasons(a.groupId);
        for(DemoDb.Season s:items)if(s.id==a.leagueSeasonId)return s;
        return db.activeSeason(a.groupId);
    }
    void render(){
        LinearLayout hero=Ui.col(a);
        LinearLayout header=Ui.row(a);
        header.addView(new Ui.Icon(a,"trophy",Ui.GOLD),Ui.lp(a,40,44));
        LinearLayout descriptions=Ui.col(a);descriptions.setPadding(Ui.dp(a,9),0,0,0);
        descriptions.addView(text("Campeonato da Pelada",20,Ui.WHITE,true));
        Ui.put(descriptions,text(db.group(a.groupId)[0],12,Ui.MUTED,false),5);
        header.addView(descriptions,new LinearLayout.LayoutParams(0,-2,1));
        hero.addView(header);
        add(a.mark("league_header",stadium(hero,96,true)));
        gap(12);
        if(db.groupCount()>1) {
            TextView choose=Ui.chip(a,"Trocar pelada",false,()->{
                List<String[]> groups=db.groups();
                new AlertDialog.Builder(a).setTitle("Classificação de qual pelada?")
                    .setItems(groups.stream().map(g->g[1]).toArray(String[]::new),(d,index)->{
                        a.groupId=Long.parseLong(groups.get(index)[0]);
                        a.leagueSeasonId=0;
                        a.refresh();
                    }).show();
            });
            add(choose);gap(9);
        }
        DemoDb.Season season=chosenSeason();
        TextView selector=Ui.chip(a,(season.active?"Temporada atual: ":"Temporada encerrada: ")+season.name+"  ▾",
            false,this::chooseSeason);
        add(selector);gap(10);
        tabs(new String[]{"Tabela","Artilharia","Jogos","Temporadas"},a.leagueTab,i->{
            a.leagueTab=i;a.refresh();
        });
        if(a.leagueTab==0){
            standings(season);gap(15);heading("Artilharia · Top 3");scorers(season,3);
        }else if(a.leagueTab==1)scorers(season,30);
        else if(a.leagueTab==2)games(season);
        else seasons();
        gap(10);
        sub("Premiações calculadas apenas com súmulas aprovadas localmente. Não são estatísticas oficiais de campeonatos online.");
    }
    private void chooseSeason(){
        List<DemoDb.Season> all=db.seasons(a.groupId);
        String[] names=new String[all.size()];
        for(int i=0;i<all.size();i++)names[i]=all.get(i).name+(all.get(i).active?" · atual":" · encerrada");
        new AlertDialog.Builder(a).setTitle("Escolha a temporada").setItems(names,(dialog,index)->{
            a.leagueSeasonId=all.get(index).id;a.refresh();
        }).setNegativeButton("Cancelar",null).show();
    }
    private void games(DemoDb.Season season){
        LinearLayout list=card();
        List<DemoDb.Game> games=db.seasonGames(a.groupId,season.id);
        if(games.isEmpty())Ui.put(list,text("Sem partidas nesta temporada.",13,Ui.MUTED,false),8);
        for(DemoDb.Game game:games){gameRow(list,game);Ui.divider(list);}
        add(list);
    }
    private void standings(DemoDb.Season season){
        Set<String> names=new LinkedHashSet<>();
        List<LeagueRules.Result> results=new ArrayList<>();
        for(DemoDb.Game game:db.seasonGames(a.groupId,season.id)){
            names.add(game.a);names.add(game.b);
            results.add(new LeagueRules.Result(game.a,game.b,db.goals(game.id,0),
                db.goals(game.id,1),"DEMO_APPROVED".equals(game.status)));
        }
        List<LeagueRules.Standing> rank=LeagueRules.standings(names,results);
        if(rank.isEmpty()){
            empty("A temporada começa aqui","Partidas e resultados validados formarão a classificação.","Abrir pelada",()->a.open("group"));
            return;
        }
        LinearLayout board=card();
        LinearLayout title=Ui.row(a);
        title.addView(text("#   Time",11,Ui.MUTED,true),new LinearLayout.LayoutParams(0,-2,1));
        for(String col:new String[]{"P","J","V","SG"})title.addView(cell(col,Ui.MUTED));
        board.addView(title);Ui.space(board,8);
        int number=0;
        for(LeagueRules.Standing standing:rank){
            LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,44));
            if(number==0)row.setBackground(Ui.round(a,0x224E3D10,7,0));
            TextView place=text(""+(++number),12,number==1?Ui.GOLD:Ui.MUTED,true);
            row.addView(place,Ui.lp(a,21,-2));
            row.addView(new SportArt(a,"shirt",a.teamColor(standing.name)),Ui.lp(a,21,25));
            TextView name=text(standing.name,12,Ui.WHITE,true);
            name.setPadding(Ui.dp(a,5),0,0,0);
            row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            for(int stat:new int[]{standing.points,standing.played,standing.wins,standing.goalDifference()})
                row.addView(cell(""+stat,Ui.WHITE));
            board.addView(row);Ui.divider(board);
        }
        add(board);sub("P pontos  J jogos  V vitórias  SG saldo de gols");
    }
    private TextView cell(String value,int color){
        TextView out=text(value,12,color,true);out.setGravity(Gravity.CENTER);
        out.setLayoutParams(Ui.lp(a,27,-2));return out;
    }
    private void scorers(DemoDb.Season season,int limit){
        LinearLayout list=card();int index=0;
        for(Map.Entry<String,Integer> entry:db.seasonScorers(a.groupId,season.id).entrySet()){
            if(index>=limit)break;
            LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,48));
            row.addView(text(""+(++index),13,index==1?Ui.GOLD:Ui.MUTED,true),Ui.lp(a,23,-2));
            row.addView(Ui.initials(a,entry.getKey(),29),Ui.lp(a,29,29));
            TextView name=text(entry.getKey(),13,Ui.WHITE,true);
            name.setPadding(Ui.dp(a,8),0,Ui.dp(a,4),0);
            row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(text(entry.getValue()+" gols",12,Ui.GOLD,true));
            list.addView(row);Ui.divider(list);
        }
        if(index==0){
            list.addView(text("Ainda não há artilheiros",15,Ui.WHITE,true));
            Ui.put(list,text("Os gols passam a valer depois da aprovação da súmula.",12,Ui.MUTED,false),8);
        }
        add(list);
    }
    private void seasons(){
        heading("Temporadas da pelada");
        for(DemoDb.Season season:db.seasons(a.groupId)){
            LinearLayout block=card();
            LinearLayout line=Ui.row(a);
            line.addView(new Ui.Icon(a,"trophy",Ui.GOLD),Ui.lp(a,25,29));
            LinearLayout desc=Ui.col(a);
            desc.setPadding(Ui.dp(a,9),0,0,0);
            desc.addView(text(season.name,16,Ui.WHITE,true));
            Ui.put(desc,text(season.active?"Em andamento":"Encerrada",11,season.active?Ui.GREEN:Ui.MUTED,false),4);
            line.addView(desc,new LinearLayout.LayoutParams(0,-2,1));
            block.addView(line);
            List<DemoDb.Award> awards=db.seasonAwards(a.groupId,season.id);
            Ui.put(block,text(db.seasonGames(a.groupId,season.id).size()+" partidas registradas",12,Ui.MUTED,false),10);
            for(DemoDb.Award award:awards)Ui.put(block,
                text("✦ "+award.category+": "+award.name+" ("+award.value+")",12,Ui.GOLD,false),7);
            if(awards.isEmpty())Ui.put(block,
                text("Sem premiações: ainda faltam partidas com súmula validada.",12,Ui.MUTED,false),8);
            if(!awards.isEmpty())Ui.put(block,Ui.button(a,"Compartilhar premiações",false,()->shareAwards(season,awards)),11);
            add(block);gap(12);
        }
        if(a.canManage())button("Iniciar nova temporada",true,this::createSeason);
        sub("A criação de uma nova temporada arquiva o período anterior sem apagar partidas, gols ou premiações.");
    }
    private void shareAwards(DemoDb.Season season,List<DemoDb.Award> awards){
        StringBuilder out=new StringBuilder("PELADA LEAGUE · "+db.group(a.groupId)[0]+"
TEMPORADA: "+season.name+"
");
        for(DemoDb.Award award:awards)out.append(award.category).append(": ")
            .append(award.name).append(" (").append(award.value).append(")
");
        out.append("Resultados aprovados localmente.");
        share(out.toString());
    }
    private void createSeason(){
        EditText name=Ui.input(a,"Ex.: Temporada 2027");
        new AlertDialog.Builder(a).setTitle("Iniciar nova temporada")
            .setMessage("As partidas existentes permanecerão vinculadas à temporada anterior. Os próximos jogos serão registrados na nova temporada.")
            .setView(name).setNegativeButton("Cancelar",null)
            .setPositiveButton("Criar",(d,w)->a.safely(()->{
                a.leagueSeasonId=db.beginSeason(a.groupId,name.getText().toString());
                a.leagueTab=3;a.refresh();
            })).show();
    }
}
