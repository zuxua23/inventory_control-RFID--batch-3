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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDOList(List<DOModels.DOModel> doList);

    @Query("SELECT do_id, do_number, status, created_at, scanner_type FROM tb_DO ORDER BY created_at DESC")
    List<DOModels.DOModel> getAllDO();

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
}