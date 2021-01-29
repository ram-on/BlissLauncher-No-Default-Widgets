package foundation.e.blisslauncher.features.test

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.features.launcher.Hotseat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : Activity() {
    private var mCompositeDisposable: CompositeDisposable? = null
    private lateinit var mHotseat: Hotseat

    private val TAG = "TestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        mHotseat = findViewById(R.id.hotseat)
        workspace.initParentViews(root)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        createOrUpdateIconGrid()
    }

    fun getHotseat() = mHotseat

    fun isWorkspaceLoading() = false

    fun getDeviceProfile() = BlissLauncher.getApplication(this).deviceProfile

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
        // TODO: Remove after test is finished
        fun getLauncher(context: Context): TestActivity {
            return if (context is TestActivity) {
                context
            } else (context as ContextWrapper).baseContext as TestActivity
        }
    }
}