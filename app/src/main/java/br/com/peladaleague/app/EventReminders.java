package br.com.peladaleague.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Opt-in, inexact LOCAL event reminders. No paid service or hidden network requests.
 * Does NOT send a push notification to other users when a new pelada is created.
 */
final class EventReminders {
    static final int PERMISSION_REQUEST=1701;
    private static final String PREF="pelada_reminders";
    private static final String CHANNEL="pelada_league_events";
    private static final long[] LEADS={24L*3600000L,2L*3600000L};
    private EventReminders(){}

    static boolean enabled(Context c){return c.getSharedPreferences(PREF,0).getBoolean("enabled",false);}
    static boolean allowed(Context c){
        return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
    }
    static void setEnabled(Context c,boolean enabled){
        SharedPreferences pref=c.getSharedPreferences(PREF,0);
        pref.edit().putBoolean("enabled",enabled).apply();
        if(!enabled){
            for(JSONObject e:stored(c))cancel(c,e.optString("id"));
            pref.edit().remove("events").apply();
        }
    }
    static long millis(String iso){
        try{return OffsetDateTime.parse(iso).toInstant().toEpochMilli();}
        catch(DateTimeParseException ex){
            try{return java.time.Instant.parse(iso).toEpochMilli();}
            catch(Exception ignored){return -1L;}
        }
    }
    static void channel(Context context){
        if(Build.VERSION.SDK_INT<26)return;
        NotificationManager mgr=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(mgr==null)return;
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Lembretes de peladas",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Lembretes de eventos que você escolheu acompanhar neste aparelho");
        mgr.createNotificationChannel(channel);
    }
    static void schedule(Context c,JSONObject event){
        if(!enabled(c)||!allowed(c)||event==null)return;
        String id=event.optString("id"),code=event.optString("invite_code");
        String title=event.optString("title","Pelada");
        long at=millis(event.optString("starts_at"));
        if(id.isEmpty()||!code.matches("[a-fA-F0-9]{32}")||at<=System.currentTimeMillis()
           ||!"OPEN".equals(event.optString("status","OPEN")))return;
        channel(c);
        cancel(c,id);
        long now=System.currentTimeMillis();
        for(int i=0;i<LEADS.length;i++){
            long when=at-LEADS[i];
            if(when<=now+60000L)continue;
            AlarmManager mgr=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            if(mgr!=null)mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,intent(c,id,title,code,at,i));
        }
        List<JSONObject> old=stored(c);
        JSONArray all=new JSONArray();
        for(JSONObject existing:old)if(!id.equals(existing.optString("id")))all.put(existing);
        all.put(event);
        c.getSharedPreferences(PREF,0).edit().putString("events",all.toString()).apply();
    }
    static void cancel(Context c,String eventId){
        AlarmManager mgr=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(mgr==null)return;
        for(int i=0;i<LEADS.length;i++){
            PendingIntent pi=intent(c,eventId,"","",0,i);
            mgr.cancel(pi);pi.cancel();
        }
    }
    private static PendingIntent intent(Context c,String id,String title,String code,long at,int index){
        Intent intent=new Intent(c,EventReminderReceiver.class);
        intent.setAction("br.com.peladaleague.REMINDER");
        intent.setData(Uri.parse("peladaleague-reminder://event/"+id+"/"+index));
        intent.putExtra("id",id);intent.putExtra("title",title);intent.putExtra("code",code);
        intent.putExtra("starts_at",at);intent.putExtra("lead",index);
        return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static List<JSONObject> stored(Context c){
        List<JSONObject> events=new ArrayList<>();
        try{
            JSONArray data=new JSONArray(c.getSharedPreferences(PREF,0).getString("events","[]"));
            for(int i=0;i<data.length();i++){
                JSONObject item=data.optJSONObject(i);if(item!=null)events.add(item);
            }
        }catch(Exception ignored){}
        return events;
    }
    static void restore(Context c){
        if(!enabled(c)||!allowed(c))return;
        List<JSONObject> items=stored(c);
        for(JSONObject e:items)schedule(c,e);
    }
    static void notify(Context c,Intent alarm){
        if(!enabled(c)||!allowed(c))return;
        String id=alarm.getStringExtra("id");
        String code=alarm.getStringExtra("code");
        String title=alarm.getStringExtra("title");
        if(id==null||code==null||!code.matches("[a-fA-F0-9]{32}"))return;
        long when=alarm.getLongExtra("starts_at",0L);
        if(when<System.currentTimeMillis()-3600000L)return;
        channel(c);
        int lead=alarm.getIntExtra("lead",0);
        String detail=lead==0?"A pelada é amanhã. Confira sua presença.":"A pelada começa em aproximadamente 2 horas.";
        Intent launch=new Intent(Intent.ACTION_VIEW,Uri.parse("peladaleague://convite?code="+code),c,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open=PendingIntent.getActivity(c,0,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder notification=new Notification.Builder(c,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title)
            .setContentText(detail).setContentIntent(open).setAutoCancel(true);
        NotificationManager mgr=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(mgr!=null)mgr.notify(id.hashCode()*3+lead,notification.build());
    }
}
final class EventReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if("br.com.peladaleague.REMINDER".equals(intent.getAction()))
            EventReminders.notify(context,intent);
    }
}
/** Explicitly exported only for system reboot broadcasts, never for private reminder actions. */
final class EventReminderBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||
           Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
            EventReminders.restore(context);
    }
}
