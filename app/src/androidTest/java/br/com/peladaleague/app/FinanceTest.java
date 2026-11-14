package br.com.peladaleague.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import java.util.List;

/** Independent SQLite tests; never touch a user's local finance data. */
public final class FinanceTest extends InstrumentationTestCase {
    private DemoDb db;
    private LocalFinance finance;
    private Context sandbox;
    @Override protected void setUp()throws Exception {
        super.setUp();
        Context base=getInstrumentation().getTargetContext();
        sandbox=new ContextWrapper(base){
            @Override public java.io.File getDatabasePath(String name){return super.getDatabasePath("qa_fin_"+name);}
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){
                return super.openOrCreateDatabase("qa_fin_"+name,mode,factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler handler){
                return super.openOrCreateDatabase("qa_fin_"+name,mode,factory,handler);
            }
        };
        base.deleteDatabase("qa_fin_pelada_league_demo.db");
        base.deleteDatabase("qa_fin_pelada_league_finance.db");
        db=new DemoDb(sandbox);db.getWritableDatabase();
        finance=new LocalFinance(sandbox,db);finance.getWritableDatabase();
    }
    @Override protected void tearDown()throws Exception{
        finance.close();db.close();
        Context base=getInstrumentation().getTargetContext();
        base.deleteDatabase("qa_fin_pelada_league_demo.db");
        base.deleteDatabase("qa_fin_pelada_league_finance.db");
        super.tearDown();
    }
    private DemoDb.Player player(long id){
        for(DemoDb.Player p:db.players())if(p.id==id)return p;
        throw new AssertionError("Fixture missing player "+id);
    }
    private void confirm(){for(int id:new int[]{1,2,3,4})db.markAttendance(1,id,DemoDb.CONFIRMED);}
    public void testKeeperFeePartialPaymentsAndAudit(){
        confirm();
        finance.save(1,2000,7000,true,"chave-pix-teste");
        assertTrue(finance.isExempt(1,player(4)));
        assertEquals(0,finance.due(1,player(4)));
        assertEquals(6000,finance.summary(1).expected);
        finance.setTotalPaid(1,player(1),1000,"PIX");
        finance.setTotalPaid(1,player(2),2000,"DINHEIRO");
        LocalFinance.Summary s=finance.summary(1);
        assertEquals(3000,s.received);
        assertEquals(3000,s.pending);
        assertEquals(1,s.paid);
        assertEquals(1,s.waived);
        assertEquals("PARCIAL",finance.paymentStatus(1,player(1)));
        assertEquals("PAGO",finance.paymentStatus(1,player(2)));
        finance.setTotalPaid(1,player(1),500,"AJUSTE");
        assertEquals(500,finance.paid(1,1));
        assertEquals("-500",finance.ledger(1,10).get(0)[1]);
        finance.close();finance=new LocalFinance(sandbox,db);
        assertEquals(500,finance.paid(1,1));
        assertEquals("chave-pix-teste",finance.config(1).pixKey);
    }
    public void testKeeperOverrideAndNewSessionIsolation(){
        confirm();finance.save(1,2000,5000,true,"");
        finance.setPosition(1,4,"ATA");
        assertFalse(finance.isExempt(1,player(4)));
        assertEquals(8000,finance.summary(1).expected);
        finance.setExemption(1,4,true);
        assertTrue(finance.isExempt(1,player(4)));
        finance.setExemption(1,1,true);
        assertEquals(4000,finance.summary(1).expected);
        finance.setExemption(1,1,null);
        assertEquals(6000,finance.summary(1).expected);
        finance.setTotalPaid(1,player(2),2000,"PIX");
        db.newSession(1);
        assertEquals(0,finance.config(1).fee);
        assertEquals(0,finance.paid(1,2));
        assertTrue(finance.ledger(1,10).isEmpty());
        assertEquals(0,finance.summary(1).people);
    }
    public void testRejectOverpaymentsAndNonMembers(){
        confirm();finance.save(1,2000,4000,true,"");
        try{finance.setTotalPaid(1,player(4),100,"PIX");fail("keeper payment accepted");}
        catch(IllegalArgumentException expected){}
        try{finance.setTotalPaid(1,player(1),2500,"PIX");fail("overpayment accepted");}
        catch(IllegalArgumentException expected){}
        long outsider=db.addPlayer("Convidado","ATA");
        db.addGroup("Outra pelada","Quadra","Sábado");
        // A group that does not contain the original player must not be charged in that group's session.
        try{finance.setTotalPaid(2,player(1),500,"PIX");fail("non-member accepted");}
        catch(IllegalArgumentException expected){}
        assertEquals(0,finance.paid(1,1));
    }
}
