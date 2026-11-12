package com.tropicalisima.estereo;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;

public final class MainActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private ThemeManager.Season season;
    private TextView status,playAction,volumeLabel,seasonLabel;
    private GlowPlay glowPlay;
    private Equalizer equalizer;
    private SeekBar volume;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private final Player.Listener listener=new Player.Listener(){
        @Override public void onEvents(Player player,Player.Events events){
            runOnUiThread(()->refreshPlayback());
        }
    };
    private boolean changingVolume=false;
    private final Runnable checkTheme=new Runnable(){@Override public void run(){
        ThemeManager.Season updated=ThemeManager.current(MainActivity.this);
        if(season!=null&&!season.id.equals(updated.id)){
            season=updated;buildScreen();refreshPlayback();
        }
        handler.postDelayed(this,60000);
    }};

    @Override public void onCreate(Bundle b){super.onCreate(b);season=ThemeManager.current(this);buildScreen();showIntro();}
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(checkTheme);handler.post(checkTheme);refreshPlayback();}
    @Override protected void onPause(){handler.removeCallbacks(checkTheme);super.onPause();}
    @Override protected void onStart(){super.onStart();connectPlayer();}
    @Override protected void onStop(){
        if(controller!=null){controller.removeListener(listener);controller=null;}
        if(controllerFuture!=null){MediaController.releaseFuture(controllerFuture);controllerFuture=null;}
        super.onStop();
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}

    private void connectPlayer(){
        SessionToken token=new SessionToken(this,new ComponentName(this,RadioPlaybackService.class));
        controllerFuture=new MediaController.Builder(this,token).buildAsync();
        final ListenableFuture<MediaController> pending=controllerFuture;
        pending.addListener(()->{
            if(isFinishing()||isDestroyed()||pending!=controllerFuture)return;
            try{controller=pending.get();controller.addListener(listener);refreshPlayback();}
            catch(Exception e){Toast.makeText(this,"No se pudo iniciar el reproductor. Revisa la conexión.",Toast.LENGTH_LONG).show();}
        },command->runOnUiThread(command));
    }
    private void buildScreen(){
        root=Ui.frame(this,season.accent);setContentView(root);
        LinearLayout col=Ui.scrollContent(this,root);
        TextView header=Ui.text(this,"●  RADIO EN VIVO  ●",14,Ui.GOLD,true);
        header.setGravity(Gravity.CENTER);Ui.add(col,header,6);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.logo_original);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Logotipo oficial TROPICALISIMA ESTÉREO");
        LinearLayout.LayoutParams imgP=new LinearLayout.LayoutParams(-1,Ui.dp(this,235));imgP.topMargin=Ui.dp(this,10);
        col.addView(logo,imgP);
        Ui.add(col,Ui.heading(this,"LA QUE TE PONE A GOZAR"),0);
        seasonLabel=Ui.subtitle(this,"✦  "+season.name+"  ✦");seasonLabel.setTextColor(season.accent);
        Ui.add(col,seasonLabel,7);

        LinearLayout playerCard=Ui.card(this);Ui.add(col,playerCard,18);
        status=Ui.text(this,"LISTA PARA ESCUCHAR",17,Ui.WHITE,true);status.setGravity(Gravity.CENTER);
        Ui.add(playerCard,status,0);
        glowPlay=new GlowPlay(this,season.accent,season.second);
        LinearLayout.LayoutParams playP=new LinearLayout.LayoutParams(Ui.dp(this,166),Ui.dp(this,166));
        playP.gravity=Gravity.CENTER;playP.topMargin=Ui.dp(this,12);
        playerCard.addView(glowPlay,playP);
        glowPlay.setContentDescription("Reproducir la emisora");glowPlay.setOnClickListener(v->togglePlayback());
        playAction=Ui.action(this,"▶  ESCUCHAR EN VIVO",Ui.PINK,Ui.ORANGE,this::togglePlayback);
        Ui.add(playerCard,playAction,10);
        equalizer=new Equalizer(this);LinearLayout.LayoutParams eqp=new LinearLayout.LayoutParams(-1,Ui.dp(this,35));
        eqp.topMargin=Ui.dp(this,14);playerCard.addView(equalizer,eqp);
        TextView volHeading=Ui.text(this,"VOLUMEN DE LA RADIO",16,Ui.WHITE,true);
        volHeading.setGravity(Gravity.CENTER);Ui.add(playerCard,volHeading,8);
        LinearLayout volRow=Ui.row(this);Ui.add(playerCard,volRow,2);
        TextView minus=volumeButton("−");volRow.addView(minus,new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,54)));
        volume=new SeekBar(this);volume.setMax(100);volume.setProgress(50);
        volume.setProgressTintList(ColorStateList.valueOf(Ui.CYAN));
        volume.setThumbTintList(ColorStateList.valueOf(Ui.GOLD));
        volume.setProgressBackgroundTintList(ColorStateList.valueOf(0xff4b4764));
        volRow.addView(volume,new LinearLayout.LayoutParams(0,Ui.dp(this,53),1));
        TextView plus=volumeButton("+");volRow.addView(plus,new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,54)));
        minus.setOnClickListener(v->adjustVolume(-10));plus.setOnClickListener(v->adjustVolume(10));
        volumeLabel=Ui.text(this,"50 %",16,Ui.GOLD,true);volumeLabel.setGravity(Gravity.CENTER);
        Ui.add(playerCard,volumeLabel,0);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int p,boolean user){
                if(user)applyVolume(p);
            }
            @Override public void onStartTrackingTouch(SeekBar b){changingVolume=true;}
            @Override public void onStopTrackingTouch(SeekBar b){changingVolume=false;refreshPlayback();}
        });

        Ui.add(col,Ui.heading(this,"CONÉCTATE CON NOSOTROS"),25);
        Ui.add(col,Ui.subtitle(this,"Todo a un toque. Sin menús complicados."),3);
        LinearLayout actions1=Ui.row(this);Ui.add(col,actions1,15);
        addActionTile(actions1,"✉","TU VOZ EN\nLA RADIO",Ui.CYAN,()->startActivity(new Intent(this,VozActivity.class)));
        addActionTile(actions1,"♫","PIDE TU\nCANCIÓN",Ui.PINK,()->startActivity(new Intent(this,PideCancionActivity.class)));
        LinearLayout actions2=Ui.row(this);Ui.add(col,actions2,10);
        addActionTile(actions2,"☎","WHATSAPP",0xff35e6a0,()->openUrl("https://wa.me/573011835350"));
        addActionTile(actions2,"↗","COMPARTIR",Ui.ORANGE,this::shareRadio);

        LinearLayout themeCard=Ui.card(this);Ui.add(col,themeCard,18);
        TextView t=Ui.text(this,"✦  TROPICALISIMA VIVA",19,Ui.GOLD,true);Ui.add(themeCard,t,0);
        Ui.add(themeCard,Ui.text(this,"Ambientes que cambian con las fechas de Colombia, sin mover los botones ni cortar la música.",14,Ui.MUTE,false),6);
        Switch automatic=new Switch(this);automatic.setText("Ambientes automáticos");
        automatic.setTextColor(Ui.WHITE);automatic.setTextSize(16);
        automatic.setChecked(ThemeManager.automatic(this));automatic.setMinHeight(Ui.dp(this,54));
        automatic.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.CYAN,Ui.MUTE}));
        automatic.setOnCheckedChangeListener((b,checked)->{
            ThemeManager.setAutomatic(this,checked);season=ThemeManager.current(this);buildScreen();refreshPlayback();
        });
        Ui.add(themeCard,automatic,10);
        TextView web=Ui.text(this,"VISITA NUESTRA PÁGINA OFICIAL  ↗",15,Ui.CYAN,true);
        web.setGravity(Gravity.CENTER);web.setPadding(8,Ui.dp(this,15),8,Ui.dp(this,15));
        web.setOnClickListener(v->openUrl("https://tropicalisimaonline.vercel.app/"));
        Ui.add(col,web,10);
        TextView footer=Ui.subtitle(this,"TROPICALISIMA ESTÉREO  •  COLOMBIA\nLA QUE TE PONE A GOZAR");
        Ui.add(col,footer,7);
    }
    private TextView volumeButton(String label){
        TextView b=Ui.text(this,label,32,Ui.WHITE,true);b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.solid(this,0xff2d2446,17,Ui.CYAN));
        b.setContentDescription(label.equals("+")?"Subir volumen":"Bajar volumen");
        return b;
    }
    private void addActionTile(LinearLayout row,String icon,String label,int accent,Runnable action){
        TextView tile=Ui.nav(this,icon,label,accent,action);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.leftMargin=Ui.dp(this,4);p.rightMargin=Ui.dp(this,4);row.addView(tile,p);
    }
    private void togglePlayback(){
        if(controller==null){Toast.makeText(this,"Conectando el reproductor…",Toast.LENGTH_SHORT).show();return;}
        if(controller.getPlayWhenReady()){controller.pause();}
        else {if(controller.getPlaybackState()==Player.STATE_IDLE||controller.getPlaybackState()==Player.STATE_ENDED)controller.prepare();controller.play();}
        refreshPlayback();
    }
    private void adjustVolume(int delta){int now=controller!=null?Math.round(controller.getVolume()*100):volume.getProgress();applyVolume(Math.max(0,Math.min(100,now+delta)));}
    private void applyVolume(int percent){
        if(controller!=null)controller.setVolume(percent/100f);
        if(volume!=null&&volume.getProgress()!=percent)volume.setProgress(percent);
        if(volumeLabel!=null)volumeLabel.setText(percent+" %");
    }
    private void refreshPlayback(){
        if(status==null)return;
        boolean ready=controller!=null,shouldPlay=ready&&controller.getPlayWhenReady();
        boolean playing=ready&&controller.isPlaying();
        boolean buffering=shouldPlay&&!playing;
        status.setText(ready?(playing?"●  AL AIRE · SONANDO":buffering?"◌  CONECTANDO LA SEÑAL…":"LISTA PARA ESCUCHAR"):
                "PREPARANDO REPRODUCTOR…");
        status.setTextColor(playing?Ui.CYAN:Ui.WHITE);
        playAction.setText(shouldPlay?"Ⅱ  PAUSAR RADIO":"▶  ESCUCHAR EN VIVO");
        glowPlay.setPlaying(shouldPlay);equalizer.setPlaying(playing);
        glowPlay.setContentDescription(shouldPlay?"Pausar la radio":"Escuchar la radio en vivo");
        if(ready&&!changingVolume){int v=Math.round(controller.getVolume()*100);volume.setProgress(v);volumeLabel.setText(v+" %");}
    }
    private void openUrl(String link){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)));}
        catch(Exception e){Toast.makeText(this,"No se pudo abrir el enlace.",Toast.LENGTH_SHORT).show();}}
    private void shareRadio(){
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,"¡Escucha TROPICALISIMA ESTÉREO, la que te pone a gozar! 📻 https://tropicalisimaonline.vercel.app/");
        startActivity(Intent.createChooser(send,"Comparte la emisora"));
    }
    private void showIntro(){
        boolean first=!ThemeManager.prefs(this).getBoolean("intro_mostrada",false);
        if(!ValueAnimator.areAnimatorsEnabled())return;
        FrameLayout overlay=new FrameLayout(this);overlay.setBackground(Ui.grad(this,0xff09091c,0xff281039,0));
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout panel=Ui.column(this);panel.setGravity(Gravity.CENTER);panel.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),0);
        overlay.addView(panel,new FrameLayout.LayoutParams(-1,-1));
        TextView tagline=Ui.text(this,"◉  LA CABINA ESTÁ ENCENDIENDO",16,Ui.CYAN,true);tagline.setGravity(Gravity.CENTER);
        Ui.add(panel,tagline,0);
        ImageView image=new ImageView(this);image.setImageResource(R.drawable.logo_original);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,Ui.dp(this,315));ilp.topMargin=Ui.dp(this,12);panel.addView(image,ilp);
        TextView txt=Ui.text(this,"¡LA QUE TE PONE A GOZAR!",30,Ui.GOLD,true);txt.setGravity(Gravity.CENTER);Ui.add(panel,txt,14);
        TextView skip=Ui.text(this,"OMITIR INTRO  ›",16,Ui.WHITE,true);skip.setGravity(Gravity.CENTER);
        skip.setPadding(0,Ui.dp(this,24),0,Ui.dp(this,24));Ui.add(panel,skip,15);
        long delay=first?3250:680;
        image.setAlpha(0f);image.setScaleX(.76f);image.setScaleY(.76f);
        image.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(first?1250:430).start();
        txt.setAlpha(0f);txt.animate().alpha(1f).setStartDelay(first?900:260).setDuration(first?950:300).start();
        final boolean[] done={false};
        Runnable exit=()->{
            if(done[0])return;done[0]=true;
            ThemeManager.prefs(this).edit().putBoolean("intro_mostrada",true).apply();
            overlay.animate().alpha(0f).setDuration(320).withEndAction(()->root.removeView(overlay)).start();
        };
        skip.setOnClickListener(v->exit.run());handler.postDelayed(exit,delay);
    }
    static class GlowPlay extends View {
        private final Paint p=new Paint(3);private final int a,b;private boolean playing;
        GlowPlay(Activity c,int a,int b){super(c);this.a=a;this.b=b;setClickable(true);setFocusable(true);}
        void setPlaying(boolean b){playing=b;invalidate();}
        @Override public void onDraw(Canvas c){super.onDraw(c);float s=Math.min(getWidth(),getHeight());float cx=getWidth()/2f,cy=getHeight()/2f,r=s*.42f;
            p.setShader(new SweepGradient(cx,cy,new int[]{a,b,Ui.PINK,Ui.ORANGE,a},null));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.048f);
            c.drawCircle(cx,cy,r,p);p.setShader(null);p.setStyle(Paint.Style.FILL);
            p.setColor(0xff11152f);c.drawCircle(cx,cy,r-s*.044f,p);
            p.setColor(Ui.WHITE);
            if(playing){float bw=s*.075f,hh=s*.16f;c.drawRoundRect(cx-bw*1.6f,cy-hh,cx-bw*.55f,cy+hh,bw*.25f,bw*.25f,p);
                c.drawRoundRect(cx+bw*.55f,cy-hh,cx+bw*1.6f,cy+hh,bw*.25f,bw*.25f,p);
            }else{Path path=new Path();path.moveTo(cx-s*.09f,cy-s*.17f);path.lineTo(cx+s*.19f,cy);
                path.lineTo(cx-s*.09f,cy+s*.17f);path.close();c.drawPath(path,p);}
        }
    }
    static class Equalizer extends View {
        final Paint p=new Paint(3);boolean playing;
        Equalizer(Activity c){super(c);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void setPlaying(boolean play){playing=play;invalidate();}
        @Override public void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight(),unit=w/28f;
            long tick=SystemClock.uptimeMillis();
            for(int i=0;i<23;i++){
                float wave=playing?(float)(.24+.76*Math.abs(Math.sin(tick*.004+i*.75))):(.16f+.12f*(i%4));
                float hh=h*wave,x=(i+2.5f)*unit;
                p.setColor(i%4==0?Ui.PINK:i%3==0?Ui.ORANGE:Ui.CYAN);
                c.drawRoundRect(x,h-hh,x+unit*.55f,h,unit*.22f,unit*.22f,p);
            }
            if(playing&&ValueAnimator.areAnimatorsEnabled())postInvalidateDelayed(120);
        }
    }
}
