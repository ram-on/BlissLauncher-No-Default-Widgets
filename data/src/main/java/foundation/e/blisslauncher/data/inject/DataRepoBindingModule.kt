package foundation.e.blisslauncher.data.inject

import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.data.LauncherStateManagerImpl
import foundation.e.blisslauncher.domain.manager.LauncherStateManager
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository

@Module
class DataRepoBindingModule {

    @Provides
    fun bindLauncherStateManager(launcherStateManagerImpl: LauncherStateManagerImpl): LauncherStateManager = launcherStateManagerImpl

    @Provides
    fun bindLauncherRepository(launcherRepositoryImpl: LauncherItemRepository): LauncherItemRepository = launcherRepositoryImpl
}