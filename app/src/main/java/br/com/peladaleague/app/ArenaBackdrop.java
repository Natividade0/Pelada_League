package br.com.peladaleague.app;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Quiet stadium-light accent anchored to the lower edge, independent of text layout. */
final class ArenaBackdrop extends Drawable {
    private final Paint p=new Paint(3);private final Path light=new Path();private Shader glow;
    protected void onBoundsChange(Rect r){float w=r.width(),h=r.height();glow=new RadialGradient(w*.95f,h,Math.max(1,w*.9f),new int[]{0x293B2B03,0x0007090B},null,Shader.TileMode.CLAMP);light.reset();light.moveTo(w*.33f,h);light.cubicTo(w*.65f,h-w*.16f,w*.82f,h-w*.28f,w,h-w*.4f);}
    public void draw(Canvas c){c.drawColor(Ui.BG);p.setShader(glow);p.setStyle(Paint.Style.FILL);c.drawRect(getBounds(),p);p.setShader(null);p.setColor(0x255F4713);p.setStrokeWidth(1);p.setStyle(Paint.Style.STROKE);c.drawPath(light,p);}
    public void setAlpha(int alpha){}public void setColorFilter(ColorFilter filter){}public int getOpacity(){return PixelFormat.OPAQUE;}
}
