package br.com.peladaleague.app;

import android.content.Context;
import android.test.InstrumentationTestCase;

/** No push providers or network; verifies local schedule boundaries and consent. */
public final class ReminderTest extends InstrumentationTestCase {
    public void testTimezoneConversionAndInvalidDates(){
        long utc=EventReminders.millis("2027-01-15T23:30:00Z");
        long brazil=EventReminders.millis("2027-01-15T20:30:00-03:00");
        assertTrue(utc>0);
        assertEquals(utc,brazil);
        assertEquals(-1L,EventReminders.millis("not-a-real-date"));
    }
    public void testNotificationOptInCanBeRevoked(){
        Context context=getInstrumentation().getTargetContext();
        EventReminders.setEnabled(context,false);
        assertFalse(EventReminders.enabled(context));
        EventReminders.setEnabled(context,true);
        assertTrue(EventReminders.enabled(context));
        EventReminders.setEnabled(context,false);
        assertFalse(EventReminders.enabled(context));
        assertEquals(0,EventReminders.stored(context).size());
    }
}
