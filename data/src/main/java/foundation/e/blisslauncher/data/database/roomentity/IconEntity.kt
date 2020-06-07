package foundation.e.blisslauncher.data.database.roomentity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "icons", primaryKeys = ["componentName", "profileId"])
data class IconEntity(
    @ColumnInfo(name = "componentName", typeAffinity = ColumnInfo.TEXT)
    val componentName: String,
    @ColumnInfo(name = "profileId")
    val profileId: Long,
    val lastUpdated: Long = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
    val version: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val icon: ByteArray,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val label: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val systemState: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IconEntity

        if (componentName != other.componentName) return false
        if (profileId != other.profileId) return false
        if (lastUpdated != other.lastUpdated) return false
        if (version != other.version) return false
        if (!icon.contentEquals(other.icon)) return false
        if (label != other.label) return false
        if (systemState != other.systemState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentName.hashCode()
        result = 31 * result + profileId.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + version
        result = 31 * result + icon.contentHashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + systemState.hashCode()
        return result
    }
}