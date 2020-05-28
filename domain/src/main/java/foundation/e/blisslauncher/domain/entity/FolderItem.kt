package foundation.e.blisslauncher.domain.entity

import android.os.Process
import foundation.e.blisslauncher.common.Utilities

class FolderItem : LauncherItem() {

    var options: Int = 0

    val contents = mutableListOf<ShortcutItem>()

    val listeners = ArrayList<FolderListener>()

    init {
        itemType = LauncherConstants.ItemType.FOLDER
        user = Process.myUserHandle()
    }

    /**
     * Add an app or shortcut
     *
     * @param item
     */
    fun add(item: ShortcutItem, animate: Boolean) {
        add(item, contents.size, animate)
    }

    /**
     * Add an app or shortcut for a specified rank.
     */
    fun add(item: ShortcutItem, rank: Int, animate: Boolean) {
        var rank = rank
        rank = Utilities.boundToRange(rank, 0, contents.size)
        contents.add(rank, item)
        /*for (i in listeners.indices) {
            listeners.get(i).onAdd(item, rank)
        }*/
        itemsChanged(animate)
    }

    /**
     * Remove an app or shortcut. Does not change the DB.
     *
     * @param item
     */
    fun remove(item: ShortcutItem, animate: Boolean) {
        contents.remove(item)
        /*for (i in listeners.indices) {
            listeners.get(i).onRemove(item)
        }*/
        itemsChanged(animate)
    }

    fun addListener(listener: FolderListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: FolderListener) {
        listeners.remove(listener)
    }

    fun itemsChanged(animate: Boolean) {
        for (i in listeners.indices) {
            listeners.get(i).onItemsChanged(animate)
        }
    }

    fun prepareAutoUpdate() {
        for (i in listeners.indices) {
            listeners.get(i).prepareAutoUpdate()
        }
    }

    interface FolderListener {
        fun onAdd(item: ShortcutItem, rank: Int)
        fun onRemove(item: ShortcutItem)
        fun onTitleChanged(title: CharSequence)
        fun onItemsChanged(animate: Boolean)
        fun prepareAutoUpdate()
    }

    fun hasOption(optionFlag: Int): Boolean {
        return options and optionFlag != 0
    }

    /**
     * @param option flag to set or clear
     * @param isEnabled whether to set or clear the flag
     * @param writer if not null, save changes to the db.
     */
    fun setOption(option: Int, isEnabled: Boolean) {
        val oldOptions = options
        options = if (isEnabled) {
            options or option
        } else {
            options and option.inv()
        }
        /*if (writer != null && oldOptions != options) {
            writer.updateItemInDatabase(this)
        }*/
    }

    companion object {
        const val NO_FLAGS = 0x00000000

        /**
         * Represent a work folder
         */
        const val FLAG_WORK_FOLDER = 0x00000002
    }
}