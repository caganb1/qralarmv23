package com.qralarm.app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlarmDao {

    @Insert
    long insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    LiveData<List<Alarm>> getAllAlarms();

    @Query("SELECT * FROM alarms WHERE id = :id")
    Alarm getAlarmById(int id);

    @Query("SELECT * FROM alarms WHERE is_enabled = 1")
    List<Alarm> getEnabledAlarms();

    @Query("UPDATE alarms SET is_enabled = :enabled WHERE id = :id")
    void setEnabled(int id, boolean enabled);

    @Query("DELETE FROM alarms WHERE id = :id")
    void deleteById(int id);
}
