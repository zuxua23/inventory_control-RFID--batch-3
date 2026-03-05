package com.example.inventory_system_ht.Helper;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.inventory_system_ht.Models.DOModel;
import com.example.inventory_system_ht.Models.TagModel;

import java.util.List;

@Dao
public interface AppDao {

    // ===========================================
    // OPERASI UNTUK DO (DELIVERY ORDER)
    // ===========================================

    // Masukin list DO dari API ke dalam Database Lokal HP
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDOList(List<DOModel> doList);

    // Narik semua data DO buat ditampilin di layar (Offline Mode)
    @Query("SELECT * FROM tb_DO ORDER BY created_at DESC")
    List<DOModel> getAllDO();

    // ===========================================
    // OPERASI UNTUK TAG/BARANG YANG DI-SCAN
    // ===========================================

    // Tiap kali laser HT nyala dan dapet EPC Tag, simpen pake ini
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScannedTag(TagModel tag);

    // Buat ngecek: Ada tag yang belum kesinkron ke ASP.NET gak?
    @Query("SELECT * FROM tb_Tag_Local WHERE sync_status = 0")
    List<TagModel> getPendingTags();

    // Kalau Saga Pattern di backend bilang "COMMIT" (Sukses), panggil ini biar statusnya ganti jadi 1
    @Query("UPDATE tb_Tag_Local SET sync_status = 1 WHERE epc_tag = :epc")
    void markTagAsSynced(String epc);
}