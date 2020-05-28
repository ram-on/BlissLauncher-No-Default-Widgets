package foundation.e.blisslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import foundation.e.blisslauncher.data.database.dao.LauncherDao
import foundation.e.blisslauncher.data.database.roomentity.WorkspaceItem
import foundation.e.blisslauncher.data.database.roomentity.WorkspaceScreen

@Database(entities = [WorkspaceItem::class, WorkspaceScreen::class], version = 1)
abstract class BlissLauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao
}