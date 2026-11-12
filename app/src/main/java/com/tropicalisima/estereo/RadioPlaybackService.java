package com.tropicalisima.estereo;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/** El servicio controla el audio incluso con la pantalla bloqueada. Sin inicio automático. */
public final class RadioPlaybackService extends MediaSessionService {
    public static final String STREAM="https://streampanel.online/8631/stream";
    private ExoPlayer player;
    private MediaSession session;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private int failures=0;
    private final Runnable retry=new Runnable(){@Override public void run(){
        if(player!=null && player.getPlayWhenReady() && player.getPlaybackState()!=Player.STATE_READY){
            player.prepare();player.play();
        }
    }};
    @Override public void onCreate(){
        super.onCreate();
        player=new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.setVolume(0.50f); // volumen de la app, NO volumen del teléfono.
        MediaMetadata meta=new MediaMetadata.Builder().setTitle("TROPICALISIMA ESTÉREO")
                .setArtist("LA QUE TE PONE A GOZAR").build();
        MediaItem item=new MediaItem.Builder().setMediaId("radio-tropicalisima")
                .setUri(Uri.parse(STREAM)).setMediaMetadata(meta).build();
        player.setMediaItem(item);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException error){
                handler.removeCallbacks(retry);
                failures=Math.min(failures+1,5);
                if(player!=null && player.getPlayWhenReady()){
                    long delay=Math.min(30000,3000L*(1<<Math.min(failures-1,3)));
                    handler.postDelayed(retry,delay);
                }
            }
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_READY)failures=0;
            }
            @Override public void onPlayWhenReadyChanged(boolean playWhenReady,int reason){
                if(!playWhenReady)handler.removeCallbacks(retry);
            }
        });
        session=new MediaSession.Builder(this,player).build();
    }
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo){return session;}
    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        if(session!=null){session.release();session=null;}
        if(player!=null){player.release();player=null;}
        super.onDestroy();
    }
}
