package br.com.peladaleague.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores only user-approved future reminders after device restart or APK update. */
public final class EventReminderBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||
           Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
            EventReminders.restore(context);
    }
}
