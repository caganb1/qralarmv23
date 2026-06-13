package com.qralarm.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements AlarmAdapter.AlarmActionListener {

    private AlarmViewModel viewModel;
    private AlarmAdapter adapter;
    private TextView tvEmpty;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Check if exact alarm permission needed
                checkExactAlarmPermission();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recyclerView = findViewById(R.id.recycler_alarms);
        FloatingActionButton fab = findViewById(R.id.fab_add_alarm);

        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        adapter = new AlarmAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel.getAllAlarms().observe(this, alarms -> {
            adapter.setAlarms(alarms);
            tvEmpty.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        });

        fab.setOnClickListener(v -> openAddAlarm(-1));

        requestPermissions();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                return;
            }
        }
        checkExactAlarmPermission();
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.exact_alarm_permission_needed,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.grant, v -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }).show();
                return;
            }
        }
        checkBatteryOptimization();
    }

    /**
     * Samsung One UI (ve diğer üreticilerin) agresif pil optimizasyonu,
     * arka planda zamanlanan alarmların gecikmesine veya hiç çalmamasına
     * sebep olabilir. Bu yüzden uygulamayı pil optimizasyonu listesinden
     * hariç tutmak için kullanıcıdan izin istiyoruz.
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.battery_optimization_needed,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.grant, v -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    }
                }).show();
            }
        }
    }

    private void openAddAlarm(int alarmId) {
        Intent intent = new Intent(this, AddAlarmActivity.class);
        if (alarmId != -1) {
            intent.putExtra(AddAlarmActivity.EXTRA_ALARM_ID, alarmId);
        }
        startActivity(intent);
    }

    // AlarmActionListener callbacks
    @Override
    public void onAlarmToggle(Alarm alarm, boolean enabled) {
        alarm.isEnabled = enabled;
        viewModel.update(alarm);
        if (enabled) {
            AlarmScheduler.schedule(this, alarm);
        } else {
            AlarmScheduler.cancel(this, alarm);
        }
    }

    @Override
    public void onAlarmEdit(Alarm alarm) {
        openAddAlarm(alarm.id);
    }

    @Override
    public void onAlarmDelete(Alarm alarm) {
        AlarmScheduler.cancel(this, alarm);
        viewModel.delete(alarm);
    }
}
