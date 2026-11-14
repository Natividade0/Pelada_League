package br.com.peladaleague.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Short, compositor-only animations. Honors the Android animation/reduced-motion setting. */
public final class Motion {
    private Motion(){}
    public static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
    public static void arrive(View v,boolean forward){
        if(!enabled())return;
        v.animate().cancel();v.setAlpha(0f);
        v.setTranslationY(Ui.dp(v.getContext(),forward?12:-12));
        v.animate().alpha(1f).translationY(0).setDuration(220)
            .setInterpolator(new DecelerateInterpolator()).start();
    }
    public static void reveal(View v,int index){
        if(!enabled())return;
        v.animate().cancel();v.setAlpha(0);v.setTranslationY(Ui.dp(v.getContext(),10));
        v.animate().alpha(1).translationY(0).setStartDelay(Math.min(index*38,152))
            .setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
    }
    public static void score(View v){
        if(!enabled())return;
        v.animate().cancel();v.setScaleX(1.13f);v.setScaleY(1.13f);
        v.animate().scaleX(1).scaleY(1).setDuration(320)
            .setInterpolator(new DecelerateInterpolator()).start();
    }
    public static void intro(View v,int delay){
        if(!enabled())return;
        v.animate().cancel();v.setAlpha(0);v.setTranslationY(Ui.dp(v.getContext(),14));
        v.animate().alpha(1).translationY(0).setStartDelay(delay).setDuration(350)
            .setInterpolator(new DecelerateInterpolator()).start();
    }
    /** One-time crest accent; no infinite animator or background battery drain. */
    public static void breathe(View v){
        if(!enabled())return;
        v.setScaleX(.96f);v.setScaleY(.96f);
        v.animate().scaleX(1).scaleY(1).setDuration(380)
            .setInterpolator(new DecelerateInterpolator()).start();
    }
}
