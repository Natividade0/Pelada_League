package br.com.peladaleague.app;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Android host: navigation, lifecycle and state. Screens and rules live separately. */
public class MainActivity extends Activity {
    DemoDb db; LocalFinance finance; LocalMatchClock matchClock; CloudAuth auth; LinearLayout page; ScrollView scroll; LinearLayout shell;
    String route="welcome"; long groupId=1,gameId=0,playerId=1;
    int groupTab,drawTab,matchTab,leagueTab,profileTab,teamCount=2;
    boolean balanced=true,drawBusy=false,revealTeams=false,highlightScore=false;
    final Set<Long> selected=new LinkedHashSet<>(); String selectionFingerprint="";
    final Deque<String> history=new ArrayDeque<>(); final Map<String,Integer> positions=new HashMap<>();
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final Map<String,View> tutorialTargets=new HashMap<>();
    Onboarding onboarding;
    final OnlineEventsScreen.State onlineState=new OnlineEventsScreen.State();
    <T extends View> T mark(String name,T v){tutorialTargets.put(name,v);return v;}
    String role="Organizador"; int shirtNumber=0;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);db=new DemoDb(this);finance=new LocalFinance(this,db);matchClock=new LocalMatchClock(this);auth=new CloudAuth(this);onboarding=new Onboarding(this);
        android.content.SharedPreferences pref=getPreferences(0);
        groupId=pref.getLong("group",1);playerId=pref.getLong("player",1);shirtNumber=pref.getInt("shirt",0);role=pref.getString("role","Organizador");
        if(state!=null){groupId=state.getLong("group",groupId);gameId=state.getLong("game");route=state.getString("route","home");groupTab=state.getInt("groupTab");drawTab=state.getInt("drawTab");matchTab=state.getInt("matchTab");leagueTab=state.getInt("leagueTab");profileTab=state.getInt("profileTab");teamCount=state.getInt("teamCount",2);balanced=state.getBoolean("balanced",true);selectionFingerprint=state.getString("fingerprint","");long[] ids=state.getLongArray("selected");if(ids!=null)for(long id:ids)selected.add(id);ArrayList<String> h=state.getStringArrayList("history");if(h!=null)history.addAll(h);Bundle pos=state.getBundle("positions");if(pos!=null)for(String k:pos.keySet())positions.put(k,pos.getInt(k));}
        else route=pref.getBoolean("entered",false)?"home":"welcome";
        acceptInvite(getIntent());
        getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        render(false);
    }
    private void acceptInvite(Intent intent){
        if(intent==null||!Intent.ACTION_VIEW.equals(intent.getAction()))return;
        android.net.Uri uri=intent.getData();
        if(uri==null||!"peladaleague".equals(uri.getScheme())||!"convite".equals(uri.getHost()))return;
        String code=uri.getQueryParameter("code");
        if(code!=null&&code.matches("[a-fA-F0-9]{32}")){
            onlineState.code=code.toLowerCase(Locale.ROOT);
            onlineState.preview=null;route="online";
        }
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);acceptInvite(intent);render(false);
    }
    @Override protected void onSaveInstanceState(Bundle out){rememberPosition();out.putString("route",route);out.putLong("group",groupId);out.putLong("game",gameId);out.putInt("groupTab",groupTab);out.putInt("drawTab",drawTab);out.putInt("matchTab",matchTab);out.putInt("leagueTab",leagueTab);out.putInt("profileTab",profileTab);out.putInt("teamCount",teamCount);out.putBoolean("balanced",balanced);out.putString("fingerprint",selectionFingerprint);out.putLongArray("selected",selected.stream().mapToLong(Long::longValue).toArray());out.putStringArrayList("history",new ArrayList<>(history));Bundle pos=new Bundle();for(Map.Entry<String,Integer> e:positions.entrySet())pos.putInt(e.getKey(),e.getValue());out.putBundle("positions",pos);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){worker.shutdownNow();matchClock.close();finance.close();db.close();super.onDestroy();}
    void rememberPosition(){if(scroll!=null)positions.put(route+":"+groupId,scroll.getScrollY());}
    void open(String target){rememberPosition();if(!route.equals(target))history.push(route);route=target;render(true);}
    void tab(String target){rememberPosition();history.clear();route=target;render(true);}
    void refresh(){rememberPosition();render(false);}
    void selectGroup(long id){groupId=id;groupTab=0;selected.clear();selectionFingerprint="";getPreferences(0).edit().putLong("group",id).apply();open("group");}
    void openGame(DemoDb.Game game){gameId=game.id;groupId=game.groupId;matchTab=0;open("PENDING".equals(game.status)?"approve":"match");}
    void back(){if(onboarding!=null&&onboarding.active()){onboarding.skip();return;}if(!history.isEmpty()){rememberPosition();route=history.pop();render(true);}else if(!route.equals("home")&&!route.equals("welcome")){route="home";render(true);}else finish();}
    @Override public void onBackPressed(){back();}
    void render(boolean animate){
        if(isFinishing()||isDestroyed())return;
        tutorialTargets.clear();
        shell=Ui.col(this);shell.setBackground(new ArenaBackdrop());
        shell.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(shell);shell.requestApplyInsets();
        if(route.equals("welcome")){scroll=null;new WelcomeScreen(this).render(shell);return;}
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(false);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        page=Ui.col(this);page.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,20));scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        switch(route){
            case "home":new HomeScreen(this).render();break;
            case "groups":case "group":case "attendance":new GroupScreen(this).render();break;
            case "draw":new DrawScreen(this).render();break;
            case "match":new MatchScreen(this).render();break;
            case "approve":new ApprovalScreen(this).render();break;
            case "league":new LeagueScreen(this).render();break;
            case "profile":new ProfileScreen(this).render();break;
            case "finance":new FinanceScreen(this).render();break;
            case "account":new AccountScreen(this).render();break;
            case "online":new OnlineEventsScreen(this).render();break;
            default:route="home";new HomeScreen(this).render();
        }
        if(Arrays.asList("home","groups","league","profile").contains(route))bottom();
        int y=positions.getOrDefault(route+":"+groupId,0);scroll.post(()->{if(scroll!=null)scroll.scrollTo(0,y);});
        if(animate)Motion.arrive(page,true);
        if(onboarding!=null)onboarding.afterRender();
    }
    /** Fixed navigation dock: every icon and label shares the same centered tab column. */
    void bottom(){
        LinearLayout bar=Ui.row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(Ui.round(this,0xFF121517,18,0xFF3C392B));
        bar.setPadding(Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6));
        String[] routes={"home","groups","league","profile"};
        String[] labels={"Início","Peladas","Ligas","Perfil"};
        String[] icons={"home","users","trophy","profile"};
        for(int i=0;i<routes.length;i++){
            final String target=routes[i];
            boolean active=route.equals(target);
            LinearLayout item=Ui.col(this);
            item.setGravity(Gravity.CENTER);
            item.setMinimumHeight(Ui.dp(this,62));
            item.setPadding(Ui.dp(this,2),Ui.dp(this,5),Ui.dp(this,2),Ui.dp(this,4));
            Ui.Icon icon=new Ui.Icon(this,icons[i],active?Ui.GOLD:Ui.MUTED);
            LinearLayout.LayoutParams iconParams=Ui.lp(this,24,24);
            iconParams.gravity=Gravity.CENTER_HORIZONTAL;
            item.addView(icon,iconParams);
            Ui.space(item,5);
            TextView label=Ui.t(this,labels[i],11,active?Ui.GOLD:Ui.MUTED,active);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.addView(label,new LinearLayout.LayoutParams(-1,-2));
            item.setContentDescription(labels[i]+(active?", selecionado":""));
            Ui.tappable(item,()->tab(target));
            bar.addView(item,new LinearLayout.LayoutParams(0,Ui.dp(this,66),1));
        }
        LinearLayout.LayoutParams dockParams=Ui.lp(this,-1,-2);
        dockParams.setMargins(Ui.dp(this,10),0,Ui.dp(this,10),Ui.dp(this,8));
        shell.addView(bar,dockParams);
    }
    DemoDb.Player me(){for(DemoDb.Player p:db.players())if(p.id==playerId)return p;return db.players().get(0);}
    boolean canManage(){return role.equals("Organizador")||role.equals("Administrador");}
    boolean canRecord(){return canManage()||role.equals("Mesário / árbitro");}
    boolean canApprove(){return canManage()||role.equals("Capitão");}
    void requireManage(Runnable action){if(canManage())action.run();else toast("Esta ação é do organizador da pelada.");}
    void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    void confirm(String title,String message,Runnable yes){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->yes.run()).show();}
    void safely(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){toast(e.getMessage()==null?"Não foi possível concluir. Confira os dados.":e.getMessage());}}
    void info(){new AlertDialog.Builder(this).setTitle("Pelada League · 0.4")
        .setMessage("Versão local em desenvolvimento. Os dados ficam neste aparelho.\\n\\n"
            +"Ainda não existem contas autenticadas, sincronização entre celulares ou aprovação oficial. "
            +"Os códigos de 4 dígitos testam apenas o fluxo local.\\n\\n"
            +"Modo atual: "+role+". Nenhuma compra ou assinatura é necessária.")
        .setPositiveButton("Entendi",null)
        .setNeutralButton("Ver tutorial",(d,w)->{tab("home");onboarding.start();})
        .setNegativeButton("Perfil de teste",(d,w)->chooseRole()).show();}
    void chooseRole(){String[] roles={"Jogador","Capitão","Mesário / árbitro","Organizador","Administrador"};new AlertDialog.Builder(this).setTitle("Simular permissões locais").setItems(roles,(d,i)->{role=roles[i];getPreferences(0).edit().putString("role",role).apply();refresh();}).setNegativeButton("Voltar",null).show();}
    String status(String s){switch(s){case DemoDb.CONFIRMED:return "Confirmado";case DemoDb.WAITLIST:return "Na espera";case DemoDb.DECLINED:return "Ausente";default:return "Pendente";}}
    String teamName(int i){return new String[]{"Time Amarelo","Time Preto","Time Branco","Time Cinza"}[i];}
    int teamColor(String name){return name.contains("Amarelo")?Ui.GOLD:name.contains("Branco")?0xFFEAEAEA:name.contains("Cinza")?0xFF999CA3:0xFF60676F;}
}
