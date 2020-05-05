package foundation.e.blisslauncher.data

import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import android.util.LongSparseArray
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import foundation.e.blisslauncher.utils.MultiHashMap
import foundation.e.blisslauncher.data.database.roomentity.LauncherItemRoomEntity
import foundation.e.blisslauncher.data.shortcuts.PinnedShortcutManager
import foundation.e.blisslauncher.domain.entity.AppShortcutItem
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.FolderItem
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.keys.ShortcutKey
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import timber.log.Timber
import javax.inject.Inject

class LauncherItemRepositoryImpl
@Inject constructor(
    private val context: Context,
    private val launcherApps: LauncherAppsCompat,
    private val launcherDatabase: LauncherDatabaseGateway,
    private val userManager: UserManagerRepository,
    private val packageManagerHelper: PackageManagerHelper,
    private val shortcutManager: PinnedShortcutManager
) : LauncherItemRepository {

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
        val shortcutKeyToPinnedShortcuts = HashMap<ShortcutKey, ShortcutInfoCompat>()
        val allUsers: LongSparseArray<UserHandle> = LongSparseArray()
        val quietMode: LongSparseArray<Boolean> = LongSparseArray()
        val unlockedUsers: LongSparseArray<Boolean> = LongSparseArray()

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
        }

        //Populate item from database and fill necessary details based on users.
        val launcherItems = ArrayList<LauncherItem>()
        launcherDatabase.getAllWorkspaceItems()
            .onEach {
                it.apply {
                    user = allUsers[profileId]
                    validTarget =
                        targetPackage.isNullOrEmpty() or launcherApps.isPackageEnabledForProfile(
                            targetPackage,
                            user
                        )
                }
            }
            .filter {
                checkAndValidate(
                    it,
                    unlockedUsers,
                    shortcutKeyToPinnedShortcuts,
                    isSdCardReady
                )
            }
            .mapNotNull {
                convertToLauncherItem(
                    it,
                    quietMode,
                    isSdCardReady,
                    isSafeMode,
                    unlockedUsers,
                    pendingPackages,
                    shortcutKeyToPinnedShortcuts
                )
            }
            .toCollection(launcherItems)

        return launcherItems
    }

    private fun checkAndValidate(
        item: LauncherItemRoomEntity,
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

    private fun convertToLauncherItem(
        item: LauncherItemRoomEntity,
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

                var launcherItem: AppShortcutItem? = null
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
                            launcherItem = AppShortcutItem()
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
                        launcherItem = AppShortcutItem()
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
                    launcherItem = AppShortcutItem()
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
    ): AppShortcutItem? {
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