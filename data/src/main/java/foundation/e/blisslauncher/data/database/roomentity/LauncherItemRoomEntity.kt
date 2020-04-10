package foundation.e.blisslauncher.data.database.roomentity

import android.content.ComponentName
import android.content.Intent
import android.os.UserHandle
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import foundation.e.blisslauncher.domain.entity.LauncherItem
import timber.log.Timber
import java.net.URISyntaxException

@Entity(tableName = "launcherItems")
data class LauncherItemRoomEntity(
    @PrimaryKey
    val _id: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val title: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT, name = "intent")
    val intentStr: String?,
    val container: Long,
    val screen: Long,
    val cellX: Int,
    val cellY: Int,
    val itemType: Int,
    @ColumnInfo(defaultValue = "0", typeAffinity = ColumnInfo.INTEGER)
    @NonNull
    val modified: Int,
    @ColumnInfo(defaultValue = "0", typeAffinity = ColumnInfo.INTEGER)
    @NonNull
    val rank: Int,
    val profileId: Long
) {
    // Properties to initialise for proper validation
    val targetPackage: String?
    val intent: Intent?
    val componentName: ComponentName?
    var user: UserHandle? = null
    var validTarget: Boolean = false

    init {
        intent = getParsedIntent()
        componentName = intent?.component
        targetPackage = componentName?.packageName ?: intent?.`package`
    }

    private fun getParsedIntent(): Intent? {
        try {
            return if (intentStr.isNullOrEmpty()) null else Intent.parseUri(intentStr, 0)
        } catch (e: URISyntaxException) {
            Timber.e(e)
            return null
        }
    }

    fun applyCommonProperties(
        destItem: LauncherItem
    ) {
        destItem.id = _id
        destItem.container = container
        destItem.screenId = screen
        destItem.cellX = cellX
        destItem.cellY = cellY
    }
}