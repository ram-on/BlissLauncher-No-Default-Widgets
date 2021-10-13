package foundation.e.blisslauncher.features.test;

import android.content.ComponentName;
import android.os.UserHandle;

import java.util.HashSet;

import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.FolderItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.core.utils.LongArrayMap;
import foundation.e.blisslauncher.features.shortcuts.ShortcutKey;

public abstract class LauncherItemMatcher {

    public abstract boolean matches(LauncherItem info, ComponentName cn);

    /**
     * Filters {@param infos} to those satisfying the {@link #matches(LauncherItem, ComponentName)}.
     */
    public final HashSet<LauncherItem> filterItemInfos(Iterable<LauncherItem> infos) {
        HashSet<LauncherItem> filtered = new HashSet<>();
        for (LauncherItem i : infos) {
            if (i instanceof ApplicationItem) {
                ApplicationItem info = (ApplicationItem) i;
                ComponentName cn = info.getTargetComponent();
                if (cn != null && matches(info, cn)) {
                    filtered.add(info);
                }
            } else if (i instanceof ShortcutItem) {
                ShortcutItem info = (ShortcutItem) i;
                ComponentName cn = info.getTargetComponent();
                if (cn != null && matches(info, cn)) {
                    filtered.add(info);
                }
            } else if (i instanceof FolderItem) {
                FolderItem info = (FolderItem) i;
                for (LauncherItem s : info.items) {
                    ComponentName cn = s.getTargetComponent();
                    if (cn != null && matches(s, cn)) {
                        filtered.add(s);
                    }
                }
            }
        }
        return filtered;
    }

    /**
     * Returns a new matcher with returns true if either this or {@param matcher} returns true.
     */
    public LauncherItemMatcher or(final LauncherItemMatcher matcher) {
        final LauncherItemMatcher that = this;
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return that.matches(info, cn) || matcher.matches(info, cn);
            }
        };
    }

    /**
     * Returns a new matcher with returns true if both this and {@param matcher} returns true.
     */
    public LauncherItemMatcher and(final LauncherItemMatcher matcher) {
        final LauncherItemMatcher that = this;
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return that.matches(info, cn) && matcher.matches(info, cn);
            }
        };
    }

    /**
     * Returns a new matcher which returns the opposite boolean value of the provided
     * {@param matcher}.
     */
    public static LauncherItemMatcher not(final LauncherItemMatcher matcher) {
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return !matcher.matches(info, cn);
            }
        };
    }

    public static LauncherItemMatcher ofUser(final UserHandle user) {
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return info.user.equals(user);
            }
        };
    }

    public static LauncherItemMatcher ofComponents(
        final HashSet<ComponentName> components, final UserHandle user
    ) {
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return components.contains(cn) && info.user.equals(user);
            }
        };
    }

    public static LauncherItemMatcher ofPackages(
        final HashSet<String> packageNames, final UserHandle user
    ) {
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return packageNames.contains(cn.getPackageName()) && info.user.equals(user);
            }
        };
    }

    public static LauncherItemMatcher ofShortcutKeys(final HashSet<ShortcutKey> keys) {
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return info.itemType == Constants.ITEM_TYPE_SHORTCUT &&
                    keys.contains(ShortcutKey.fromItem(((ShortcutItem) info)));
            }
        };
    }

    public static LauncherItemMatcher ofItemIds(
        final LongArrayMap<Boolean> ids, final Boolean matchDefault
    ) {
        return new LauncherItemMatcher() {
            @Override
            public boolean matches(LauncherItem info, ComponentName cn) {
                return ids.get(info.keyId, matchDefault);
            }
        };
    }
}
