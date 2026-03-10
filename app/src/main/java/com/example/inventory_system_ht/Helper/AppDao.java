package com.example.inventory_system_ht.Helper;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.Models.TagModels;

import java.util.List;

@Dao
public interface AppDao {
    // Masukin list DO dari API ke dalam Database Lokal HP
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDOList(List<DOModels.DOModel> doList);

    // Narik semua data DO buat ditampilin di layar (Offline Mode)
    @Query("SELECT * FROM tb_DO ORDER BY created_at DESC")
    List<DOModels.DOModel> getAllDO();


    // --- OPERASI UNTUK TAG/BARANG YANG DI-SCAN ---

    // Tiap kali laser HT nyala dan dapet EPC Tag, simpen ke SQLite biar aman kalau internet mati
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScannedTag(TagModels.TagModel tag);

    // Buat nangkep barang yang belum kesinkron ke server ASP.NET lu
    @Query("SELECT * FROM tb_Tag_Local WHERE sync_status = 0")
    List<TagModels.TagModel> getPendingTags();

    // Kalau tombol SAVE sukses tembus ke Backend, panggil ini buat tandain barang udah sinkron
    @Query("UPDATE tb_Tag_Local SET sync_status = 1 WHERE epc_tag = :epc")
    void markTagAsSynced(String epc);

    // Tambahan: Hapus data lama yang sudah tersinkron (Biar database HP gak penuh)
    @Query("DELETE FROM tb_Tag_Local WHERE sync_status = 1")
    void clearSyncedTags();
}