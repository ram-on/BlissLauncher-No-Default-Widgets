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
import android.util.Log
import foundation.e.blisslauncher.common.BitmapRenderer
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.data.database.dao.IconDao
import foundation.e.blisslauncher.data.database.roomentity.IconEntity
import foundation.e.blisslauncher.domain.keys.ComponentKey
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
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
        var bitmap: Bitmap,
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

    protected fun makeDefaultIcon(user: UserHandle?): Bitmap {
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
            updateDBIcons(
                user,
                apps,
                if (Process.myUserHandle() == user) ignorePackagesForMainUser else emptySet()
            )
        }
    }

    private fun updateDBIcons(
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

    companion object {
        private const val TAG = "IconCache"

        private const val INITIAL_ICON_CAPACITY = 50

        private const val EMPTY_CLASS_NAME = "."

        private const val LOW_RES_SCALE_FACTOR = 5
    }
}