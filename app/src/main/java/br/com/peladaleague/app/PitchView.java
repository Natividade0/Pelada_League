package br.com.peladaleague.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.*;

/**
 * Native perspective formation pitch. Fully original 2.5D vector rendering,
 * no real 3D engine or licensed club artwork. Player reveal respects reduced motion.
 */
final class PitchView extends View {
    private final List<DemoDb.Player> players;
    private final String team;
    private final int teamColor;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float reveal=1f;
    private ValueAnimator animator;
    PitchView(Context context,String name,int color,List<DemoDb.Player> squad){
        super(context);team=name;teamColor=color;players=new ArrayList<>(squad);
        setContentDescription("Campo tático de "+name+" com "+players.size()+" jogadores");
        setLayerType(LAYER_TYPE_SOFTWARE,null);
    }
    @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        int w=MeasureSpec.getSize(widthMeasureSpec);
        if(w==0)w=Ui.dp(getContext(),310);
        int desired=(int)(w*.95f);
        setMeasuredDimension(resolveSize(w,widthMeasureSpec),
            resolveSize(Math.max(Ui.dp(getContext(),250),desired),heightMeasureSpec));
    }
    private void stroke(int color,float width){
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);
    }
    private void fill(int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
    private Path shape(float... xy){
        Path z=new Path();z.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)z.lineTo(xy[i],xy[i+1]);z.close();return z;
    }
    private float left(float y,float w,float h){
        float t=(y-h*.12f)/(h*.80f);return w*(.17f-.13f*Math.max(0,Math.min(1,t)));
    }
    private float right(float y,float w,float h){return w-left(y,w,h);}
    private float textWidth(String name,float size){
        p.setTextSize(size);return p.measureText(name);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        int layer=c.saveLayer(0,0,w,h,null);
        fill(0xFF0E1318);c.drawRoundRect(0,0,w,h,Ui.dp(getContext(),16),Ui.dp(getContext(),16),p);
        p.setShader(new LinearGradient(0,0,0,h,new int[]{0xFF102124,0xFF172E25,0xFF071C1B},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(1,1,w-1,h-1,Ui.dp(getContext(),16),Ui.dp(getContext(),16),p);p.setShader(null);
        // Floodlights above the pitch.
        for(int i=0;i<6;i++){
            float x=w*(.12f+i*.15f);
            p.setShader(new RadialGradient(x,h*.055f,w*.15f,
                new int[]{0x44FFE2A2,0x07FFE2A2,0x00FFFFFF},null,Shader.TileMode.CLAMP));
            c.drawCircle(x,h*.055f,w*.15f,p);
        }
        p.setShader(null);
        fill(0xFF163D28);c.drawPath(shape(w*.17f,h*.12f,w*.83f,h*.12f,w*.96f,h*.91f,w*.04f,h*.91f),p);
        // Stripes follow the field perspective.
        for(int i=0;i<8;i++){
            float top=h*(.12f+i*.10f),bottom=top+h*.10f;
            if(i%2==0){fill(0x2516C576);c.drawPath(shape(left(top,w,h),top,right(top,w,h),top,
                right(bottom,w,h),bottom,left(bottom,w,h),bottom),p);}
        }
        stroke(0x90E4EDE4,Math.max(1f,w*.003f));
        c.drawPath(shape(w*.17f,h*.12f,w*.83f,h*.12f,w*.96f,h*.91f,w*.04f,h*.91f),p);
        c.drawLine(left(h*.52f,w,h),h*.52f,right(h*.52f,w,h),h*.52f,p);
        c.drawOval(w*.36f,h*.47f,w*.64f,h*.57f,p);
        c.drawRect(w*.39f,h*.12f,w*.61f,h*.18f,p);
        c.drawRect(w*.34f,h*.85f,w*.66f,h*.91f,p);
        // Night stadium caption.
        fill(0xFFFAFAFC);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        p.setTextSize(w*.047f);
        c.drawText(team,w*.06f,h*.075f,p);
        fill(0xFFFFD000);p.setTextSize(w*.03f);
        c.drawText("ESCALAÇÃO • "+players.size()+" JOGADORES",w*.06f,h*.975f,p);
        // Group players by primary roles; labels reveal in a stagger.
        Map<Integer,List<DemoDb.Player>> byBand=new TreeMap<>();
        for(DemoDb.Player player:players){
            int band="GOL".equals(player.pos)?0:"ZAG".equals(player.pos)?1:"MEI".equals(player.pos)?2:3;
            byBand.computeIfAbsent(band,k->new ArrayList<>()).add(player);
        }
        int index=0;
        for(Map.Entry<Integer,List<DemoDb.Player>> entry:byBand.entrySet()){
            int band=entry.getKey();List<DemoDb.Player> row=entry.getValue();
            float y=h*(.79f-band*.19f);
            float x1=left(y,w,h)+w*.075f,x2=right(y,w,h)-w*.075f;
            for(int i=0;i<row.size();i++){
                float x=x1+(x2-x1)*(i+1)/(row.size()+1);
                float alpha=Math.max(0,Math.min(1,(reveal-index*.085f)*1.8f));
                if(alpha>0)player(c,row.get(i),x,y,w,h,alpha);
                index++;
            }
        }
        c.restoreToCount(layer);
    }
    private void player(Canvas c,DemoDb.Player player,float x,float y,float w,float h,float opacity){
        int save=c.save();c.scale(.65f+opacity*.35f,.65f+opacity*.35f,x,y);
        int radius=(int)Math.max(13,w*.037f);
        p.setShader(new RadialGradient(x,y,radius*2,
            new int[]{0x99FFE998,0x00F3CD21},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,radius*2,p);p.setShader(null);
        fill(0xFF10151A);c.drawCircle(x,y,radius+2,p);
        fill(teamColor);c.drawCircle(x,y,radius,p);
        stroke(0xFFF3EBB4,Math.max(1,w*.004f));c.drawCircle(x,y,radius,p);
        fill(0xFF101318);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(radius*.86f);
        String initials=player.name.isEmpty()?"?":player.name.substring(0,1).toUpperCase(Locale.ROOT);
        c.drawText(initials,x,y+radius*.31f,p);
        String name=player.name.length()>10?player.name.substring(0,9)+"…":player.name;
        float font=Math.max(9,w*.034f);p.setTextSize(font);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        p.setShadowLayer(5,0,1,0xFF000000);
        fill(0xFFF8F9FB);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(font);
        c.drawText(name,x,y+radius+font*1.2f,p);
        p.clearShadowLayer();p.setTextAlign(Paint.Align.LEFT);c.restoreToCount(save);
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        if(!Motion.enabled())return;
        reveal=0;
        animator=ValueAnimator.ofFloat(0,1f);
        animator.setDuration(1050);animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(v->{reveal=(float)v.getAnimatedValue();invalidate();});
        animator.start();
    }
    @Override protected void onDetachedFromWindow(){
        if(animator!=null){animator.cancel();animator=null;}super.onDetachedFromWindow();
    }
}
