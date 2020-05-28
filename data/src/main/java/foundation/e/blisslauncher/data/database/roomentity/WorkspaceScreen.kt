package foundation.e.blisslauncher.data.database.roomentity

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaceScreens")
data class WorkspaceScreen(
    @PrimaryKey
    val _id: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
    val screenRank: Int,
    @ColumnInfo(defaultValue = "0", typeAffinity = ColumnInfo.INTEGER)
    @NonNull
    val modified: Int
)