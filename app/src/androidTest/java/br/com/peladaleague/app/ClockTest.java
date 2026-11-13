package br.com.peladaleague.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.test.InstrumentationTestCase;

/** Clock tests run only in an isolated SQLite database. */
public final class ClockTest extends InstrumentationTestCase {
    public void testClockSurvivesPauseAndReopen()throws Exception{
        Context base=getInstrumentation().getTargetContext();
        Context sandbox=new ContextWrapper(base){
            @Override public java.io.File getDatabasePath(String name){
                return super.getDatabasePath("qa_clock_"+name);
            }
        };
        base.deleteDatabase("qa_clock_pelada_match_clock.db");
        LocalMatchClock c=new LocalMatchClock(sandbox);
        c.start(42);
        Thread.sleep(85L);
        assertTrue(c.running(42));
        c.pause(42);
        long elapsed=c.elapsed(42);
        assertTrue(elapsed>=70);
        assertFalse(c.running(42));
        c.close();
        c=new LocalMatchClock(sandbox);
        assertEquals(elapsed,c.elapsed(42));
        c.start(42);
        Thread.sleep(80);
        c.pause(42);
        assertTrue(c.elapsed(42)>elapsed);
        c.reset(42);
        assertEquals(0,c.elapsed(42));
        c.close();
        base.deleteDatabase("qa_clock_pelada_match_clock.db");
    }
}
