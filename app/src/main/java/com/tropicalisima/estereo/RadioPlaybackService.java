package com.tropicalisima.estereo;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * TROPICALISIMA ESTEREO: radio EN VIVO (no podcast).
 * Cada PLAY crea una conexión nueva. STOP descarta el búfer. APAGAR termina el servicio.
 * Ningún comando se ejecuta automáticamente al abrir la aplicación.
 */
@OptIn(markerClass = UnstableApi.class)
public final class RadioPlaybackService extends MediaSessionService {
    public static final String STREAM = "https://streampanel.online/8631/stream";
    public static final String ACTION_PLAY_FRESH = "com.tropicalisima.estereo.PLAY_FRESH";
    public static final String ACTION_STOP_LIVE = "com.tropicalisima.estereo.STOP_LIVE";
    public static final String ACTION_SHUTDOWN = "com.tropicalisima.estereo.SHUTDOWN";

    private ExoPlayer player;
    private MediaSession session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean wantsLive = false;
    private boolean internalTransition = false;
    private boolean retryScheduled = false;
    private int failures = 0;
    private long bufferingSince = 0;

    private final Runnable retry = new Runnable() {
        @Override public void run() {
            retryScheduled = false;
            if (wantsLive && player != null) startFreshConnection();
        }
    };
    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            if (wantsLive && !retryScheduled) {
                // Una espera demasiado larga no debe reanudar audio antiguo.
                if (player.getPlaybackState() == Player.STATE_BUFFERING &&
                        bufferingSince > 0 &&
                        SystemClock.elapsedRealtime() - bufferingSince > 25000L) {
                    scheduleReconnect();
                } else if (player.getPlaybackState() == Player.STATE_ENDED ||
                        player.getPlaybackState() == Player.STATE_IDLE) {
                    scheduleReconnect();
                }
            }
            handler.postDelayed(this, 5000L);
        }
    };

    private MediaItem freshMediaItem() {
        MediaMetadata meta = new MediaMetadata.Builder()
                .setTitle("TROPICALISIMA ESTÉREO")
                .setArtist("LA QUE TE PONE A GOZAR").build();
        return new MediaItem.Builder().setMediaId("radio-tropicalisima")
                .setUri(Uri.parse(STREAM)).setMediaMetadata(meta).build();
    }

    @Override public void onCreate() {
        super.onCreate();
        // Búfer moderado para radio, en vez de una cola larga de audio antiguo.
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(3000, 12000, 1500, 3000).build();
        player = new ExoPlayer.Builder(this).setLoadControl(loadControl).build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        player.setVolume(0.50f); // Volumen interno, nunca cambia el del teléfono.
        // Sin setMediaItem inicial: no reconectarse ni reanudar al abrir la app.
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                if (wantsLive) scheduleReconnect();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    failures = 0;
                    bufferingSince = 0;
                    retryScheduled = false;
                    handler.removeCallbacks(retry);
                } else if (state == Player.STATE_BUFFERING && wantsLive) {
                    bufferingSince = SystemClock.elapsedRealtime();
                } else if (state == Player.STATE_ENDED && wantsLive) {
                    scheduleReconnect();
                }
            }
            @Override public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                // Los controles externos de Android pueden enviar PAUSE/PLAY.
                // PAUSE se convierte en STOP; PLAY establece un directo NUEVO.
                if (internalTransition || player == null) return;
                if (!playWhenReady && wantsLive) {
                    handler.post(() -> {
                        if (player != null && wantsLive && !player.getPlayWhenReady()) stopAndDiscard();
                    });
                } else if (playWhenReady && !wantsLive) {
                    handler.post(() -> {
                        if (player != null && !wantsLive && player.getPlayWhenReady())
                            startFreshConnection();
                    });
                }
            }
        });
        session = new MediaSession.Builder(this, player).build();
        handler.postDelayed(watchdog, 5000L);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        // Siempre delegar los comandos de los botones multimedia al MediaSessionService.
        int result = super.onStartCommand(intent, flags, startId);
        String action = intent == null ? null : intent.getAction();
        if (ACTION_PLAY_FRESH.equals(action)) {
            failures = 0;
            startFreshConnection();
        } else if (ACTION_STOP_LIVE.equals(action)) {
            stopAndDiscard();
        } else if (ACTION_SHUTDOWN.equals(action)) {
            stopAndDiscard();
            stopSelf();
        }
        // No resucitar automáticamente la radio al reiniciar un proceso eliminado.
        return action != null && (ACTION_PLAY_FRESH.equals(action) ||
                ACTION_STOP_LIVE.equals(action) || ACTION_SHUTDOWN.equals(action))
                ? START_NOT_STICKY : result;
    }

    private void startFreshConnection() {
        if (player == null) return;
        handler.removeCallbacks(retry);
        retryScheduled = false;
        wantsLive = true;
        bufferingSince = SystemClock.elapsedRealtime();
        internalTransition = true;
        try {
            // Esto descarta los paquetes almacenados y solicita un nuevo directo.
            player.stop();
            player.clearMediaItems();
            player.setMediaItem(freshMediaItem());
            player.prepare();
            player.play();
        } finally {
            internalTransition = false;
        }
    }

    private void stopAndDiscard() {
        if (player == null) return;
        wantsLive = false;
        bufferingSince = 0;
        failures = 0;
        retryScheduled = false;
        handler.removeCallbacks(retry);
        internalTransition = true;
        try {
            player.pause();
            player.stop();
            player.clearMediaItems();
        } finally {
            internalTransition = false;
        }
    }

    private void scheduleReconnect() {
        if (player == null || !wantsLive || retryScheduled) return;
        handler.removeCallbacks(retry);
        retryScheduled = true;
        failures = Math.min(failures + 1, 6);
        long delayMs = Math.min(30000L, 2000L * (1L << Math.min(failures - 1, 4)));
        handler.postDelayed(retry, delayMs);
    }

    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    @Override public void onDestroy() {
        wantsLive = false;
        handler.removeCallbacksAndMessages(null);
        if (session != null) { session.release(); session = null; }
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
