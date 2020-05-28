package foundation.e.blisslauncher.inject

import android.content.Context
import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.features.LauncherStore

@Module
class AppModule {

    @Provides
    fun provideContext(application: BlissLauncher): Context = application.applicationContext

    @Provides
    fun provideIdp(context: Context): InvariantDeviceProfile = InvariantDeviceProfile(context)
}