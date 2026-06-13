package com.qralarm.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class AlarmViewModel extends AndroidViewModel {

    private final AlarmRepository repository;
    private final LiveData<List<Alarm>> allAlarms;

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        repository = new AlarmRepository(application);
        allAlarms = repository.getAllAlarms();
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void update(Alarm alarm) {
        repository.update(alarm);
    }

    public void delete(Alarm alarm) {
        repository.delete(alarm);
    }
}
