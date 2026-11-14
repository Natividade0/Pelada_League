package br.com.peladaleague.app;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/**
 * Match-centred OLED dashboard. One primary action per area; no buried core flows.
 * Uses exactly the same SQLite data and navigation as the prior version.
 */
final class HomeScreen extends Screen {
    HomeScreen(MainActivity a){super(a);}
    void render(){
        header();gap(22);
        add(text("BOM JOGO COMEÇA AQUI",11,Ui.GOLD,true));
        gap(7);
        add(text("Olá, "+a.me().name.split(" ")[0]+"!",27,Ui.WHITE,true));
        sub("Sua pelada, seus times, sua história.");
        gap(19);
        nextMatch();
        if(db.confirmedCount(a.groupId)<4){
            gap(12);
            LinearLayout prepare=Ui.surface(a);
            prepare.addView(text("Falta pouco para começar",15,Ui.WHITE,true));
            Ui.put(prepare,text("Confirme pelo menos quatro jogadores antes de sortear os times.",12,Ui.MUTED,false),7);
            Ui.put(prepare,Ui.button(a,"Preparar a partida",true,()->a.open("attendance")),13);
            add(prepare);
        }
        gap(24);
        LinearLayout quickHeader=Ui.row(a);
        quickHeader.addView(text("Acesso rápido",18,Ui.WHITE,true),
            new LinearLayout.LayoutParams(0,-2,1));
        quickHeader.addView(Ui.badge(a,"4 atalhos",false));
        add(quickHeader);gap(12);
        pair(page,
            a.mark("home_groups",quick("users","Minhas peladas","Grupos e presenças",()->a.tab("groups"))),
            quick("trophy","Campeonatos","Tabela e prêmios",()->a.tab("league")));
        gap(11);
        pair(page,
            quick("shuffle","Sortear times","Equipes equilibradas",()->a.open("draw")),
            quick("stats","Minha carreira","Gols e partidas",()->a.tab("profile")));
        gap(19);
        LinearLayout online=Ui.surface(a);
        LinearLayout row=Ui.row(a);
        LinearLayout copy=Ui.col(a);
        copy.addView(text("JOGUE COM SUA GALERA",10,Ui.GOLD,true));
        Ui.put(copy,text("Peladas online e convites",15,Ui.WHITE,true),6);
        Ui.put(copy,text("Crie eventos, compartilhe e confirme presença.",12,Ui.MUTED,false),5);
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(new Ui.Icon(a,"chevron",Ui.GOLD),Ui.lp(a,24,24));
        online.addView(row);Ui.tappable(online,()->a.open("online"));
        add(online);
        gap(23);
        LinearLayout recent=Ui.row(a);
        recent.addView(text("Último resultado",18,Ui.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView all=text("Ver jogos",12,Ui.GOLD,true);
        all.setGravity(Gravity.CENTER);all.setMinHeight(Ui.dp(a,48));
        all.setPadding(Ui.dp(a,8),0,0,0);
        Ui.tappable(all,()->{a.groupTab=1;a.open("group");});
        recent.addView(all);add(recent);gap(10);
        boolean shown=false;
        for(DemoDb.Game game:db.games(a.groupId))if(game.status.equals("DEMO_APPROVED")){
            LinearLayout result=card();gameRow(result,game);add(result);shown=true;break;
        }
        if(!shown){
            LinearLayout empty=card();
            empty.addView(text("A história começa no primeiro jogo.",13,Ui.MUTED,false));
            add(empty);
        }
        gap(16);
    }
    private void header(){
        LinearLayout brand=Ui.row(a);
        SportArt crest=new SportArt(a,"crest",Ui.GOLD);
        brand.addView(crest,Ui.lp(a,42,48));Motion.breathe(crest);
        LinearLayout words=Ui.col(a);words.setPadding(Ui.dp(a,11),0,0,0);
        TextView top=text("PELADA",16,Ui.WHITE,true),bottom=text("LEAGUE",16,Ui.GOLD,true);
        top.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));
        bottom.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));
        words.addView(top);words.addView(bottom);
        brand.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        brand.addView(iconButton("profile","Minha conta por e-mail",()->a.open("account")));
        brand.addView(iconButton("settings","Informações do aplicativo",a::info));
        add(brand);
    }
    private void nextMatch(){
        String[] g=db.group(a.groupId);
        String status=db.attendance(a.groupId).getOrDefault(a.playerId,DemoDb.PENDING);
        LinearLayout hero=Ui.col(a);
        LinearLayout head=Ui.row(a);
        head.addView(text("Sua próxima partida",13,Ui.WHITE,true),
            new LinearLayout.LayoutParams(0,-2,1));
        head.addView(Ui.badge(a,a.status(status),DemoDb.CONFIRMED.equals(status)));
        hero.addView(head);
        Ui.put(hero,text(g[0],23,Ui.WHITE,true),17);
        Ui.space(hero,13);
        line(hero,"calendar",g[2],null);
        Ui.space(hero,5);
        line(hero,"location",g[1],null);
        Ui.space(hero,12);
        int count=db.confirmedCount(a.groupId),vacancies=Math.max(0,db.capacity(a.groupId)-count);
        hero.addView(text(count+" confirmados  ·  "+vacancies+" vagas disponíveis",12,Ui.GOLD,true));
        Ui.space(hero,12);
        TextView action=text("ABRIR PELADA  ›",12,Ui.GOLD,true);
        hero.addView(action);
        FrameLayout banner=stadium(hero,204,true);
        banner.setContentDescription("Sua próxima partida. "+g[0]+". "+count+" confirmados. Abrir pelada");
        Ui.tappable(banner,()->a.open("group"));
        add(a.mark("home_match",banner));
    }
    private LinearLayout quick(String icon,String title,String detail,Runnable action){
        LinearLayout card=Ui.card(a);
        card.setMinimumHeight(Ui.dp(a,127));
        android.widget.FrameLayout symbol=new FrameLayout(a);
        symbol.setBackground(Ui.round(a,Ui.GOLD_TINT,13,Ui.GOLD_STROKE));
        Ui.Icon glyph=new Ui.Icon(a,icon,Ui.GOLD);
        symbol.addView(glyph,new android.widget.FrameLayout.LayoutParams(
            Ui.dp(a,25),Ui.dp(a,25),Gravity.CENTER));
        card.addView(symbol,Ui.lp(a,43,43));
        Ui.put(card,text(title,14,Ui.WHITE,true),12);
        Ui.put(card,text(detail,11,Ui.MUTED,false),5);
        card.setContentDescription(title+". "+detail);
        Ui.tappable(card,action);
        return card;
    }
}
