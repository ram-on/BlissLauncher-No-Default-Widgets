package foundation.e.blisslauncher.data

import foundation.e.blisslauncher.data.database.roomentity.LauncherItemRoomEntity

interface LauncherDatabaseGateway {
    fun createEmptyDatabase()

    fun generateNewItemId()

    fun generateNewScreenId()

    fun deleteEmptyFolders()

    fun loadDefaultWorkspace()

    fun getAllWorkspaceItems(): List<LauncherItemRoomEntity>

    fun loadWorkspaceScreensInOrder(): List<Long>

    fun markDeleted(id: Long)

    fun markDeleted(item: LauncherItemRoomEntity)
}