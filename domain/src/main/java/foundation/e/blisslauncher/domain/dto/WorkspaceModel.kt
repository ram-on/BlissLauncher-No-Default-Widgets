package foundation.e.blisslauncher.domain.dto

import foundation.e.blisslauncher.utils.LongArrayMap
import foundation.e.blisslauncher.domain.entity.FolderItem
import foundation.e.blisslauncher.domain.entity.LauncherItem

data class WorkspaceModel(
    val itemsIdMap: LongArrayMap<LauncherItem> = LongArrayMap(),
    val workspaceItems: ArrayList<LauncherItem> = ArrayList(),
    val folders: LongArrayMap<FolderItem> = LongArrayMap(),
    val workspaceScreens: ArrayList<Long> = ArrayList()

)