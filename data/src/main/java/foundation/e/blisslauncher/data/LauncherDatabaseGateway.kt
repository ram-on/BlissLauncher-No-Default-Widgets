package foundation.e.blisslauncher.data

import foundation.e.blisslauncher.data.database.roomentity.LauncherItemRoomEntity
import javax.inject.Inject

class LauncherDatabaseGateway @Inject constructor() {
    fun createEmptyDatabase() {}

    fun generateNewItemId() {}

    fun generateNewScreenId() {}

    fun deleteEmptyFolders() {}

    fun loadDefaultWorkspace() {}

    fun getAllWorkspaceItems(): List<LauncherItemRoomEntity> = emptyList()

    fun loadWorkspaceScreensInOrder(): List<Long> = emptyList()

    fun markDeleted(id: Long) {}

    fun markDeleted(item: LauncherItemRoomEntity) {
        markDeleted(item._id)
    }
}