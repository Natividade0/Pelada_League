package br.com.peladaleague.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * First-run interactive tour. Anchors are actual native controls, not screenshots.
 * Skipping never changes a player, a match, a group or any SQLite record.
 */
final class Onboarding {
    static final String DONE = "pelada_onboarding_v1_done";

    private static final class Step {
        final String route, anchor, title, description;
        Step(String route, String anchor, String title, String description) {
            this.route=route; this.anchor=anchor; this.title=title; this.description=description;
        }
    }

    /** Six short steps. Reopen any time from the information menu. */
    private static final Step[] STEPS = {
        new Step("home","home_match","Sua próxima partida",
            "Veja horário, local e vagas. Toque no cartão para abrir sua pelada."),
        new Step("home","home_groups","Suas peladas em um lugar",
            "Abra seus grupos, cadastre os amigos e organize os jogos."),
        new Step("group","group_presence","Quem vai jogar?",
            "Confirme os presentes. Se a lista estiver cheia, o aplicativo organiza a espera."),
        new Step("draw","draw_action","Times prontos em segundos",
            "Selecione os confirmados e sorteie equipes. Veja a escalação no campinho."),
        new Step("league","league_header","Classificação e premiações",
            "Acompanhe artilharia, temporadas e resultados registrados."),
        new Step("profile","profile_header","Sua carreira no futebol",
            "Veja seus jogos, gols e histórico. Você pode repetir este guia no menu de informações.")
    };

    private final MainActivity a;
    private int current=-1;
    private FrameLayout overlay;

    Onboarding(MainActivity activity) { a=activity; }
    boolean active() { return current>=0; }

    void afterRender() {
        if(a.route.equals("welcome"))return;
        a.shell.post(() -> {
            if(a.isFinishing() || a.isDestroyed())return;
            if(active()) showCurrent();
            else if(a.route.equals("home") && !a.getPreferences(0).getBoolean(DONE,false))start();
        });
    }

    void start() {
        if(a.isFinishing() || a.isDestroyed())return;
        current=0;
        showCurrent();
    }

    void next() {
        if(!active())return;
        if(current==STEPS.length-1){finish(true);return;}
        current++;
        showCurrent();
    }

    void previous() {
        if(current<=0)return;
        current--;
        showCurrent();
    }

    void skip() {finish(false);}

    private void finish(boolean completed) {
        if(!active())return;
        removeOverlay();
        current=-1;
        a.getPreferences(0).edit().putBoolean(DONE,true).apply();
        a.route="home";
        a.history.clear();
        a.render(false);
        if(completed)a.toast("Tudo pronto! Organize sua primeira pelada.");
    }

    private void removeOverlay() {
        if(overlay!=null && overlay.getParent() instanceof ViewGroup)
            ((ViewGroup)overlay.getParent()).removeView(overlay);
        overlay=null;
    }

    private void showCurrent() {
        if(!active() || a.isFinishing() || a.isDestroyed())return;
        Step s=STEPS[current];
        if(!s.route.equals(a.route)) {
            removeOverlay();
            a.rememberPosition();
            a.route=s.route;
            a.history.clear();
            a.positions.put(a.route+":"+a.groupId,0);
            a.render(false);
            return;
        }
        View target=a.tutorialTargets.get(s.anchor);
        if(target!=null && a.scroll!=null && isDescendant(a.scroll,target)) {
            Rect rect=new Rect();
            target.getDrawingRect(rect);
            a.scroll.offsetDescendantRectToMyCoords(target,rect);
            int desired=Math.max(0,rect.top-Ui.dp(a,65));
            int viewport=a.scroll.getHeight();
            if(rect.bottom-a.scroll.getScrollY()>viewport-Ui.dp(a,40) ||
                    rect.top-a.scroll.getScrollY()<Ui.dp(a,35)) {
                a.scroll.scrollTo(0,desired);
            }
        }
        a.shell.post(() -> mount(s,a.tutorialTargets.get(s.anchor)));
    }

    private boolean isDescendant(ViewGroup parent, View child) {
        View p=child;
        while(p!=null) {
            if(p==parent)return true;
            if(!(p.getParent() instanceof View))break;
            p=(View)p.getParent();
        }
        return false;
    }

