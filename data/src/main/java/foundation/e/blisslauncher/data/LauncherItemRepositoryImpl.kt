package foundation.e.blisslauncher.data

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.text.TextUtils
import android.util.LongSparseArray
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.util.MultiHashMap
import foundation.e.blisslauncher.data.database.WorkspaceLauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import javax.inject.Inject

class LauncherItemRepositoryImpl
@Inject constructor(
    private val context: Context,
    private val launcherApps: LauncherAppsCompat,
    private val launcherDatabase: LauncherDatabaseGateway,
    private val userManager: UserManagerRepository,
    private val packageManagerHelper: PackageManagerHelper
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

        val allUsers: LongSparseArray<UserHandle> = LongSparseArray()
        val quietMode: LongSparseArray<Boolean> = LongSparseArray()
        val unlockedUsers: LongSparseArray<Boolean> = LongSparseArray()

        userManager.userProfiles.forEach {
            val serialNo = userManager.getSerialNumberForUser(it)
            allUsers.put(serialNo, it)
            quietMode.put(serialNo, userManager.isQuietModeEnabled(it))

            val userUnlocked = userManager.isUserUnlocked(it)
            unlockedUsers.put(serialNo, userUnlocked)
        }

        //Populate item from database and fill necessary details based on users.
        val launcherItems = ArrayList<LauncherItem>()
        launcherDatabase.getAllWorkspaceItems().asSequence()
            .filter { checkAndValidate(allUsers, quietMode, unlockedUsers) }
            .map { }


        launcherDatabase.getAllWorkspaceItems().forEach { item ->
        }

        return launcherItems
    }

    private fun checkAndValidate(
        item: WorkspaceLauncherItem,
        allUsers: LongSparseArray<UserHandle>,
        quietMode: LongSparseArray<Boolean>,
        unlockedUsers: LongSparseArray<Boolean>,
        isSdcardReady: Boolean
    ): Boolean {
        // Load necessary properties.
        val itemType = item.itemType
        val container = item.container
        val id = item._id
        val serialNumber = item.profileId

        //val restoreFlag = getInt(restoredIndex)
        val user = allUsers[item.profileId]
        if (user == null) {
            launcherDatabase.markDeleted(id)
            return false
        }

        when (itemType) {
            LauncherConstants.ItemType.APPLICATION,
            LauncherConstants.ItemType.SHORTCUT,
            LauncherConstants.ItemType.DEEP_SHORTCUT -> {
                val intent = item.getParsedIntent()
                if (intent == null) {
                    launcherDatabase.markDeleted(id)
                    return false
                }

                val disabledState =
                    if (quietMode[serialNumber]) LauncherItemWithIcon.FLAG_DISABLED_QUIET_USER
                    else 0
                val componentName = intent.component
                val targetPackage =
                    if (componentName == null) intent.`package` else componentName.packageName
                if (Process.myUserHandle() != user) {
                    if (itemType == LauncherConstants.ItemType.SHORTCUT) {
                        launcherDatabase.markDeleted(id)
                        return false
                    }
                }

                if (targetPackage.isNullOrEmpty() and (itemType != LauncherConstants.ItemType.SHORTCUT)) {
                    launcherDatabase.markDeleted(id)
                    return false
                }

                // If there is no target  package, its an implicit intent
                // (legacy shortcut) which is always valid
                val validTarget = targetPackage.isNullOrEmpty() ||
                    launcherApps.isPackageEnabledForProfile(targetPackage, user)
                if (componentName != null && validTarget) {
                    if (!launcherApps.isActivityEnabledForProfile(componentName, user)) {
                        launcherDatabase.markDeleted(id)
                        return false
                    }
                }

                // else if componentName == null => can't infer much, leave it
                // else if !validPackage => could be restored icon or missing sd-card

                if (targetPackage?.isNotEmpty() == true && !validTarget) {
                    // Points to a valid app (superset of componentName != null) but the apk
                    // is not available.
                    if(!packageManagerHelper.isAppOnSdcard(targetPackage, user) and isSdcardReady) {
                        launcherDatabase.markDeleted(id)
                        return false
                    }
                }
            }
        }
        return true
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