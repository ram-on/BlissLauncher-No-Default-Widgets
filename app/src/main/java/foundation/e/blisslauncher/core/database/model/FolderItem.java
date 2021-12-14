package foundation.e.blisslauncher.core.database.model;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class FolderItem extends LauncherItem {

    /**
     * Stores networkItems that user saved in this folder.
     */
    public List<LauncherItem> items;

    public FolderItem() {
        itemType = Constants.ITEM_TYPE_FOLDER;
    }

    ArrayList<FolderListener> listeners = new ArrayList<>();

    public void setTitle(CharSequence title) {
        this.title = title;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onTitleChanged(title);
        }
    }

    public void addListener(FolderListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FolderListener listener) {
        listeners.remove(listener);
    }

    public void remove(@NotNull LauncherItem launcherItem, boolean animate) {
        items.remove(launcherItem);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onRemove(launcherItem);
        }
        itemsChanged(animate);
    }

    public void itemsChanged(boolean animate) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onItemsChanged(animate);
        }
    }

    /**
     * Add an app or shortcut
     *
     * @param item
     */
    public void add(LauncherItem item, boolean animate) {
        add(item, items.size(), animate);
    }

    /**
     * Add an app or shortcut for a specified rank.
     */
    public void add(LauncherItem item, int rank, boolean animate) {
        rank = Utilities.boundToRange(rank, 0, items.size());
        items.add(rank, item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAdd(item);
        }
        itemsChanged(animate);
    }

    public interface FolderListener {
        void onAdd(LauncherItem item);
        void onTitleChanged(CharSequence title);
        void onRemove(LauncherItem item);
        void onItemsChanged(boolean animate);
    }
}
