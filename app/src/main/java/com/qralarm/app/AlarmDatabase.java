package com.qralarm.app;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
public abstract class AlarmDatabase extends RoomDatabase {

    private static volatile AlarmDatabase INSTANCE;

    public abstract AlarmDao alarmDao();

    public static AlarmDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AlarmDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AlarmDatabase.class,
                            "alarm_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
