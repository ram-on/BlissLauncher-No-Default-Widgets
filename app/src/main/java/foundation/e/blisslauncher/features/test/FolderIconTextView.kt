package foundation.e.blisslauncher.features.test

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import foundation.e.blisslauncher.core.database.model.FolderItem
import foundation.e.blisslauncher.features.notification.FolderDotInfo

/**
 * A text view which displays an icon on top side of it.
 */
@SuppressLint("AppCompatCustomView")
class FolderIconTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : IconTextView(context, attrs, defStyle), FolderItem.FolderListener {

    private var folderItem: FolderItem? = null

    override fun onTitleChanged(title: CharSequence?) {
        text = title
    }

    fun applyFromFolderItem(item: FolderItem) {
        applyFromShortcutItem(item)
        folderItem = item
    }

    fun setDotInfo(dotInfo: FolderDotInfo) {
        val wasDotted = mDotInfo is FolderDotInfo && (mDotInfo as FolderDotInfo).hasDot()
        updateDotScale(wasDotted, dotInfo.hasDot(), true)
        mDotInfo = dotInfo
    }

    override fun hasDot(): Boolean {
        return mDotInfo != null && (mDotInfo as FolderDotInfo).hasDot()
    }

    fun removeListeners() {
        folderItem?.removeListener(this)
    }
}
