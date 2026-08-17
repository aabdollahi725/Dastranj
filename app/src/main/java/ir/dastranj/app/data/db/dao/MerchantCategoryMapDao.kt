package ir.dastranj.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.dastranj.app.data.db.entity.MerchantCategoryMapEntity

@Dao
interface MerchantCategoryMapDao {

    /**
     * The category to pre-select for a merchant, or null if it has not been seen.
     *
     * Orders by hit count then recency, so one mis-tap cannot re-point a merchant the user has
     * categorised the same way many times before.
     */
    @Query(
        """
        SELECT category_id FROM merchant_category_map
        WHERE merchant_key = :merchantKey
        ORDER BY hit_count DESC, last_used_at DESC
        LIMIT 1
        """
    )
    suspend fun suggestCategoryId(merchantKey: String): Long?

    /** Every category this merchant has been filed under, strongest first. */
    @Query(
        """
        SELECT * FROM merchant_category_map
        WHERE merchant_key = :merchantKey
        ORDER BY hit_count DESC, last_used_at DESC
        """
    )
    suspend fun getByKey(merchantKey: String): List<MerchantCategoryMapEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicate(entry: MerchantCategoryMapEntity): Long

    /**
     * Records one more confirmation of an existing (merchant, category) pairing.
     *
     * Note it matches on both columns and does **not** reassign `category_id`: the pair is the
     * identity of the row, so confirming «کافه لمیز → تفریح» must increment that pairing's own
     * tally rather than overwrite whatever category the merchant last had.
     *
     * Written as an UPDATE rather than a read-modify-write so two rapid entries cannot both read
     * the same count and each write back the same incremented value, losing one.
     */
    @Query(
        """
        UPDATE merchant_category_map
        SET hit_count = hit_count + 1, last_used_at = :nowMillis
        WHERE merchant_key = :merchantKey AND category_id = :categoryId
        """
    )
    suspend fun recordHit(merchantKey: String, categoryId: Long, nowMillis: Long)

    @Query("DELETE FROM merchant_category_map WHERE merchant_key = :merchantKey")
    suspend fun forget(merchantKey: String)
}
