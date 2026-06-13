package com.qralarm.app;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmRepository {

    private final AlarmDao alarmDao;
    private final LiveData<List<Alarm>> allAlarms;
    private final ExecutorService executor;

    public AlarmRepository(Context context) {
        AlarmDatabase db = AlarmDatabase.getInstance(context);
        alarmDao = db.alarmDao();
        allAlarms = alarmDao.getAllAlarms();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void insert(Alarm alarm, OnInsertListener listener) {
        executor.execute(() -> {
            long id = alarmDao.insert(alarm);
            if (listener != null) listener.onInserted((int) id);
        });
    }

    public void update(Alarm alarm) {
        executor.execute(() -> alarmDao.update(alarm));
    }

    public void delete(Alarm alarm) {
        executor.execute(() -> alarmDao.delete(alarm));
    }

    public void setEnabled(int id, boolean enabled) {
        executor.execute(() -> alarmDao.setEnabled(id, enabled));
    }

    public void getAlarmById(int id, OnAlarmLoadedListener listener) {
        executor.execute(() -> {
            Alarm alarm = alarmDao.getAlarmById(id);
            if (listener != null) listener.onAlarmLoaded(alarm);
        });
    }

    public void getEnabledAlarms(OnAlarmsLoadedListener listener) {
        executor.execute(() -> {
            List<Alarm> alarms = alarmDao.getEnabledAlarms();
            if (listener != null) listener.onAlarmsLoaded(alarms);
        });
    }

    public interface OnInsertListener {
        void onInserted(int id);
    }

    public interface OnAlarmLoadedListener {
        void onAlarmLoaded(Alarm alarm);
    }

    public interface OnAlarmsLoadedListener {
        void onAlarmsLoaded(List<Alarm> alarms);
    }
}
