package foundation.e.blisslauncher.domain.inject

import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.executors.AppExecutors

/**
 * Interface that lists all public repositories and data access layer components which are needed
 * to be exposed to the `domain` layer
 */
interface DomainComponent {

    fun appExecutors(): AppExecutors

    fun launcherAppsCompat(): LauncherAppsCompat

    companion object {
        @Volatile
        @JvmStatic
        lateinit var INSTANCE: DomainComponent
    }
}