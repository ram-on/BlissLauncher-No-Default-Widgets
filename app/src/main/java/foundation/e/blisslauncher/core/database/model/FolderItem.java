package foundation.e.blisslauncher.core.database.model;


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

    public interface FolderListener {
        void onTitleChanged(CharSequence title);
    }
}
