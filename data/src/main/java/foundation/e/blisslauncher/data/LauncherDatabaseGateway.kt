package foundation.e.blisslauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import foundation.e.blisslauncher.data.database.BlissLauncherDatabase
import foundation.e.blisslauncher.data.database.BlissLauncherFiles
import foundation.e.blisslauncher.data.database.roomentity.WorkspaceItem
import foundation.e.blisslauncher.data.database.roomentity.WorkspaceScreen
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class LauncherDatabaseGateway @Inject constructor(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
) {

    private lateinit var db: BlissLauncherDatabase

    private var maxItemId: Long = -1
    private var maxScreenId: Long = -1

    private fun initIds() {
        if (maxItemId == -1L) {
            initializeMaxItemId()
        }

        if (maxScreenId == -1L) {
            initializeMaxScreenId()
        }
    }

    private fun initializeMaxItemId() {
        getMaxId(db, "launcherItems")
    }

    private fun initializeMaxScreenId() {
        getMaxId(db, "workspaceScreens")
    }

    private fun getMaxId(
        db: BlissLauncherDatabase,
        tableName: String
    ) {
        Observable.fromCallable {
            db.launcherDao().getMaxIdInTable(SimpleSQLiteQuery("SELECT MAX(_id) FROM $tableName"))
        }.subscribeOn(Schedulers.io())
            .subscribe {
                if (it == -1L) {
                    throw RuntimeException("Error: could not query max id in $tableName")
                }
                if (tableName == "launcherItems")
                    maxItemId = it
                else if (tableName == "workspaceScreens")
                    maxScreenId = it
                else throw IllegalArgumentException("Error: specified table doesn't match")
            }
    }

    fun createEmptyDatabase() {
        db.launcherDao().createEmptyDb()
    }

    fun insertAndCheck(item: WorkspaceItem): Long {
        checkItemId(item)
        return db.launcherDao().insert(item)
    }

    fun checkItemId(item: WorkspaceItem) {
        val id = item._id
        maxItemId = Math.max(id, maxItemId)
    }

    fun generateNewItemId(): Long {
        if (maxItemId < 0) {
            throw RuntimeException("Error: max item id was not initialized")
        }
        maxItemId += 1
        return maxItemId
    }

    fun generateNewScreenId(): Long {
        if (maxScreenId < 0) {
            throw RuntimeException("Error: max screen id was not initialized")
        }

        maxScreenId += 1
        return maxScreenId
    }

    fun deleteEmptyFolders() {}

    fun getAllWorkspaceItems(): List<WorkspaceItem> = db.launcherDao().getAllWorkspaceItems()

    fun loadWorkspaceScreensInOrder(): List<Long> =
        db.launcherDao().getAllWorkspaceScreens().map { it._id }

    fun markDeleted(id: Long) {}

    fun markDeleted(item: WorkspaceItem) {
        markDeleted(item._id)
    }

    fun saveAll(items: ArrayList<WorkspaceItem>) {
        db.launcherDao().insertAll(items)
    }

    // Helper function to initialise the database
    fun createDbIfNotExist() {
        db = Room.databaseBuilder(
            context,
            BlissLauncherDatabase::class.java,
            BlissLauncherFiles.LAUNCHER_DB
        ).addCallback(
            object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Timber.d("Room database created")
                    maxItemId = 0
                    maxScreenId = 0
                    sharedPrefs.edit().putBoolean(EMPTY_DATABASE_CREATED, true).commit()
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Timber.d("Room database opened")
                    initIds()
                }
            }
        ).build()
        db.openHelper.readableDatabase
    }

    fun updateWorkspaceScreenOrder(screenIds: ArrayList<Long>) {
        val list = ArrayList<WorkspaceScreen>()
        for(i in screenIds.indices) {
            val id = screenIds[i]
            if(id >= 0) {
                list.add(WorkspaceScreen(id, i))
            }
        }
        db.launcherDao().insertAllWorkspaceScreens(list)
    }

    companion object {
        const val EMPTY_DATABASE_CREATED = "EMPTY_DATABASE_CREATED"
    }
}