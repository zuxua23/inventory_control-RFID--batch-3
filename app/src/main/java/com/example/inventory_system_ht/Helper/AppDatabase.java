package com.example.inventory_system_ht.Helper;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.Models.PendingSubmitEntity;
import com.example.inventory_system_ht.Models.TagModels;

@Database(
        entities = {
                DOModels.DOModel.class,
                TagModels.TagModel.class,
                TagModels.SearchItemEntity.class,
                TagModels.TagCacheEntity.class,
                PendingSubmitEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AppDao appDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "sato_inventory_db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration() // wipe & recreate kalau versi naik
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}