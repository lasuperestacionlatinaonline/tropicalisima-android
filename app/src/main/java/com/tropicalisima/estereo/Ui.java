package com.tropicalisima.estereo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int DARK=Color.rgb(9,9,27), PANEL=Color.rgb(17,18,43), WHITE=0xfff6f7ff,
            CYAN=0xff00e0ec, PINK=0xffff2c95, ORANGE=0xffffa21e,
            MUTE=0xffbdc9dc, GOLD=0xffffc74a;
    private Ui(){}
    static int dp(Context c,float d){return Math.round(c.getResources().getDisplayMetrics().density*d);}
    static GradientDrawable solid(Context c,int color,float radius,int line){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));
        if(line!=0)d.setStroke(dp(c,1),line);return d;
    }
    static GradientDrawable grad(Context c,int a,int b,float r){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{a,b});
        d.setCornerRadius(dp(c,r));return d;
    }
    static TextView text(Context c,String s,int sp,int color,boolean bold){
        TextView v=new TextView(c);v.setText(s);v.setTextColor(color);v.setTextSize(sp);
        v.setTypeface(Typeface.create(bold?"sans-serif-condensed":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        v.setIncludeFontPadding(true);return v;
    }
    static TextView action(Context c,String s,int a,int b,Runnable call){
        TextView t=text(c,s,17,WHITE,true);t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c,16),dp(c,16),dp(c,16),dp(c,16));
        t.setBackground(grad(c,a,b,18));
        t.setMinHeight(dp(c,56));t.setClickable(true);t.setFocusable(true);
        t.setOnClickListener(v->call.run());return t;
    }
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static LinearLayout card(Context c){
        LinearLayout l=column(c);l.setPadding(dp(c,16),dp(c,17),dp(c,16),dp(c,18));
        l.setBackground(solid(c,PANEL,22,0xff403767));return l;
    }
    static void add(LinearLayout parent, View v, int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(parent.getContext(),top);parent.addView(v,p);
    }
    static FrameLayout frame(Activity a, int accent){
        FrameLayout root=new FrameLayout(a);root.setBackgroundColor(DARK);
        if(Build.VERSION.SDK_INT>=35){
            root.setOnApplyWindowInsetsListener((view,insets)->{
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0,bars.top,0,bars.bottom);
                return insets;
            });
        }
        root.addView(new BackGlow(a,accent,ThemeManager.current(a).id),new FrameLayout.LayoutParams(-1,-1));return root;
    }
    static LinearLayout scrollContent(Activity a,FrameLayout root){
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col=column(a);
        int side=dp(a,19);col.setPadding(side,dp(a,12),side,dp(a,32));
        scroll.addView(col,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));return col;
    }
    static TextView heading(Context c,String s){TextView t=text(c,s,30,WHITE,true);t.setGravity(Gravity.CENTER);return t;}
    static TextView subtitle(Context c,String s){TextView t=text(c,s,15,MUTE,false);t.setGravity(Gravity.CENTER);return t;}
    static TextView nav(Context c,String icon,String label,int accent,Runnable call){
        TextView t=text(c,icon+"\n"+label,17,WHITE,true);t.setGravity(Gravity.CENTER);
        t.setBackground(solid(c,0xff161631,19,accent));
        t.setPadding(dp(c,9),dp(c,14),dp(c,9),dp(c,14));t.setMinHeight(dp(c,98));
        t.setOnClickListener(v->call.run());t.setFocusable(true);t.setClickable(true);
        return t;
    }
    static class BackGlow extends View {
        final int accent;final String theme;final Paint p=new Paint(3);
        BackGlow(Context c,int accent,String theme){super(c);this.accent=accent;this.theme=theme;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();c.drawColor(DARK);
            p.setShader(new RadialGradient(w*.5f,h*.25f,w*.64f,new int[]{(accent&0x00ffffff)|0x66000000,0xff14102c,DARK},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(0x2aff2b91);c.drawCircle(w*1.06f,h*.62f,w*.4f,p);
            p.setColor(0x1600dff0);c.drawCircle(w*.02f,h*.86f,w*.44f,p);
            p.setShader(new LinearGradient(0,h*.93f,w,h,new int[]{0x005ee9ff,0x558e00ff,0x00ff9a00},null,Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(0,h*.93f,w,h*.945f),18,18,p);p.setShader(null);
            // Detalles de temporada livianos: decoran el fondo, nunca tapan ni mueven botones.
            boolean christmas=theme.contains("navidad")||theme.contains("nochebuena")||theme.contains("velitas")||theme.contains("novena")||theme.equals("mes-12");
            boolean carnival=theme.contains("carnaval")||theme.contains("feria")||theme.equals("mes-2");
            boolean love=theme.contains("amor")||theme.contains("valentin")||theme.contains("madres")||theme.equals("mes-9");
            boolean patriotic=theme.contains("colombia")||theme.contains("independencia")||theme.contains("boyaca")||theme.equals("mes-7");
            if(christmas||carnival||love||patriotic){
                for(int i=0;i<16;i++){
                    float x=(i*73%97)/97f*w,y=(i*41%83)/83f*h;
                    int color=christmas?(i%2==0?0x44ffd05a:0x4434e8bd):
                              carnival?(i%2==0?0x44ff36a0:0x44ffae31):
                              love?0x44ff4b9a:(i%2==0?0x44ffce32:0x4447b3ff);
                    p.setColor(color);p.setStrokeWidth(dp(getContext(),2));
                    float r=dp(getContext(),i%3==0?6:4);
                    if(carnival){c.drawLine(x-r,y-r,x+r,y+r,p);}
                    else {c.drawCircle(x,y,r*.34f,p);c.drawLine(x-r,y,x+r,y,p);c.drawLine(x,y-r,x,y+r,p);}
                }
            }
        }
    }
}
