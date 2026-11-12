package com.tropicalisima.estereo;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
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
    private TextView status,playAction,volumeLabel;
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
    // Pantalla principal compacta: la radio y sus acciones ocupan el primer plano.
    // Se conserva desplazamiento en móviles pequeños y con tamaños de letra accesibles.
    private LinearLayout compactContent(){
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);
        LinearLayout col=Ui.column(this);
        // Reserva una zona inferior para que el boton flotante no tape controles.
        col.setPadding(Ui.dp(this,15),Ui.dp(this,8),Ui.dp(this,15),Ui.dp(this,82));
        sc.addView(col,new ScrollView.LayoutParams(-1,-2));
        root.addView(sc,new FrameLayout.LayoutParams(-1,-1));
        return col;
    }
    private void buildScreen(){
        root=Ui.frame(this,season.accent);setContentView(root);
        LinearLayout col=compactContent();
        final int screenDp=getResources().getConfiguration().screenHeightDp;
        final boolean small=screenDp<740;
        TextView header=Ui.text(this,"●  TROPICALISIMA ESTÉREO  ·  EN VIVO",13,Ui.GOLD,true);
        header.setGravity(Gravity.CENTER);Ui.add(col,header,0);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.logo_original);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("TROPICALISIMA ESTÉREO. La que te pone a gozar.");
        LinearLayout.LayoutParams imgP=new LinearLayout.LayoutParams(-1,Ui.dp(this,small?104:135));
        imgP.topMargin=Ui.dp(this,2);
        col.addView(logo,imgP);
        LinearLayout playerCard=Ui.card(this);
        playerCard.setPadding(Ui.dp(this,14),Ui.dp(this,11),Ui.dp(this,14),Ui.dp(this,13));
        Ui.add(col,playerCard,6);
        status=Ui.text(this,"LISTA PARA ESCUCHAR",16,Ui.WHITE,true);
        status.setGravity(Gravity.CENTER);
        Ui.add(playerCard,status,0);
        glowPlay=new GlowPlay(this,season.accent,season.second);
        int playSize=small?104:128;
        LinearLayout.LayoutParams playP=new LinearLayout.LayoutParams(Ui.dp(this,playSize),Ui.dp(this,playSize));
        playP.gravity=Gravity.CENTER;playP.topMargin=Ui.dp(this,5);
        playerCard.addView(glowPlay,playP);
        glowPlay.setContentDescription("Reproducir la emisora");
        glowPlay.setOnClickListener(v->togglePlayback());
        playAction=Ui.action(this,"▶  ESCUCHAR EN VIVO",Ui.PINK,Ui.ORANGE,this::togglePlayback);
        playAction.setMinHeight(Ui.dp(this,50));
        playAction.setPadding(Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10));
        Ui.add(playerCard,playAction,3);
        equalizer=new Equalizer(this);
        LinearLayout.LayoutParams eqp=new LinearLayout.LayoutParams(-1,Ui.dp(this,small?16:23));
        eqp.topMargin=Ui.dp(this,8);playerCard.addView(equalizer,eqp);

        LinearLayout volHeader=Ui.row(this);Ui.add(playerCard,volHeader,6);
        TextView volTitle=Ui.text(this,"VOLUMEN",14,Ui.WHITE,true);
        volHeader.addView(volTitle,new LinearLayout.LayoutParams(0,-2,1));
        volumeLabel=Ui.text(this,"50 %",15,Ui.GOLD,true);
        volHeader.addView(volumeLabel,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout volRow=Ui.row(this);Ui.add(playerCard,volRow,2);
        TextView minus=volumeButton("−");volRow.addView(minus,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        volume=new SeekBar(this);volume.setMax(100);volume.setProgress(50);
        volume.setContentDescription("Ajustar volumen de la emisora");
        volume.setProgressTintList(ColorStateList.valueOf(Ui.CYAN));
        volume.setThumbTintList(ColorStateList.valueOf(Ui.GOLD));
        volume.setProgressBackgroundTintList(ColorStateList.valueOf(0xff4b4764));
        volRow.addView(volume,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        TextView plus=volumeButton("+");volRow.addView(plus,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        minus.setOnClickListener(v->adjustVolume(-10));plus.setOnClickListener(v->adjustVolume(10));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int p,boolean user){if(user)applyVolume(p);}
            @Override public void onStartTrackingTouch(SeekBar b){changingVolume=true;}
            @Override public void onStopTrackingTouch(SeekBar b){changingVolume=false;refreshPlayback();}
        });

        // Un solo acceso protagonista. El FAB queda fuera del ScrollView:
        // permanece visible sin multiplicar botones ni interrumpir la radio.
        addParticipaButton();
    }
    private TextView volumeButton(String label){
        TextView b=Ui.text(this,label,32,Ui.WHITE,true);b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.solid(this,0xff2d2446,17,Ui.CYAN));
        b.setContentDescription(label.equals("+")?"Subir volumen":"Bajar volumen");
        return b;
    }
    /** Boton flotante unico: accesible, visible y separado del volumen. */
    private void addParticipaButton(){
        TextView fab=Ui.text(this,"✦  PARTICIPA",18,Ui.WHITE,true);
        fab.setGravity(Gravity.CENTER);
        fab.setPadding(Ui.dp(this,24),Ui.dp(this,14),Ui.dp(this,24),Ui.dp(this,14));
        fab.setMinHeight(Ui.dp(this,60));
        fab.setMinWidth(Ui.dp(this,174));
        fab.setBackground(Ui.grad(this,Ui.PINK,Ui.ORANGE,26));
        fab.setElevation(Ui.dp(this,9));
        fab.setClickable(true);fab.setFocusable(true);
        fab.setContentDescription("PARTICIPA. Abre saludos, peticiones de canciones y WhatsApp");
        fab.setOnClickListener(v->showParticipaMenu());
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,Ui.dp(this,60),Gravity.END|Gravity.BOTTOM);
        p.setMargins(Ui.dp(this,12),0,Ui.dp(this,17),Ui.dp(this,15));
        root.addView(fab,p);
    }

    /** Panel inferior Android nativo, facil de usar con dedos grandes. */
    private void showParticipaMenu(){
        Dialog dialog=new Dialog(this);
        LinearLayout panel=Ui.column(this);
        panel.setPadding(Ui.dp(this,19),Ui.dp(this,18),Ui.dp(this,19),Ui.dp(this,23));
        panel.setBackground(Ui.solid(this,0xff11152f,26,Ui.PINK));
        TextView head=Ui.text(this,"✦  PARTICIPA EN LA RADIO",22,Ui.GOLD,true);
        head.setGravity(Gravity.CENTER);
        Ui.add(panel,head,0);
        TextView sub=Ui.text(this,"¡Tu música y tu voz hacen parte de TROPICALISIMA!",15,Ui.MUTE,false);
        sub.setGravity(Gravity.CENTER);
        Ui.add(panel,sub,7);
        addParticipaOption(panel,"✉  DEJA TU SALUDO",Ui.CYAN,()->{
            dialog.dismiss();startActivity(new Intent(this,VozActivity.class));
        });
        addParticipaOption(panel,"♫  PIDE TU CANCIÓN",Ui.PINK,()->{
            dialog.dismiss();startActivity(new Intent(this,PideCancionActivity.class));
        });
        addParticipaOption(panel,"☎  WHATSAPP",0xff35e6a0,()->{
            dialog.dismiss();openUrl("https://wa.me/573011835350");
        });
        TextView share=Ui.text(this,"↗  COMPARTIR LA EMISORA",15,Ui.CYAN,true);
        share.setGravity(Gravity.CENTER);
        share.setMinHeight(Ui.dp(this,50));
        share.setClickable(true);share.setFocusable(true);
        share.setOnClickListener(v->{dialog.dismiss();shareRadio();});
        Ui.add(panel,share,8);
        TextView close=Ui.text(this,"CERRAR  ×",15,Ui.MUTE,true);
        close.setGravity(Gravity.CENTER);
        close.setMinHeight(Ui.dp(this,48));
        close.setClickable(true);close.setFocusable(true);
        close.setOnClickListener(v->dialog.dismiss());
        Ui.add(panel,close,2);
        dialog.setContentView(panel);
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.68f);
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    private void addParticipaOption(LinearLayout panel,String label,int color,Runnable action){
        TextView option=Ui.text(this,label,18,Ui.WHITE,true);
        option.setGravity(Gravity.CENTER_VERTICAL);
        option.setPadding(Ui.dp(this,18),Ui.dp(this,10),Ui.dp(this,14),Ui.dp(this,10));
        option.setMinHeight(Ui.dp(this,64));
        option.setBackground(Ui.solid(this,0xff1c203e,17,color));
        option.setClickable(true);option.setFocusable(true);
        option.setOnClickListener(v->action.run());
        Ui.add(panel,option,10);
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
        // Accesibilidad Android: sin movimiento si el sistema tiene animaciones reducidas.
        if(!ValueAnimator.areAnimatorsEnabled()){
            ThemeManager.prefs(this).edit().putBoolean("intro_mostrada",true).apply();
            return;
        }
        final FrameLayout activeRoot=root;
        FrameLayout overlay=new FrameLayout(this);
        overlay.setBackground(Ui.grad(this,0xff05091c,0xff220b35,0));
        activeRoot.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        IntroLights lights=new IntroLights(this,season.accent,season.second);
        overlay.addView(lights,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout panel=Ui.column(this);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),0);
        overlay.addView(panel,new FrameLayout.LayoutParams(-1,-1));
        TextView tagline=Ui.text(this,"◉  ENCENDIENDO LA CABINA",15,Ui.CYAN,true);
        tagline.setGravity(Gravity.CENTER);Ui.add(panel,tagline,0);
        ImageView image=new ImageView(this);
        image.setImageResource(R.drawable.logo_original);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("Logotipo TROPICALISIMA ESTÉREO");
        int screenDp=getResources().getConfiguration().screenHeightDp;
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,Ui.dp(this,screenDp<650?220:280));
        ilp.topMargin=Ui.dp(this,10);panel.addView(image,ilp);
        TextView txt=Ui.text(this,"¡LA QUE TE PONE A GOZAR!",27,Ui.GOLD,true);
        txt.setGravity(Gravity.CENTER);Ui.add(panel,txt,9);
        TextView skip=Ui.text(this,"OMITIR INTRO  ›",16,Ui.WHITE,true);
        skip.setGravity(Gravity.CENTER);
        skip.setPadding(0,Ui.dp(this,20),0,Ui.dp(this,20));Ui.add(panel,skip,8);
        image.setAlpha(0f);image.setScaleX(.63f);image.setScaleY(.63f);
        image.setTranslationY(Ui.dp(this,44));
        image.setRotation(-3f);
        long full=first?2850:850;
        ValueAnimator ignite=ValueAnimator.ofFloat(0f,1f);
        ignite.setDuration(first?2100:610);
        ignite.addUpdateListener(a->{lights.progress=(float)a.getAnimatedValue();lights.invalidate();});
        ignite.start();
        image.animate().alpha(1f).translationY(0f).scaleX(1.04f).scaleY(1.04f).rotation(0f)
                .setStartDelay(first?230:40).setDuration(first?1030:430)
                .withEndAction(()->image.animate().scaleX(1f).scaleY(1f).setDuration(210).start()).start();
        tagline.setAlpha(0f);
        tagline.animate().alpha(1f).setStartDelay(first?150:20).setDuration(360).start();
        txt.setAlpha(0f);txt.setTranslationY(Ui.dp(this,18));
        txt.animate().alpha(1f).translationY(0f).setStartDelay(first?1080:350)
                .setDuration(first?680:220).start();
        final boolean[] done={false};
        Runnable exit=()->{
            if(done[0])return;done[0]=true;
            ThemeManager.prefs(this).edit().putBoolean("intro_mostrada",true).apply();
            ignite.cancel();
            if(overlay.getParent()==activeRoot){
                overlay.animate().alpha(0f).setDuration(220)
                        .withEndAction(()->activeRoot.removeView(overlay)).start();
            }
        };
        skip.setOnClickListener(v->exit.run());
        handler.postDelayed(exit,full);
    }
    // Efectos originales dibujados en Android, sin vídeos ni recursos externos:
    // halo tropical, luces de cabina y destellos suaves; no hay flashes rápidos.
    static class IntroLights extends View {
        final Paint p=new Paint(3);final int accent,second;float progress=0f;
        IntroLights(Activity c,int a,int b){super(c);accent=a;second=b;
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w*.5f,cy=h*.46f;
            float rise=Math.min(1f,progress*1.5f);
            p.setShader(new RadialGradient(cx,cy,Math.max(w,h)*.56f,
                    new int[]{(accent&0x00ffffff)|((int)(rise*70)<<24),0x41221a55,0x0007071b},
                    null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
            for(int j=0;j<3;j++){
                float r=w*(.25f+.12f*j)+w*.06f*progress;
                p.setColor(((int)(65*rise)&0xff)<<24 | ((j==0?accent:j==1?second:Ui.PINK)&0x00ffffff));
                p.setStrokeWidth(Ui.dp(getContext(),j==0?2:1));p.setStyle(Paint.Style.STROKE);
                c.drawCircle(cx,cy,r,p);
            }
            p.setStyle(Paint.Style.FILL);
            for(int j=0;j<18;j++){
                float ang=(float)(Math.PI*2*j/18);
                float x=cx+(float)Math.cos(ang)*w*.39f,y=cy+(float)Math.sin(ang)*h*.28f;
                int color=j%3==0?Ui.ORANGE:j%2==0?Ui.PINK:Ui.CYAN;
                p.setColor((((int)(115*rise))&0xff)<<24 | (color&0x00ffffff));
                float r=Ui.dp(getContext(),1.3f)+(j%3)*.8f;
                c.drawCircle(x,y,r,p);
            }
        }
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
