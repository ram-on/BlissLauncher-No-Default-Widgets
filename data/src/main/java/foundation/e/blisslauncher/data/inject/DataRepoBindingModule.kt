package foundation.e.blisslauncher.data.inject

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.data.WorkspaceRepositoryImpl
import foundation.e.blisslauncher.data.LauncherStateManagerImpl
import foundation.e.blisslauncher.data.WorkspaceScreenRepositoryImpl
import foundation.e.blisslauncher.data.database.BlissLauncherFiles
import foundation.e.blisslauncher.domain.manager.LauncherStateManager
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import foundation.e.blisslauncher.domain.repository.WorkspaceRepository
import foundation.e.blisslauncher.domain.repository.WorkspaceScreenRepository

@Module
class DataRepoBindingModule {

    @Provides
    fun bindLauncherStateManager(launcherStateManagerImpl: LauncherStateManagerImpl): LauncherStateManager =
        launcherStateManagerImpl

    @Provides
    fun bindLauncherRepository(workspaceRepositoryImpl: WorkspaceRepositoryImpl): WorkspaceRepository =
        workspaceRepositoryImpl

    @Provides
    fun bindWorkspaceScreenRepository(workspaceScreenRepositoryImpl: WorkspaceScreenRepositoryImpl): WorkspaceScreenRepository =
        workspaceScreenRepositoryImpl

    @Provides
    fun provideSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(
            BlissLauncherFiles.SHARED_PREFERENCES_KEY,
            Context.MODE_PRIVATE
        )
}