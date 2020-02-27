package foundation.e.blisslauncher.data.inject

import android.content.Context
import android.os.Process
import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.common.executors.MainThreadExecutor
import foundation.e.blisslauncher.data.LauncherAppsChangedCallbackCompat
import foundation.e.blisslauncher.data.compat.LauncherAppsCompatVL
import foundation.e.blisslauncher.data.compat.LauncherAppsCompatVO
import foundation.e.blisslauncher.data.compat.UserManagerCompatVN
import foundation.e.blisslauncher.data.compat.UserManagerCompatVNMr1
import foundation.e.blisslauncher.data.compat.UserManagerCompatVP
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
class CompatModule {

    @Provides
    @Singleton
    fun provideLauncherAppsCompat(context: Context): LauncherAppsCompat =
        if (Utilities.ATLEAST_OREO) {
            LauncherAppsCompatVO(context)
        } else LauncherAppsCompatVL(context)

    @Provides
    @Singleton
    fun provideUserManagerCompat(context: Context): UserManagerRepository = when {
        Utilities.ATLEAST_P -> {
            UserManagerCompatVP(context)
        }
        Utilities.ATLEAST_NOUGAT_MR1 -> {
            UserManagerCompatVNMr1(context)
        }
        else -> UserManagerCompatVN(context)
    }

    @Provides
    @Singleton
    fun provideExecutors() =
        AppExecutors(
            io = Executors.newSingleThreadExecutor().apply {
                execute {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
                }
            },
            computation = Executors.newSingleThreadExecutor(),
            main = MainThreadExecutor()
        )

    @Provides
    @Singleton
    fun provideOnAppsChangedCallback(launcherAppsChangedCallbackCompat: LauncherAppsChangedCallbackCompat): LauncherAppsCompat.OnAppsChangedCallbackCompat =
        launcherAppsChangedCallbackCompat
}