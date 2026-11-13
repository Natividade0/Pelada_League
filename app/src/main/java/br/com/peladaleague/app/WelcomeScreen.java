package br.com.peladaleague.app;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;

final class WelcomeScreen extends Screen {
    WelcomeScreen(MainActivity a){super(a);}
    void render(LinearLayout shell){
        FrameLayout frame=new FrameLayout(a);shell.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        ImageView bg=Ui.img(a,R.drawable.stadium_night);bg.setScaleType(ImageView.ScaleType.CENTER_CROP);frame.addView(bg,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout body=Ui.col(a);body.setGravity(Gravity.CENTER_HORIZONTAL);body.setPadding(Ui.dp(a,28),Ui.dp(a,32),Ui.dp(a,28),Ui.dp(a,26));scroll.addView(body);
        Ui.space(body,32);SportArt crest=new SportArt(a,"crest",Ui.GOLD);body.addView(crest,Ui.lp(a,142,156));Motion.intro(crest,0);Ui.space(body,18);
        for(String word:new String[]{"PELADA","LEAGUE"}){TextView t=text(word,39,word.equals("PELADA")?Ui.WHITE:Ui.GOLD,true);t.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));t.setGravity(Gravity.CENTER);body.addView(t);Motion.intro(t,80);}
        Ui.space(body,16);TextView slogan=text("Organize, jogue, compita.\nFaça história com sua pelada.",15,Ui.WHITE,false);slogan.setGravity(Gravity.CENTER);body.addView(slogan);Motion.intro(slogan,130);
        android.view.View spacer=new android.view.View(a);body.addView(spacer,new LinearLayout.LayoutParams(1,Ui.dp(a,56),1));
        TextView explore=Ui.button(a,"Explorar demonstração",true,()->{a.getPreferences(0).edit().putBoolean("entered",true).apply();a.tab("home");});body.addView(explore,Ui.lp(a,-1,-2));Motion.intro(explore,180);
        Ui.put(body,Ui.button(a,"Entrar ou criar conta",false,()->a.open("account")),10);
        Ui.put(body,Ui.button(a,"Sobre o Pelada League",false,a::info),10);
        Ui.space(body,14);TextView note=text("Seus jogos, no seu ritmo.",12,Ui.WHITE,false);note.setGravity(Gravity.CENTER);body.addView(note);
    }
}
