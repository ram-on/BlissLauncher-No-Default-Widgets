package foundation.e.blisslauncher

import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import foundation.e.blisslauncher.databridge.DataBridgeInitializer
import foundation.e.blisslauncher.domain.inject.DomainComponent
import foundation.e.blisslauncher.inject.DaggerAppComponent
import timber.log.Timber
import javax.inject.Inject

class BlissLauncher : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        DataBridgeInitializer.initialize(this)
        DaggerAppComponent.factory().create(
            this, DomainComponent.INSTANCE
        ).inject(this)
        setupTimber()
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}