package br.com.peladaleague.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;

/**
 * Actual local bookkeeping, scoped to the active session of each pelada.
 * NEVER claims to confirm a bank transfer: payments are manual records only.
 * Stored separately from the v0.4 SQLite file to avoid destructive migrations.
 */
final class LocalFinance extends SQLiteOpenHelper {
    static final class Config {
        final int fee,fieldCost;final boolean keepersExempt;final String pixKey;
        Config(int fee,int fieldCost,boolean keepersExempt,String pixKey){
            this.fee=fee;this.fieldCost=fieldCost;this.keepersExempt=keepersExempt;this.pixKey=pixKey;
        }
    }
    static final class Summary {
        final int expected,received,pending,people,paid,waived;
        Summary(int expected,int received,int pending,int people,int paid,int waived) {
            this.expected=expected;this.received=received;this.pending=pending;this.people=people;this.paid=paid;this.waived=waived;
        }
    }
    private final DemoDb members;
    LocalFinance(Context context,DemoDb db){super(context,"pelada_league_finance.db",null,1);members=db;}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE config(session_id INTEGER PRIMARY KEY, fee INTEGER NOT NULL DEFAULT 0 CHECK(fee>=0), field_cost INTEGER NOT NULL DEFAULT 0 CHECK(field_cost>=0), keeper_exempt INTEGER NOT NULL DEFAULT 1, pix_key TEXT NOT NULL DEFAULT '')");
        d.execSQL("CREATE TABLE participant(session_id INTEGER NOT NULL,player_id INTEGER NOT NULL,position TEXT,exempt_override INTEGER CHECK(exempt_override IN (0,1) OR exempt_override IS NULL),PRIMARY KEY(session_id,player_id))");
        d.execSQL("CREATE TABLE payment(session_id INTEGER NOT NULL,player_id INTEGER NOT NULL,paid INTEGER NOT NULL DEFAULT 0 CHECK(paid>=0),method TEXT NOT NULL DEFAULT 'MANUAL',changed_at INTEGER NOT NULL,PRIMARY KEY(session_id,player_id))");
        d.execSQL("CREATE TABLE ledger(id INTEGER PRIMARY KEY AUTOINCREMENT,session_id INTEGER NOT NULL,player_id INTEGER NOT NULL,delta INTEGER NOT NULL,method TEXT NOT NULL,created_at INTEGER NOT NULL)");
        d.execSQL("CREATE INDEX ledger_by_session ON ledger(session_id,player_id,created_at)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int old,int next){
        if(old>next)throw new IllegalStateException("Versão financeira não suportada");
    }
    private long session(long group){return members.activeSession(group);}
    Config config(long group){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT fee,field_cost,keeper_exempt,pix_key FROM config WHERE session_id=?",new String[]{""+session(group)})){
            if(c.moveToFirst())return new Config(c.getInt(0),c.getInt(1),c.getInt(2)==1,c.getString(3));
        }
        return new Config(0,0,true,"");
    }
    void save(long group,int fee,int cost,boolean freeKeepers,String pixKey){
        if(fee<0||fee>10000000||cost<0||cost>10000000)throw new IllegalArgumentException("Informe valores entre R$ 0 e R$ 100.000.");
        String pix=pixKey==null?"":pixKey.trim();
        if(pix.length()>160)throw new IllegalArgumentException("Chave Pix muito longa.");
        ContentValues v=new ContentValues();v.put("session_id",session(group));v.put("fee",fee);v.put("field_cost",cost);v.put("keeper_exempt",freeKeepers?1:0);v.put("pix_key",pix);
        getWritableDatabase().insertWithOnConflict("config",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    private void verifyMember(long group,long player){
        boolean exists=false;
        for(DemoDb.Player p:members.groupPlayers(group))if(p.id==player){exists=true;break;}
        if(!exists)throw new IllegalArgumentException("Jogador não pertence à pelada.");
    }
    String position(long group,DemoDb.Player player){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT position FROM participant WHERE session_id=? AND player_id=?",new String[]{""+session(group),""+player.id})){
            if(c.moveToFirst()&&!c.isNull(0))return c.getString(0);
        }
        return player.pos;
    }
    private void ensureParticipant(long group,long player){
        ContentValues v=new ContentValues();v.put("session_id",session(group));v.put("player_id",player);
        getWritableDatabase().insertWithOnConflict("participant",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    void setPosition(long group,long player,String position){
        if(!Arrays.asList("GOL","ZAG","MEI","ATA","LIN").contains(position))throw new IllegalArgumentException("Posição inválida.");
        verifyMember(group,player);ensureParticipant(group,player);
        ContentValues v=new ContentValues();v.put("position",position);
        getWritableDatabase().update("participant",v,"session_id=? AND player_id=?",new String[]{""+session(group),""+player});
    }
    /** null = automatic according to keeper policy, true = manually exempt, false = charge. */
    void setExemption(long group,long player,Boolean override){
        verifyMember(group,player);ensureParticipant(group,player);
        ContentValues v=new ContentValues();
        if(override==null)v.putNull("exempt_override");else v.put("exempt_override",override?1:0);
        getWritableDatabase().update("participant",v,"session_id=? AND player_id=?",new String[]{""+session(group),""+player});
    }
    boolean isExempt(long group,DemoDb.Player player){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT exempt_override FROM participant WHERE session_id=? AND player_id=?",new String[]{""+session(group),""+player.id})){
            if(c.moveToFirst()&&!c.isNull(0))return c.getInt(0)==1;
        }
        return config(group).keepersExempt&&"GOL".equals(position(group,player));
    }
    int due(long group,DemoDb.Player player){return isExempt(group,player)?0:config(group).fee;}
    int paid(long group,long player){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT paid FROM payment WHERE session_id=? AND player_id=?",new String[]{""+session(group),""+player})){
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    /** Set the complete amount already received, not a payment increment. Negative corrections are audited. */
    void setTotalPaid(long group,DemoDb.Player player,int total,String method){
        verifyMember(group,player);
        if(total<0||total>10000000)throw new IllegalArgumentException("Valor pago inválido.");
        if(total>due(group,player))throw new IllegalArgumentException("O valor informado excede a contribuição do jogador.");
        if(!Arrays.asList("PIX","DINHEIRO","OUTRO","AJUSTE").contains(method))throw new IllegalArgumentException("Forma de pagamento inválida.");
        long ss=session(group);int previous=paid(group,player.id);
        if(previous==total)return;
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
        try{
            ContentValues v=new ContentValues();v.put("session_id",ss);v.put("player_id",player.id);v.put("paid",total);
            v.put("method",method);v.put("changed_at",System.currentTimeMillis());
            d.insertWithOnConflict("payment",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            ContentValues audit=new ContentValues();audit.put("session_id",ss);audit.put("player_id",player.id);
            audit.put("delta",total-previous);audit.put("method",method);audit.put("created_at",System.currentTimeMillis());
            d.insertOrThrow("ledger",null,audit);
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    String paymentStatus(long group,DemoDb.Player p){
        int due=due(group,p),paid=paid(group,p.id);
        if(due==0)return paid>0?"ISENTO · revisar valor pago":"ISENTO";
        if(paid==0)return "PENDENTE";
        if(paid>=due)return "PAGO";
        return "PARCIAL";
    }
    Summary summary(long group){
        int expected=0,received=0,paidCount=0,waived=0;List<DemoDb.Player> people=members.confirmedPlayers(group);
        for(DemoDb.Player p:people){
            int due=due(group,p),paid=paid(group,p.id);
            expected+=due;received+=paid;
            if(due==0)waived++;
            else if(paid>=due)paidCount++;
        }
        return new Summary(expected,received,Math.max(0,expected-received),people.size(),paidCount,waived);
    }
    List<String[]> ledger(long group,int limit){
        List<String[]> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT player_id,delta,method,created_at FROM ledger WHERE session_id=? ORDER BY id DESC LIMIT "+Math.max(1,Math.min(100,limit)),new String[]{""+session(group)})){
            while(c.moveToNext())list.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});
        }
        return list;
    }
}
