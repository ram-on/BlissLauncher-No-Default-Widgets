package foundation.e.blisslauncher.data.icon

import android.R
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.text.TextUtils
import android.util.Log
import foundation.e.blisslauncher.common.BitmapRenderer
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.data.database.dao.IconDao
import foundation.e.blisslauncher.data.database.roomentity.IconEntity
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.entity.PackageItem
import foundation.e.blisslauncher.domain.keys.ComponentKey
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import timber.log.Timber
import java.util.HashSet
import java.util.Stack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconCache @Inject constructor(
    private val context: Context,
    private val inv: InvariantDeviceProfile,
    private val iconProvider: IconProvider,
    private val launcherApps: LauncherAppsCompat,
    private val userManager: UserManagerRepository,
    private val iconDao: IconDao,
    private val launcherIcons: LauncherIcons
) {

    data class CacheEntry(
        var bitmap: Bitmap? = null,
        var title: CharSequence? = "",
        var contentDescription: CharSequence = ""
    )

    private val mDefaultIcons: HashMap<UserHandle, Bitmap> = HashMap()
    private val packageManager: PackageManager = context.packageManager
    private val cache = HashMap<ComponentKey, CacheEntry>()
    private val iconDpi: Int = inv.fillResIconDpi
    private val lowResOptions =
        BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }

    @SuppressLint("NewApi")
    private val highResOptions = if (BitmapRenderer.USE_HARDWARE_BITMAP) {
        BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
    } else {
        null
    }

    private fun getFullResDefaultActivityIcon(): Drawable {
        return getFullResIcon(
            Resources.getSystem(),
            if (Utilities.ATLEAST_OREO) R.drawable.sym_def_app_icon else R.mipmap.sym_def_app_icon
        )
    }

    private fun getFullResIcon(
        resources: Resources,
        iconId: Int
    ): Drawable {
        val d: Drawable? = try {
            resources.getDrawableForDensity(iconId, iconDpi)
        } catch (e: Resources.NotFoundException) {
            null
        }
        return d ?: getFullResDefaultActivityIcon()
    }

    fun getFullResIcon(packageName: String, iconId: Int): Drawable {
        return try {
            packageManager.getResourcesForApplication(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }.let {
            if (it != null && iconId != 0)
                getFullResIcon(it, iconId)
            else getFullResDefaultActivityIcon()
        }
    }

    fun getFullResIcon(info: ActivityInfo): Drawable {
        return try {
            packageManager.getResourcesForApplication(
                info.applicationInfo
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }.let {
            if (it != null && info.iconResource != 0)
                getFullResIcon(it, info.iconResource)
            else getFullResDefaultActivityIcon()
        }
    }

    fun getFullResIcon(info: LauncherActivityInfo): Drawable {
        return getFullResIcon(info, true)
    }

    fun getFullResIcon(
        info: LauncherActivityInfo,
        flattenDrawable: Boolean
    ): Drawable {
        return iconProvider.getIcon(info, iconDpi, flattenDrawable)
    }

    fun makeDefaultIcon(user: UserHandle?): Bitmap {
        return launcherIcons.createBadgedIconBitmap(
            getFullResDefaultActivityIcon(), user, Build.VERSION.SDK_INT
        )
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    @Synchronized
    fun remove(componentName: ComponentName, user: UserHandle) {
        cache.remove(ComponentKey(componentName, user))
    }

    /**
     * Remove any records for the supplied package name from memory.
     */
    private fun removeFromMemCacheLocked(
        packageName: String,
        user: UserHandle
    ) {
        val forDeletion = HashSet<ComponentKey>()
        for (key in cache.keys) {
            if (key.componentName.packageName == packageName &&
                key.user == user
            ) {
                forDeletion.add(key)
            }
        }
        for (condemned in forDeletion) {
            cache.remove(condemned)
        }
    }

    /**
     * Updates the entries related to the given package in memory and persistent DB.
     */
    @Synchronized
    fun updateIconsForPkg(
        packageName: String,
        user: UserHandle
    ) {
        removeIconsForPkg(packageName, user)
        try {
            val info: PackageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            )
            val userSerial: Long = userManager.getSerialNumberForUser(user)
            for (app in launcherApps.getActivityList(packageName, user)) {
                addIconToDBAndMemCache(app, info, userSerial, false /*replace existing*/)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Package not found", e)
        }
    }

    private fun addIconToDBAndMemCache(
        app: LauncherActivityInfo,
        info: PackageInfo,
        userSerial: Long,
        replaceExisting: Boolean
    ) {
        val componentKey = ComponentKey(app.componentName, app.user)
        var entry: CacheEntry? = null
        if (!replaceExisting) {
            entry = cache[componentKey]
            if (entry?.bitmap == null) {
                entry == null
            }
        }

        if (entry == null) {
            entry = launcherIcons.createBadgedIconBitmap(
                getFullResIcon(app),
                app.user,
                app.applicationInfo.targetSdkVersion
            ).let {
                CacheEntry(it, app.label, userManager.getBadgedLabelForUser(app.label, app.user))
            }
        }
        cache.put(componentKey, entry)
        addIconToDB(entry, app.componentName, app.applicationInfo.packageName, info, userSerial)
    }

    private fun addIconToDB(
        entry: CacheEntry,
        componentName: ComponentName,
        packageName: String,
        info: PackageInfo,
        userSerial: Long
    ) {
        val iconEntity = IconEntity(
            componentName.flattenToString(),
            userSerial,
            info.lastUpdateTime,
            info.versionCode,
            Utilities.flattenBitmap(entry.bitmap),
            entry.title.toString(),
            iconProvider.getIconSystemState(packageName)
        )

        iconDao.insertOrReplace(iconEntity)
    }

    /**
     * Removes the entries related to the given package in memory and persistent DB.
     */
    @Synchronized
    fun removeIconsForPkg(
        packageName: String,
        user: UserHandle
    ) {
        removeFromMemCacheLocked(packageName, user)
        val userSerial: Long = userManager.getSerialNumberForUser(user)
        iconDao.delete("$packageName/%", userSerial.toInt())
    }

    fun updateDbIcons(ignorePackagesForMainUser: Set<String>) {
        //TODO: Dispose all current running tasks
        // Remove all active icon update tasks.
        //mWorkerHandler.removeCallbacksAndMessages(IconCache.ICON_UPDATE_TOKEN)
        iconProvider.updateSystemStateString(context)
        for (user in userManager.userProfiles) {
            // Query for the set of apps
            val apps: List<LauncherActivityInfo> = launcherApps.getActivityList(null, user)
            // Fail if we don't have any apps
            // TODO: Fix this. Only fail for the current user.
            if (apps.isEmpty()) {
                return
            }
            // Update icon cache. This happens in segments and {@link #onPackageIconsUpdated}
            // is called by the icon cache when the job is complete.
            updateDbIcons(
                user,
                apps,
                if (Process.myUserHandle() == user) ignorePackagesForMainUser else emptySet()
            )
        }
    }

    private fun updateDbIcons(
        user: UserHandle,
        apps: List<LauncherActivityInfo>,
        ignorePackages: Set<String>
    ) {
        val userSerial = userManager.getSerialNumberForUser(user)
        val pm = context.packageManager
        val pkgInfoMap = HashMap<String, PackageInfo>()
        pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES).forEach {
            pkgInfoMap[it.packageName] = it
        }

        val componentMap = HashMap<ComponentName, LauncherActivityInfo>()
        for (app in apps) {
            componentMap[app.componentName] = app
        }

        val itemsToRemove = HashSet<String>()
        val appsToUpdate = Stack<LauncherActivityInfo>()

        iconDao.query(userSerial).forEach {
            val cn = it.componentName
            val component = ComponentName.unflattenFromString(cn)
            val info = pkgInfoMap[component.packageName]
            if (info == null) {
                if (!ignorePackages.contains(component.packageName)) {
                    remove(component, user)
                    itemsToRemove.add(cn)
                }
                return@forEach
            }

            if (info.applicationInfo.flags and ApplicationInfo.FLAG_IS_DATA_ONLY != 0) {
                return@forEach
            }

            val app = componentMap.remove(component)
            if (it.version == info.versionCode && it.lastUpdated == info.lastUpdateTime
                && it.systemState == iconProvider.getIconSystemState(info.packageName)
            ) {
                return@forEach
            }

            if (app == null) {
                remove(component, user)
                itemsToRemove.add(cn)
            } else {
                appsToUpdate.add(app)
            }
        }

        if (itemsToRemove.isNotEmpty()) {
            iconDao.delete(itemsToRemove.toList())
        }

        if (componentMap.isNotEmpty() || appsToUpdate.isNotEmpty()) {
            val appsToAdd =
                Stack<LauncherActivityInfo>()
            appsToAdd.addAll(componentMap.values)
            /*SerializedIconUpdateTask(
                userSerial, pkgInfoMap,
                appsToAdd, appsToUpdate
            ).scheduleNext()*/
        }
    }

    /**
     * Updates {@param application} only if a valid entry is found.
     */
    @Synchronized
    fun updateTitleAndIcon(application: ApplicationItem) {
        val entry: CacheEntry = cacheLocked(
            application.componentName,
            null,
            application.user, false
        )
        if (entry.bitmap != null && !isDefaultIcon(entry.bitmap!!, application.user)) {
            applyCacheEntry(entry, application)
        }
    }

    /**
     * Fill in {@param info} with the icon and label for {@param activityInfo}
     */
    @Synchronized
    fun getTitleAndIcon(
        info: LauncherItemWithIcon,
        activityInfo: LauncherActivityInfo?
    ) {
        // If we already have activity info, no need to use package icon
        getTitleAndIcon(info, activityInfo, false)
    }

    /**
     * Fill in {@param info} with the icon and label. If the
     * corresponding activity is not found, it reverts to the package icon.
     */
    @Synchronized
    fun getTitleAndIcon(info: LauncherItemWithIcon) {
        // null info means not installed, but if we have a component from the intent then
        // we should still look in the cache for restored app icons.
        if (info.getTargetComponent() == null) {
            getDefaultIcon(info.user).let { info.iconBitmap = it }
            info.title = ""
            info.contentDescription = ""
            info.usingLowResIcon = false
        } else {
            getTitleAndIcon(
                info,
                launcherApps.resolveActivity(info.getIntent(), info.user),
                true
            )
        }
    }

    /**
     * Fill in {@param shortcutInfo} with the icon and label for {@param info}
     */
    @Synchronized
    private fun getTitleAndIcon(
        infoInOut: LauncherItemWithIcon,
        activityInfoProvider: LauncherActivityInfo?,
        usePkgIcon: Boolean
    ) {
        val entry: CacheEntry = cacheLocked(
            infoInOut.getTargetComponent()!!, activityInfoProvider,
            infoInOut.user, usePkgIcon
        )
        applyCacheEntry(entry, infoInOut)
    }

    /**
     * Fill in {@param infoInOut} with the corresponding icon and label.
     */
    @Synchronized
    fun getTitleAndIconForApp(infoInOut: PackageItem) {
        val entry: CacheEntry = getEntryForPackageLocked(
            infoInOut.packageName!!, infoInOut.user
        )
        applyCacheEntry(entry, infoInOut)
    }

    private fun applyCacheEntry(entry: CacheEntry, info: LauncherItemWithIcon) {
        info.title = Utilities.trim(entry.title)
        info.contentDescription = entry.contentDescription
        (if (entry.bitmap == null) getDefaultIcon(info.user) else entry.bitmap).let {
            info.iconBitmap = it
        }
    }

    /**
     * Retrieves the entry from the cache. If the entry is not present, it creates a new entry.
     * This method is not thread safe, it must be called from a synchronized method.
     */
    protected fun cacheLocked(
        componentName: ComponentName,
        info: LauncherActivityInfo?,
        user: UserHandle, usePackageIcon: Boolean
    ): CacheEntry {
        //Preconditions.assertWorkerThread()
        val cacheKey = ComponentKey(componentName, user)
        var entry: CacheEntry? = cache[cacheKey]
        if (entry == null) {
            entry = CacheEntry()
            cache[cacheKey] = entry

            if (!getEntryFromDB(cacheKey, entry)) {
                if (info != null) {
                    launcherIcons.createBadgedIconBitmap(
                        getFullResIcon(info), info.user,
                        info.applicationInfo.targetSdkVersion
                    ).let {
                        entry.bitmap = it
                    }
                } else {
                    if (usePackageIcon) {
                        val packageEntry: CacheEntry =
                            getEntryForPackageLocked(
                                componentName.packageName, user
                            )
                        entry.bitmap = packageEntry.bitmap
                        entry.title = packageEntry.title
                        entry.contentDescription = packageEntry.contentDescription
                    }
                    if (entry.bitmap == null) {
                        getDefaultIcon(user).let {
                            entry.bitmap = it
                        }
                    }
                }
            }
            if (TextUtils.isEmpty(entry.title)) {
                if (info != null) {
                    entry.title = info.label
                    entry.contentDescription =
                        userManager.getBadgedLabelForUser(entry.title.toString(), user)
                }
            }
        }
        return entry
    }

    @Synchronized
    fun getDefaultIcon(user: UserHandle): Bitmap? {
        if (!mDefaultIcons.containsKey(user)) {
            mDefaultIcons[user] = makeDefaultIcon(user)
        }
        return mDefaultIcons[user]
    }

    fun isDefaultIcon(
        icon: Bitmap,
        user: UserHandle
    ): Boolean {
        return getDefaultIcon(user) === icon
    }

    private fun getEntryFromDB(
        cacheKey: ComponentKey,
        entry: CacheEntry
    ): Boolean {
        val iconEntity = iconDao.query(
            cacheKey.componentName.flattenToString(),
            userManager.getSerialNumberForUser(cacheKey.user)
        )
        if (iconEntity != null) {
            entry.bitmap = loadIcon(iconEntity.icon, highResOptions)
            entry.title = iconEntity.label
            if (entry.title == null) {
                entry.title = ""
                entry.contentDescription = ""
            } else {
                entry.contentDescription = userManager.getBadgedLabelForUser(
                    entry.title.toString(), cacheKey.user
                )
            }
            return true
        } else return false
    }

    /**
     * Gets an entry for the package, which can be used as a fallback entry for various components.
     * This method is not thread safe, it must be called from a synchronized method.
     */
    private fun getEntryForPackageLocked(
        packageName: String, user: UserHandle
    ): CacheEntry {
        val cacheKey: ComponentKey = getPackageKey(packageName, user)
        var entry: CacheEntry? = cache.get(cacheKey)
        if (entry == null) {
            entry = CacheEntry()
            var entryUpdated = true

            // Check the DB first.
            if (!getEntryFromDB(cacheKey, entry)) {
                try {
                    val flags =
                        if (Process.myUserHandle() == user) 0 else PackageManager.MATCH_UNINSTALLED_PACKAGES
                    val info: PackageInfo =
                        packageManager.getPackageInfo(packageName, flags)
                    val appInfo = info.applicationInfo
                        ?: throw PackageManager.NameNotFoundException("ApplicationInfo is null")
                    // Load the full res icon for the application, but if useLowResIcon is set, then
                    // only keep the low resolution icon instead of the larger full-sized icon
                    val icon: Bitmap = launcherIcons.createBadgedIconBitmap(
                        appInfo.loadIcon(packageManager), user, appInfo.targetSdkVersion
                    )
                    entry.title = appInfo.loadLabel(packageManager)
                    entry.contentDescription =
                        userManager.getBadgedLabelForUser(entry.title.toString(), user)
                    entry.bitmap = icon

                    // Add the icon in the DB here, since these do not get written during
                    // package updates.
                    addIconToDB(
                        entry,
                        cacheKey.componentName,
                        packageName,
                        info,
                        userManager.getSerialNumberForUser(user)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.d("Application not installed $packageName")
                    entryUpdated = false
                }
            }
            // Only add a filled-out entry to the cache
            if (entryUpdated) {
                cache[cacheKey] = entry
            }
        }
        return entry
    }

    private fun getPackageKey(packageName: String, user: UserHandle): ComponentKey {
        val cn = ComponentName(packageName, packageName + EMPTY_CLASS_NAME)
        return ComponentKey(cn, user)
    }

    private fun loadIcon(
        blob: ByteArray,
        highResOptions: BitmapFactory.Options?
    ): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(blob, 0, blob.size, highResOptions)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun clear() {
        iconDao.clear()
    }

    companion object {
        private const val TAG = "IconCache"

        private const val INITIAL_ICON_CAPACITY = 50

        private const val EMPTY_CLASS_NAME = "."

        private const val LOW_RES_SCALE_FACTOR = 5
    }
}