package foundation.e.blisslauncher.domain.entity

import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.os.UserHandle

/**
 * Represents an item in BlissLauncher.
 */
open class LauncherItem : Entity {

    /**
     * The id in the settings database for this item
     */
    var id: Long = NO_ID.toLong()

    /**
     * One of [LauncherConstants.ItemType.APPLICATION],
     * [LauncherConstants.ItemType.SHORTCUT],
     * [LauncherConstants.ItemType.FOLDER]
     * [LauncherConstants.ItemType.APPWIDGET],
     * [LauncherConstants.ItemType.CUSTOM_APPWIDGET] or
     * [LauncherConstants.ItemType.DEEP_SHORTCUT].
     */
    var itemType = 0

    /**
     * The id of the container that holds this item. For the desktop, this will be
     * [LauncherConstants.ContainerType.CONTAINER_DESKTOP]. For the all applications folder it
     * will be [.NO_ID] (since it is not stored in the settings DB). For user folders
     * it will be the id of the folder.
     */
    var container: Long = NO_ID.toLong()

    /**
     * Indicates the screen in which the shortcut appears if the container types is
     * [LauncherConstants.ContainerType.CONTAINER_DESKTOP]. (i.e., ignore if the container type is
     * [LauncherConstants.ContainerType.CONTAINER_HOTSEAT])
     */
    var screenId: Long = -1

    /**
     * Indicates the X position of the associated cell.
     */
    var cellX = -1

    /**
     * Indicates the Y position of the associated cell.
     */
    var cellY = -1

    /**
     * Indicates the X cell span.
     */
    var spanX = 1

    /**
     * Indicates the Y cell span.
     */
    var spanY = 1

    /**
     * Indicates the minimum X cell span.
     */
    var minSpanX = 1

    /**
     * Indicates the minimum Y cell span.
     */
    var minSpanY = 1

    /**
     * Indicates the position in an ordered list.
     */
    var rank = 0

    /**
     * Title of the item
     */
    var title: CharSequence? = null

    /**
     * Content description of the item.
     */
    var contentDescription: CharSequence? = null

    lateinit var user: UserHandle

    constructor() {
        user = Process.myUserHandle()
    }

    constructor(item: LauncherItem) {
        copyFrom(item)
    }

    private fun copyFrom(item: LauncherItem) {
        id = item.id
        cellX = item.cellX
        cellY = item.cellY
        spanX = item.spanX
        spanY = item.spanY
        rank = item.rank
        screenId = item.screenId
        itemType = item.itemType
        container = item.container
        user = item.user
        contentDescription = item.contentDescription
    }

    open fun getIntent(): Intent? {
        return null
    }

    open fun getTargetComponent(): ComponentName? {
        val intent = getIntent()
        return intent?.component
    }

    override fun toString(): String {
        return javaClass.simpleName + "(" + dumpProperties() + ")"
    }

    protected open fun dumpProperties(): String? {
        return ("id=" + id +
            " type=" + LauncherConstants.ItemType.itemTypeToString(itemType) +
            " container=" + LauncherConstants.ContainerType.containerToString(container.toInt()) +
            " screen=" + screenId +
            " cell(" + cellX + "," + cellY + ")" +
            " span(" + spanX + "," + spanY + ")" +
            " minSpan(" + minSpanX + "," + minSpanY + ")" +
            " rank=" + rank +
            " user=" + user +
            " title=" + title)
    }

    /**
     * Whether this item is disabled.
     */
    open fun isDisabled(): Boolean {
        return false
    }

    companion object {
        const val NO_ID = -1
    }
}