package foundation.e.blisslauncher.data.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcherItems")
data class WorkspaceLauncherItem(
    @PrimaryKey
    val _id: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val title: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val intent: String,
    val container: Int,
    val screen: Int,
    val cellX: Int,
    val cellY: Int,
    val itemType: Int,
    @ColumnInfo(defaultValue = "0", typeAffinity = ColumnInfo.INTEGER)
    @NonNull
    val modified: Int,
    @ColumnInfo(defaultValue = "0", typeAffinity = ColumnInfo.INTEGER)
    @NonNull
    val rank: Int,

    val profileId: Int
)