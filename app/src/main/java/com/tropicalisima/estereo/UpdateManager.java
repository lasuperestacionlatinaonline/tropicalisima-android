package com.tropicalisima.estereo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Verifica exclusivamente GitHub Releases publicadas en el repositorio oficial.
 * Nunca descarga ni instala nada sin una acción explícita del oyente.
 * Android siempre decide la autorización final para instalar una actualización.
 * No interfiere con RadioPlaybackService ni con la transmisión de audio.
 */
public final class UpdateManager {
    private static final String API =
            "https://api.github.com/repos/lasuperestacionlatinaonline/tropicalisima-android/releases/latest";
    private static final String ALLOWED_PREFIX =
            "https://github.com/lasuperestacionlatinaonline/tropicalisima-android/releases/download/";
    private static final String PREFS = "actualizaciones_tropicalisima";
    private static final long CHECK_EVERY = 24L * 60 * 60 * 1000;
    private static final long REMIND_EVERY = 7L * CHECK_EVERY;
    private static final String APK_MIME = "application/vnd.android.package-archive";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean visible;
    private boolean checkRunning;
    private boolean installDialogShowing;
    private boolean verifying;
    private Runnable poller;

    public void onResume(Activity activity) {
        visible = true;
        // Las versiones debug son otro paquete (.debug); nunca instalar la release encima.
        if (BuildConfig.DEBUG) return;
        SharedPreferences p = prefs(activity);
        if (p.getLong("download_id", -1) != -1) resumeDownload(activity);
        handler.postDelayed(() -> {
            if (visible && !activity.isFinishing() && !activity.isDestroyed()) checkLatest(activity);
        }, 3900);
    }

    public void onPause() {
        visible = false;
        if (poller != null) handler.removeCallbacks(poller);
        poller = null;
    }

    public void onDestroy() {
        onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void checkLatest(Activity activity) {
        if (checkRunning || !visible) return;
        SharedPreferences p = prefs(activity);
        long now = System.currentTimeMillis();
        if (Math.abs(now - p.getLong("last_check", 0)) < CHECK_EVERY) return;
        checkRunning = true;
        new Thread(() -> {
            HttpURLConnection conn = null;
            Release newest = null;
            boolean requestSucceeded = false;
            try {
                conn = (HttpURLConnection) new URL(API).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "TROPICALISIMA-ESTEREO-ANDROID");
                int status = conn.getResponseCode();
                if (status == 200) {
                    byte[] data = readLimited(conn, 128 * 1024);
                    JSONObject obj = new JSONObject(new String(data, java.nio.charset.StandardCharsets.UTF_8));
                    newest = parseRelease(obj);
                    requestSucceeded = true;
                } else if (status == 404) {
                    // El repositorio puede no tener todavía ninguna Release pública.
                    requestSucceeded = true;
                }
            } catch (Exception ignored) {
                // Nunca interrumpir el funcionamiento de la radio por falta de internet.
            } finally {
                if (conn != null) conn.disconnect();
            }
            final Release release = newest;
            final boolean ok = requestSucceeded;
            handler.post(() -> {
                checkRunning = false;
                if (ok) prefs(activity).edit().putLong("last_check", System.currentTimeMillis()).apply();
                if (release != null && visible && !activity.isFinishing() && !activity.isDestroyed()) {
                    notifyIfNew(activity, release);
                }
            });
        }, "buscar-actualizaciones").start();
    }

    private static byte[] readLimited(HttpURLConnection c, int max) throws Exception {
        try (java.io.InputStream in = c.getInputStream();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) >= 0) {
                if (out.size() + n > max) throw new IllegalStateException("Respuesta excesiva");
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static final class Release {
        final String tag, apkUrl, digest;
        Release(String t, String u, String d) { tag = t; apkUrl = u; digest = d; }
    }

    private static Release parseRelease(JSONObject obj) {
        if (obj.optBoolean("draft") || obj.optBoolean("prerelease")) return null;
        String tag = obj.optString("tag_name", "");
        if (!tag.matches("v?[0-9]+\\.[0-9]+\\.[0-9]+")) return null;
        JSONArray assets = obj.optJSONArray("assets");
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.optJSONObject(i);
            if (item == null || !item.optString("name", "").toLowerCase(Locale.ROOT).endsWith(".apk")) continue;
            String url = item.optString("browser_download_url", "");
            if (!url.startsWith(ALLOWED_PREFIX)) continue;
            if (!url.equals(ALLOWED_PREFIX + tag + "/" + item.optString("name", ""))) continue;
            return new Release(tag, url, item.optString("digest", ""));
        }
        return null;
    }

