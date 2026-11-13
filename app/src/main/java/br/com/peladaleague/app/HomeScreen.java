package br.com.peladaleague.app;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

final class HomeScreen extends Screen {
    HomeScreen(MainActivity a){super(a);}
    void render(){
        LinearLayout brand=Ui.row(a);brand.addView(new SportArt(a,"crest",Ui.GOLD),Ui.lp(a,43,48));LinearLayout words=Ui.col(a);words.setPadding(Ui.dp(a,8),0,0,0);
        for(String word:new String[]{"PELADA","LEAGUE"}){TextView t=text(word,16,word.equals("PELADA")?Ui.WHITE:Ui.GOLD,true);t.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));words.addView(t);}brand.addView(words,new LinearLayout.LayoutParams(0,-2,1));brand.addView(iconButton("settings","Informações do aplicativo",a::info));add(brand);gap(16);
        add(text("Olá, "+a.me().name.split(" ")[0]+"!",25,Ui.WHITE,true));sub("Pronto para entrar em campo?");gap(14);
        String[] g=db.group(a.groupId);String status=db.attendance(a.groupId).getOrDefault(a.playerId,DemoDb.PENDING);
        LinearLayout content=Ui.col(a),head=Ui.row(a);head.addView(text("Sua próxima partida",13,Ui.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));head.addView(Ui.badge(a,a.status(status),status.equals(DemoDb.CONFIRMED)));content.addView(head);
        Ui.put(content,text(g[0],18,Ui.WHITE,true),16);Ui.space(content,6);line(content,"calendar",g[2],null);line(content,"location",g[1],null);
        Ui.put(content,text(db.confirmedCount(a.groupId)+" confirmados · "+Math.max(0,db.capacity(a.groupId)-db.confirmedCount(a.groupId))+" vagas",11,Ui.GOLD,true),4);
        FrameLayout hero=stadium(content,190,false);add(hero);Ui.tappable(hero,()->a.open("group"));gap(12);
        pair(page,quick("users","Minhas peladas","Grupos e partidas",()->a.tab("groups")),quick("trophy","Campeonatos","Ligas e tabelas",()->a.tab("league")));gap(10);
        pair(page,quick("shuffle","Sortear times","Equipes equilibradas",()->a.open("draw")),quick("stats","Minha carreira","Gols, jogos e mais",()->a.tab("profile")));
        gap(18);LinearLayout recent=Ui.row(a);recent.addView(text("Último resultado",14,Ui.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));TextView all=text("Ver jogos",12,Ui.GOLD,true);all.setMinHeight(Ui.dp(a,48));Ui.tappable(all,()->{a.groupTab=1;a.open("group");});recent.addView(all);add(recent);
        for(DemoDb.Game m:db.games(a.groupId))if(m.status.equals("DEMO_APPROVED")){LinearLayout c=card();gameRow(c,m);add(c);break;}
    }
    private LinearLayout quick(String icon,String name,String hint,Runnable action){LinearLayout c=card();c.setGravity(Gravity.CENTER);c.setMinimumHeight(Ui.dp(a,102));c.addView(new Ui.Icon(a,icon,Ui.GOLD),Ui.lp(a,33,33));Ui.space(c,7);TextView t=text(name,14,Ui.WHITE,true);t.setGravity(Gravity.CENTER);c.addView(t);TextView sub=text(hint,11,Ui.MUTED,false);sub.setGravity(Gravity.CENTER);Ui.put(c,sub,4);Ui.tappable(c,action);return c;}
}
