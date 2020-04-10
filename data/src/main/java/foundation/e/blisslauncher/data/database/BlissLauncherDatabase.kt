package foundation.e.blisslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import foundation.e.blisslauncher.data.database.dao.LauncherItemDao
import foundation.e.blisslauncher.data.database.roomentity.LauncherItemRoomEntity

@Database(entities = [LauncherItemRoomEntity::class], version = 1)
abstract class BlissLauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherItemDao
}