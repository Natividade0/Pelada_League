package br.com.peladaleague.app;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Motion uses compositor properties only and honours Android animator preferences. */
public final class Motion {
    private Motion() {}
    public static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
    public static void arrive(View v, boolean forward){
        if(!enabled())return;
        v.setAlpha(0);v.setTranslationX(Ui.dp(v.getContext(),forward?10:-10));
        v.animate().alpha(1).translationX(0).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
    }
    public static void reveal(View v,int index){
        if(!enabled())return;
        v.setAlpha(0);v.setRotationY(-12);v.setScaleX(.96f);v.setScaleY(.96f);
        v.animate().alpha(1).rotationY(0).scaleX(1).scaleY(1).setStartDelay(Math.min(index*45,135)).setDuration(240).start();
    }
    public static void score(View v){if(!enabled())return;v.setScaleX(1.08f);v.setScaleY(1.08f);v.animate().scaleX(1).scaleY(1).setDuration(240).start();}
    public static void intro(View v,int delay){if(!enabled())return;v.setAlpha(0);v.setScaleX(.96f);v.setScaleY(.96f);v.animate().alpha(1).scaleX(1).scaleY(1).setStartDelay(delay).setDuration(320).start();}

    /** Subtle living crest: slow breathing motion, automatically cancelled on detach. */
    public static void breathe(View v){
        if(!enabled())return;
        android.animation.ObjectAnimator horizontal=android.animation.ObjectAnimator.ofFloat(v,View.SCALE_X,1f,1.04f,1f);
        android.animation.ObjectAnimator vertical=android.animation.ObjectAnimator.ofFloat(v,View.SCALE_Y,1f,1.04f,1f);
        for(android.animation.ObjectAnimator a:new android.animation.ObjectAnimator[]{horizontal,vertical}){
            a.setDuration(3600);
            a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        }
        View.OnAttachStateChangeListener listener=new View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(View w){}
            @Override public void onViewDetachedFromWindow(View w){
                horizontal.cancel();vertical.cancel();
                w.removeOnAttachStateChangeListener(this);
            }
        };
        v.addOnAttachStateChangeListener(listener);
        horizontal.start();vertical.start();
    }
}
