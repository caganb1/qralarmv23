package com.qralarm.app;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms")
public class Alarm {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "hour")
    public int hour;

    @ColumnInfo(name = "minute")
    public int minute;

    @ColumnInfo(name = "label")
    public String label;

    @ColumnInfo(name = "is_enabled")
    public boolean isEnabled;

    // Days: index 0=Mon ... 6=Sun, stored as comma-separated "0,1,1,0,0,0,0"
    @ColumnInfo(name = "repeat_days")
    public String repeatDays;

    // Uri of custom sound (null = default ringtone)
    @ColumnInfo(name = "sound_uri")
    public String soundUri;

    // The QR/barcode value the user must scan to dismiss the alarm
    @ColumnInfo(name = "qr_code_value")
    public String qrCodeValue;

    // Display name of the QR/barcode (user-friendly label)
    @ColumnInfo(name = "qr_code_label")
    public String qrCodeLabel;

    // Vibration
    @ColumnInfo(name = "vibrate")
    public boolean vibrate;

    // Snooze duration in minutes (0 = disabled)
    @ColumnInfo(name = "snooze_minutes")
    public int snoozeMinutes;

    public Alarm() {}

    public Alarm(int hour, int minute, String label) {
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.isEnabled = true;
        this.repeatDays = "0,0,0,0,0,0,0";
        this.vibrate = true;
        this.snoozeMinutes = 5;
    }

    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }

    public boolean hasQrCode() {
        return qrCodeValue != null && !qrCodeValue.isEmpty();
    }

    public boolean[] getRepeatDaysArray() {
        boolean[] days = new boolean[7];
        if (repeatDays == null) return days;
        String[] parts = repeatDays.split(",");
        for (int i = 0; i < 7 && i < parts.length; i++) {
            days[i] = "1".equals(parts[i].trim());
        }
        return days;
    }

    public void setRepeatDaysArray(boolean[] days) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) sb.append(",");
            sb.append(days[i] ? "1" : "0");
        }
        this.repeatDays = sb.toString();
    }

    public boolean repeats() {
        if (repeatDays == null) return false;
        for (boolean d : getRepeatDaysArray()) {
            if (d) return true;
        }
        return false;
    }
}
