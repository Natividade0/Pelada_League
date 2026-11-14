package br.com.peladaleague.app;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** True-black OLED background; discreet stadium contour, no bright wash behind content. */
final class ArenaBackdrop extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas c){
        Rect b=getBounds();c.drawColor(Ui.BG);
        float w=b.width(),h=b.height();if(w<=0||h<=0)return;
        paint.setColor(0x16FFB52E);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1,w*.002f));
        Path trace=new Path();trace.moveTo(w*.62f,h);
        trace.cubicTo(w*.7f,h*.93f,w*.89f,h*.92f,w,h*.79f);
        c.drawPath(trace,paint);paint.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter colorFilter){}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
