package foundation.e.blisslauncher.features.folder

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import foundation.e.blisslauncher.core.customviews.Folder
import foundation.e.blisslauncher.core.database.model.FolderItem
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.touch.ItemClickHandler
import foundation.e.blisslauncher.core.utils.GraphicsUtil
import foundation.e.blisslauncher.features.notification.FolderDotInfo
import foundation.e.blisslauncher.features.test.IconTextView

/**
 * A text view which displays an icon on top side of it.
 */
@SuppressLint("AppCompatCustomView")
class FolderIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : IconTextView(context, attrs, defStyle), FolderItem.FolderListener {

    private lateinit var folderItem: FolderItem
    var folder: Folder? = null

    companion object {
        fun fromXml(
            resId: Int,
            group: ViewGroup,
            folderInfo: FolderItem
        ): FolderIcon {
            val icon = LayoutInflater.from(group.context)
                .inflate(resId, group, false) as FolderIcon
            icon.tag = folderInfo
            icon.setOnClickListener(ItemClickHandler.INSTANCE)
            icon.folderItem = folderInfo
            val folder = Folder.fromXml(icon.launcher).apply {
                this.dragController = launcher.dragController
                this.folderIcon = icon
            }
            folder.bind(folderInfo)
            icon.folder = folder
            folderInfo.addListener(icon)
            return icon
        }
    }

    override fun onAdd(item: LauncherItem?) {
        if (mDotInfo is FolderDotInfo) {
            (mDotInfo as FolderDotInfo).let {
                val wasDotted: Boolean = it.hasDot()
                it.addDotInfo(launcher.getDotInfoForItem(item))
                val isDotted: Boolean = it.hasDot()
                updateDotScale(wasDotted, isDotted, true)
                invalidate()
                requestLayout()
            }
        }
    }

    override fun onTitleChanged(title: CharSequence?) {
        text = title
    }

    override fun onRemove(item: LauncherItem?) {
        if (mDotInfo is FolderDotInfo) {
            (mDotInfo as FolderDotInfo).let {
                val wasDotted: Boolean = it.hasDot()
                it.subtractDotInfo(launcher.getDotInfoForItem(item))
                val isDotted: Boolean = it.hasDot()
                updateDotScale(wasDotted, isDotted, true)
                invalidate()
                requestLayout()
            }
        }
    }

    override fun onItemsChanged(animate: Boolean) {
        updateFolderIcon()
        invalidate()
        requestLayout()
    }

    private fun updateFolderIcon() {
        folderItem.icon = GraphicsUtil(context).generateFolderIcon(context, folderItem)
        applyFromFolderItem(folderItem)
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
        folderItem.removeListener(this)
    }

    fun acceptDrop(): Boolean {
        return !(folder?.isDestroyed() ?: true)
    }

    fun addItem(item: LauncherItem) {
        addItem(item, true)
    }

    fun addItem(item: LauncherItem?, animate: Boolean) {
        folderItem.add(item, animate)
    }
}
