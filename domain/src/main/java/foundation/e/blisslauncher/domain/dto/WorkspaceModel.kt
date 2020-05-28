package foundation.e.blisslauncher.domain.dto

import android.content.Context
import foundation.e.blisslauncher.common.util.LongArrayMap
import foundation.e.blisslauncher.domain.entity.FolderItem
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherConstants.ContainerType.CONTAINER_DESKTOP
import foundation.e.blisslauncher.domain.entity.LauncherConstants.ContainerType.CONTAINER_HOTSEAT
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.entity.ShortcutItem

data class WorkspaceModel(

    /**
     * Map of all the Items (shortcuts, folders, and widgets) created by
     * LoadLauncher Interactor to their ids
     */
    val itemsIdMap: LongArrayMap<LauncherItem> = LongArrayMap(),

    /**
     * List of all the folders and shortcuts directly on the home screen (no widgets
     * or shortcuts within folders).
     */
    val workspaceItems: ArrayList<LauncherItem> = ArrayList(),

    /**
     * Map of id to FolderItems of all the folders created by LauncherModel
     */
    val folders: LongArrayMap<FolderItem> = LongArrayMap(),

    /**
     * Ordered list of workspace screens ids.
     */
    val workspaceScreens: ArrayList<Long> = ArrayList()
) {

    /**
     * Clear all data that this model holds
     */
    @Synchronized
    fun clear() {
        workspaceItems.clear()
        workspaceScreens.clear()
        itemsIdMap.clear()
        folders.clear()
    }

    @Synchronized
    fun removeItem(context: Context, items: List<LauncherItem>) {
        items.forEach {
            when (it.itemType) {
                LauncherConstants.ItemType.APPLICATION,
                LauncherConstants.ItemType.SHORTCUT -> {
                    workspaceItems.remove(it)
                }
                LauncherConstants.ItemType.FOLDER -> {
                    folders.remove(it.id)
                    //TODO: Add debug log if folder contains some items
                }
            }
            itemsIdMap.remove(it.id)
        }
    }

    @Synchronized
    fun addItem(context: Context, item: LauncherItem, newItem: Boolean) {
        itemsIdMap.put(item.id, item)
        when (item.itemType) {
            LauncherConstants.ItemType.FOLDER -> {
                folders.put(item.id, item as FolderItem)
                workspaceItems.add(item)
            }
            LauncherConstants.ItemType.APPLICATION,
            LauncherConstants.ItemType.SHORTCUT -> {
                if (item.container == CONTAINER_DESKTOP || item.container == CONTAINER_HOTSEAT) {
                    workspaceItems.add(item)
                } else {
                    if (newItem) {
                        if (!folders.containsKey(item.container)) {
                            // Adding an item to a folder that doesn't exist.
                            val msg =
                                "adding item: " + item + " to a folder that " +
                                    " doesn't exist"
                        }
                    } else {
                        findOrMakeFolder(item.container).add(item as ShortcutItem, false)
                    }
                }
            }
        }
    }

    private fun findOrMakeFolder(id: Long): FolderItem {
        // See if a placeholder was created for us already
        var folderInfo: FolderItem? = folders[id]
        if (folderInfo == null) {
            // No placeholder -- create a new instance
            folderInfo = FolderItem()
            folders.put(id, folderInfo)
        }
        return folderInfo
    }
}