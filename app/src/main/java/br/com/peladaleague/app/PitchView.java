package br.com.peladaleague.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.*;

/**
 * Original AMOLED tactical line-up graphic inspired by match-day graphics.
 * Real confirmed players, their roles and team colors; no invented jersey numbers.
 * Lightweight native Canvas: not a 3D scene.
 */
final class PitchView extends View {
    private final List<DemoDb.Player> players;
    private final String team;
    private final int teamColor;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float reveal=1f;
    PitchView(Context context,String name,int color,List<DemoDb.Player> squad){
        super(context);team=name;teamColor=color;players=new ArrayList<>(squad);
        StringBuilder description=new StringBuilder("Escalação de ").append(name).append(": ");
        for(DemoDb.Player player:players)description.append(player.name).append(", ").append(player.pos).append("; ");
        setContentDescription(description.toString());
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    @Override protected void onMeasure(int wSpec,int hSpec){
        int width=MeasureSpec.getSize(wSpec);
        if(width<=0)width=Ui.dp(getContext(),320);
        int desired=Math.max(Ui.dp(getContext(),315),Math.round(width*1.20f));
        setMeasuredDimension(resolveSize(width,wSpec),resolveSize(desired,hSpec));
    }
    private void color(int color){
        p.setShader(null);p.clearShadowLayer();p.setStyle(Paint.Style.FILL);p.setColor(color);
    }
    private void line(int color,float width){
        color(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();if(w<1||h<1)return;
        float radius=Ui.dp(getContext(),20),top=h*.15f,bottom=h*.92f;
        color(Ui.SURFACE);c.drawRoundRect(0,0,w,h,radius,radius,p);
        // Matte black pitch with tasteful amber borders, as requested.
        color(0xFF060607);c.drawRoundRect(w*.035f,top,w*.965f,bottom,radius*.48f,radius*.48f,p);
        for(int i=0;i<8;i++){
            if(i%2==0){color(0x0CFFB52E);
                float y1=top+i*(bottom-top)/8,y2=top+(i+1)*(bottom-top)/8;
                c.drawRect(w*.039f,y1,w*.961f,y2,p);
            }
        }
        float left=w*.07f,right=w*.93f,fieldTop=top+h*.018f,fieldBottom=bottom-h*.024f;
        line(0x667E7770,Math.max(1.3f,w*.003f));
        c.drawRect(left,fieldTop,right,fieldBottom,p);
        c.drawLine(left,(fieldTop+fieldBottom)/2,right,(fieldTop+fieldBottom)/2,p);
        c.drawCircle(w*.5f,(fieldTop+fieldBottom)/2,w*.13f,p);
        c.drawRect(w*.31f,fieldTop,w*.69f,fieldTop+(fieldBottom-fieldTop)*.15f,p);
        c.drawRect(w*.40f,fieldTop,w*.60f,fieldTop+(fieldBottom-fieldTop)*.055f,p);
        c.drawRect(w*.31f,fieldBottom-(fieldBottom-fieldTop)*.15f,w*.69f,fieldBottom,p);
        c.drawRect(w*.40f,fieldBottom-(fieldBottom-fieldTop)*.055f,w*.60f,fieldBottom,p);
        line(Ui.GOLD,Math.max(2f,w*.007f));
        c.drawLine(w*.065f,fieldTop,w*.065f,fieldTop+h*.09f,p);
        // Header is intentionally inside the black card, not a bright stadium photograph.
        color(Ui.GOLD);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        p.setTextSize(w*.030f);p.setTextAlign(Paint.Align.LEFT);
        c.drawText("ESCALAÇÃO EM CAMPO",w*.055f,h*.055f,p);
        color(Ui.WHITE);p.setTextSize(w*.056f);
        c.drawText(team,w*.055f,h*.119f,p);
        color(Ui.GOLD);p.setTextSize(w*.028f);
        c.drawText(players.size()+" JOGADORES SORTEADOS",w*.055f,h*.975f,p);
        // Keep the goalkeeper downfield and the forwards upfield, as in match graphics.
        Map<Integer,List<DemoDb.Player>> bands=new TreeMap<>();
        for(DemoDb.Player player:players){
            int band="GOL".equals(player.pos)?0:"ZAG".equals(player.pos)?1:
                "MEI".equals(player.pos)?2:3;
            bands.computeIfAbsent(band,k->new ArrayList<>()).add(player);
        }
        int count=0;
        for(Map.Entry<Integer,List<DemoDb.Player>> e:bands.entrySet()){
            int band=e.getKey();List<DemoDb.Player> row=e.getValue();
            float y=fieldTop+(fieldBottom-fieldTop)*(.83f-band*.21f);
            float widthAvailable=(right-left)*.91f;
            for(int i=0;i<row.size();i++){
                float x=w*.5f+widthAvailable*((i+.5f)/row.size()-.5f);
                float alpha=Math.max(0,Math.min(1,(reveal-count*.085f)*1.6f));
                if(alpha>0)drawPlayer(c,row.get(i),x,y,w,alpha,row.size());
                count++;
            }
        }
        p.setTextAlign(Paint.Align.LEFT);p.setStyle(Paint.Style.FILL);
    }
    private void drawPlayer(Canvas c,DemoDb.Player player,float x,float y,float width,float opacity,int rowSize){
        int saved=c.saveLayerAlpha(x-55,y-50,x+55,y+72,(int)(opacity*255));
        float scale=Math.max(.72f,Math.min(1f,4f/rowSize));
        c.save();c.translate(x,y);c.scale(scale,scale);
        float jersey=width*.059f;
        color(0x99000000);p.setShadowLayer(8,0,4,0xBB000000);
        Path shirt=new Path();
        shirt.moveTo(-jersey*.56f,-jersey*.58f);
        shirt.lineTo(-jersey*1.04f,-jersey*.24f);
        shirt.lineTo(-jersey*1.24f,jersey*.49f);
        shirt.lineTo(-jersey*.68f,jersey*.67f);
        shirt.lineTo(-jersey*.58f,jersey*.26f);
        shirt.lineTo(-jersey*.58f,jersey*1.12f);
        shirt.lineTo(jersey*.58f,jersey*1.12f);
        shirt.lineTo(jersey*.58f,jersey*.26f);
        shirt.lineTo(jersey*.68f,jersey*.67f);
        shirt.lineTo(jersey*1.24f,jersey*.49f);
        shirt.lineTo(jersey*1.04f,-jersey*.24f);
        shirt.lineTo(jersey*.56f,-jersey*.58f);
        shirt.quadTo(0,-jersey*.13f,-jersey*.56f,-jersey*.58f);
        shirt.close();
        boolean dark=(teamColor&0x00FFFFFF)<0x00707070;
        color(dark?0xFF292B30:teamColor);c.drawPath(shirt,p);p.clearShadowLayer();
        line(dark?Ui.GOLD:0xFFFAF6EB,Math.max(1.5f,width*.004f));c.drawPath(shirt,p);
        color(dark?Ui.GOLD:Ui.BG);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
        p.setTextSize(jersey*.62f);
        c.drawText(player.pos,0,jersey*.52f,p);
        color(Ui.WHITE);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        String first=player.name.trim().split(" ")[0];
        String name=first.length()>10?first.substring(0,9)+"…":first;
        float font=Math.max(10,width*.029f);
        p.setTextSize(font);
        // Labels are against a dark, unobstructed part of the pitch.
        c.drawText(name,0,jersey*1.72f,p);
        c.restore();c.restoreToCount(saved);
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        if(!Motion.enabled())return;
        reveal=0;
        animator=ValueAnimator.ofFloat(0f,1f);
        animator.setDuration(820);animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(v->{reveal=(float)v.getAnimatedValue();invalidate();});
        animator.start();
    }
    @Override protected void onDetachedFromWindow(){
        if(animator!=null){animator.cancel();animator=null;}super.onDetachedFromWindow();
    }
}
