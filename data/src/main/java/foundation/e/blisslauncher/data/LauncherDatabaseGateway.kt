package foundation.e.blisslauncher.data

import foundation.e.blisslauncher.domain.entity.LauncherItem

interface LauncherDatabaseGateway {
    fun createEmptyDatabase()

    fun generateNewItemId()

    fun generateNewScreenId()

    fun deleteEmptyFolders()

    fun loadDefaultWorkspace()

    fun loadAllLauncherItems(): List<LauncherItem>

    fun loadWorkspaceScreensInOrder(): List<Long>

    fun markDeleted(id: Long)
}