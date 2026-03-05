package com.example.inventory_system_ht.Helper; // Sesuaikan kalau lu taruh di folder Database

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.inventory_system_ht.Models.DOModel;
import com.example.inventory_system_ht.Models.TagModel;

// 👇 Daftarin semua tabel (Entity) lu di sini, version = 1 buat rilis pertama 👇
@Database(entities = {DOModel.class, TagModel.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Daftarin DAO lu biar bisa dipanggil dari Activity
    public abstract AppDao appDao();

    // Bikin Singleton Instance biar hemat memori HT
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // "sato_inventory_db" ini nama file SQLite di dalem HP lu nanti
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "sato_inventory_db")
                            // Mode ini ngijinin kita hit database di Main Thread sementara buat ngetes
                            // Nanti pas rilis beneran, kita pindahin ke Background Thread (BGT)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}