    private void mount(Step expected, View target) {
        if(!active() || STEPS[current]!=expected || !a.route.equals(expected.route) ||
                a.isFinishing() || a.isDestroyed())return;
        removeOverlay();
        FrameLayout host=a.findViewById(android.R.id.content);
        if(host==null || host.getWidth()<=0 || host.getHeight()<=0)return;

        overlay=new FrameLayout(a);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setContentDescription("Tutorial interativo do Pelada League");
        host.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

        RectF focus=new RectF();
        if(target!=null && target.isShown()) {
            int[] origin=new int[2], pos=new int[2];
            host.getLocationOnScreen(origin);
            target.getLocationOnScreen(pos);
            float padding=Ui.dp(a,5);
            focus.set(pos[0]-origin[0]-padding,pos[1]-origin[1]-padding,
                pos[0]-origin[0]+target.getWidth()+padding,
                pos[1]-origin[1]+target.getHeight()+padding);
            focus.intersect(Ui.dp(a,2),Ui.dp(a,2),host.getWidth()-Ui.dp(a,2),host.getHeight()-Ui.dp(a,2));
        }
        Spotlight mask=new Spotlight(a,focus);
        overlay.addView(mask,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout tip=Ui.col(a);
        tip.setBackground(Ui.round(a,0xFF121214,20,Ui.GOLD_STROKE));
        tip.setPadding(Ui.dp(a,17),Ui.dp(a,15),Ui.dp(a,17),Ui.dp(a,16));
        tip.setElevation(Ui.dp(a,16));
        TextView count=Ui.t(a,String.format(java.util.Locale.ROOT,"SEU GUIA · %02d / %02d",current+1,STEPS.length),11,Ui.GOLD,true);
        tip.addView(count);
        Ui.space(tip,7);
        tip.addView(Ui.t(a,expected.title,19,Ui.WHITE,true));
        Ui.space(tip,8);
        TextView description=Ui.t(a,expected.description,13,Ui.WHITE,false);
        description.setLineSpacing(Ui.dp(a,4),1f);
        tip.addView(description);
        Ui.space(tip,15);

        LinearLayout controls=Ui.row(a);
        TextView skip=Ui.t(a,"Pular",12,Ui.MUTED,false);
        skip.setGravity(Gravity.CENTER);
        skip.setMinHeight(Ui.dp(a,48));
        Ui.tappable(skip,this::skip);
        controls.addView(skip,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));
        if(current>0) {
            TextView back=Ui.t(a,"Voltar",12,Ui.WHITE,false);
            back.setGravity(Gravity.CENTER);
            back.setMinHeight(Ui.dp(a,48));
            Ui.tappable(back,this::previous);
            controls.addView(back,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));
        }
        TextView next=Ui.button(a,current==STEPS.length-1?"Concluir":"Próximo",true,this::next);
        controls.addView(next,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1.4f));
        tip.addView(controls);

        int side=Ui.dp(a,16), width=host.getWidth()-side*2;
        tip.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(host.getHeight(),View.MeasureSpec.AT_MOST));
        int cardHeight=tip.getMeasuredHeight(), gap=Ui.dp(a,16);
        int top;
        if(focus.isEmpty())top=(host.getHeight()-cardHeight)/2;
        else {
            top=(int)focus.bottom+gap;
            if(top+cardHeight>host.getHeight()-Ui.dp(a,12))
                top=(int)focus.top-cardHeight-gap;
            if(top<Ui.dp(a,12))top=Math.max(Ui.dp(a,12),(host.getHeight()-cardHeight)/2);
        }
        top=Math.max(Ui.dp(a,8),Math.min(top,host.getHeight()-cardHeight-Ui.dp(a,8)));
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(width,-2,Gravity.TOP|Gravity.LEFT);
        params.leftMargin=side;
        params.topMargin=top;
        overlay.addView(tip,params);
        if(Motion.enabled())Motion.reveal(tip,0);
    }

    private static final class Spotlight extends View {
        private final RectF hole;
        private final Paint dim=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint clear=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG);

        Spotlight(MainActivity a,RectF area) {
            super(a);
            hole=new RectF(area);
            dim.setColor(0xE5000000);
            clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            ring.setColor(Ui.GOLD);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(Ui.dp(a,2));
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
            canvas.drawRect(0,0,getWidth(),getHeight(),dim);
            if(!hole.isEmpty())canvas.drawRoundRect(hole,Ui.dp(getContext(),15),Ui.dp(getContext(),15),clear);
            canvas.restoreToCount(layer);
            if(!hole.isEmpty())canvas.drawRoundRect(hole,Ui.dp(getContext(),15),Ui.dp(getContext(),15),ring);
        }
    }
}
