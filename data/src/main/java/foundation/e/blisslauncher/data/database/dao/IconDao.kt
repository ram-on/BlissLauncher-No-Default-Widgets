package foundation.e.blisslauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import foundation.e.blisslauncher.data.database.roomentity.IconEntity

@Dao
interface IconDao {
    @Query("DELETE FROM icons WHERE componentName LIKE :componentName AND profileId = :userSerial")
    fun delete(componentName: String, userSerial: Int)

    @Query("DELETE FROM icons WHERE componentName in (:components)")
    fun delete(components: List<String>)

    @Query("DELETE FROM icons")
    fun clear()

    @Query("SELECT * FROM icons WHERE profileId = :userSerial")
    fun query(userSerial: Long): List<IconEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(iconEntity: IconEntity)
}