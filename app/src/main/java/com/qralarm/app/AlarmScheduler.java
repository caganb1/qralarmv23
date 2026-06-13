package com.qralarm.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";

    public static void schedule(Context context, Alarm alarm) {
        if (!alarm.isEnabled) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return;
            }
        }

        if (alarm.repeats()) {
            // Schedule for each enabled day
            boolean[] days = alarm.getRepeatDaysArray();
            for (int i = 0; i < 7; i++) {
                if (days[i]) {
                    scheduleForDay(context, alarmManager, alarm, i);
                }
            }
        } else {
            // One-time alarm
            scheduleOneTime(context, alarmManager, alarm);
        }
    }

    private static void scheduleOneTime(Context context, AlarmManager alarmManager, Alarm alarm) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        PendingIntent pendingIntent = createPendingIntent(context, alarm.id, alarm.id);
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }

    private static void scheduleForDay(Context context, AlarmManager alarmManager, Alarm alarm, int dayOfWeek) {
        // dayOfWeek: 0=Mon, 1=Tue, ... 6=Sun
        // Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, ... 7=Sat
        int calDay = (dayOfWeek + 2) % 7; // Mon=2, Tue=3, ... Sun=1
        if (calDay == 0) calDay = 7;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calDay);
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        // Use a unique request code per alarm+day
        int requestCode = alarm.id * 10 + dayOfWeek;
        PendingIntent pendingIntent = createPendingIntent(context, requestCode, alarm.id);
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }

    public static void cancel(Context context, Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (alarm.repeats()) {
            for (int i = 0; i < 7; i++) {
                int requestCode = alarm.id * 10 + i;
                PendingIntent pendingIntent = createPendingIntent(context, requestCode, alarm.id);
                alarmManager.cancel(pendingIntent);
            }
        } else {
            PendingIntent pendingIntent = createPendingIntent(context, alarm.id, alarm.id);
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void rescheduleAfterBoot(Context context) {
        AlarmRepository repo = new AlarmRepository(context);
        repo.getEnabledAlarms(alarms -> {
            for (Alarm alarm : alarms) {
                schedule(context, alarm);
            }
        });
    }

    private static PendingIntent createPendingIntent(Context context, int requestCode, int alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
}
