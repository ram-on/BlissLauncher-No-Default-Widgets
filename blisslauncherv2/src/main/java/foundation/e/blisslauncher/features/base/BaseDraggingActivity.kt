package foundation.e.blisslauncher.features.base

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Process
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.ActionMode
import android.view.View
import android.widget.Toast
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import javax.inject.Inject

/**
 * BaseActivity Extension with the support of Drag and Drop
 */
abstract class BaseDraggingActivity : BaseActivity() {

    private var currentActionMode: ActionMode? = null
    protected var isSafeModeEnabled = false

    @Inject
    lateinit var launcherAppsRepository: LauncherAppsCompat

    // TODO Replace with LauncherTheme
    var themeRes: Int = R.style.AppTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSafeModeEnabled = packageManager.isSafeMode
        setTheme(themeRes)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        currentActionMode = mode
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        currentActionMode = null
    }

    abstract fun getRootView(): View

    abstract fun invalidateParent(launcherItem: LauncherItem)

    fun getViewBounds(v: View): Rect {
        val pos = IntArray(2)
        v.getLocationOnScreen(pos)
        return Rect(pos[0], pos[1], pos[0] + v.width, pos[1] + v.height)
    }

    abstract fun getActivityLaunchOptions(v: View): ActivityOptions?

    fun getActivityLaunchOptionsAsBundle(v: View): Bundle? {
        val activityOptions = getActivityLaunchOptions(v)
        return activityOptions?.toBundle()
    }

    fun startActivitySafely(v: View, intent: Intent, item: LauncherItem?): Boolean {
        if (isSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show()
            return false
        }

        val useLaunchAnimation = !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION)
        val optsBundle = if (useLaunchAnimation) getActivityLaunchOptionsAsBundle(v) else null

        val user = item?.user
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.sourceBounds = getViewBounds(v)
        try {

            //TODO
            /*val isShortcut = (item is ShortcutInfo
                && (item!!.itemType === Favorites.ITEM_TYPE_SHORTCUT
                || item!!.itemType === Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                && !(item as ShortcutInfo).isPromise()) */
            val isShortcut = false
            if (isShortcut)
                startShortcutIntentSafely(intent, optsBundle!!, item!!)
            else if (user == null || user == Process.myUserHandle()) {
                startActivity(intent, optsBundle)
            } else launcherAppsRepository.startActivityForProfile(
                intent.component,
                user,
                intent.sourceBounds,
                optsBundle
            )

            return true
        } catch (e: Exception) {
            when (e) {
                is SecurityException, is ActivityNotFoundException -> Toast.makeText(
                    this,
                    R.string.activity_not_found,
                    Toast.LENGTH_SHORT
                ).show()
                else -> throw e
            }
        }
        return false
    }

    private fun startShortcutIntentSafely(
        intent: Intent,
        optsBundle: Bundle,
        item: LauncherItem
    ) {
        try {
            val oldPolicy = StrictMode.getVmPolicy()
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(
                    VmPolicy.Builder().detectAll()
                        .penaltyLog().build()
                )
                if (item.itemType == LauncherConstants.ItemType.DEEP_SHORTCUT) {
                    /*val id: String = (info as ShortcutInfo).getDeepShortcutId()
                    val packageName = intent.getPackage()
                    DeepShortcutManager.getInstance(this).startShortcut(
                        packageName, id, intent.sourceBounds, optsBundle, info.user
                    )*/
                } else { // Could be launching some bookkeeping activity
                    startActivity(intent, optsBundle)
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy)
            }
        } catch (e: SecurityException) {
            throw e
        }
    }

    companion object {
        private const val TAG = "BaseDraggingActivity"
        const val INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "foundation.e.blisslauncher.intent.extra.shortcut.IGNORE_LAUNCH_ANIMATION"
        val AUTO_CANCEL_ACTION_MODE = Any()

        fun fromContext(context: Context): BaseDraggingActivity =
            if (context is BaseDraggingActivity) context
            else ((context as ContextWrapper).baseContext) as BaseDraggingActivity
    }
}