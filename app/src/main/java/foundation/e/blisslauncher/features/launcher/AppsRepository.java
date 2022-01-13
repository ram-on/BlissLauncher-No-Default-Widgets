package foundation.e.blisslauncher.features.launcher;


import com.jakewharton.rxrelay2.BehaviorRelay;
import foundation.e.blisslauncher.core.database.model.LauncherItem;

import java.util.List;

public class AppsRepository {

    private static final String TAG = "AppsRepository";

    @Deprecated
    private BehaviorRelay<List<LauncherItem>> appsRelay;
    private BehaviorRelay<AllItems> allItems;

    private static AppsRepository sAppsRepository;

    private AppsRepository() {
        appsRelay = BehaviorRelay.create();
        allItems = BehaviorRelay.create();
    }

    public static AppsRepository getAppsRepository() {
        if (sAppsRepository == null) {
            sAppsRepository = new AppsRepository();
        }
        return sAppsRepository;
    }

    public void clearAll(){
        appsRelay = BehaviorRelay.create();
        allItems = BehaviorRelay.create();
    }

    public void updateAppsRelay(List<LauncherItem> launcherItems) {
        this.appsRelay.accept(launcherItems);
    }

    public void updateAllAppsRelay(AllItems allItems) {
        this.allItems.accept(allItems);
    }

    public BehaviorRelay<List<LauncherItem>> getAppsRelay() {
        return appsRelay;
    }

    public BehaviorRelay<AllItems> getAllItemsRelay() {
        return allItems;
    }

    public static class AllItems {
        private final List<LauncherItem> items;
        private final List<LauncherItem> newAddedItems;

        public AllItems(
            List<LauncherItem> items,
            List<LauncherItem> newAddedItems
        ) {
            this.items = items;
            this.newAddedItems = newAddedItems;
        }

        public List<LauncherItem> getItems() {
            return items;
        }

        public List<LauncherItem> getNewAddedItems() {
            return newAddedItems;
        }
    }
}