    private static boolean isNewer(String remote, String local) {
        String remoteBase = remote.replaceFirst("^[vV]", "");
        String localBase = local.replaceFirst("^[vV]", "");
        String[] r = remoteBase.split("[-+]", 2)[0].split("\\.");
        String[] l = localBase.split("[-+]", 2)[0].split("\\.");
        try {
            if (r.length != 3 || l.length != 3) return false;
            for (int i = 0; i < 3; i++) {
                int a = Integer.parseInt(r[i]), b = Integer.parseInt(l[i]);
                if (a > b) return true;
                if (a < b) return false;
            }
            // Estable 1.4.0 reemplaza 1.4.0-rc2.
            return localBase.contains("-") && !remoteBase.contains("-");
        } catch (NumberFormatException e) { return false; }
    }

    private void notifyIfNew(Activity activity, Release release) {
        if (!isNewer(release.tag, BuildConfig.VERSION_NAME) || installDialogShowing) return;
        SharedPreferences p = prefs(activity);
        if (release.tag.equals(p.getString("dismissed_tag", "")) &&
                System.currentTimeMillis() - p.getLong("dismissed_at", 0) < REMIND_EVERY) return;
        installDialogShowing = true;
        new AlertDialog.Builder(activity)
                .setTitle("¡TENEMOS NOVEDADES! 📻")
                .setMessage("Nueva versión " + release.tag + " de TROPICALISIMA ESTÉREO. " +
                        "Puedes actualizar sin perder tu aplicación. La radio seguirá funcionando si eliges más tarde.")
                .setPositiveButton("ACTUALIZAR APP", (d, w) -> download(activity, release))
                .setNegativeButton("MÁS TARDE", (d, w) ->
                        p.edit().putString("dismissed_tag", release.tag)
                                .putLong("dismissed_at", System.currentTimeMillis()).apply())
                .setOnDismissListener(d -> installDialogShowing = false)
                .show();
    }

