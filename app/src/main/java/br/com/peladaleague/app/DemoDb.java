package br.com.peladaleague.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** Offline-first alpha. Attendance and scores are local; no authenticated official approvals. */
public final class DemoDb extends SQLiteOpenHelper {
    public static final class Player {
        public final long id; public final String name, pos; public final int skill;
        public Player(long id,String name,String pos,int skill){this.id=id;this.name=name;this.pos=pos;this.skill=skill;}
    }
    public static final class Game {
        public final long id,groupId;public final String a,b,status,day;
        public Game(long id,long groupId,String a,String b,String status,String day){this.id=id;this.groupId=groupId;this.a=a;this.b=b;this.status=status;this.day=day;}
    }
    public static final class Goal {
        public final long id,gameId,playerId,assistId;public final String name,assistName;public final int team,minute;public final boolean ownGoal;
        public Goal(long id,long gameId,long playerId,String name,int team,int minute,long assistId,String assistName,boolean ownGoal){this.id=id;this.gameId=gameId;this.playerId=playerId;this.name=name;this.team=team;this.minute=minute;this.assistId=assistId;this.assistName=assistName;this.ownGoal=ownGoal;}
    }
    public static final class Card {
        public final long id,gameId,playerId;public final String name,color;public final int team,minute;
        public Card(long id,long gameId,long playerId,String name,int team,String color,int minute){this.id=id;this.gameId=gameId;this.playerId=playerId;this.name=name;this.team=team;this.color=color;this.minute=minute;}
    }
    public static final String CONFIRMED="CONFIRMED",WAITLIST="WAITLIST",DECLINED="DECLINED",PENDING="PENDING";
    private static final int VERSION=3;
    private final SecureRandom rng=new SecureRandom();
    public DemoDb(Context c){super(c,"pelada_league_demo.db",null,VERSION);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE players(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, pos TEXT NOT NULL, skill INTEGER NOT NULL)");
        d.execSQL("CREATE TABLE groups(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, location TEXT NOT NULL, schedule TEXT NOT NULL, capacity INTEGER NOT NULL DEFAULT 14)");
        d.execSQL("CREATE TABLE members(group_id INTEGER NOT NULL, player_id INTEGER NOT NULL, PRIMARY KEY(group_id,player_id))");
        d.execSQL("CREATE TABLE games(id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER NOT NULL, team_a TEXT NOT NULL, team_b TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'DRAFT', day TEXT NOT NULL, session_id INTEGER)");
        d.execSQL("CREATE TABLE goals(id INTEGER PRIMARY KEY AUTOINCREMENT, game_id INTEGER NOT NULL, player_id INTEGER NOT NULL, team INTEGER NOT NULL, minute INTEGER NOT NULL, assist_id INTEGER, own_goal INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE approvals(game_id INTEGER NOT NULL, side INTEGER NOT NULL, salt TEXT, secret_hash TEXT, expires INTEGER NOT NULL DEFAULT 0, attempts INTEGER NOT NULL DEFAULT 0, approved INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(game_id,side))");
        createExtras(d);
        createV3(d);
        for(String[] x:new String[][]{{"Rafael","ATA","88"},{"Bruno","MEI","78"},{"Lucas","ATA","84"},{"Matheus","GOL","79"},{"Carlos","ZAG","74"},{"Pedro","ATA","81"},{"Gabriel","MEI","82"},{"André","GOL","80"},{"João","ZAG","76"},{"Caio","MEI","73"},{"Felipe","ZAG","79"},{"Diego","ATA","77"},{"Henrique","MEI","74"},{"Thiago","ZAG","72"}}){
            ContentValues p=new ContentValues();p.put("name",x[0]);p.put("pos",x[1]);p.put("skill",Integer.parseInt(x[2]));d.insert("players",null,p);
        }
        ContentValues g=new ContentValues();g.put("name","Pelada dos Amigos");g.put("location","Arena Society");g.put("schedule","Quintas • 20h");long group=d.insert("groups",null,g);
        for(int i=1;i<=14;i++){ContentValues m=new ContentValues();m.put("group_id",group);m.put("player_id",i);d.insert("members",null,m);}
        startSession(d,group);
        long past1=insertGame(d,group,"Time Amarelo","Time Preto","DEMO_APPROVED","10/11 • 20:00");
        seedGoals(d,past1,new int[][]{{1,0,12},{1,0,21},{3,0,18},{5,0,30},{2,1,10},{6,1,20},{7,1,38}});
        long past2=insertGame(d,group,"Time Preto","Time Amarelo","DEMO_APPROVED","03/11 • 20:00");
        seedGoals(d,past2,new int[][]{{2,0,10},{6,0,15},{1,1,22},{3,1,38}});
        insertGame(d,group,"Time Amarelo","Time Preto","DRAFT","Jogo de exemplo • 20:00");
    }
    private static void createExtras(SQLiteDatabase d){
        d.execSQL("CREATE TABLE IF NOT EXISTS sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,group_id INTEGER NOT NULL,created_at INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1)");
        d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_active_session ON sessions(group_id) WHERE active=1");
        d.execSQL("CREATE TABLE IF NOT EXISTS attendance(session_id INTEGER NOT NULL,player_id INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'PENDING',updated_at INTEGER NOT NULL,PRIMARY KEY(session_id,player_id))");
        d.execSQL("CREATE TABLE IF NOT EXISTS game_roster(game_id INTEGER NOT NULL,player_id INTEGER NOT NULL,team INTEGER NOT NULL CHECK(team IN (0,1)),PRIMARY KEY(game_id,player_id))");
        d.execSQL("CREATE TABLE IF NOT EXISTS cards(id INTEGER PRIMARY KEY AUTOINCREMENT,game_id INTEGER NOT NULL,player_id INTEGER NOT NULL,team INTEGER NOT NULL,color TEXT NOT NULL CHECK(color IN ('YELLOW','RED')),minute INTEGER NOT NULL)");
        d.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_status ON attendance(session_id,status)");
        d.execSQL("CREATE INDEX IF NOT EXISTS idx_cards_game ON cards(game_id)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int old,int version){
        if(old<2){
            d.execSQL("ALTER TABLE groups ADD COLUMN capacity INTEGER NOT NULL DEFAULT 14");
            d.execSQL("ALTER TABLE games ADD COLUMN session_id INTEGER");
            d.execSQL("ALTER TABLE goals ADD COLUMN assist_id INTEGER");
            d.execSQL("ALTER TABLE goals ADD COLUMN own_goal INTEGER NOT NULL DEFAULT 0");
            createExtras(d);
            try(Cursor c=d.rawQuery("SELECT id FROM groups",null)){while(c.moveToNext())startSession(d,c.getLong(0));}
        }
        if(old<3)createV3(d);
    }
    private static void createV3(SQLiteDatabase d){
        d.execSQL("CREATE TABLE IF NOT EXISTS saved_draws(session_id INTEGER PRIMARY KEY, fingerprint TEXT NOT NULL, squads TEXT NOT NULL)");
        d.execSQL("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY AUTOINCREMENT,game_id INTEGER,action TEXT NOT NULL,created_at INTEGER NOT NULL)");
    }
    private static long startSession(SQLiteDatabase d,long group){ContentValues v=new ContentValues();v.put("group_id",group);v.put("created_at",System.currentTimeMillis());v.put("active",1);return d.insertOrThrow("sessions",null,v);}
    private static void seedGoals(SQLiteDatabase d,long game,int[][] data){for(int[] x:data){ContentValues g=new ContentValues();g.put("game_id",game);g.put("player_id",x[0]);g.put("team",x[1]);g.put("minute",x[2]);d.insert("goals",null,g);}}
    private static long insertGame(SQLiteDatabase d,long group,String a,String b,String status,String day){ContentValues v=new ContentValues();v.put("group_id",group);v.put("team_a",a);v.put("team_b",b);v.put("status",status);v.put("day",day);return d.insertOrThrow("games",null,v);}
    private int count(String sql,String[] args){try(Cursor c=getReadableDatabase().rawQuery(sql,args)){return c.moveToFirst()?c.getInt(0):0;}}
    public List<Player> players(){return playerQuery("SELECT id,name,pos,skill FROM players ORDER BY id",null);}
    public List<Player> groupPlayers(long group){return playerQuery("SELECT p.id,p.name,p.pos,p.skill FROM players p INNER JOIN members m ON m.player_id=p.id WHERE m.group_id=? ORDER BY p.name",new String[]{""+group});}
    private List<Player> playerQuery(String sql,String[] args){List<Player> o=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext())o.add(new Player(c.getLong(0),c.getString(1),c.getString(2),c.getInt(3)));}return o;}
    public long addPlayer(String name,String pos,long group){ContentValues v=new ContentValues();v.put("name",name.trim());v.put("pos",pos);v.put("skill",75);SQLiteDatabase d=getWritableDatabase();long id=d.insertOrThrow("players",null,v);ContentValues m=new ContentValues();m.put("group_id",group);m.put("player_id",id);d.insertOrThrow("members",null,m);return id;}
    public long addPlayer(String name,String pos){return addPlayer(name,pos,1);}
    public void setPlayerSkill(long id,int skill){if(skill<1||skill>100)throw new IllegalArgumentException("Avaliação entre 1 e 100");ContentValues v=new ContentValues();v.put("skill",skill);getWritableDatabase().update("players",v,"id=?",new String[]{""+id});}
    public int groupCount(){return count("SELECT COUNT(*) FROM groups",null);}
    public long addGroup(String name,String location,String schedule){SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();v.put("name",name);v.put("location",location);v.put("schedule",schedule);long id=d.insertOrThrow("groups",null,v);startSession(d,id);return id;}
    public List<String[]> groups(){List<String[]> o=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name,location,schedule FROM groups ORDER BY id DESC",null)){while(c.moveToNext())o.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}return o;}
    public String[] group(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT name,location,schedule FROM groups WHERE id=?",new String[]{""+id})){if(c.moveToFirst())return new String[]{c.getString(0),c.getString(1),c.getString(2)};}return new String[]{"Minha pelada","Campo","20h"};}
    public long activeSession(long group){try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM sessions WHERE group_id=? AND active=1 LIMIT 1",new String[]{""+group})){if(c.moveToFirst())return c.getLong(0);}throw new IllegalStateException("Grupo sem sessão ativa");}
    public long newSession(long group){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{invalidateDraw(d,activeSession(group));ContentValues off=new ContentValues();off.put("active",0);d.update("sessions",off,"group_id=? AND active=1",new String[]{""+group});long id=startSession(d,group);d.setTransactionSuccessful();return id;}finally{d.endTransaction();}}
    public int capacity(long group){try(Cursor c=getReadableDatabase().rawQuery("SELECT capacity FROM groups WHERE id=?",new String[]{""+group})){return c.moveToFirst()?c.getInt(0):14;}}
    public void setCapacity(long group,int cap){
        if(cap<2||cap>100||cap<confirmedCount(group))throw new IllegalArgumentException("A capacidade precisa acomodar todos os confirmados (2 a 100 vagas).");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            ContentValues v=new ContentValues();v.put("capacity",cap);d.update("groups",v,"id=?",new String[]{""+group});
            long session=activeSession(group);while(confirmedCount(group)<cap&&waitCount(group)>0)promoteWaitlist(d,session);
            invalidateDraw(d,session);d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    public Map<Long,String> attendance(long group){long session=activeSession(group);Map<Long,String> statuses=new HashMap<>();for(Player p:groupPlayers(group))statuses.put(p.id,PENDING);try(Cursor c=getReadableDatabase().rawQuery("SELECT player_id,status FROM attendance WHERE session_id=?",new String[]{""+session})){while(c.moveToNext())statuses.put(c.getLong(0),c.getString(1));}return statuses;}
    public int confirmedCount(long group){return count("SELECT COUNT(*) FROM attendance WHERE session_id=? AND status='CONFIRMED'",new String[]{""+activeSession(group)});}
    public int waitCount(long group){return count("SELECT COUNT(*) FROM attendance WHERE session_id=? AND status='WAITLIST'",new String[]{""+activeSession(group)});}
    public List<Player> confirmedPlayers(long group){return playerQuery("SELECT p.id,p.name,p.pos,p.skill FROM players p JOIN attendance a ON a.player_id=p.id JOIN members m ON m.player_id=p.id WHERE a.session_id=? AND a.status='CONFIRMED' AND m.group_id=? ORDER BY p.name",new String[]{""+activeSession(group),""+group});}
    public String markAttendance(long group,long player,String requested){
        if(!Arrays.asList(CONFIRMED,DECLINED,PENDING).contains(requested))throw new IllegalArgumentException("Presença inválida");
        if(count("SELECT COUNT(*) FROM members WHERE group_id=? AND player_id=?",new String[]{""+group,""+player})!=1)throw new IllegalArgumentException("Jogador não pertence ao grupo");
        SQLiteDatabase d=getWritableDatabase();long session=activeSession(group);d.beginTransaction();try{
            String prior=PENDING;try(Cursor c=d.rawQuery("SELECT status FROM attendance WHERE session_id=? AND player_id=?",new String[]{""+session,""+player})){if(c.moveToFirst())prior=c.getString(0);}
            int confirmed=0;try(Cursor c=d.rawQuery("SELECT COUNT(*) FROM attendance WHERE session_id=? AND status='CONFIRMED'",new String[]{""+session})){if(c.moveToFirst())confirmed=c.getInt(0);}
            String actual=CONFIRMED.equals(requested)&&confirmed>=capacity(group)&&!CONFIRMED.equals(prior)?WAITLIST:requested;
            if(!prior.equals(actual))upsertAttendance(d,session,player,actual);
            if(!prior.equals(actual))invalidateDraw(d,session);
            if(CONFIRMED.equals(prior)&&!CONFIRMED.equals(actual))promoteWaitlist(d,session);
            d.setTransactionSuccessful();return actual;
        }finally{d.endTransaction();}
    }
    private static void upsertAttendance(SQLiteDatabase d,long session,long player,String status){ContentValues v=new ContentValues();v.put("session_id",session);v.put("player_id",player);v.put("status",status);v.put("updated_at",System.currentTimeMillis());d.insertWithOnConflict("attendance",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static void promoteWaitlist(SQLiteDatabase d,long session){try(Cursor c=d.rawQuery("SELECT player_id FROM attendance WHERE session_id=? AND status='WAITLIST' ORDER BY updated_at,player_id LIMIT 1",new String[]{""+session})){if(c.moveToFirst())upsertAttendance(d,session,c.getLong(0),CONFIRMED);}}
    public long addGame(long group,String a,String b){return insertGame(getWritableDatabase(),group,a,b,"DRAFT","Hoje • 20:00");}
    public long addGameWithLineup(long group,String a,String b,List<Player> teamA,List<Player> teamB){
        if(teamA.size()<2||teamB.size()<2)throw new IllegalArgumentException("Cada time precisa de ao menos 2 jogadores");
        Set<Long> present=new HashSet<>();for(Player p:confirmedPlayers(group))present.add(p.id);
        Set<Long> chosen=new HashSet<>();for(Player p:teamA)if(!present.contains(p.id)||!chosen.add(p.id))throw new IllegalArgumentException("Escalação contém ausente ou duplicado");
        for(Player p:teamB)if(!present.contains(p.id)||!chosen.add(p.id))throw new IllegalArgumentException("Escalação contém ausente ou duplicado");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            long id=insertGame(d,group,a,b,"DRAFT","Hoje • 20:00");ContentValues link=new ContentValues();link.put("session_id",activeSession(group));d.update("games",link,"id=?",new String[]{""+id});
            for(Player p:teamA)insertLineup(d,id,p.id,0);for(Player p:teamB)insertLineup(d,id,p.id,1);
            d.setTransactionSuccessful();return id;
        }finally{d.endTransaction();}
    }
    private static void insertLineup(SQLiteDatabase d,long game,long player,int side){ContentValues v=new ContentValues();v.put("game_id",game);v.put("player_id",player);v.put("team",side);d.insertOrThrow("game_roster",null,v);}
    public List<Player> roster(long game,int side){return playerQuery("SELECT p.id,p.name,p.pos,p.skill FROM players p JOIN game_roster r ON r.player_id=p.id WHERE r.game_id=? AND r.team=? ORDER BY p.name",new String[]{""+game,""+side});}
    public Game game(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,group_id,team_a,team_b,status,day FROM games WHERE id=?",new String[]{""+id})){if(c.moveToFirst())return readGame(c);}return null;}
    private static Game readGame(Cursor c){return new Game(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5));}
    public List<Game> games(long group){List<Game> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,group_id,team_a,team_b,status,day FROM games WHERE group_id=? ORDER BY id DESC",new String[]{""+group})){while(c.moveToNext())out.add(readGame(c));}return out;}
    public List<Goal> goals(long id){List<Goal> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT g.id,g.game_id,g.player_id,p.name,g.team,g.minute,COALESCE(g.assist_id,0),COALESCE(a.name,''),g.own_goal FROM goals g JOIN players p ON g.player_id=p.id LEFT JOIN players a ON a.id=g.assist_id WHERE g.game_id=? ORDER BY g.minute,g.id",new String[]{""+id})){while(c.moveToNext())out.add(new Goal(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getInt(4),c.getInt(5),c.getLong(6),c.getString(7),c.getInt(8)==1));}return out;}
    public int goals(long id,int side){return count("SELECT COUNT(*) FROM goals WHERE game_id=? AND team=?",new String[]{""+id,""+side});}
    private void requireDraft(long game){Game m=game(game);if(m==null||!"DRAFT".equals(m.status))throw new IllegalStateException("Súmula bloqueada");}
    public void addGoal(long game,long scorer,int scoringSide,int minute,long assist,boolean ownGoal){
        requireDraft(game);if(minute<0||minute>240||scoringSide<0||scoringSide>1)throw new IllegalArgumentException("Dados do lance inválidos");
        int expectedSide=ownGoal?1-scoringSide:scoringSide;
        requireRoster(game);
        if(count("SELECT COUNT(*) FROM game_roster WHERE game_id=?",new String[]{""+game})>0){
            if(count("SELECT COUNT(*) FROM game_roster WHERE game_id=? AND player_id=? AND team=?",new String[]{""+game,""+scorer,""+expectedSide})!=1)throw new IllegalArgumentException("Autor do lance não estava escalado");
            if(assist>0&&count("SELECT COUNT(*) FROM game_roster WHERE game_id=? AND player_id=? AND team=?",new String[]{""+game,""+assist,""+scoringSide})!=1)throw new IllegalArgumentException("Assistência fora da equipe");
        }
        if(assist==scorer&&assist>0||ownGoal&&assist>0)throw new IllegalArgumentException("Assistência inválida");
        ContentValues v=new ContentValues();v.put("game_id",game);v.put("player_id",scorer);v.put("team",scoringSide);v.put("minute",minute);v.put("own_goal",ownGoal?1:0);if(assist>0)v.put("assist_id",assist);getWritableDatabase().insertOrThrow("goals",null,v);
    }
    public void addGoal(long game,long player,int side,int minute){addGoal(game,player,side,minute,0,false);}
    public void deleteGoal(long goal,long game){requireDraft(game);getWritableDatabase().delete("goals","id=? AND game_id=?",new String[]{""+goal,""+game});}
    public List<Card> cards(long game){List<Card> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT c.id,c.game_id,c.player_id,p.name,c.team,c.color,c.minute FROM cards c JOIN players p ON p.id=c.player_id WHERE c.game_id=? ORDER BY c.minute,c.id",new String[]{""+game})){while(c.moveToNext())out.add(new Card(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getInt(4),c.getString(5),c.getInt(6)));}return out;}
    public void addCard(long game,long player,int side,String color,int minute){requireDraft(game);requireRoster(game);if(side<0||side>1)throw new IllegalArgumentException("Equipe inválida");if(!"YELLOW".equals(color)&&!"RED".equals(color)||minute<0||minute>240)throw new IllegalArgumentException("Cartão inválido");if(count("SELECT COUNT(*) FROM game_roster WHERE game_id=?",new String[]{""+game})>0&&count("SELECT COUNT(*) FROM game_roster WHERE game_id=? AND player_id=? AND team=?",new String[]{""+game,""+player,""+side})!=1)throw new IllegalArgumentException("Jogador fora da escalação");ContentValues v=new ContentValues();v.put("game_id",game);v.put("player_id",player);v.put("team",side);v.put("color",color);v.put("minute",minute);getWritableDatabase().insertOrThrow("cards",null,v);}
    public void deleteCard(long id,long game){requireDraft(game);getWritableDatabase().delete("cards","id=? AND game_id=?",new String[]{""+id,""+game});}
    public void submit(long id){requireDraft(id);requireRoster(id);ContentValues v=new ContentValues();v.put("status","PENDING");getWritableDatabase().update("games",v,"id=? AND status='DRAFT'",new String[]{""+id});}
    /** Local demo only. No account ownership proof: never label as official verification. */
    public String generatePin(long game,int side){if(approved(game,side))return null;if(side!=0&&side!=1)throw new IllegalArgumentException("Representante inválido");Game m=game(game);if(m==null||!"PENDING".equals(m.status))return null;String pin=String.format(Locale.US,"%04d",rng.nextInt(10000));byte[] saltBytes=new byte[16];rng.nextBytes(saltBytes);String salt=Base64.getEncoder().encodeToString(saltBytes);ContentValues v=new ContentValues();v.put("game_id",game);v.put("side",side);v.put("salt",salt);v.put("secret_hash",hash(salt+":"+pin));v.put("expires",System.currentTimeMillis()+180000);v.put("attempts",0);v.put("approved",0);getWritableDatabase().insertWithOnConflict("approvals",null,v,SQLiteDatabase.CONFLICT_REPLACE);return pin;}
    public boolean verifyPin(long game,int side,String pin){Game m=game(game);if(pin==null||!pin.matches("[0-9]{4}")||m==null||!"PENDING".equals(m.status))return false;SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
        boolean result=false;try(Cursor c=d.rawQuery("SELECT salt,secret_hash,expires,attempts,approved FROM approvals WHERE game_id=? AND side=?",new String[]{""+game,""+side})){
            if(c.moveToFirst()&&c.getInt(4)==0&&c.getInt(3)<3&&System.currentTimeMillis()<=c.getLong(2)&&c.getString(1)!=null){
                boolean correct=MessageDigest.isEqual(hash(c.getString(0)+":"+pin).getBytes(StandardCharsets.UTF_8),c.getString(1).getBytes(StandardCharsets.UTF_8));ContentValues v=new ContentValues();v.put("attempts",c.getInt(3)+1);if(correct){v.put("approved",1);v.putNull("secret_hash");}d.update("approvals",v,"game_id=? AND side=?",new String[]{""+game,""+side});result=correct;
            }
        }
        if(result){try(Cursor c=d.rawQuery("SELECT COUNT(*) FROM approvals WHERE game_id=? AND approved=1",new String[]{""+game})){if(c.moveToFirst()&&c.getInt(0)==2){ContentValues v=new ContentValues();v.put("status","DEMO_APPROVED");d.update("games",v,"id=? AND status='PENDING'",new String[]{""+game});}}}
        d.setTransactionSuccessful();return result;
    }finally{d.endTransaction();}}
    private static String hash(String value){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] out=md.digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:out)b.append(String.format(Locale.US,"%02x",x&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public boolean approved(long game,int side){return count("SELECT COUNT(*) FROM approvals WHERE game_id=? AND side=? AND approved=1",new String[]{""+game,""+side})>0;}
    public Map<String,Integer> topScorers(long group){Map<String,Integer> o=new LinkedHashMap<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT p.name,COUNT(*) g FROM goals x JOIN games m ON m.id=x.game_id JOIN players p ON p.id=x.player_id WHERE m.group_id=? AND m.status='DEMO_APPROVED' AND x.own_goal=0 GROUP BY p.id ORDER BY g DESC,p.name LIMIT 30",new String[]{""+group})){while(c.moveToNext())o.put(c.getString(0),c.getInt(1));}return o;}
    public int playerGoals(long player){return count("SELECT COUNT(*) FROM goals g JOIN games m ON m.id=g.game_id WHERE m.status='DEMO_APPROVED' AND g.player_id=? AND g.own_goal=0",new String[]{""+player});}
    public int playerAssists(long player){return count("SELECT COUNT(*) FROM goals g JOIN games m ON m.id=g.game_id WHERE m.status='DEMO_APPROVED' AND g.assist_id=?",new String[]{""+player});}
    public int playerMatches(long player){return count("SELECT COUNT(DISTINCT m.id) FROM games m JOIN game_roster r ON r.game_id=m.id WHERE m.status='DEMO_APPROVED' AND r.player_id=?",new String[]{""+player});}
    private void requireRoster(long game){
        if(roster(game,0).size()<2||roster(game,1).size()<2)throw new IllegalStateException("Esta partida antiga não tem escalação. Crie uma partida pelo sorteador.");
    }
    private static void invalidateDraw(SQLiteDatabase d,long session){d.delete("saved_draws","session_id=?",new String[]{""+session});}
    public String attendanceFingerprint(long group){
        StringBuilder s=new StringBuilder().append(activeSession(group)).append(':').append(capacity(group)).append(':');
        List<Long> ids=new ArrayList<>();for(Player p:confirmedPlayers(group))ids.add(p.id);Collections.sort(ids);
        for(long id:ids)s.append(id).append(',');return s.toString();
    }
    public void saveDraw(long group,List<List<Player>> squads){
        if(squads.size()<2||squads.size()>4)throw new IllegalArgumentException("Escolha entre 2 e 4 times.");
        Set<Long> eligible=new HashSet<>(),used=new HashSet<>();for(Player p:confirmedPlayers(group))eligible.add(p.id);
        int min=101,max=0;StringBuilder encoded=new StringBuilder();
        for(List<Player> squad:squads){min=Math.min(min,squad.size());max=Math.max(max,squad.size());if(squad.size()<2)throw new IllegalArgumentException("São necessários dois jogadores por time.");
            if(encoded.length()>0)encoded.append(';');boolean first=true;
            for(Player p:squad){if(!eligible.contains(p.id)||!used.add(p.id))throw new IllegalArgumentException("A presença mudou. Sorteie novamente.");if(!first)encoded.append(',');encoded.append(p.id);first=false;}
        }
        if(max-min>1||used.size()>capacity(group))throw new IllegalArgumentException("Distribuição inválida de jogadores.");
        ContentValues v=new ContentValues();v.put("session_id",activeSession(group));v.put("fingerprint",attendanceFingerprint(group));v.put("squads",encoded.toString());getWritableDatabase().insertWithOnConflict("saved_draws",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<List<Player>> savedDraw(long group){
        List<List<Player>> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT fingerprint,squads FROM saved_draws WHERE session_id=?",new String[]{""+activeSession(group)})){
            if(!c.moveToFirst()||!attendanceFingerprint(group).equals(c.getString(0)))return out;
            Map<Long,Player> byId=new HashMap<>();for(Player p:confirmedPlayers(group))byId.put(p.id,p);
            for(String team:c.getString(1).split(";")){List<Player> squad=new ArrayList<>();for(String id:team.split(",")){Player p=byId.get(Long.parseLong(id));if(p==null)return new ArrayList<>();squad.add(p);}out.add(squad);}
        }return out;
    }
    public void clearDraw(long group){invalidateDraw(getWritableDatabase(),activeSession(group));}
    public void editGoal(long id,long game,long scorer,int side,int minute,long assist,boolean own){
        requireDraft(game);if(count("SELECT COUNT(*) FROM goals WHERE id=? AND game_id=?",new String[]{""+id,""+game})!=1)throw new IllegalArgumentException("Lance não encontrado.");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{deleteGoal(id,game);addGoal(game,scorer,side,minute,assist,own);d.setTransactionSuccessful();}finally{d.endTransaction();}
    }
    public void editCard(long id,long game,long player,int side,String color,int minute){
        requireDraft(game);if(count("SELECT COUNT(*) FROM cards WHERE id=? AND game_id=?",new String[]{""+id,""+game})!=1)throw new IllegalArgumentException("Lance não encontrado.");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{deleteCard(id,game);addCard(game,player,side,color,minute);d.setTransactionSuccessful();}finally{d.endTransaction();}
    }
    public void reopen(long id){
        Game m=game(id);if(m==null||!"PENDING".equals(m.status))throw new IllegalStateException("Somente súmulas pendentes podem ser corrigidas.");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            ContentValues v=new ContentValues();v.put("status","DRAFT");d.update("games",v,"id=?",new String[]{""+id});d.delete("approvals","game_id=?",new String[]{""+id});
            ContentValues audit=new ContentValues();audit.put("game_id",id);audit.put("action","LOCAL_CORRECTION_REQUESTED");audit.put("created_at",System.currentTimeMillis());d.insertOrThrow("audit",null,audit);d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    public void updatePlayer(long id,String name,String position,int skill){
        if(name.trim().length()<2||skill<1||skill>100)throw new IllegalArgumentException("Informe nome e avaliação de 1 a 100.");
        ContentValues v=new ContentValues();v.put("name",name.trim());v.put("pos",position);v.put("skill",skill);getWritableDatabase().update("players",v,"id=?",new String[]{""+id});
    }
    public List<String[]> sessions(long group){
        List<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT s.id,s.created_at,s.active,(SELECT COUNT(*) FROM attendance a WHERE a.session_id=s.id AND a.status='CONFIRMED') FROM sessions s WHERE group_id=? ORDER BY id DESC",new String[]{""+group})){while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}return out;
    }
    public List<Game> playerHistory(long player){
        List<Game> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT m.id,m.group_id,m.team_a,m.team_b,m.status,m.day FROM games m JOIN game_roster r ON r.game_id=m.id WHERE r.player_id=? ORDER BY m.id DESC",new String[]{""+player})){while(c.moveToNext())out.add(readGame(c));}return out;
    }
    public int cardCount(long game,int side,String color){return count("SELECT COUNT(*) FROM cards WHERE game_id=? AND team=? AND color=?",new String[]{""+game,""+side,color});}
    public void addMember(long group,long player){ContentValues v=new ContentValues();v.put("group_id",group);v.put("player_id",player);getWritableDatabase().insertWithOnConflict("members",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    public int gamesPlayed(){return count("SELECT COUNT(*) FROM games WHERE status='DEMO_APPROVED'",null);}
}
