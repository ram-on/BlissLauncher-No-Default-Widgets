package foundation.e.blisslauncher.data

import android.content.Context
import android.os.UserHandle
import android.util.LongSparseArray
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.util.MultiHashMap
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import javax.inject.Inject

class LauncherItemRepositoryImpl
@Inject constructor(
    private val context: Context,
    private val launcherApps: LauncherAppsCompat,
    private val launcherDatabase: LauncherDatabaseGateway,
    private val userManager: UserManagerRepository
) : LauncherItemRepository {

    private val TAG = "LauncherRepositoryImpl"

    override fun <S : LauncherItem> save(entity: S): S {
        TODO("Not yet implemented")
    }

    override fun <S : LauncherItem> saveAll(entities: Iterable<S>): Iterable<S> {
        TODO("Not yet implemented")
    }

    override fun findById(id: Long): LauncherItem? {
        TODO("Not yet implemented")
    }

    override fun findAll(): Iterable<LauncherItem> {
        val pmHelper = PackageManagerHelper(context, launcherApps)
        val isSafeMode = pmHelper.isSafeMode
        val isSdCardReady = Utilities.isBootCompleted()
        val pendingPackages = MultiHashMap<UserHandle, String>()

        val allUsers:LongSparseArray<UserHandle> = LongSparseArray()
        val quietMode:LongSparseArray<Boolean> = LongSparseArray()
        val unlockedUsers:LongSparseArray<Boolean> = LongSparseArray()

        userManager.userProfiles.forEach {
            val serialNo = userManager.getSerialNumberForUser(it)
            allUsers.put(serialNo, it)
            quietMode.put(serialNo, userManager.isQuietModeEnabled(it))

            val userUnlocked = userManager.isUserUnlocked(it)
            unlockedUsers.put(serialNo, userUnlocked)
        }

        //Populate item from database and fill necessary details based on users.
        val launcherItems =  launcherDatabase.loadAllLauncherItems()

        launcherItems.forEach {item ->
            if(item.user == null) {
                launcherDatabase.markDeleted(item.id)
            }

            when(item.itemType) {
                LauncherConstants.ItemType.APPLICATION, LauncherConstants.ItemType.SHORTCUT -> {
                    val intent = item.getIntent()
                    if(intent == null) {
                        launcherDatabase.markDeleted(item.id)
                        return@forEach
                    }
                }
            }
        }

        return launcherItems
    }

    override fun delete(entity: LauncherItem) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Long) {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: Iterable<LauncherItem>) {
        TODO("Not yet implemented")
    }
}