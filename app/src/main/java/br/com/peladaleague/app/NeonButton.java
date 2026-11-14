package br.com.peladaleague.app;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.TextView;

/** Compatibility component: premium matte button, never a perpetual neon spinner. */
final class NeonButton extends TextView {
    NeonButton(Context context,String title,boolean filled){
        super(context);
        setText(title);
        setTextColor(filled?Ui.BG:Ui.GOLD);
        setTextSize(14);
        setLetterSpacing(0.015f);
        setIncludeFontPadding(false);
        setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        setGravity(Gravity.CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
}
