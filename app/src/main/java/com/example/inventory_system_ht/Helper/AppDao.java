package com.example.inventory_system_ht.Helper;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.Models.PendingSubmitEntity;
import com.example.inventory_system_ht.Models.StockInScanEntity;
import com.example.inventory_system_ht.Models.StockTakingModels;
import com.example.inventory_system_ht.Models.TagModels;

import java.util.List;

@Dao
public interface AppDao {

    // ─── DO ───────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDOList(List<DOModels.DOModel> doList);

    @Query("SELECT do_id, do_number, status, created_at, scanner_type FROM tb_DO ORDER BY created_at DESC")
    List<DOModels.DOModel> getAllDO();

    @Query("DELETE FROM tb_DO")
    void deleteAllDO();

    // ─── Scanned Tag ──────────────────────────────────────────────
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

    // ─── Tag Cache ────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTagCache(TagModels.TagCacheEntity cache);

    @Query("SELECT * FROM tb_tag_cache WHERE epc_tag = :key OR tag_id = :key LIMIT 1")
    TagModels.TagCacheEntity getTagCacheByKey(String key);

    @Query("DELETE FROM tb_tag_cache")
    void clearTagCache();

    // ─── Pending Submit ───────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPendingSubmit(PendingSubmitEntity entity);

    @Query("SELECT * FROM tb_pending_submit ORDER BY created_at ASC")
    List<PendingSubmitEntity> getAllPendingSubmit();

    @Query("DELETE FROM tb_pending_submit WHERE id = :id")
    void deletePendingSubmitById(int id);

    // ─── Search Item ──────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSearchItems(List<TagModels.SearchItemEntity> items);

    @Query("SELECT * FROM tb_search_item")
    List<TagModels.SearchItemEntity> getAllSearchItems();

    @Query("DELETE FROM tb_search_item")
    void deleteAllSearchItems();

    // ─── Scan Queue (offline-first stock taking) ──────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScanQueue(StockTakingModels.ScanQueueEntity entity);

    @Query("SELECT * FROM tb_scan_queue WHERE stt_id = :sttId AND is_synced = 0 ORDER BY created_at ASC")
    List<StockTakingModels.ScanQueueEntity> getUnsyncedBySttId(String sttId);

    @Query("SELECT COUNT(*) FROM tb_scan_queue WHERE stt_id = :sttId AND is_synced = 0")
    int countUnsyncedBySttId(String sttId);

    @Query("UPDATE tb_scan_queue SET is_synced = 1 WHERE stt_id = :sttId AND epc_tag = :epc AND action = 'FOUND'")
    void markSyncedByEpc(String sttId, String epc);

    @Query("UPDATE tb_scan_queue SET is_synced = 1 WHERE id = :id")
    void markSyncedById(int id);

    @Query("UPDATE tb_scan_queue SET is_synced = 1 WHERE stt_id = :sttId AND epc_tag IN (:epcs) AND action = 'FOUND'")
    void markBulkSynced(String sttId, List<String> epcs);

    @Query("DELETE FROM tb_scan_queue WHERE stt_id = :sttId AND is_synced = 1")
    void clearSyncedBySttId(String sttId);

    @Query("DELETE FROM tb_scan_queue WHERE stt_id = :sttId")
    void clearAllBySttId(String sttId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSessionItems(List<StockTakingModels.SessionItemEntity> items);

    @Query("SELECT * FROM tb_session_items WHERE stt_id = :sttId")
    List<StockTakingModels.SessionItemEntity> getSessionItemsBySttId(String sttId);

    @Query("DELETE FROM tb_session_items WHERE stt_id = :sttId")
    void clearSessionItemsBySttId(String sttId);

    // ─── StockIn Offline-First ────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStockInScan(StockInScanEntity entity);

    @Query("SELECT * FROM tb_stockin_scan ORDER BY created_at DESC")
    List<StockInScanEntity> getAllStockInScans();

    @Query("SELECT * FROM tb_stockin_scan WHERE is_synced = 0 ORDER BY created_at ASC")
    List<StockInScanEntity> getUnsyncedStockInScans();

    @Query("SELECT COUNT(*) FROM tb_stockin_scan WHERE epc_tag = :epc")
    int countStockInScanByEpc(String epc);

    @Query("UPDATE tb_stockin_scan SET item_id = :itemId, item_name = :itemName, is_resolved = 1 WHERE epc_tag = :epc")
    void resolveStockInScan(String epc, String itemId, String itemName);

    @Query("UPDATE tb_stockin_scan SET location_id = :locationId WHERE is_synced = 0")
    void updateStockInLocation(String locationId);

    @Query("UPDATE tb_stockin_scan SET scanner_type = :scannerType WHERE is_synced = 0")
    void updateStockInScannerType(String scannerType);

    @Query("UPDATE tb_stockin_scan SET is_synced = 1")
    void markAllStockInSynced();

    @Query("DELETE FROM tb_stockin_scan WHERE is_synced = 1")
    void clearSyncedStockInScans();

    @Query("DELETE FROM tb_stockin_scan")
    void clearAllStockInScans();

    @Query("DELETE FROM tb_stockin_scan WHERE epc_tag = :epc")
    void deleteStockInScanByEpc(String epc);
}