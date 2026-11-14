package br.com.peladaleague.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import android.graphics.Typeface;

/** Pelada League AMOLED / Amber visual system V0.7. Dependency-free native Android. */
public final class Ui {
    // True black for OLED pixels. Amber is warm rather than fluorescent yellow.
    public static final int BG=0xFF000000, SURFACE=0xFF0D0D0F, RAISED=0xFF19191D,
        GOLD=0xFFFFB52E, GOLD_DARK=0xFFE68E16, WHITE=0xFFF6F5F1,
        MUTED=0xFFAAA9AD, BORDER=0xFF2C2B2E, GREEN=0xFF46D99A, RED=0xFFFF6572;
    public static final int GOLD_TINT=0x22FFB52E, GOLD_STROKE=0x55FFB52E;
    private Ui(){}
    public static int dp(Context c,float v){return (int)(v*c.getResources().getDisplayMetrics().density+.5f);}
    public static LinearLayout col(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static LinearLayout.LayoutParams lp(Context c,int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));}
    public static LinearLayout.LayoutParams weight(Context c){return new LinearLayout.LayoutParams(0,-2,1);}
    public static void space(LinearLayout l,int h){View v=new View(l.getContext());l.addView(v,lp(l.getContext(),1,h));}
    public static TextView t(Context c,String content,int sp,int color,boolean bold){TextView v=new TextView(c);v.setText(content);v.setTextColor(color);v.setTextSize(sp);v.setIncludeFontPadding(false);v.setFontFeatureSettings("kern");v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));v.setGravity(Gravity.CENTER_VERTICAL);v.setLetterSpacing(sp>=19?-.015f:0f);return v;}
    public static GradientDrawable round(Context c,int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));if(stroke!=0)d.setStroke(dp(c,1),stroke);return d;}
    public static GradientDrawable gradient(Context c,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF181513,0xFF101011,0xFF09090A});d.setCornerRadius(dp(c,radius));d.setStroke(dp(c,1),BORDER);return d;}
    public static GradientDrawable gold(Context c,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFFFC455,GOLD,0xFFF69D1C});d.setCornerRadius(dp(c,radius));return d;}
    public static Drawable shimmer(Context c,int radius){GradientDrawable inner=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF19191B,0xFF101012,0xFF09090B});inner.setCornerRadius(dp(c,radius));inner.setStroke(dp(c,1),BORDER);return inner;}
    public static LinearLayout card(Context c){LinearLayout l=col(c);l.setBackground(shimmer(c,18));l.setPadding(dp(c,16),dp(c,16),dp(c,16),dp(c,16));return l;}
    public static LinearLayout surface(Context c){LinearLayout l=col(c);l.setBackground(round(c,SURFACE,18,BORDER));l.setPadding(dp(c,16),dp(c,16),dp(c,16),dp(c,16));return l;}
    public static boolean motionEnabled(Context c){try{return Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f;}catch(Exception e){return true;}}
    public static void enter(View view,int dy){if(!motionEnabled(view.getContext()))return;view.setAlpha(0f);view.setTranslationY(dp(view.getContext(),dy));view.animate().alpha(1f).translationY(0f).setInterpolator(new DecelerateInterpolator()).setDuration(300).start();}
    public static void pop(View view,int delay){if(!motionEnabled(view.getContext()))return;view.setScaleX(.91f);view.setScaleY(.91f);view.setAlpha(0f);view.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay).setInterpolator(new OvershootInterpolator(.8f)).setDuration(280).start();}
    public static void press(View view){if(!motionEnabled(view.getContext()))return;view.animate().cancel();view.setScaleX(.965f);view.setScaleY(.965f);view.animate().scaleX(1f).scaleY(1f).setInterpolator(new DecelerateInterpolator()).setDuration(180).start();}
    @android.annotation.SuppressLint("ClickableViewAccessibility") // returning false preserves the framework click and accessibility path
    public static void tappable(View v,Runnable click){
        v.setClickable(true);v.setFocusable(true);
        v.setOnTouchListener((view,event)->{
            if(Motion.enabled()){
                boolean down=event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN;
                if(down||event.getActionMasked()==android.view.MotionEvent.ACTION_UP||event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)
                    view.animate().scaleX(down?.98f:1f).scaleY(down?.98f:1f).setDuration(down?120:160).start();
            }return false;
        });
        v.setOnClickListener(x->{v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);click.run();});
    }
    public static TextView button(Context c,String title,boolean filled,Runnable click){
        TextView v=new NeonButton(c,title,filled);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(c,52));
        v.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
        // Animated, accessible BLACK ink response on amber. No constant running neon.
        Drawable base=filled?gold(c,15):round(c,SURFACE,15,GOLD_STROKE);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(filled?0x66000000:GOLD_TINT),base,null));
        tappable(v,click);return v;
    }
    public static TextView chip(Context c,String text,boolean selected,Runnable click){
        TextView v=t(c,text,12,selected?BG:MUTED,selected);v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(c,46));v.setPadding(dp(c,9),dp(c,8),dp(c,9),dp(c,8));
        Drawable base=selected?gold(c,13):round(c,SURFACE,13,BORDER);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(selected?0x44000000:GOLD_TINT),base,null));
        if(click!=null)tappable(v,click);return v;
    }
    public static TextView badge(Context c,String text,boolean selected){TextView v=t(c,text,11,selected?BG:GOLD,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(c,11),dp(c,6),dp(c,11),dp(c,6));v.setBackground(selected?gold(c,20):round(c,GOLD_TINT,20,GOLD_STROKE));return v;}
    public static void heading(LinearLayout parent,String text){TextView v=t(parent.getContext(),text,17,WHITE,true);parent.addView(v);space(parent,11);}
    public static void sub(LinearLayout parent,String text){TextView v=t(parent.getContext(),text,12,MUTED,false);parent.addView(v);}
    public static void divider(LinearLayout l){View v=new View(l.getContext());v.setBackgroundColor(BORDER);LinearLayout.LayoutParams p=lp(l.getContext(),-1,1);p.topMargin=dp(l.getContext(),7);p.bottomMargin=dp(l.getContext(),7);l.addView(v,p);}
    public static void put(LinearLayout parent,View child,int top){LinearLayout.LayoutParams p=lp(parent.getContext(),-1,-2);p.topMargin=dp(parent.getContext(),top);parent.addView(child,p);}
    public static EditText input(Context c,String hint){EditText v=new EditText(c);v.setSingleLine(true);v.setTextSize(15);v.setMinHeight(dp(c,52));v.setTextColor(WHITE);v.setHintTextColor(MUTED);v.setHint(hint);v.setPadding(dp(c,14),dp(c,11),dp(c,14),dp(c,11));v.setBackground(round(c,RAISED,13,BORDER));return v;}
    public static void imageBackground(View v,int id){v.setBackgroundResource(id);}
    public static ImageView img(Context c,int drawable){ImageView i=new ImageView(c);i.setImageResource(drawable);i.setScaleType(ImageView.ScaleType.FIT_CENTER);return i;}
    public static LinearLayout stat(Context c,String value,String label){LinearLayout l=col(c);l.setGravity(Gravity.CENTER);l.setPadding(dp(c,6),dp(c,16),dp(c,6),dp(c,16));l.setBackground(shimmer(c,15));TextView n=t(c,value,24,GOLD,true);n.setGravity(Gravity.CENTER);TextView d=t(c,label,11,MUTED,false);d.setGravity(Gravity.CENTER);l.addView(n);space(l,6);l.addView(d);return l;}
    public static TextView initials(Context c,String name,int size){String[] tokens=name.trim().split("\\s+");String ini=tokens.length>1?tokens[0].substring(0,1)+tokens[tokens.length-1].substring(0,1):name.substring(0,1);TextView v=t(c,ini.toUpperCase(java.util.Locale.ROOT),size/3,GOLD,true);v.setGravity(Gravity.CENTER);v.setBackground(round(c,0xFF24201A,100,GOLD_STROKE));return v;}
    /** Hand-drawn, scalable sport glyphs: no emoji fonts or asset dependencies. */
    public static final class Icon extends View{
        private final String kind;private final Paint p=new Paint(3);private final int color;private final Picture picture=new Picture();
        public Icon(Context c){this(c,"goal",GOLD);}
        public Icon(Context c,String k,int color){super(c);kind=k;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);Canvas canvas=picture.beginRecording(48,48);drawIcon(canvas);picture.endRecording();}
        private void fill(Canvas c,Path path){p.setStyle(Paint.Style.FILL);c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int save=c.save();float dim=Math.min(getWidth(),getHeight());c.translate((getWidth()-dim)/2f,(getHeight()-dim)/2f);c.scale(dim/48f,dim/48f);c.drawPicture(picture);c.restoreToCount(save);}
        private void drawIcon(Canvas c){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(kind){
                case "home":{Path z=new Path();z.moveTo(8,23);z.lineTo(24,10);z.lineTo(40,23);z.lineTo(40,40);z.lineTo(29,40);z.lineTo(29,29);z.lineTo(19,29);z.lineTo(19,40);z.lineTo(8,40);z.close();c.drawPath(z,p);break;}
                case "users":c.drawCircle(19,17,7,p);c.drawCircle(34,19,5,p);c.drawArc(7,27,31,46,195,150,false,p);c.drawArc(29,28,45,43,215,135,false,p);break;
                case "trophy":c.drawRoundRect(15,10,33,29,4,4,p);c.drawArc(6,14,20,27,85,205,false,p);c.drawArc(28,14,42,27,250,205,false,p);c.drawLine(24,29,24,38,p);c.drawLine(17,38,31,38,p);break;
                case "profile":c.drawCircle(24,16,8,p);c.drawArc(9,27,39,44,180,180,false,p);break;
                case "shuffle":c.drawLine(8,15,15,15,p);c.drawLine(15,15,34,34,p);c.drawLine(34,34,40,34,p);c.drawLine(34,29,40,34,p);c.drawLine(34,39,40,34,p);c.drawLine(8,34,15,34,p);c.drawLine(15,34,34,15,p);c.drawLine(34,15,40,15,p);c.drawLine(34,10,40,15,p);c.drawLine(34,20,40,15,p);break;
                case "stats":c.drawLine(9,40,9,27,p);c.drawLine(19,40,19,18,p);c.drawLine(29,40,29,9,p);c.drawLine(39,40,39,23,p);break;
                case "timer":c.drawCircle(24,26,16,p);c.drawLine(24,26,24,15,p);c.drawLine(24,26,32,30,p);c.drawLine(18,6,30,6,p);c.drawLine(24,10,24,7,p);break;
                case "wallet":c.drawRoundRect(7,14,41,39,5,5,p);c.drawLine(7,20,39,20,p);c.drawRoundRect(28,24,44,34,3,3,p);c.drawCircle(34,29,1.7f,p);break;
                case "calendar":c.drawRoundRect(8,12,40,40,4,4,p);c.drawLine(8,21,40,21,p);c.drawLine(16,8,16,16,p);c.drawLine(32,8,32,16,p);break;
                case "bell":c.drawArc(14,12,34,38,185,170,false,p);c.drawLine(10,38,38,38,p);c.drawLine(21,42,27,42,p);break;
                case "back":c.drawLine(34,24,14,24,p);c.drawLine(14,24,24,14,p);c.drawLine(14,24,24,34,p);break;
                case "check":c.drawLine(10,24,20,34,p);c.drawLine(20,34,39,12,p);break;
                case "plus":c.drawLine(24,10,24,38,p);c.drawLine(10,24,38,24,p);break;
                case "shield":{Path z=new Path();z.moveTo(24,6);z.lineTo(40,12);z.lineTo(37,30);z.quadTo(31,41,24,44);z.quadTo(17,41,11,30);z.lineTo(8,12);z.close();c.drawPath(z,p);break;}
                case "goal":{c.drawCircle(24,24,17,p);Path h=new Path();for(int i=0;i<5;i++){double a=i*2*Math.PI/5-Math.PI/2;float xx=24+(float)Math.cos(a)*8,yy=24+(float)Math.sin(a)*8;if(i==0)h.moveTo(xx,yy);else h.lineTo(xx,yy);}h.close();c.drawPath(h,p);for(int i=0;i<5;i++){double a=i*2*Math.PI/5-Math.PI/2;c.drawLine(24+(float)Math.cos(a)*8,24+(float)Math.sin(a)*8,24+(float)Math.cos(a)*17,24+(float)Math.sin(a)*17,p);}break;}
                case "shirt":{Path z=new Path();z.moveTo(16,9);z.lineTo(8,13);z.lineTo(3,26);z.lineTo(13,30);z.lineTo(16,25);z.lineTo(16,41);z.lineTo(32,41);z.lineTo(32,25);z.lineTo(35,30);z.lineTo(45,26);z.lineTo(40,13);z.lineTo(32,9);z.quadTo(24,17,16,9);z.close();p.setStyle(Paint.Style.FILL);c.drawPath(z,p);p.setColor(0x66000000);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawLine(16,18,16,40,p);break;}
                case "location":{Path z=new Path();z.moveTo(24,43);z.cubicTo(16,31,12,27,12,20);z.cubicTo(12,3,36,3,36,20);z.cubicTo(36,27,31,35,24,43);z.close();c.drawPath(z,p);c.drawCircle(24,20,5,p);break;}
                case "chevron":c.drawLine(19,13,30,24,p);c.drawLine(30,24,19,35,p);break;
                case "whistle":c.drawCircle(17,30,9,p);c.drawRect(18,16,40,26,p);c.drawCircle(33,19,2,p);break;
                case "award":c.drawCircle(24,17,11,p);c.drawLine(17,27,13,41,p);c.drawLine(13,41,24,36,p);c.drawLine(24,36,35,41,p);c.drawLine(35,41,31,27,p);break;
                case "edit":c.drawLine(13,35,33,13,p);c.drawLine(13,35,11,41,p);c.drawLine(11,41,18,39,p);c.drawLine(18,39,38,18,p);c.drawLine(30,16,35,21,p);break;
                case "delete":c.drawRoundRect(14,17,34,40,3,3,p);c.drawLine(10,12,38,12,p);c.drawLine(19,8,29,8,p);c.drawLine(21,23,21,34,p);c.drawLine(27,23,27,34,p);break;
                case "settings":c.drawCircle(24,24,7,p);c.drawCircle(24,24,16,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(24+(float)Math.cos(a)*16,24+(float)Math.sin(a)*16,24+(float)Math.cos(a)*21,24+(float)Math.sin(a)*21,p);}break;
                default:c.drawCircle(24,24,14,p);
            }
        }
    }
    public static LinearLayout action(Context c,String icon,String title,String subtitle,Runnable click){LinearLayout box=row(c);box.setMinimumHeight(dp(c,48));box.setPadding(dp(c,8),dp(c,7),dp(c,6),dp(c,7));Icon v=new Icon(c,icon,GOLD);box.addView(v,lp(c,27,27));LinearLayout texts=col(c);texts.setPadding(dp(c,13),0,0,0);texts.addView(t(c,title,14,WHITE,true));if(subtitle!=null)texts.addView(t(c,subtitle,11,MUTED,false));box.addView(texts,new LinearLayout.LayoutParams(0,-2,1));box.addView(new Icon(c,"chevron",MUTED),lp(c,20,20));tappable(box,click);return box;}
}
