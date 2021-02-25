package foundation.e.blisslauncher.features.test

import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.View
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.features.launcher.Hotseat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : BaseDraggingActivity() {

    private lateinit var mOldConfig: Configuration
    private var mCompositeDisposable: CompositeDisposable? = null
    private lateinit var mHotseat: Hotseat

    private lateinit var deviceProfile: VariantDeviceProfile

    private val TAG = "TestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
        TraceHelper.beginSection("Launcher-onCreate")

        super.onCreate(savedInstanceState)
        TraceHelper.partitionSection("Launcher-onCreate", "super call")

        mOldConfig = Configuration(resources.configuration)
        initDeviceProfile(BlissLauncher.getApplication(this).invariantDeviceProfile)

        setContentView(R.layout.activity_test)
        mHotseat = findViewById(R.id.hotseat)
        workspace.initParentViews(root)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        createOrUpdateIconGrid()
    }

    private fun initDeviceProfile(idp: InvariantDeviceProfile) {
        // Load device specific profile
        deviceProfile = idp.getDeviceProfile(this)
        onDeviceProfileInitiated()
    }

    fun getHotseat() = mHotseat

    fun isWorkspaceLoading() = false

    fun getDeviceProfile() = BlissLauncher.getApplication(this).deviceProfile

    override fun getDragLayer(): BaseDragLayer {
        return mDragLayer
    }

    override fun <T : View?> getOverviewPanel(): T {
        TODO("Not yet implemented")
    }

    override fun getRootView(): View {
        TODO("Not yet implemented")
    }

    override fun getActivityLaunchOptions(v: View?): ActivityOptions {
        TODO("Not yet implemented")
    }

    override fun reapplyUi() {
        getR
    }

    private fun getCompositeDisposable(): CompositeDisposable {
        if (mCompositeDisposable == null || mCompositeDisposable!!.isDisposed) {
            mCompositeDisposable = CompositeDisposable()
        }
        return mCompositeDisposable!!
    }

    private fun createOrUpdateIconGrid() {
        getCompositeDisposable().add(
            BlissLauncher.getApplication(this)
                .appProvider
                .appsRepository
                .appsRelay
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<LauncherItem?>?>() {
                    override fun onNext(launcherItems: List<LauncherItem?>) {
                        showApps(launcherItems)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {}
                })
        )
    }

    private fun showApps(launcherItems: List<LauncherItem?>) {
        workspace.bindItems(launcherItems)
    }

    companion object {
        const val TAG = "Launcher"
        const val LOGD = false

        const val DEBUG_STRICT_MODE = false

        // TODO: Remove after test is finished
        fun getLauncher(context: Context): TestActivity {
            return if (context is TestActivity) {
                context
            } else (context as ContextWrapper).baseContext as TestActivity
        }
    }
}