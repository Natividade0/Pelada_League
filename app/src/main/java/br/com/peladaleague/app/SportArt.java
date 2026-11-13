package br.com.peladaleague.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original vector sports art. No licensed team identity or emoji artwork. */
public final class SportArt extends View {
    private final String kind; private final int color; private final Paint p=new Paint(3); private final Picture picture=new Picture();
    public SportArt(Context c){this(c,"crest",Ui.GOLD);}
    public SportArt(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);Canvas canvas=picture.beginRecording(100,100);drawArt(canvas);picture.endRecording();}
    private Path path(float... xy){Path z=new Path();z.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)z.lineTo(xy[i],xy[i+1]);z.close();return z;}
    private void fill(Canvas c,Path z,int col){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawPath(z,p);}
    private void metal(float x,float y,float xx,float yy,int...colors){p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(x,y,xx,yy,colors,null,Shader.TileMode.CLAMP));}
    protected void onDraw(Canvas c){super.onDraw(c);int save=c.save();float d=Math.min(getWidth(),getHeight());c.translate((getWidth()-d)/2,(getHeight()-d)/2);c.scale(d/100,d/100);c.drawPicture(picture);c.restoreToCount(save);}
    private void drawArt(Canvas c){
        if(kind.equals("crest")){
            Path crown=path(24,23,20,7,34,16,38,3,50,15,62,3,66,16,80,7,76,23);
            metal(20,0,75,30,0xFFAD750B,0xFFFFE27A,0xFFFFD000,0xFFC38B13);c.drawPath(crown,p);
            Path shield=new Path();shield.moveTo(50,27);shield.cubicTo(66,34,76,34,89,31);shield.lineTo(84,64);shield.cubicTo(80,81,65,91,50,98);shield.cubicTo(35,91,20,81,16,64);shield.lineTo(11,31);shield.cubicTo(24,34,34,34,50,27);shield.close();
            metal(12,30,85,100,0xFFF9DC76,0xFFE8B41F,0xFF825109,0xFFFFD64B);c.drawPath(shield,p);
            c.save();c.scale(.88f,.87f,50,59);fill(c,shield,0xFF12161A);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);p.setColor(0xFF9B823D);c.drawPath(shield,p);c.restore();
            p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(40,45,35,new int[]{0xFFFFFFFF,0xFFE9ECEB,0xFF606769},null,Shader.TileMode.CLAMP));c.drawCircle(50,59,26,p);p.setShader(null);
            fill(c,path(50,46,62,54,58,68,43,68,38,54),0xFF161C21);
            p.setStyle(Paint.Style.STROKE);p.setColor(0xFF60696B);p.setStrokeWidth(.9f);
            for(int i=0;i<5;i++){double a=(i*72-90)*Math.PI/180;float x=50+(float)Math.cos(a)*13,y=59+(float)Math.sin(a)*13;c.drawLine(x,y,50+(float)Math.cos(a)*26,59+(float)Math.sin(a)*26,p);}
            for(int i=0;i<5;i++){double a=(i*72-90)*Math.PI/180;float x=50+(float)Math.cos(a)*25,y=59+(float)Math.sin(a)*25;c.save();c.rotate(i*72,x,y);fill(c,path(x-5,y-3,x+5,y-3,x+6,y+2,x,y+7,x-6,y+2),0xFF22292D);c.restore();}
        }else{
            Path shirt=path(34,15,20,20,6,40,22,51,30,40,30,88,70,88,70,40,78,51,94,40,80,20,66,15,59,23,41,23);
            metal(9,15,85,80,color,blend(color,.65f),color,blend(color,.6f));c.drawPath(shirt,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0x776F747B);c.drawPath(shirt,p);
            p.setColor(0x66202729);p.setStrokeWidth(2);c.drawLine(30,40,30,85,p);c.drawLine(70,40,70,85,p);c.drawLine(34,18,41,27,p);c.drawLine(41,27,59,27,p);c.drawLine(59,27,66,18,p);
            p.setStyle(Paint.Style.FILL);p.setColor(0xAAFFFFFF);c.drawRect(58,35,63,41,p);
        }p.setShader(null);
    }
    private int blend(int color,float factor){return Color.rgb((int)(Color.red(color)*factor),(int)(Color.green(color)*factor),(int)(Color.blue(color)*factor));}
}
