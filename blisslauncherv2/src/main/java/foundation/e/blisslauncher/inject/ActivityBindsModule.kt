package foundation.e.blisslauncher.inject

import dagger.Module
import dagger.android.ContributesAndroidInjector
import foundation.e.blisslauncher.common.inject.PerActivity
import foundation.e.blisslauncher.features.launcher.LauncherActivity
import foundation.e.blisslauncher.features.launcher.LauncherActivityModule

@Module
abstract class ActivityBindsModule {

    @PerActivity
    @ContributesAndroidInjector(modules = [LauncherActivityModule::class])
    abstract fun contributesLauncherActivity(): LauncherActivity


}