    private void download(Activity activity, Release release) {
        // Solo una acción consciente del oyente inicia la descarga.
        try {
            String fileName = fileName(release.tag);
            File destination = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (destination.exists() && !destination.delete()) throw new IllegalStateException("No se pudo preparar la carpeta");
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(release.apkUrl))
                    .setTitle("TROPICALISIMA ESTÉREO " + release.tag)
                    .setDescription("Actualización oficial de la radio")
                    .setMimeType(APK_MIME)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            long id = manager.enqueue(req);
            prefs(activity).edit().putLong("download_id", id)
                    .putString("download_tag", release.tag)
                    .putString("download_digest", release.digest).apply();
            Toast.makeText(activity, "Descargando actualización oficial…", Toast.LENGTH_LONG).show();
            resumeDownload(activity);
        } catch (Exception e) {
            new AlertDialog.Builder(activity).setTitle("No se pudo descargar")
                    .setMessage("Comprueba tu conexión e inténtalo de nuevo más tarde.")
                    .setPositiveButton("ENTENDIDO", null).show();
        }
    }

    private static String fileName(String tag) {
        return "TROPICALISIMA-ESTEREO-" + tag + ".apk";
    }

    private void resumeDownload(Activity activity) {
        if (poller != null) handler.removeCallbacks(poller);
        poller = new Runnable() {
            @Override public void run() {
                if (!visible || activity.isFinishing() || activity.isDestroyed()) return;
                SharedPreferences p = prefs(activity);
                long id = p.getLong("download_id", -1);
                if (id < 0) return;
                try {
                    DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
                    try (Cursor cursor = manager.query(query)) {
                        if (!cursor.moveToFirst()) { clearDownload(activity); return; }
                        int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            String tag = p.getString("download_tag", "");
                            String digest = p.getString("download_digest", "");
                            if (tag.matches("v?[0-9]+\\.[0-9]+\\.[0-9]+")) verifyAndOffer(activity, tag, digest);
                            else clearDownload(activity);
                            return;
                        }
                        if (status == DownloadManager.STATUS_FAILED) {
                            clearDownload(activity);
                            Toast.makeText(activity, "La descarga falló. Inténtalo nuevamente.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(activity, "No se pudo comprobar la descarga.", Toast.LENGTH_SHORT).show();
                    return;
                }
                handler.postDelayed(this, 1600);
            }
        };
        handler.post(poller);
    }

    private void verifyAndOffer(Activity activity, String tag, String expectedDigest) {
        if (verifying || installDialogShowing) return;
        verifying = true;
        new Thread(() -> {
            File apk = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName(tag));
            boolean valid = apk.isFile() && apk.length() > 100 * 1024;
            try {
                if (valid && expectedDigest.startsWith("sha256:")) {
                    String expected = expectedDigest.substring(7);
                    valid = expected.matches("[0-9a-fA-F]{64}") && expected.equalsIgnoreCase(sha256(apk));
                }
            } catch (Exception e) { valid = false; }
            final boolean safe = valid;
            handler.post(() -> {
                verifying = false;
                if (!visible || activity.isFinishing() || activity.isDestroyed()) return;
                if (!safe) {
                    // Una descarga alterada JAMÁS se ofrece para instalar.
                    if (apk.exists()) apk.delete();
                    clearDownload(activity);
                    new AlertDialog.Builder(activity).setTitle("Descarga no verificada")
                            .setMessage("No se pudo comprobar la APK. No se instalará este archivo.")
                            .setPositiveButton("ENTENDIDO", null).show();
                    return;
                }
                installDialogShowing = true;
                new AlertDialog.Builder(activity).setTitle("¡ACTUALIZACIÓN LISTA!")
                        .setMessage("Android te pedirá autorización para instalar la nueva versión. " +
                                "No necesitas desinstalar la emisora.")
                        .setPositiveButton("INSTALAR AHORA", (d, w) -> install(activity, apk))
                        .setNegativeButton("DESPUÉS", (d, w) -> {
                            clearDownload(activity);
                            prefs(activity).edit().putString("dismissed_tag", tag)
                                    .putLong("dismissed_at", System.currentTimeMillis()).apply();
                        })
                        .setOnDismissListener(d -> installDialogShowing = false)
                        .show();
            });
        }, "verificar-actualizacion").start();
    }

    private static String sha256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[64 * 1024]; int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte b : md.digest()) sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return sb.toString();
    }

    private void install(Activity activity, File apk) {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Permitir actualización")
                    .setMessage("Para instalar la actualización, Android debe autorizar esta aplicación como fuente. " +
                            "Activa 'Permitir desde esta fuente' y vuelve a TROPICALISIMA.")
                    .setPositiveButton("ABRIR AJUSTES", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        try { activity.startActivity(intent); }
                        catch (Exception ex) { Toast.makeText(activity, "Abre los ajustes de instalación del teléfono.", Toast.LENGTH_LONG).show(); }
                    })
                    .setNegativeButton("CANCELAR", null).show();
            return;
        }
        try {
            Uri fileUri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apk);
            Intent installer = new Intent(Intent.ACTION_VIEW);
            installer.setDataAndType(fileUri, APK_MIME);
            installer.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(installer);
            // La firma y versionCode son comprobados finalmente por Android.
        } catch (Exception e) {
            Toast.makeText(activity, "No se pudo abrir el instalador. Revisa la descarga.", Toast.LENGTH_LONG).show();
        }
    }

    private void clearDownload(Context ctx) {
        prefs(ctx).edit().remove("download_id").remove("download_tag").remove("download_digest").apply();
        if (poller != null) handler.removeCallbacks(poller);
        poller = null;
    }
}
