package br.com.peladaleague.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Instantiated by Android; not exported to other apps. */
public final class EventReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent!=null&&"br.com.peladaleague.REMINDER".equals(intent.getAction()))
            EventReminders.notify(context,intent);
    }
}
