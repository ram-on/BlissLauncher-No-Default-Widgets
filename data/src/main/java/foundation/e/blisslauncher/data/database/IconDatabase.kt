package foundation.e.blisslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IconDatabase::class], version = 1)
abstract class IconDatabase : RoomDatabase() {
    abstract fun iconDao(): IconDao
}