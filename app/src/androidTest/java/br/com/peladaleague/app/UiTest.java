package br.com.peladaleague.app;

import android.test.InstrumentationTestCase;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Real Android rendering and native click navigation. Run on a disposable QA emulator. */
public final class UiTest extends InstrumentationTestCase {
    private MainActivity a;
    private void ui(Runnable r){getInstrumentation().runOnMainSync(r);getInstrumentation().waitForIdleSync();}
    private View text(View v,String label){if(v instanceof TextView&&label.equals(((TextView)v).getText().toString()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=text(g.getChildAt(i),label);if(found!=null)return found;}}return null;}
    private void click(String label){ui(()->{View v=text(a.getWindow().getDecorView(),label);assertNotNull("Missing action: "+label,v);while(v!=null&&!v.isClickable())v=(View)v.getParent();assertNotNull(v);assertTrue(v.performClick());});}
    private void screenshot(String name)throws Exception{getInstrumentation().waitForIdleSync();SystemClock.sleep(900);Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull("Android screenshot",b);File folder=new File(a.getFilesDir(),"qa");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private void top(){ui(()->{if(a.scroll!=null)a.scroll.scrollTo(0,0);});}
    public void testEightScreensAndNavigation()throws Exception{
        Intent intent=new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a=(MainActivity)getInstrumentation().startActivitySync(intent);getInstrumentation().waitForIdleSync();
        ui(()->{a.role="Organizador";a.route="welcome";a.render(false);});screenshot("01-welcome");click("Explorar demonstração");assertEquals("home",a.route);
        ui(()->{
            // A new install shows the tour before ordinary navigation.
            assertTrue("First-run walkthrough missing",a.onboarding.active());
            a.onboarding.skip();
            assertFalse("Skip should dismiss the tour",a.onboarding.active());
            a.onboarding.start();
            assertTrue("Tutorial must be replayable",a.onboarding.active());
            a.onboarding.skip();
        });
        // Known demo fixtures, persisted through the same repository methods as the app.
        ui(()->{a.groupId=1;a.db.setCapacity(1,14);for(int i=1;i<=10;i++)a.db.markAttendance(1,i,DemoDb.CONFIRMED);a.refresh();});top();screenshot("02-home");
        click("Sua próxima partida");assertEquals("group",a.route);top();screenshot("03-group");click("Sortear times");assertEquals("draw",a.route);click("Sortear Times");
        for(int i=0;i<100&&a.drawBusy;i++)SystemClock.sleep(200);assertFalse("Draw worker did not finish",a.drawBusy);getInstrumentation().waitForIdleSync();assertEquals(2,a.db.savedDraw(1).size());top();screenshot("04-draw");
        click("Criar partida com os times");assertEquals("match",a.route);assertTrue(a.db.roster(a.gameId,0).size()>=2);
        ui(()->{List<DemoDb.Player> x=a.db.roster(a.gameId,0),y=a.db.roster(a.gameId,1);a.db.addGoal(a.gameId,x.get(0).id,0,12,x.get(1).id,false);a.db.addGoal(a.gameId,y.get(0).id,1,18,0,false);a.db.addGoal(a.gameId,x.get(1).id,0,24,0,false);a.db.addCard(a.gameId,y.get(1).id,1,"YELLOW",22);a.refresh();});top();screenshot("05-match");
        click("Cartões");assertEquals(1,a.matchTab);click("Gols");assertEquals(0,a.matchTab);
        ui(()->{a.db.submit(a.gameId);a.open("approve");});top();screenshot("06-approval");
        ui(()->{String p=a.db.generatePin(a.gameId,0),q=a.db.generatePin(a.gameId,1);assertTrue(a.db.verifyPin(a.gameId,0,p));assertTrue(a.db.verifyPin(a.gameId,1,q));a.tab("league");});top();screenshot("07-league");
        click("Perfil");assertEquals("profile",a.route);top();screenshot("08-profile");click("Histórico");assertEquals(2,a.profileTab);assertFalse(a.db.playerHistory(1).isEmpty());
        click("Início");assertEquals("home",a.route);click("Minhas peladas");assertEquals("groups",a.route);ui(()->a.open("group"));ui(a::back);assertEquals("groups",a.route);
        // Verify Android's motion-off setting is respected by the actual animation code.
        getInstrumentation().getUiAutomation().executeShellCommand("settings put global animator_duration_scale 0").close();SystemClock.sleep(500);assertFalse(Motion.enabled());ui(()->a.tab("home"));screenshot("09-motion-off");getInstrumentation().getUiAutomation().executeShellCommand("settings put global animator_duration_scale 1").close();
        ui(()->{a.db.close();a.db=new DemoDb(a);assertTrue(a.db.playerHistory(1).size()>0);a.tab("home");});
    }
}
