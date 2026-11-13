package br.com.peladaleague.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

/**
 * A native, animated luminous sweep over the perimeter of primary buttons.
 * Animation stops when the button leaves the view hierarchy.
 */
final class NeonButton extends TextView {
    private final boolean primary;
    private final Paint light=new Paint(Paint.ANTI_ALIAS_FLAG);
    private SweepGradient sweep;
    private final Matrix rotationMatrix=new Matrix();
    private ValueAnimator animator;
    private float degrees;

    NeonButton(Context c,String title,boolean filled) {
        super(c);
        primary=filled;
        setText(title);
        setTextColor(filled?Ui.BG:Ui.WHITE);
        setTextSize(15);
        setIncludeFontPadding(false);
        setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        setGravity(android.view.Gravity.CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        if(w<=0 || h<=0 || !primary)return;
        sweep=new SweepGradient(w/2f,h/2f,
            new int[]{0x33FFE178,0x33FFE178,0xBBFFF1A0,0xFFFFFFFF,
                    0xCCFFF6AB,0x44FFD000,0x33FFE178},
            new float[]{0f,.36f,.47f,.53f,.60f,.72f,1f});
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!primary || sweep==null || getWidth()<6 || getHeight()<6)return;
        float cx=getWidth()/2f,cy=getHeight()/2f;
        rotationMatrix.setRotate(degrees,cx,cy);
        sweep.setLocalMatrix(rotationMatrix);
        light.setStyle(Paint.Style.STROKE);
        light.setShader(sweep);
        float outer=Ui.dp(getContext(),3.3f);
        light.setStrokeWidth(Ui.dp(getContext(),7));
        light.setAlpha(60);
        canvas.drawRoundRect(outer,outer,getWidth()-outer,getHeight()-outer,Ui.dp(getContext(),12),Ui.dp(getContext(),12),light);
        light.setStrokeWidth(Ui.dp(getContext(),2));
        light.setAlpha(255);
        canvas.drawRoundRect(outer,outer,getWidth()-outer,getHeight()-outer,Ui.dp(getContext(),12),Ui.dp(getContext(),12),light);
        light.setShader(null);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!primary || !ValueAnimator.areAnimatorsEnabled())return;
        if(animator!=null)animator.cancel();
        animator=ValueAnimator.ofFloat(0f,360f);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(3400L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation->{
            degrees=(float)animation.getAnimatedValue();
            if(isShown())invalidate();
        });
        animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        if(animator!=null){animator.cancel();animator=null;}
        super.onDetachedFromWindow();
    }
}
