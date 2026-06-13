package com.qralarm.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service that plays the alarm sound with an escalating volume ramp.
 *
 * Behaviour:
 *  - 3 "rounds" total.
 *  - Each round lasts ROUND_DURATION_MS (2 minutes).
 *  - Within a round, volume ramps linearly from MIN_VOLUME to MAX_VOLUME.
 *  - At the start of each round a different sound is used (round 1 = user's
 *    chosen sound, rounds 2 & 3 = fallback system alarm sounds).
 *  - If the user has not dismissed the alarm after 3 full rounds (6 minutes
 *    total), the service stops itself automatically (alarm "kapanır").
 *  - No vibration is used.
 */
public class AlarmService extends Service {

    private static final String TAG = "AlarmService";
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";
    public static final String CHANNEL_ID = "alarm_channel";
    public static final int NOTIFICATION_ID = 1001;

    private static final long ROUND_DURATION_MS = 2 * 60 * 1000L; // 2 minutes
    private static final long VOLUME_STEP_MS = 1000L; // update volume every second
    private static final float MIN_VOLUME = 0.08f;
    private static final float MAX_VOLUME = 1.0f;
    private static final int TOTAL_ROUNDS = 3;

    // Guards against duplicate triggers for the same alarm (prevents the
    // "uygulama kendini defalarca çalıştırıyor" crash loop).
    private static volatile boolean isRinging = false;
    private static volatile int ringingAlarmId = -1;

    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private int currentAlarmId = -1;
    private Alarm currentAlarm;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable volumeRampRunnable;
    private long roundStartTime;
    private int currentRound = 0; // 0,1,2 -> round 1,2,3
    private List<Uri> soundUris;

    public static boolean isAlarmRinging(int alarmId) {
        return isRinging && ringingAlarmId == alarmId;
    }

    public static boolean isAnyAlarmRinging() {
        return isRinging;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);
        if (alarmId == -1) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Ignore duplicate start commands for the alarm that is already ringing.
        if (isRinging && ringingAlarmId == alarmId) {
            return START_STICKY;
        }

        // If a different alarm is already ringing, ignore the new one (edge case).
        if (isRinging) {
            return START_STICKY;
        }

        isRinging = true;
        ringingAlarmId = alarmId;
        currentAlarmId = alarmId;
        currentRound = 0;

        acquireWakeLock();
        startForeground(NOTIFICATION_ID, buildNotification());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            currentAlarm = AlarmDatabase.getInstance(this).alarmDao().getAlarmById(currentAlarmId);
            if (currentAlarm == null) {
                stopSelf();
                return;
            }
            soundUris = buildSoundList(currentAlarm);

            handler.post(() -> {
                // Launch the ringing screen from here (single source of truth).
                launchRingActivity();
                startRound(0);
            });
        });

        return START_STICKY;
    }

    private void launchRingActivity() {
        Intent ringIntent = new Intent(this, AlarmRingActivity.class);
        ringIntent.putExtra(AlarmRingActivity.EXTRA_ALARM_ID, currentAlarmId);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(ringIntent);
    }

    /** Builds an ordered list of up to 3 distinct sound URIs for the escalation rounds. */
    private List<Uri> buildSoundList(Alarm alarm) {
        List<Uri> list = new ArrayList<>();

        // Round 1: user's chosen sound (or default alarm sound).
        Uri first = null;
        if (alarm.soundUri != null && !alarm.soundUri.isEmpty()) {
            try {
                first = Uri.parse(alarm.soundUri);
            } catch (Exception ignored) {}
        }
        if (first == null) first = Settings.System.DEFAULT_ALARM_ALERT_URI;
        list.add(first);

        // Rounds 2 & 3: pick different system alarm sounds if available.
        try {
            RingtoneManager rm = new RingtoneManager(this);
            rm.setType(RingtoneManager.TYPE_ALARM);
            android.database.Cursor cursor = rm.getCursor();
            List<Uri> systemSounds = new ArrayList<>();
            while (cursor.moveToNext() && systemSounds.size() < 6) {
                Uri uri = rm.getRingtoneUri(cursor.getPosition());
                if (uri != null && !uri.equals(first)) {
                    systemSounds.add(uri);
                }
            }
            for (Uri u : systemSounds) {
                if (list.size() >= TOTAL_ROUNDS) break;
                if (!list.contains(u)) list.add(u);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not enumerate system alarm sounds", e);
        }

        // Fallback: fill remaining slots with the default alarm sound.
        while (list.size() < TOTAL_ROUNDS) {
            Uri def = Settings.System.DEFAULT_ALARM_ALERT_URI;
            list.add(def);
        }

        return list;
    }

    private void startRound(int roundIndex) {
        if (roundIndex >= TOTAL_ROUNDS) {
            // All rounds exhausted without dismissal -> auto stop ("alarm kapansın")
            stopSelf();
            return;
        }
        currentRound = roundIndex;
        roundStartTime = System.currentTimeMillis();

        Uri soundUri = soundUris.get(Math.min(roundIndex, soundUris.size() - 1));
        startPlayback(soundUri, MIN_VOLUME);
        scheduleVolumeRamp();
    }

    private void startPlayback(Uri soundUri, float startVolume) {
        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(this, soundUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(startVolume, startVolume);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Error playing alarm sound, falling back to default", e);
            try {
                releasePlayer();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mediaPlayer.setDataSource(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(startVolume, startVolume);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException ex) {
                Log.e(TAG, "Fallback alarm sound also failed", ex);
            }
        }
    }

    /** Gradually raises the volume from MIN_VOLUME to MAX_VOLUME over ROUND_DURATION_MS. */
    private void scheduleVolumeRamp() {
        if (volumeRampRunnable != null) handler.removeCallbacks(volumeRampRunnable);

        volumeRampRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - roundStartTime;
                if (elapsed >= ROUND_DURATION_MS) {
                    // Round finished -> move to next round with a different sound.
                    startRound(currentRound + 1);
                    return;
                }
                float progress = (float) elapsed / (float) ROUND_DURATION_MS;
                float volume = MIN_VOLUME + (MAX_VOLUME - MIN_VOLUME) * progress;
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.setVolume(volume, volume);
                    } catch (IllegalStateException ignored) {}
                }
                handler.postDelayed(this, VOLUME_STEP_MS);
            }
        };
        handler.postDelayed(volumeRampRunnable, VOLUME_STEP_MS);
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private Notification buildNotification() {
        Intent ringIntent = new Intent(this, AlarmRingActivity.class);
        ringIntent.putExtra(AlarmRingActivity.EXTRA_ALARM_ID, currentAlarmId);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) piFlags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ringIntent, piFlags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(getString(R.string.alarm_ringing))
                .setContentText(getString(R.string.scan_qr_to_dismiss))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "QRAlarm::AlarmWakeLock"
            );
            // Max runtime safety net: 3 rounds * 2 min + buffer.
            wakeLock.acquire((TOTAL_ROUNDS * ROUND_DURATION_MS) + 60_000L);
        }
    }

    /** Called by AlarmRingActivity once the correct QR/barcode has been scanned. */
    public static void stop(android.content.Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        context.stopService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (volumeRampRunnable != null) handler.removeCallbacks(volumeRampRunnable);
        releasePlayer();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        isRinging = false;
        ringingAlarmId = -1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.alarm_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.alarm_channel_desc));
            channel.setBypassDnd(true);
            channel.enableVibration(false);
            channel.setSound(null, null); // sound handled manually by MediaPlayer
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
