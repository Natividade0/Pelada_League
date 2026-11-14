package br.com.peladaleague.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Locale;

/** Durable local match clock. Not a server-synchronized official timer. */
final class LocalMatchClock extends SQLiteOpenHelper {
    LocalMatchClock(Context c){super(c,"pelada_match_clock.db",null,1);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE clocks(game_id INTEGER PRIMARY KEY,elapsed_ms INTEGER NOT NULL DEFAULT 0,started_at INTEGER NOT NULL DEFAULT 0,running INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int old,int version){}
    private long[] read(long game){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT elapsed_ms,started_at,running FROM clocks WHERE game_id=?",new String[]{""+game})){
            if(c.moveToFirst())return new long[]{c.getLong(0),c.getLong(1),c.getLong(2)};
        }
        return new long[]{0,0,0};
    }
    long elapsed(long game){
        long[] r=read(game);
        long t=r[0];
        if(r[2]!=0)t+=Math.max(0,System.currentTimeMillis()-r[1]);
        return Math.max(0,t);
    }
    boolean running(long game){return read(game)[2]!=0;}
    void start(long game){
        if(game<=0)throw new IllegalArgumentException("Partida inválida.");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
        try{
            try(Cursor c=d.rawQuery("SELECT running FROM clocks WHERE game_id=?",new String[]{""+game})){
                if(c.moveToFirst()&&c.getInt(0)==1){d.setTransactionSuccessful();return;}
            }
            long total=elapsed(game);
            ContentValues v=new ContentValues();v.put("game_id",game);v.put("elapsed_ms",total);
            v.put("started_at",System.currentTimeMillis());v.put("running",1);
            d.insertWithOnConflict("clocks",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    void pause(long game){
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
        try{
            long[] record=read(game);
            if(record[2]==0){d.setTransactionSuccessful();return;}
            ContentValues v=new ContentValues();
            v.put("elapsed_ms",Math.max(0,record[0]+Math.max(0,System.currentTimeMillis()-record[1])));
            v.put("started_at",0);v.put("running",0);
            d.update("clocks",v,"game_id=?",new String[]{""+game});
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    void reset(long game){
        ContentValues v=new ContentValues();v.put("game_id",game);
        v.put("elapsed_ms",0);v.put("started_at",0);v.put("running",0);
        getWritableDatabase().insertWithOnConflict("clocks",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    static String display(long ms){
        long seconds=Math.max(0,ms)/1000L;long minutes=seconds/60L;
        return String.format(Locale.ROOT,"%02d:%02d",minutes,seconds%60L);
    }
}
