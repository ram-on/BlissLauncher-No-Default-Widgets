package foundation.e.blisslauncher.data.database

import android.content.Intent
import android.os.UserHandle
import android.text.TextUtils
import android.util.LongSparseArray
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import timber.log.Timber
import java.net.URISyntaxException

@Entity(tableName = "launcherItems")
data class WorkspaceLauncherItem(
    @PrimaryKey
    val _id: Long,
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
    val profileId: Long
) {
    fun getParsedIntent(): Intent? {
        try {
            return if (intent.isNullOrEmpty()) null else Intent.parseUri(intent, 0)
        } catch (e: URISyntaxException) {
            Timber.e(e)
            return null
        }
    }
}