package br.com.peladaleague.app;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** First-launch entry, dark from edge to edge and clear for players of any age. */
final class WelcomeScreen extends Screen {
    WelcomeScreen(MainActivity a){super(a);}
    void render(LinearLayout shell){
        FrameLayout frame=new FrameLayout(a);
        frame.setBackground(new ArenaBackdrop());
        shell.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout body=Ui.col(a);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(Ui.dp(a,28),Ui.dp(a,30),Ui.dp(a,28),Ui.dp(a,24));
        scroll.addView(body);
        Ui.space(body,26);
        TextView tag=Ui.badge(a,"FUTEBOL ENTRE AMIGOS",false);
        body.addView(tag,Ui.lp(a,-2,-2));
        Ui.space(body,18);
        SportArt crest=new SportArt(a,"crest",Ui.GOLD);
        LinearLayout.LayoutParams emblem=Ui.lp(a,136,143);
        emblem.gravity=Gravity.CENTER_HORIZONTAL;
        body.addView(crest,emblem);
        Motion.intro(crest,0);
        Ui.space(body,16);
        for(String word:new String[]{"PELADA","LEAGUE"}){
            TextView name=text(word,39,word.equals("PELADA")?Ui.WHITE:Ui.GOLD,true);
            name.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));
            name.setGravity(Gravity.CENTER);
            body.addView(name);Motion.intro(name,80);
        }
        Ui.space(body,17);
        TextView intro=text("Sua pelada organizada.\nSeus jogos, sua história.",16,Ui.WHITE,false);
        intro.setGravity(Gravity.CENTER);body.addView(intro);
        Motion.intro(intro,130);
        Ui.space(body,9);
        TextView help=text("Crie o grupo · confirme presenças · sorteie os times",12,Ui.MUTED,false);
        help.setGravity(Gravity.CENTER);body.addView(help);
        View spacer=new View(a);
        body.addView(spacer,new LinearLayout.LayoutParams(1,Ui.dp(a,54),1));
        TextView explore=Ui.button(a,"Explorar demonstração",true,()->{
            a.getPreferences(0).edit().putBoolean("entered",true).apply();
            a.tab("home");
        });
        body.addView(explore,Ui.lp(a,-1,-2));
        Motion.intro(explore,180);
        Ui.put(body,Ui.button(a,"Entrar ou criar conta",false,()->a.open("account")),11);
        Ui.put(body,Ui.button(a,"Sobre o Pelada League",false,a::info),10);
        Ui.space(body,18);
        TextView note=text("Experimente grátis. Tutorial guiado na primeira abertura.",11,Ui.MUTED,false);
        note.setGravity(Gravity.CENTER);body.addView(note);
    }
}
