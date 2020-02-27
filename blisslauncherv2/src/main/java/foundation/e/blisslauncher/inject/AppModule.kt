package foundation.e.blisslauncher.inject

import android.content.Context
import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.BlissLauncher

@Module
class AppModule {

    @Provides
    fun provideContext(application: BlissLauncher): Context = application.applicationContext
}