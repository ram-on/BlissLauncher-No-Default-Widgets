package foundation.e.blisslauncher.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IconDao {
    @Query("DELETE FROM icons WHERE componentName LIKE :componentName AND profileId = :userSerial")
    fun delete(componentName: String, userSerial: Int)

    @Query("DROP TABLE IF EXISTS icons")
    fun clear()

    @Query("SELECT * FROM icons WHERE profileId = :userSerial")
    fun query(userSerial: Int): IconEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(iconEntity: IconEntity)
}