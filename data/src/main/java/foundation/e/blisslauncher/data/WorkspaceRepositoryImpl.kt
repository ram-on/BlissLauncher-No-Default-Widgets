package foundation.e.blisslauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherActivityInfo
import android.os.Process
import android.os.UserHandle
import android.util.LongSparseArray
import androidx.core.graphics.drawable.toBitmap
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import foundation.e.blisslauncher.common.util.MultiHashMap
import foundation.e.blisslauncher.data.database.roomentity.WorkspaceItem
import foundation.e.blisslauncher.data.parser.DefaultHotseatParser
import foundation.e.blisslauncher.data.shortcuts.PinnedShortcutManager
import foundation.e.blisslauncher.data.util.LauncherItemComparator
import foundation.e.blisslauncher.domain.dto.WorkspaceModel
import foundation.e.blisslauncher.domain.entity.ShortcutItem
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.FolderItem
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.keys.ShortcutKey
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import foundation.e.blisslauncher.domain.repository.WorkspaceRepository
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

class WorkspaceRepositoryImpl
@Inject constructor(
    private val context: Context,
    private val launcherApps: LauncherAppsCompat,
    private val launcherDatabase: LauncherDatabaseGateway,
    private val userManager: UserManagerRepository,
    private val packageManagerHelper: PackageManagerHelper,
    private val shortcutManager: PinnedShortcutManager,
    private val sharedPrefs: SharedPreferences,
    private val idp: InvariantDeviceProfile,
    private val launcherItemComparator: LauncherItemComparator
) : WorkspaceRepository {

    override fun loadWorkspace(): WorkspaceModel {
        val workspaceModel = WorkspaceModel()
        val pmHelper = PackageManagerHelper(context, launcherApps)
        val isSafeMode = pmHelper.isSafeMode
        val isSdCardReady = Utilities.isBootCompleted()
        val pendingPackages = MultiHashMap<UserHandle, String>()
        val shortcutKeyToPinnedShortcuts = HashMap<ShortcutKey, ShortcutInfoCompat>()
        val allUsers: LongSparseArray<UserHandle> = LongSparseArray()
        val quietMode: LongSparseArray<Boolean> = LongSparseArray()
        val unlockedUsers: LongSparseArray<Boolean> = LongSparseArray()

        var clearDb = false

        //TODO: GridSize Migration Task
        /*if (!clearDb && GridSizeMigrationTask.ENABLED &&
            !GridSizeMigrationTask.migrateGridIfNeeded(context)
        ) {
            // Migration failed. Clear workspace.
            clearDb = true
        }*/

        if (clearDb) {
            Timber.d("loadLauncher: resetting launcher database")
            clearAllDbs()
        }

        val allAppsMap = hashMapOf<ComponentName, LauncherActivityInfo>()

        userManager.userProfiles.forEach { user ->
            val serialNo = userManager.getSerialNumberForUser(user)
            allUsers.put(serialNo, user)
            quietMode.put(serialNo, userManager.isQuietModeEnabled(user))

            var userUnlocked = userManager.isUserUnlocked(user)

            // Query for pinned shortcuts only when user is unlocked.
            if (userUnlocked) {
                val pinnedShortcuts =
                    shortcutManager.queryForPinnedShortcuts(null, user)
                if (shortcutManager.wasLastCallSuccess()) {
                    pinnedShortcuts.map { shortcut -> ShortcutKey.fromShortcutInfoCompat(shortcut) to shortcut }
                        .toMap(shortcutKeyToPinnedShortcuts)
                } else {
                    // Shortcut Manager can fail due to various reasons.
                    // Consider this condition as user locked.
                    userUnlocked = false
                }
            }
            unlockedUsers.put(serialNo, userUnlocked)
            launcherApps.getActivityList(null, user).forEach {
                allAppsMap[it.componentName] = it
            }
        }

        loadDefaultWorkspaceIfNecessary(allAppsMap)

        //Populate item from database and fill necessary details based on users.
        val launcherDatabaseItems = launcherDatabase.getAllWorkspaceItems()
        Timber.d("LauncherDatabase size is ${launcherDatabaseItems.size}")
        launcherDatabaseItems
            .forEach {
                it.apply {
                    user = allUsers[profileId]
                    validTarget =
                        targetPackage.isNullOrEmpty() or launcherApps.isPackageEnabledForProfile(
                            targetPackage,
                            user
                        )
                }

                if (!checkAndValidate(
                        it,
                        unlockedUsers,
                        shortcutKeyToPinnedShortcuts,
                        isSdCardReady
                    )
                )
                    return@forEach

                val launcherItem = convertToLauncherItem(
                    it,
                    quietMode,
                    isSdCardReady,
                    isSafeMode,
                    unlockedUsers,
                    pendingPackages,
                    shortcutKeyToPinnedShortcuts
                )
                if (launcherItem != null && checkAndAddItem(launcherItem, workspaceModel)) {
                    if (launcherItem.itemType == LauncherConstants.ItemType.APPLICATION) {
                        launcherItem.getTargetComponent()?.let { componentName ->
                            allAppsMap.remove(componentName)
                        }
                    }
                }

            }

        Timber.d(
            "Size of launcherItems before processing remaining app items: " +
                "${workspaceModel.workspaceItems.size}"
        )
        allAppsMap.values.forEach { info ->
            val applicationItem = ApplicationItem(
                info,
                info.user,
                quietMode[userManager.getSerialNumberForUser(info.user)]
            )
            applicationItem.iconBitmap = info.getBadgedIcon(0).toBitmap()
            checkAndAddItem(applicationItem, workspaceModel)
        }

        sortWorkspaceItems(workspaceModel.workspaceItems)

        return workspaceModel
    }

    private fun checkAndAddItem(
        launcherItem: LauncherItem,
        workspaceModel: WorkspaceModel
    ): Boolean {
        workspaceModel.addItem(context, launcherItem, false)
        return true
    }

    private fun loadDefaultWorkspaceIfNecessary(allAppsMap: HashMap<ComponentName, LauncherActivityInfo>) {
        launcherDatabase.createDbIfNotExist()
        if (sharedPrefs.getBoolean(LauncherDatabaseGateway.EMPTY_DATABASE_CREATED, false)) {
            val hotseatParser =
                DefaultHotseatParser(launcherDatabase, idp.defaultLayoutId, context, userManager)
            val count = hotseatParser.loadDefaultLayout()
            Timber.d("Hotseat count is $count")
            val existingWorkspaceItems = launcherDatabase.getAllWorkspaceItems()
            val apps = ArrayList<WorkspaceItem>()

            allAppsMap.values.forEach {
                if (!isApplicationAlreadyAdded(existingWorkspaceItems, it.componentName)) {
                    val intent = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(it.componentName)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                    apps.add(
                        WorkspaceItem(
                            launcherDatabase.generateNewItemId(),
                            it.label.toString(),
                            intent.toUri(0),
                            LauncherConstants.ContainerType.CONTAINER_DESKTOP,
                            -1, -1, -1, LauncherConstants.ItemType.APPLICATION
                            , 0, userManager.getSerialNumberForUser(Process.myUserHandle())
                        )
                    )
                }
            }

            launcherDatabase.saveAll(apps)
        }
    }

    private fun isApplicationAlreadyAdded(
        existingWorkspaceItems: List<WorkspaceItem>,
        componentName: ComponentName
    ): Boolean {
        for (i in existingWorkspaceItems.indices) {
            val item = existingWorkspaceItems[i]
            if (item.componentName != null && item.componentName == componentName) {
                return true
            }
        }
        return false
    }

    private fun checkAndValidate(
        item: WorkspaceItem,
        unlockedUsers: LongSparseArray<Boolean>,
        shortcutKeyToPinnedShortcuts: HashMap<ShortcutKey, ShortcutInfoCompat>,
        isSdcardReady: Boolean
    ): Boolean {

        //val restoreFlag = getInt(restoredIndex)

        if (item.user == null) {
            launcherDatabase.markDeleted(item)
            return false
        }

        when (item.itemType) {
            LauncherConstants.ItemType.APPLICATION,
            LauncherConstants.ItemType.SHORTCUT,
            LauncherConstants.ItemType.DEEP_SHORTCUT -> {
                if (item.intent == null) {
                    launcherDatabase.markDeleted(item)
                    return false
                }

                if (Process.myUserHandle() != item.user) {
                    if (item.itemType == LauncherConstants.ItemType.SHORTCUT) {
                        launcherDatabase.markDeleted(item)
                        return false
                    }
                }

                if (item.targetPackage.isNullOrEmpty() and (item.itemType != LauncherConstants.ItemType.SHORTCUT)) {
                    launcherDatabase.markDeleted(item)
                    return false
                }

                // If there is no target  package, its an implicit intent
                // (legacy shortcut) which is always valid
                val validTarget = item.targetPackage.isNullOrEmpty() ||
                    launcherApps.isPackageEnabledForProfile(item.targetPackage, item.user)
                if (item.componentName != null && validTarget) {
                    if (!launcherApps.isActivityEnabledForProfile(item.componentName, item.user)) {
                        launcherDatabase.markDeleted(item)
                        return false
                    }
                }

                // else if componentName == null => can't infer much, leave it
                // else if !validPackage => could be restored icon or missing sd-card

                if (item.targetPackage?.isNotEmpty() == true && !validTarget) {
                    // Points to a valid app (superset of componentName != null) but the apk
                    // is not available.
                    if (!packageManagerHelper.isAppOnSdcard(
                            item.targetPackage,
                            item.user!!
                        ) and isSdcardReady
                    ) {
                        launcherDatabase.markDeleted(item)
                        return false
                    }
                }

                if (item.itemType == LauncherConstants.ItemType.DEEP_SHORTCUT) {
                    val shortcutKey = ShortcutKey.fromIntent(item.intent, item.user!!)
                    if (unlockedUsers.get(item.profileId)) {
                        val pinnedShortcut: ShortcutInfoCompat? =
                            shortcutKeyToPinnedShortcuts.get(shortcutKey)
                        if (pinnedShortcut == null) {
                            // The shortcut is no longer valid.
                            launcherDatabase.markDeleted(item)
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    private fun sortWorkspaceItems(launcherItems: List<LauncherItem>) {
        val screenCols = idp.numColumns
        val screenRows = idp.numRows
        val screenCellCount = screenCols * screenRows
        Collections.sort(launcherItems, { lhs, rhs ->
            if (lhs.container == rhs.container) {
                when (lhs.container) {
                    LauncherConstants.ContainerType.CONTAINER_DESKTOP -> {
                        val lr = lhs.screenId * screenCellCount + lhs.cellY * screenCols + lhs.cellX
                        val rr = rhs.screenId * screenCellCount + rhs.cellY * screenCols + rhs.cellX
                        val result = lr.compareTo(rr)
                        if (result != 0) result
                        else launcherItemComparator.compare(lhs, rhs)
                    }
                    LauncherConstants.ContainerType.CONTAINER_HOTSEAT -> {
                        lhs.screenId.compareTo(rhs.screenId)
                    }
                    else -> throw RuntimeException("Unexpected container type when sorting: ${lhs}")
                }
            } else {
                lhs.container.compareTo(rhs.container)
            }
        })
    }

    private fun convertToLauncherItem(
        item: WorkspaceItem,
        quietMode: LongSparseArray<Boolean>,
        isSdcardReady: Boolean,
        isSafeMode: Boolean,
        unlockedUsers: LongSparseArray<Boolean>,
        pendingPackages: MultiHashMap<UserHandle, String>,
        shortcutKeyToPinnedShortcuts: HashMap<ShortcutKey, ShortcutInfoCompat>
    ): LauncherItem? {
        var allowMissingTarget = false
        // Load necessary properties.
        val itemType = item.itemType
        var disabledState =
            if (quietMode[item.profileId]) LauncherItemWithIcon.FLAG_DISABLED_QUIET_USER else 0
        //val restoreFlag = getInt(restoredIndex)
        return when (itemType) {
            LauncherConstants.ItemType.APPLICATION,
            LauncherConstants.ItemType.SHORTCUT,
            LauncherConstants.ItemType.DEEP_SHORTCUT -> {
                if (item.targetPackage?.isNotEmpty() == true and !item.validTarget) {
                    // Points to a valid app but the apk is not available.
                    if (packageManagerHelper.isAppOnSdcard(item.targetPackage, item.user!!)) {
                        disabledState =
                            disabledState or LauncherItemWithIcon.FLAG_DISABLED_NOT_AVAILABLE
                        allowMissingTarget = true
                    } else if (!isSdcardReady) {
                        Timber.d("Missing Package ${item.targetPackage}, will be checked later")
                        pendingPackages.addToList(item.user, item.targetPackage)
                        allowMissingTarget = true
                    }
                }

                var launcherItem: ShortcutItem? = null
                if (item.itemType == LauncherConstants.ItemType.APPLICATION) {
                    launcherItem = getApplicationItem(
                        item.user!!,
                        quietMode[item.profileId],
                        item.intent!!,
                        allowMissingTarget,
                        item.title
                    )
                } else if (item.itemType == LauncherConstants.ItemType.DEEP_SHORTCUT) {
                    val shortcutKey = ShortcutKey.fromIntent(item.intent!!, item.user!!)
                    if (unlockedUsers.get(item.profileId)) {
                        val pinnedShortcut = shortcutKeyToPinnedShortcuts[shortcutKey]
                        if (pinnedShortcut != null) {
                            launcherItem = ShortcutItem()
                                .apply {
                                    this.user = item.user!!
                                    this.itemType = LauncherConstants.ItemType.DEEP_SHORTCUT
                                    this.intent = pinnedShortcut.makeIntent()
                                    this.title = pinnedShortcut.getShortLabel()
                                    this.runtimeStatusFlags = if (pinnedShortcut.isEnabled()) {
                                        this.runtimeStatusFlags and LauncherItemWithIcon.FLAG_DISABLED_BY_PUBLISHER.inv()
                                    } else {
                                        this.runtimeStatusFlags or LauncherItemWithIcon.FLAG_DISABLED_BY_PUBLISHER
                                    }
                                    this.disabledMessage = pinnedShortcut.getDisabledMessage()
                                }
                            //TODO: Set Icon here
                            if (packageManagerHelper.isAppSuspended(
                                    pinnedShortcut.getPackage(),
                                    launcherItem.user
                                )
                            ) {
                                launcherItem.runtimeStatusFlags =
                                    launcherItem.runtimeStatusFlags or LauncherItemWithIcon.FLAG_DISABLED_SUSPENDED
                            }
                        }
                    } else {
                        launcherItem = ShortcutItem()
                            .apply {
                                this.user = item.user!!
                                this.itemType = item.itemType
                                this.title = if (item.title.isEmpty()) "" else item.title
                                // TODO: Set Icon here
                            }
                        launcherItem.runtimeStatusFlags =
                            launcherItem.runtimeStatusFlags or LauncherItemWithIcon.FLAG_DISABLED_LOCKED_USER
                    }
                } else {
                    launcherItem = ShortcutItem()
                        .apply {
                            this.user = item.user!!
                            this.itemType = item.itemType
                            this.title = if (item.title.isEmpty()) "" else item.title
                            // TODO: Set Icon here
                        }

                    val intent = item.intent!!
                    if (intent.action != null &&
                        intent.categories != null &&
                        intent.action == Intent.ACTION_MAIN &&
                        intent.categories.contains(Intent.CATEGORY_LAUNCHER)
                    ) {
                        item.intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        )
                    }
                }
                if (launcherItem != null) {
                    launcherItem.apply {
                        item.applyCommonProperties(this)
                    }
                    launcherItem.intent = item.intent
                    launcherItem.rank = item.rank
                    launcherItem.runtimeStatusFlags =
                        launcherItem.runtimeStatusFlags or disabledState
                    if (!isSafeMode && !Utilities.isSystemApp(context, item.intent)) {
                        launcherItem.runtimeStatusFlags =
                            launcherItem.runtimeStatusFlags or LauncherItemWithIcon.FLAG_DISABLED_SAFEMODE
                    }
                }
                launcherItem
            }
            LauncherConstants.ItemType.FOLDER -> {
                val folderItem = FolderItem()
                    .apply {
                        item.applyCommonProperties(this)
                        title = item.title
                    }
                folderItem
            }
            else -> throw RuntimeException("Unexpected type of LauncherItem encountered")
        }
    }

    private fun getApplicationItem(
        user: UserHandle,
        quietMode: Boolean,
        intent: Intent,
        allowMissingTarget: Boolean,
        title: String?
    ): ApplicationItem? {
        val componentName = intent.component
        if (componentName == null) {
            Timber.d("Missing component found in getApplicationItem")
            return null
        }
        val newIntent = Intent(Intent.ACTION_MAIN, null)
            .apply {
                component = componentName
            }
        newIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val lai = launcherApps.resolveActivity(newIntent, user)
        if ((lai == null) && !allowMissingTarget) {
            Timber.d("Missing activity found in getApplicationItem")
            return null
        }

        return if (lai != null) {
            val applicationItem = ApplicationItem(lai, user, quietMode)
                .apply { itemType = LauncherConstants.ItemType.APPLICATION }
            val isSuspended = packageManagerHelper.isAppSuspended(lai.applicationInfo)
            if (isSuspended) {
                applicationItem.runtimeStatusFlags =
                    applicationItem.runtimeStatusFlags or LauncherItemWithIcon.FLAG_DISABLED_SUSPENDED
            }
            applicationItem
        } else {
            val applicationItem = ApplicationItem()
                .apply {
                    this.user = user
                    this.intent = newIntent
                    this.componentName = componentName
                    this.title = title
                }

            if (applicationItem.title == null) {
                applicationItem.title = componentName.className
            }
            applicationItem
        }
    }

    private fun clearAllDbs() {
        launcherDatabase.createEmptyDatabase()
    }
}