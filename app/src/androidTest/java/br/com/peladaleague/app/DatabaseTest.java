package br.com.peladaleague.app;

import android.test.InstrumentationTestCase;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.DatabaseErrorHandler;
import br.com.peladaleague.core.TeamBalancer;
import java.util.*;

/** Executes against real Android SQLite, in a test-only database. */
public final class DatabaseTest extends InstrumentationTestCase {
    private DemoDb db; private Context context;
    protected void setUp() throws Exception {super.setUp();Context target=getInstrumentation().getTargetContext();context=new ContextWrapper(target){
        public java.io.File getDatabasePath(String name){return super.getDatabasePath("qa_"+name);}
        public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return super.openOrCreateDatabase("qa_"+name,mode,factory);}
        public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler handler){return super.openOrCreateDatabase("qa_"+name,mode,factory,handler);}
    };target.deleteDatabase("qa_pelada_league_demo.db");db=new DemoDb(context);db.getWritableDatabase();}
    protected void tearDown() throws Exception{db.close();getInstrumentation().getTargetContext().deleteDatabase("qa_pelada_league_demo.db");super.tearDown();}
    private List<List<DemoDb.Player>> teams(){for(int i=1;i<=8;i++)db.markAttendance(1,i,DemoDb.CONFIRMED);List<DemoDb.Player> p=db.confirmedPlayers(1);return List.of(new ArrayList<>(p.subList(0,4)),new ArrayList<>(p.subList(4,8)));}
    private long match(){List<List<DemoDb.Player>> squads=teams();return db.addGameWithLineup(1,"QA Amarelo","QA Preto",squads.get(0),squads.get(1));}
    private void rejected(Runnable operation){try{operation.run();fail("Invalid operation accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
    public void testAttendanceWaitlistAndCapacity(){db.setCapacity(1,2);assertEquals(DemoDb.CONFIRMED,db.markAttendance(1,1,DemoDb.CONFIRMED));db.markAttendance(1,2,DemoDb.CONFIRMED);assertEquals(DemoDb.WAITLIST,db.markAttendance(1,3,DemoDb.CONFIRMED));db.markAttendance(1,4,DemoDb.CONFIRMED);db.markAttendance(1,3,DemoDb.CONFIRMED);db.markAttendance(1,1,DemoDb.DECLINED);assertEquals(DemoDb.CONFIRMED,db.attendance(1).get(3L));assertEquals(DemoDb.WAITLIST,db.attendance(1).get(4L));db.setCapacity(1,3);assertEquals(DemoDb.CONFIRMED,db.attendance(1).get(4L));rejected(()->db.setCapacity(1,2));rejected(()->db.markAttendance(1,999,DemoDb.CONFIRMED));}
    public void testSavedDrawPersistenceInvalidationAndDuplicate(){List<List<DemoDb.Player>> squads=teams();db.saveDraw(1,squads);db.close();db=new DemoDb(context);assertEquals(2,db.savedDraw(1).size());assertEquals(4,db.savedDraw(1).get(0).size());rejected(()->db.saveDraw(1,List.of(squads.get(0),squads.get(0))));db.markAttendance(1,squads.get(0).get(0).id,DemoDb.DECLINED);assertTrue(db.savedDraw(1).isEmpty());rejected(()->db.addGameWithLineup(1,"A","B",squads.get(0),squads.get(1)));}
    public void testGoalOwnAssistCardsAndDraftLock(){long id=match();List<DemoDb.Player> a=db.roster(id,0),b=db.roster(id,1);long scorer=a.get(0).id,assist=a.get(1).id;int before=db.playerGoals(scorer);db.addGoal(id,scorer,0,12,assist,false);db.addGoal(id,b.get(0).id,0,20,0,true);assertEquals(2,db.goals(id,0));assertEquals(before,db.playerGoals(scorer));rejected(()->db.addGoal(id,999,0,2));rejected(()->db.addGoal(id,scorer,0,2,scorer,false));rejected(()->db.addGoal(id,b.get(0).id,0,2,assist,true));db.addCard(id,scorer,0,"YELLOW",18);assertEquals(1,db.cardCount(id,0,"YELLOW"));DemoDb.Goal g=db.goals(id).get(0);db.editGoal(g.id,id,scorer,0,13,assist,false);db.submit(id);rejected(()->db.addGoal(id,scorer,0,35));rejected(()->db.deleteGoal(g.id,id));String p=db.generatePin(id,0),q=db.generatePin(id,1);assertTrue(p.matches("[0-9]{4}"));assertFalse(db.verifyPin(id,0,p.substring(0,3)));assertTrue(db.verifyPin(id,0,p));assertFalse(db.verifyPin(id,0,p));assertEquals("PENDING",db.game(id).status);assertTrue(db.verifyPin(id,1,q));assertEquals("DEMO_APPROVED",db.game(id).status);assertEquals(before+1,db.playerGoals(scorer));assertEquals(1,db.playerMatches(scorer));assertEquals(1,db.playerAssists(assist));assertEquals(2,db.goals(id,0));rejected(()->db.reopen(id));}
    public void testCorrectionInvalidatesPins(){long id=match();db.submit(id);String pin=db.generatePin(id,0);assertTrue(db.verifyPin(id,0,pin));db.reopen(id);assertFalse(db.approved(id,0));assertEquals("DRAFT",db.game(id).status);db.submit(id);assertFalse(db.verifyPin(id,0,pin));}
    public void testPinAttemptsAndExpiry(){long id=match();db.submit(id);String pin=db.generatePin(id,0),wrong=pin.equals("0000")?"1111":"0000";for(int i=0;i<3;i++)assertFalse(db.verifyPin(id,0,wrong));assertFalse(db.verifyPin(id,0,pin));pin=db.generatePin(id,0);db.getWritableDatabase().execSQL("UPDATE approvals SET expires=0 WHERE game_id="+id);assertFalse(db.verifyPin(id,0,pin));}
    public void testUpgradePreservesV2Data(){long id=match();db.addGoal(id,db.roster(id,0).get(0).id,0,4);SQLiteDatabase d=db.getWritableDatabase();d.execSQL("DROP TABLE saved_draws");d.execSQL("DROP TABLE audit");d.setVersion(2);int people=db.players().size();db.close();db=new DemoDb(context);assertEquals(people,db.players().size());assertEquals(1,db.goals(id,0));assertEquals(8,db.confirmedCount(1));assertEquals(4,db.getReadableDatabase().getVersion());assertTrue(db.savedDraw(1).isEmpty());}
    public void testSubstitutionKeepsParticipationAndCanUndo(){
        for(int i=1;i<=9;i++)db.markAttendance(1,i,DemoDb.CONFIRMED);
        List<DemoDb.Player> p=db.confirmedPlayers(1);
        List<DemoDb.Player> teamA=new ArrayList<>(p.subList(0,4));
        List<DemoDb.Player> teamB=new ArrayList<>(p.subList(4,8));
        DemoDb.Player reserve=p.get(8),out=teamA.get(0);
        long game=db.addGameWithLineup(1,"Equipe A","Equipe B",teamA,teamB);
        db.substitute(game,0,out.id,reserve.id,18);
        assertEquals(1,db.substitutions(game).size());
        assertFalse(db.roster(game,0).stream().anyMatch(x->x.id==out.id));
        assertTrue(db.roster(game,0).stream().anyMatch(x->x.id==reserve.id));
        db.undoLastSubstitution(game);
        assertTrue(db.substitutions(game).isEmpty());
        assertTrue(db.roster(game,0).stream().anyMatch(x->x.id==out.id));
        assertFalse(db.roster(game,0).stream().anyMatch(x->x.id==reserve.id));
        db.substitute(game,0,out.id,reserve.id,19);
        db.addGoal(game,reserve.id,0,22);
        rejected(()->db.undoLastSubstitution(game));
        db.submit(game);
        String pinA=db.generatePin(game,0),pinB=db.generatePin(game,1);
        assertTrue(db.verifyPin(game,0,pinA));
        assertTrue(db.verifyPin(game,1,pinB));
        assertTrue(db.playerHistory(out.id).stream().anyMatch(m->m.id==game));
        assertTrue(db.playerHistory(reserve.id).stream().anyMatch(m->m.id==game));
        assertEquals(1,db.playerMatches(out.id));
        assertEquals(1,db.playerMatches(reserve.id));
    }
    public void testSessionArchivePreservesGames(){long id=match(),previous=db.activeSession(1);db.saveDraw(1,teams());db.newSession(1);assertTrue(previous!=db.activeSession(1));assertEquals(0,db.confirmedCount(1));assertTrue(db.savedDraw(1).isEmpty());assertNotNull(db.game(id));assertEquals(2,db.sessions(1).size());}
}
