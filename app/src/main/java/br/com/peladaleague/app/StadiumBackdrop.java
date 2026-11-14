package br.com.peladaleague.app;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original low-contrast pitch geometry, designed to keep text legible on OLED screens. */
final class StadiumBackdrop extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas c){
        Rect bounds=getBounds();float w=bounds.width(),h=bounds.height();
        if(w<=0||h<=0)return;
        p.setShader(new LinearGradient(0,0,w,h,
            new int[]{0xFF1A1511,0xFF101012,0xFF060607},null,Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);c.drawRect(bounds,p);p.setShader(null);
        // Pitch lines are subtle decorative geometry, not a readable scorecard.
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.003f));
        p.setColor(0x334B514C);
        c.save();c.translate(w*.62f,h*.09f);c.rotate(-14);
        float fw=w*.6f,fh=h*.9f;
        c.drawRect(0,0,fw,fh,p);c.drawLine(0,fh*.5f,fw,fh*.5f,p);
        c.drawCircle(fw*.5f,fh*.5f,fw*.15f,p);
        c.drawRect(fw*.27f,0,fw*.73f,fh*.19f,p);
        c.drawRect(fw*.27f,fh*.81f,fw*.73f,fh,p);
        c.restore();
        p.setStrokeWidth(Math.max(2,w*.008f));p.setColor(Ui.GOLD);
        c.drawLine(0,h*.19f,0,h*.82f,p);
        p.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter colorFilter){}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
