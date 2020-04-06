package foundation.e.blisslauncher.data.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaceScreens")
data class WorkspaceScreen(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
    val screenRank: Int,
    @ColumnInfo(defaultValue = "0", typeAffinity = ColumnInfo.INTEGER)
    @NonNull
    val modified: Int
)