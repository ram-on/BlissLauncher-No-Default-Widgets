package foundation.e.blisslauncher.features.launcher

import foundation.e.blisslauncher.domain.dto.WorkspaceModel
import foundation.e.blisslauncher.domain.entity.LauncherItem

sealed class LauncherState {

    /**
     * Very initial launcher state when there is nothing going on.
     */
    object Empty : LauncherState()

    /**
     * Launcher State when launcher is loading its content
     */
    object Loading : LauncherState()
    object Error : LauncherState() {

    }

    /**
     * Launcher State when launcher finished its loading and available to show its data
     */
    data class Loaded(val workspaceModel: WorkspaceModel) : LauncherState()

    /**
     * Launcher State when launcher swipe to search down is invoked.
     */
    data class Search(val searchQuery: String) : LauncherState()
}