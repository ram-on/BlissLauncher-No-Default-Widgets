/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.e.blisslauncher.core.utils;

import android.content.ComponentName;
import android.os.UserHandle;

import java.util.HashSet;

import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.FolderItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.features.shortcuts.ShortcutKey;

/**
 * A utility class to check for {@link foundation.e.blisslauncher.core.database.model.LauncherItem}
 */
public interface ItemInfoMatcher {

    boolean matches(LauncherItem info, ComponentName cn);

    /**
     * Filters {@param infos} to those satisfying the {@link #matches(LauncherItem, ComponentName)}.
     */
    default HashSet<LauncherItem> filterItemInfos(Iterable<LauncherItem> infos) {
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
    default ItemInfoMatcher or(ItemInfoMatcher matcher) {
        return (info, cn) -> matches(info, cn) || matcher.matches(info, cn);
    }

    /**
     * Returns a new matcher with returns true if both this and {@param matcher} returns true.
     */
    default ItemInfoMatcher and(ItemInfoMatcher matcher) {
        return (info, cn) -> matches(info, cn) && matcher.matches(info, cn);
    }

    /**
     * Returns a new matcher which returns the opposite boolean value of the provided
     * {@param matcher}.
     */
    static ItemInfoMatcher not(ItemInfoMatcher matcher) {
        return (info, cn) -> !matcher.matches(info, cn);
    }

    static ItemInfoMatcher ofUser(UserHandle user) {
        return (info, cn) -> info.user.equals(user);
    }

    static ItemInfoMatcher ofComponents(HashSet<ComponentName> components, UserHandle user) {
        return (info, cn) -> components.contains(cn) && info.user.equals(user);
    }

    static ItemInfoMatcher ofPackages(HashSet<String> packageNames, UserHandle user) {
        return (info, cn) -> packageNames.contains(cn.getPackageName()) && info.user.equals(user);
    }

    static ItemInfoMatcher ofShortcutKeys(HashSet<ShortcutKey> keys) {
        return  (info, cn) -> info.itemType == Constants.ITEM_TYPE_SHORTCUT &&
                        keys.contains(ShortcutKey.fromItem((ShortcutItem) info));
    }

    static ItemInfoMatcher ofItemIds(IntSparseArrayMap<Boolean> ids, Boolean matchDefault) {
        return (info, cn) -> ids.get(Integer.parseInt(info.id), matchDefault);
    }
}
