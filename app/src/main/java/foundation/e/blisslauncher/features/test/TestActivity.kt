package foundation.e.blisslauncher.features.test

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.database.model.LauncherItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity() {
    private var mCompositeDisposable: CompositeDisposable? = null

    private val TAG = "TestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val tv1 = layoutInflater.inflate(R.layout.test_page_view, null)
        workspace.initParentViews(root)

        createOrUpdateIconGrid()
    }

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