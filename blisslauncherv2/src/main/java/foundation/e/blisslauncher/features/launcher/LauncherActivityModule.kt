package foundation.e.blisslauncher.features.launcher

import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.common.util.SystemUiController

@Module
class LauncherActivityModule {

    @Provides
    fun provideSystemUiController(launcherActivity: LauncherActivity): SystemUiController =
        SystemUiController(launcherActivity.window)
}