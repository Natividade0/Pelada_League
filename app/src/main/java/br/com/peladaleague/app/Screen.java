package br.com.peladaleague.app;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shared native composition primitives; content never has fixed text heights. */
abstract class Screen {
    final MainActivity a;final DemoDb db;final LinearLayout page;
    Screen(MainActivity host){a=host;db=a.db;page=a.page;}
    void gap(int dp){Ui.space(page,dp);}
    TextView text(String s,int size,int color,boolean bold){return Ui.t(a,s,size,color,bold);}
    void title(String name){LinearLayout row=Ui.row(a);row.addView(iconButton("back","Voltar",a::back));TextView t=text(name,19,Ui.WHITE,true);t.setGravity(Gravity.CENTER);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));row.addView(iconButton("settings","Informações do aplicativo",a::info));page.addView(row);gap(10);}
    View iconButton(String icon,String description,Runnable action){android.widget.FrameLayout box=new FrameLayout(a);box.setContentDescription(description);box.setMinimumWidth(Ui.dp(a,48));box.setMinimumHeight(Ui.dp(a,48));Ui.Icon i=new Ui.Icon(a,icon,Ui.WHITE);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(Ui.dp(a,23),Ui.dp(a,23),Gravity.CENTER);box.addView(i,p);Ui.tappable(box,action);return box;}
    void heading(String s){Ui.heading(page,s);}
    void sub(String s){Ui.put(page,text(s,12,Ui.MUTED,false),6);}
    void button(String label,boolean primary,Runnable action){Ui.put(page,Ui.button(a,label,primary,action),10);}
    void tabs(String[] names,int selected,java.util.function.IntConsumer onSelect){LinearLayout bar=Ui.row(a);bar.setPadding(Ui.dp(a,2),0,Ui.dp(a,2),0);bar.setBackground(Ui.round(a,0xFF242629,24,0));for(int i=0;i<names.length;i++){final int index=i;TextView tab=Ui.chip(a,names[i],i==selected,()->onSelect.accept(index));tab.setTextSize(names.length==4?11:12);if(i!=selected)tab.setBackgroundColor(Color.TRANSPARENT);tab.setContentDescription(names[i]+(i==selected?", selecionado":""));bar.addView(tab,new LinearLayout.LayoutParams(0,-2,1));}page.addView(bar);gap(12);}
    LinearLayout card(){return Ui.card(a);}
    void add(View v){Ui.put(page,v,0);}
    void pair(LinearLayout parent,View left,View right){LinearLayout row=Ui.row(a);row.setGravity(Gravity.TOP);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=Ui.dp(a,5);row.addView(left,p);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,-2,1);q.leftMargin=Ui.dp(a,5);row.addView(right,q);parent.addView(row);}
    void line(LinearLayout parent,String icon,String label,String value){LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,30));row.addView(new Ui.Icon(a,icon,Ui.GOLD),Ui.lp(a,20,20));TextView l=text(label,13,Ui.WHITE,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(a,7);row.addView(l,p);if(value!=null)row.addView(text(value,13,Ui.WHITE,true));parent.addView(row);}
    void empty(String title,String message,String action,Runnable callback){LinearLayout c=card();c.addView(text(title,16,Ui.WHITE,true));Ui.put(c,text(message,13,Ui.MUTED,false),8);if(action!=null)Ui.put(c,Ui.button(a,action,true,callback),12);add(c);}
    /** Keep stadium photos from expanding wrap-content banners to the bitmap's intrinsic height. */
    FrameLayout stadium(LinearLayout content,int minimum,boolean dark){
        FrameLayout frame=new FrameLayout(a){
            @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
                int width=View.MeasureSpec.getSize(widthMeasureSpec);
                int contentWidth=Math.max(1,width-getPaddingLeft()-getPaddingRight());
                content.measure(View.MeasureSpec.makeMeasureSpec(contentWidth,View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                int height=Math.max(Ui.dp(a,minimum),content.getMeasuredHeight()+Ui.dp(a,12));
                int mode=View.MeasureSpec.getMode(heightMeasureSpec);
                if(mode==View.MeasureSpec.EXACTLY)height=View.MeasureSpec.getSize(heightMeasureSpec);
                else if(mode==View.MeasureSpec.AT_MOST)height=Math.min(height,View.MeasureSpec.getSize(heightMeasureSpec));
                super.onMeasure(widthMeasureSpec,View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            }
        };
        frame.setMinimumHeight(Ui.dp(a,minimum));
        frame.setBackground(Ui.round(a,Ui.SURFACE,15,Ui.BORDER));
        frame.setClipToOutline(true);
        ImageView image=Ui.img(a,R.drawable.stadium_night);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
        View dim=new View(a);
        dim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{dark?0xF207090B:0xDF07090B,0x9907090B,0x2207090B}));
        frame.addView(dim,new FrameLayout.LayoutParams(-1,-1));
        content.setPadding(Ui.dp(a,14),Ui.dp(a,13),Ui.dp(a,14),Ui.dp(a,13));
        frame.addView(content,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL));
        return frame;
    }
    LinearLayout score(DemoDb.Game m){LinearLayout c=card(),row=Ui.row(a);for(int side=0;side<3;side++){if(side==1){TextView t=text(db.goals(m.id,0)+" × "+db.goals(m.id,1),32,Ui.WHITE,true);t.setGravity(Gravity.CENTER);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(a.highlightScore)Motion.score(t);}else{String name=side==0?m.a:m.b;LinearLayout team=Ui.col(a);team.setGravity(Gravity.CENTER);team.addView(new SportArt(a,"shirt",a.teamColor(name)),Ui.lp(a,46,48));TextView label=text(name,12,Ui.WHITE,true);label.setGravity(Gravity.CENTER);Ui.put(team,label,4);row.addView(team,new LinearLayout.LayoutParams(0,-2,1));}}c.addView(row);a.highlightScore=false;return c;}
    String gameStatus(DemoDb.Game m){return m.status.equals("DRAFT")?"Em edição":m.status.equals("PENDING")?"Aguardando aprovação":"Validada · local";}
    void gameRow(LinearLayout parent,DemoDb.Game m){LinearLayout r=Ui.row(a);r.setMinimumHeight(Ui.dp(a,62));r.addView(new SportArt(a,"shirt",a.teamColor(m.a)),Ui.lp(a,30,34));LinearLayout name=Ui.col(a);name.setPadding(Ui.dp(a,9),0,Ui.dp(a,6),0);name.addView(text(m.a+" × "+m.b,12,Ui.WHITE,true));Ui.put(name,text(gameStatus(m),11,Ui.MUTED,false),4);r.addView(name,new LinearLayout.LayoutParams(0,-2,1));r.addView(text(db.goals(m.id,0)+" : "+db.goals(m.id,1),18,Ui.GOLD,true));Ui.tappable(r,()->a.openGame(m));parent.addView(r);}
    void share(String content){android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(android.content.Intent.EXTRA_TEXT,content);a.startActivity(android.content.Intent.createChooser(i,"Compartilhar"));}
}
