package br.com.peladaleague.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.TextView;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/** Native OLED theme regressions and rendering smoke test. */
public final class DesignTest extends InstrumentationTestCase {
    public void testTrueBlackAmberAndAccessibleButton(){
        assertEquals(Color.BLACK,Ui.BG);
        assertEquals(0xFFFFB52E,Ui.GOLD);
        assertTrue(Color.red(Ui.WHITE)>200);
        AtomicInteger pressed=new AtomicInteger();
        TextView button=Ui.button(getInstrumentation().getTargetContext(),
            "Preparar a partida",true,pressed::incrementAndGet);
        assertTrue(button.isClickable());
        assertTrue(button.getBackground() instanceof RippleDrawable);
        assertEquals(Ui.BG,button.getCurrentTextColor());
        assertTrue(button.performClick());
        assertEquals(1,pressed.get());
    }
    public void testPitchRendersEveryRoleWithoutCrashing(){
        PitchView pitch=new PitchView(getInstrumentation().getTargetContext(),
            "Time Amarelo",Ui.GOLD,Arrays.asList(
                new DemoDb.Player(1,"Gustavo","GOL",65),
                new DemoDb.Player(2,"Andre","ZAG",75),
                new DemoDb.Player(3,"Tiago","MEI",78),
                new DemoDb.Player(4,"Lucas","ATA",82)));
        pitch.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(450,View.MeasureSpec.EXACTLY));
        pitch.layout(0,0,360,450);
        Bitmap bitmap=Bitmap.createBitmap(360,450,Bitmap.Config.ARGB_8888);
        try{
            pitch.draw(new Canvas(bitmap));
            assertTrue(pitch.getContentDescription().toString().contains("Gustavo"));
            assertTrue(pitch.getContentDescription().toString().contains("Lucas"));
            assertEquals(360,pitch.getMeasuredWidth());
        }finally{bitmap.recycle();}
    }
}
