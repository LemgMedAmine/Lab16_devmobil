package com.example.servicechronometrejava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Commentaires ajoutés par Lemghili Mohammed Amine.
 * Ce service foreground garde le chronomètre actif même si l'application est fermée.
 */
public class ChronometreService extends Service {

    public static final String ACTION_STOP = "STOP";

    private static final String CHANNEL_ID = "chrono_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new LocalBinder();

    private volatile int secondes = 0;
    private volatile boolean isRunning = false;
    private ScheduledExecutorService executor;
    private NotificationManager notificationManager;

    // Binder utilisé par l'Activity pour accéder à l'instance du service.
    public class LocalBinder extends Binder {
        public ChronometreService getService() {
            return ChronometreService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        creerNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;

        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isRunning) {
            isRunning = true;
            startForeground(NOTIFICATION_ID, creerNotification());
            demarrerChronometre();
        }

        return START_STICKY;
    }

    private void demarrerChronometre() {
        executor = Executors.newSingleThreadScheduledExecutor();

        // Incrémentation automatique du temps et rafraîchissement de la notification.
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                secondes++;
                updateNotification();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void creerNotificationChannel() {
        // Création du canal obligatoire pour afficher une notification sur Android 8+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chronomètre Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification creerNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Chronomètre en cours")
                .setContentText("Temps : " + getTempsFormate())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, creerNotification());
    }

    public String getTempsFormate() {
        return formatTemps(secondes);
    }

    public boolean isRunning() {
        return isRunning;
    }

    private String formatTemps(int sec) {
        int minutes = sec / 60;
        int secondesRest = sec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secondesRest);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
