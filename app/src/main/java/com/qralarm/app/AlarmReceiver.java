package com.qralarm.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Re-schedule alarms after device reboot
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            AlarmScheduler.rescheduleAfterBoot(context);
            return;
        }

        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);
        if (alarmId == -1) return;

        // Only start the foreground AlarmService.
        // The Service itself is responsible for launching the ringing screen
        // (single source of truth -> prevents duplicate activity launches/crashes).
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(AlarmService.EXTRA_ALARM_ID, alarmId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
