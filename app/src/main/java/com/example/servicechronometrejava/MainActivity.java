package com.example.servicechronometrejava;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/*
 * Commentaires ajoutés par Lemghili Mohammed Amine.
 * Cette Activity contrôle le démarrage, l'arrêt et l'affichage du service chronomètre.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 10;

    private TextView tvTemps;
    private TextView tvStatus;
    private Button btnStart;
    private Button btnStop;
    private ChronometreService chronometreService;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Met à jour le compteur affiché tant que l'Activity est connectée au service.
    private final Runnable updateTempsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && chronometreService != null) {
                tvTemps.setText(chronometreService.getTempsFormate());
                handler.postDelayed(this, 1000);
            }
        }
    };

    // Connexion entre l'Activity et le Bound Service.
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChronometreService.LocalBinder binder = (ChronometreService.LocalBinder) service;
            chronometreService = binder.getService();
            isBound = true;
            afficherEtatEnCours();
            demarrerMiseAJourAffichage();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            chronometreService = null;
            afficherEtatArrete();
            arreterMiseAJourAffichage();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemps = findViewById(R.id.tvTemps);
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        afficherEtatPret();

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                demanderPermissionNotificationSiNecessaire();
                demarrerService();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arreterService();
            }
        });
    }

    private void demanderPermissionNotificationSiNecessaire() {
        // Android 13+ demande une permission explicite pour afficher les notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS
            );
        }
    }

    private void demarrerService() {
        // Le service est lancé en foreground pour continuer après la fermeture de l'app.
        Intent intent = new Intent(this, ChronometreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        afficherEtatEnCours();
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void arreterService() {
        Intent intent = new Intent(this, ChronometreService.class);
        intent.setAction(ChronometreService.ACTION_STOP);
        startService(intent);

        if (isBound) {
            unbindService(connection);
            isBound = false;
        }

        chronometreService = null;
        arreterMiseAJourAffichage();
        tvTemps.setText("00:00");
        afficherEtatArrete();
    }

    private void demarrerMiseAJourAffichage() {
        handler.removeCallbacks(updateTempsRunnable);
        handler.post(updateTempsRunnable);
    }

    private void arreterMiseAJourAffichage() {
        handler.removeCallbacks(updateTempsRunnable);
    }

    private void afficherEtatPret() {
        tvStatus.setText(R.string.status_ready);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void afficherEtatEnCours() {
        tvStatus.setText(R.string.status_running);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void afficherEtatArrete() {
        tvStatus.setText(R.string.status_stopped);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        arreterMiseAJourAffichage();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        super.onDestroy();
    }
}
