package foundation.e.blisslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import foundation.e.blisslauncher.data.database.dao.IconDao

@Database(entities = [IconDatabase::class], version = 1)
abstract class IconDatabase : RoomDatabase() {
    abstract fun iconDao(): IconDao
}