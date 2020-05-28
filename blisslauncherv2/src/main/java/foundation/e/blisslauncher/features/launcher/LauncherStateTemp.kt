package foundation.e.blisslauncher.features.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import androidx.core.util.set
import foundation.e.blisslauncher.common.util.LongArrayMap
import foundation.e.blisslauncher.domain.ItemInfoMatcher
import foundation.e.blisslauncher.domain.Matcher
import foundation.e.blisslauncher.domain.addFlag
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.FolderItem
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.removeFlag

/**
 * Stores data related to Launcher in memory.
 */
sealed class LauncherStateTemp constructor(
    /*val context: Context,
    val launcherApps: LauncherAppsCompat,*/
    /**
     * Map of all the items (apps, shortcuts, folder or widgets) to their ids
     */
    val itemsIdMap: LongArrayMap<LauncherItem>,

    /**
     * List of all apps, folders, shortcuts and widgets directly on screen
     * (no apps, shortcuts within folders).
     */
    val allItems: List<LauncherItem>,

    /**
     * Map of all the folders to their ids
     */
    val folders: LongArrayMap<FolderItem>,

    /**
     * Ordered list of workspace screen ids
     */
    val workspaceScreen: List<Long>,

    /** The list of all apps. */
    val data: List<ApplicationItem>
) {

    @Synchronized
    fun clear() {
        itemsIdMap.clear()
        folders.clear()
    }

    /*override fun getAllActivities(
        user: UserHandle,
        quietMode: Boolean
    ): List<ApplicationItem> {
        val apps = launcherApps.getActivityList(null, user)
        if (apps.isNotEmpty()) {
            apps.forEach {
                add(ApplicationItem(it, user, quietMode), it)
            }
        }
        return data
    }

    override fun add(
        packageName: String,
        user: UserHandle,
        quietMode: Boolean
    ): ArrayList<ApplicationItem> {
        val addedPackageApps = ArrayList<ApplicationItem>()
        val matches = launcherApps.getActivityList(packageName, user)
        matches.forEach { info ->
            add(ApplicationItem(
                info, user, quietMode
            ).apply {
                id = System.nanoTime()
            }.let {
                addedPackageApps.add(it)
                it
            },
                info
            )
        }
        return addedPackageApps
    }
*/
    fun remove(packageName: String, user: UserHandle) {
        val data = data
        val iterator = data.iterator()
        /*while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.componentName.packageName == packageName && item.user == user) {
                removed.add(item)
                iterator.remove()
            }
        }*/
    }

    /*override fun updatedPackages(
        packages: Array<out String>,
        user: UserHandle,
        quietMode: Boolean
    ): List<LauncherItem> {
        //TODO: Update icon cache for packages
        val addedApps = ArrayList<ApplicationItem>()
        val modifiedApps = ArrayList<ApplicationItem>()

        val removedPackages = HashSet<String>()
        val removedComponents = HashSet<ComponentName>()

        packages.forEach {
            if (!launcherApps.isPackageEnabledForProfile(it, user)) {
                removedPackages.add(it)
            } else {
                val matches = launcherApps.getActivityList(it, user)
                if (matches.isNotEmpty()) {
                    removedComponents.addAll(
                        removeIfNoActivityFound(
                            context,
                            matches,
                            it,
                            user
                        )
                    )

                    matches.forEach {
                        var applicationItem =
                            findApplicationItem(it.componentName, user)
                        if (applicationItem == null) {
                            applicationItem =
                                ApplicationItem(it, user, quietMode)
                            add(applicationItem, it)
                            addedApps.add(applicationItem)
                        } else {
                            //TODO: update icon and title
                            modifiedApps.add(applicationItem)
                        }
                    }
                } else {
                    removedPackages.add(it)
                }
            }
        }
        val flagOp = removeFlag(LauncherItemWithIcon.FLAG_DISABLED_NOT_AVAILABLE)
        val matcher = Matcher.ofPackages(packages.toHashSet(), user)
        itemsIdMap.forEach {
            //TODO: If user and packageSet of icon resource equals,
            //TODO: Update item flag here.
        }
        return modifiedApps
    }*/

    fun suspendPackages(
        packages: Array<out String>,
        user: UserHandle
    ): List<LauncherItem> {
        val matcher: ItemInfoMatcher = Matcher.ofPackages(packages.toHashSet(), user)
        return updateDisabledFlags(
            matcher,
            addFlag(LauncherItemWithIcon.FLAG_DISABLED_SUSPENDED)
        )
    }

    fun unsuspendPackages(
        packages: Array<out String>,
        user: UserHandle
    ): List<LauncherItem> {
        val matcher: ItemInfoMatcher = Matcher.ofPackages(packages.toHashSet(), user)
        return updateDisabledFlags(
            matcher,
            removeFlag(LauncherItemWithIcon.FLAG_DISABLED_SUSPENDED)
        )
    }

    fun updateUserAvailability(user: UserHandle, quietMode: Boolean): List<LauncherItem> {
        val matcher: ItemInfoMatcher = Matcher.ofUser(user)
        val flagOp = if (quietMode) addFlag else removeFlag
        return updateDisabledFlags(
            matcher,
            flagOp(LauncherItemWithIcon.FLAG_DISABLED_QUIET_USER)
        )
    }

    fun makePackagesUnavailable(
        packages: Array<out String>,
        user: UserHandle
    ): List<LauncherItem> {
        val matcher = Matcher.ofPackages(packages.toHashSet(), user)
        return updateDisabledFlags(
            matcher,
            addFlag(LauncherItemWithIcon.FLAG_DISABLED_NOT_AVAILABLE)
        )
    }

    fun removePackages(packages: Array<out String>, user: UserHandle): List<LauncherItem> {
        val removedPackages = packages.toHashSet()
        val matcher = Matcher.ofPackages(removedPackages, user)
        // Remove any queued items from the install queue
        //TODO: InstallShortcutReceiver.removeFromInstallQueue(context, removedPackages, mUser)
        return removePackages(matcher)
    }

    @Synchronized
    fun removeItem(context: Context, vararg items: LauncherItem) {
        items.forEach {
            when (it.itemType) {
                LauncherConstants.ItemType.FOLDER -> {
                    folders.remove(it.id)
                    //allItems.remove(it)
                }
                LauncherConstants.ItemType.APPLICATION,
                LauncherConstants.ItemType.SHORTCUT -> {
                    //allItems.remove(it)
                }
            }
            itemsIdMap.remove(it.id)
        }
    }

    @Synchronized
    fun addItem(item: LauncherItem, newItem: Boolean): LauncherStateTemp {
        val mutableAllItems = allItems.toMutableList()
        when (item.itemType) {
            LauncherConstants.ItemType.FOLDER -> {
                folders.put(item.id, item as FolderItem)
                mutableAllItems.add(item)
            }
            LauncherConstants.ItemType.APPLICATION,
            LauncherConstants.ItemType.SHORTCUT -> {
                /*if (item.container.toInt() == LauncherConstants.ContainerType.CONTAINER_DESKTOP ||
                    item.container.toInt() == LauncherConstants.ContainerType.CONTAINER_HOTSEAT
                ) {
                    mutableAllItems.add(item)
                } else {
                    if (newItem) {
                        if (!folders.containsKey(item.container)) {
                        }
                    } else {
                        findOrMakeFolder(item.container).add(item as AppShortcutItem, false)
                    }
                }*/
            }
        }
        return this
    }

    fun findOrMakeFolder(id: Long): FolderItem {
        var folderItem: FolderItem? = folders[id]
        if (folderItem == null) {
            folderItem = FolderItem()
            folders[id] = folderItem
        }
        return folderItem
    }

    /**
     * Returns whether *apps* contains *component*.
     */
    fun checkForComponent(
        apps: List<LauncherActivityInfo>,
        component: ComponentName
    ): Boolean {
        for (info in apps) {
            if (info.componentName == component) {
                return true
            }
        }
        return false
    }

    /**
     * Finds an application item corresponding to the given component name and user.
     */
    fun findApplicationItem(
        componentName: ComponentName,
        user: UserHandle
    ): ApplicationItem? {
        for (item in data) {
            if (componentName == item.componentName && user == item.user) {
                return item
            }
        }
        return null
    }

    /*fun add(item: ApplicationItem, info: LauncherActivityInfo): LauncherState {
        if (findApplicationItem(item.componentName, item.user) != null) {
            return
        }
        // TODO: Update icon from IconCache
        val mutableData = data.toMutableList()
        mutableData.add(item)
        return copy(data = mutableData)
        data.add(item)
        addItem(item, true)
    }*/

    @Synchronized
    fun updateDisabledFlags(
        matcher: ItemInfoMatcher,
        flagOp: (oldFlags: Int) -> Int
    ): List<LauncherItem> {
        val updatedItems = ArrayList<LauncherItem>()
        itemsIdMap.filter {
            it is LauncherItemWithIcon && matcher(it, it.getTargetComponent()!!)
        }.forEach {
            it as LauncherItemWithIcon
            val oldFlags = it.runtimeStatusFlags
            it.apply { flagOp(runtimeStatusFlags) }
            if (it.runtimeStatusFlags != oldFlags)
                updatedItems.add(it)
        }
        return updatedItems
    }

    @Synchronized
    fun removePackages(
        matches: (item: LauncherItem, cn: ComponentName) -> Boolean
    ): List<LauncherItem> {
        val removedItems = HashSet<LauncherItem>()
        itemsIdMap.forEach {
            if (it is LauncherItemWithIcon) it.let {
                val cn = it.getTargetComponent()
                if (cn != null && matches(it, cn)) removedItems.add(it)
            } else if (it is FolderItem) it.let { folder ->
                folder.contents.forEach {
                    val cn = it.getTargetComponent()
                    if (cn != null && matches(it, cn)) removedItems.add(it)
                }
            }
        }
        //TODO: delete items from database sequentially and remove them from itemsIdMap
        return removedItems.toList()
    }

    @Synchronized
    fun removeIfNoActivityFound(
        context: Context,
        matches: List<LauncherActivityInfo>,
        packageName: String,
        user: UserHandle
    ): HashSet<ComponentName> {
        val removedComponents = HashSet<ComponentName>()
        val modified = ArrayList<ApplicationItem>()
        itemsIdMap.filter { it.itemType == LauncherConstants.ItemType.APPLICATION }
            .forEach {
                it as ApplicationItem
                if (it.user == user && packageName == it.componentName.packageName) {
                    if (!findActivity(matches, it.componentName)) {
                        removedComponents.add(it.componentName)
                    }
                }
            }
        return removedComponents
    }

    /**
     * Returns whether *apps* contains *component*.
     */
    private fun findActivity(
        apps: List<LauncherActivityInfo>,
        component: ComponentName
    ): Boolean {
        for (info in apps) {
            if (info.componentName == component) {
                return true
            }
        }
        return false
    }

    /**
     * Find an AppInfo object for the given componentName
     *
     * @return the corresponding AppInfo or null
     */
    fun findAppInfo(
        componentName: ComponentName,
        user: UserHandle
    ): ApplicationItem? {
        for (item in itemsIdMap) {
            if (item is ApplicationItem &&
                componentName == item.componentName &&
                user == item.user
            ) {
                return item
            }
        }
        return null
    }

    fun addItem(
        item: LauncherItem,
        mutableData: MutableList<ApplicationItem>,
        mutableAllItems: MutableList<LauncherItem>,
        newItem: Boolean
    ) {
        mutableData.add(item as ApplicationItem)
        itemsIdMap.put(item.id, item)
        when (item.itemType) {
            LauncherConstants.ItemType.FOLDER -> {
                folders.put(item.id, item as FolderItem)
                mutableAllItems.add(item)
            }
            LauncherConstants.ItemType.APPLICATION,
            LauncherConstants.ItemType.SHORTCUT -> {
                /*if (item.container.toInt() == LauncherConstants.ContainerType.CONTAINER_DESKTOP ||
                    item.container.toInt() == LauncherConstants.ContainerType.CONTAINER_HOTSEAT
                ) {
                    mutableAllItems.add(item)
                } else {
                    if (newItem) {
                        if (!folders.containsKey(item.container)) {
                        }
                    } else {
                        findOrMakeFolder(item.container).add(
                            item as AppShortcutItem,
                            false
                        )
                    }
                }*/
            }
        }
    }

    companion object {
        const val TAG = "LauncherModelStore"
    }
}