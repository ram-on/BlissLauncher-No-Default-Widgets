package foundation.e.blisslauncher.data

import foundation.e.blisslauncher.data.database.WorkspaceLauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherItem

interface LauncherDatabaseGateway {
    fun createEmptyDatabase()

    fun generateNewItemId()

    fun generateNewScreenId()

    fun deleteEmptyFolders()

    fun loadDefaultWorkspace()

    fun getAllWorkspaceItems(): List<WorkspaceLauncherItem>

    fun loadWorkspaceScreensInOrder(): List<Long>

    fun markDeleted(id: Long)

    fun markDeleted(item: WorkspaceLauncherItem)
}