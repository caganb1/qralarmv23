package com.qralarm.app;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAlarmActivity extends AppCompatActivity {

    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

    private int selectedHour, selectedMinute;
    private String selectedSoundUri = null;
    private String qrCodeValue = null;
    private String qrCodeLabel = null;
    private int editAlarmId = -1;

    private TextView tvSelectedTime, tvSoundName, tvQrStatus;
    private EditText etLabel;
    private CheckBox[] dayCheckboxes = new CheckBox[7];
    private boolean[] selectedDays = new boolean[7];

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Sound file picker
    private final ActivityResultLauncher<String> soundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Take persistent permission
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    selectedSoundUri = uri.toString();
                    String name = getFileNameFromUri(uri);
                    tvSoundName.setText(name != null ? name : uri.getLastPathSegment());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvSelectedTime = findViewById(R.id.tv_selected_time);
        tvSoundName = findViewById(R.id.tv_sound_name);
        tvQrStatus = findViewById(R.id.tv_qr_status);
        etLabel = findViewById(R.id.et_label);

        int[] dayIds = {
                R.id.cb_mon, R.id.cb_tue, R.id.cb_wed,
                R.id.cb_thu, R.id.cb_fri, R.id.cb_sat, R.id.cb_sun
        };
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            dayCheckboxes[i] = findViewById(dayIds[i]);
            dayCheckboxes[i].setOnCheckedChangeListener((v, checked) -> selectedDays[idx] = checked);
        }

        // Default time = current time + 1 hour
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        selectedHour = cal.get(Calendar.HOUR_OF_DAY);
        selectedMinute = cal.get(Calendar.MINUTE);
        updateTimeDisplay();

        // Load existing alarm if editing
        editAlarmId = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);
        if (editAlarmId != -1) {
            loadExistingAlarm();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_alarm);
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.add_alarm);
            }
        }

        // Listeners
        tvSelectedTime.setOnClickListener(v -> showTimePicker());
        findViewById(R.id.btn_pick_time).setOnClickListener(v -> showTimePicker());
        findViewById(R.id.btn_pick_sound).setOnClickListener(v -> pickSound());
        findViewById(R.id.btn_setup_qr).setOnClickListener(v -> setupQr());
        findViewById(R.id.btn_save_alarm).setOnClickListener(v -> saveAlarm());
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    selectedHour = hour;
                    selectedMinute = minute;
                    updateTimeDisplay();
                },
                selectedHour,
                selectedMinute,
                true // 24h format
        );
        dialog.show();
    }

    private void updateTimeDisplay() {
        tvSelectedTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
    }

    private void pickSound() {
        // Show options: system ringtone picker or file picker
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.choose_sound_source)
                .setItems(new CharSequence[]{
                        getString(R.string.pick_from_ringtones),
                        getString(R.string.pick_from_files)
                }, (dialog, which) -> {
                    if (which == 0) pickSystemRingtone();
                    else soundPickerLauncher.launch("audio/*");
                })
                .show();
    }

    private void pickSystemRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        if (selectedSoundUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedSoundUri));
        }
        startActivityForResult(intent, 9001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedSoundUri = uri.toString();
                android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                if (ringtone != null) {
                    tvSoundName.setText(ringtone.getTitle(this));
                }
            }
        } else if (requestCode == QRSetupActivity.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            qrCodeValue = data.getStringExtra(QRSetupActivity.RESULT_QR_VALUE);
            qrCodeLabel = data.getStringExtra(QRSetupActivity.RESULT_QR_LABEL);
            updateQrStatus();
        }
    }

    private void setupQr() {
        Intent intent = new Intent(this, QRSetupActivity.class);
        if (qrCodeValue != null) {
            intent.putExtra(QRSetupActivity.EXTRA_CURRENT_VALUE, qrCodeValue);
            intent.putExtra(QRSetupActivity.EXTRA_CURRENT_LABEL, qrCodeLabel);
        }
        startActivityForResult(intent, QRSetupActivity.REQUEST_CODE);
    }

    private void updateQrStatus() {
        if (qrCodeValue != null && !qrCodeValue.isEmpty()) {
            String label = qrCodeLabel != null ? qrCodeLabel : qrCodeValue;
            tvQrStatus.setText(getString(R.string.qr_set, label));
        } else {
            tvQrStatus.setText(R.string.qr_not_set);
        }
    }

    private void saveAlarm() {
        String label = etLabel.getText().toString().trim();

        AlarmRepository repo = new AlarmRepository(this);

        if (editAlarmId != -1) {
            repo.getAlarmById(editAlarmId, existingAlarm -> {
                if (existingAlarm == null) return;
                existingAlarm.hour = selectedHour;
                existingAlarm.minute = selectedMinute;
                existingAlarm.label = label;
                existingAlarm.soundUri = selectedSoundUri;
                existingAlarm.qrCodeValue = qrCodeValue;
                existingAlarm.qrCodeLabel = qrCodeLabel;
                existingAlarm.setRepeatDaysArray(selectedDays);

                repo.update(existingAlarm);
                AlarmScheduler.cancel(this, existingAlarm);
                if (existingAlarm.isEnabled) AlarmScheduler.schedule(this, existingAlarm);

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.alarm_updated, Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            Alarm alarm = new Alarm(selectedHour, selectedMinute, label);
            alarm.soundUri = selectedSoundUri;
            alarm.qrCodeValue = qrCodeValue;
            alarm.qrCodeLabel = qrCodeLabel;
            alarm.setRepeatDaysArray(selectedDays);

            repo.insert(alarm, id -> {
                alarm.id = id;
                AlarmScheduler.schedule(this, alarm);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.alarm_saved, Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }

    private void loadExistingAlarm() {
        AlarmRepository repo = new AlarmRepository(this);
        repo.getAlarmById(editAlarmId, alarm -> {
            if (alarm == null) return;
            runOnUiThread(() -> {
                selectedHour = alarm.hour;
                selectedMinute = alarm.minute;
                updateTimeDisplay();
                etLabel.setText(alarm.label);
                selectedSoundUri = alarm.soundUri;
                qrCodeValue = alarm.qrCodeValue;
                qrCodeLabel = alarm.qrCodeLabel;

                boolean[] days = alarm.getRepeatDaysArray();
                System.arraycopy(days, 0, selectedDays, 0, 7);
                for (int i = 0; i < 7; i++) dayCheckboxes[i].setChecked(selectedDays[i]);

                if (alarm.soundUri != null && !alarm.soundUri.isEmpty()) {
                    try {
                        Uri uri = Uri.parse(alarm.soundUri);
                        android.media.Ringtone rt = RingtoneManager.getRingtone(this, uri);
                        if (rt != null) tvSoundName.setText(rt.getTitle(this));
                        else tvSoundName.setText(uri.getLastPathSegment());
                    } catch (Exception e) {
                        tvSoundName.setText(alarm.soundUri);
                    }
                }
                updateQrStatus();
            });
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
