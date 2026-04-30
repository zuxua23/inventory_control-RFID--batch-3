package com.example.inventory_system_ht.Helper;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.Models.PendingSubmitEntity;
import com.example.inventory_system_ht.Models.TagModels;

import java.util.List;

@Dao
public interface AppDao {

    // ─── DO ───────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDOList(List<DOModels.DOModel> doList);

    @Query("SELECT do_id, do_number, status, created_at, scanner_type FROM tb_DO ORDER BY created_at DESC")
    List<DOModels.DOModel> getAllDO();

    @Query("DELETE FROM tb_DO")
    void deleteAllDO();

    // ─── Scanned Tag (local session) ──────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScannedTag(TagModels.TagModel tag);

    @Query("SELECT epc_tag, tag_id, itm_id, product_name, do_id_ref, sync_status FROM tb_Tag_Local WHERE sync_status = 0")
    List<TagModels.TagModel> getPendingTags();

    @Query("UPDATE tb_Tag_Local SET sync_status = 1 WHERE epc_tag = :epc")
    void markTagAsSynced(String epc);

    @Query("DELETE FROM tb_Tag_Local WHERE sync_status = 1")
    void clearSyncedTags();

    @Query("DELETE FROM tb_Tag_Local WHERE epc_tag = :epc")
    void deleteScannedTagByEpc(String epc);

    // ─── Tag Cache (buat validasi offline) ───────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTagCache(TagModels.TagCacheEntity cache);

    // Cari by epc_tag ATAU tag_id (barcode scan kasih tagId, RFID kasih epcTag)
    @Query("SELECT * FROM tb_tag_cache WHERE epc_tag = :key OR tag_id = :key LIMIT 1")
    TagModels.TagCacheEntity getTagCacheByKey(String key);

    @Query("DELETE FROM tb_tag_cache")
    void clearTagCache();

    // ─── Pending Submit (submit offline, kirim nanti) ─────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPendingSubmit(PendingSubmitEntity entity);

    @Query("SELECT * FROM tb_pending_submit ORDER BY created_at ASC")
    List<PendingSubmitEntity> getAllPendingSubmit();

    @Query("DELETE FROM tb_pending_submit WHERE id = :id")
    void deletePendingSubmitById(int id);

    // ─── Search Item ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSearchItems(List<TagModels.SearchItemEntity> items);

    @Query("SELECT * FROM tb_search_item")
    List<TagModels.SearchItemEntity> getAllSearchItems();

    @Query("DELETE FROM tb_search_item")
    void deleteAllSearchItems();
}