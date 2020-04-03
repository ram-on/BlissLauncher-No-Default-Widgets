package foundation.e.blisslauncher.data.inject

import dagger.Module
import dagger.Provides
import foundation.e.blisslauncher.data.LauncherStateManagerImpl
import foundation.e.blisslauncher.data.apps.AppsRepositoryImpl
import foundation.e.blisslauncher.data.launcher.LauncherRepositoryImpl
import foundation.e.blisslauncher.data.shortcuts.ShortcutsRepositoryImpl
import foundation.e.blisslauncher.data.widgets.WidgetsRepositoryImpl
import foundation.e.blisslauncher.data.workspace.WorkspaceRepositoryImpl
import foundation.e.blisslauncher.domain.manager.LauncherStateManager
import foundation.e.blisslauncher.domain.repository.AppsRepository
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import foundation.e.blisslauncher.domain.repository.ShortcutRepository
import foundation.e.blisslauncher.domain.repository.WidgetsRepository
import foundation.e.blisslauncher.domain.repository.WorkspaceRepository

@Module
class DataRepoBindingModule {

    @Provides
    fun bindLauncherStateManager(launcherStateManagerImpl: LauncherStateManagerImpl): LauncherStateManager = launcherStateManagerImpl

    @Provides
    fun bindAppsRepository(appsRepositoryImpl: AppsRepositoryImpl): AppsRepository = appsRepositoryImpl

    @Provides
    fun bindShortcutRepository(shortcutRepositoryImpl: ShortcutsRepositoryImpl): ShortcutRepository = shortcutRepositoryImpl

    @Provides
    fun bindLauncherRepository(launcherRepositoryImpl: LauncherRepositoryImpl): LauncherRepository = launcherRepositoryImpl

    @Provides
    fun bindWorkspaceRepository(workspaceRepositoryImpl: WorkspaceRepositoryImpl): WorkspaceRepository = workspaceRepositoryImpl

    @Provides
    fun bindWidgetsRepository(widgetsRepositoryImpl: WidgetsRepositoryImpl): WidgetsRepository = widgetsRepositoryImpl
}