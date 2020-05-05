package foundation.e.blisslauncher.domain

import android.content.ComponentName
import android.os.UserHandle
import foundation.e.blisslauncher.utils.LongArrayMap
import foundation.e.blisslauncher.domain.entity.LauncherItem

typealias ItemInfoMatcher = (item: LauncherItem, cn: ComponentName) -> Boolean

fun ItemInfoMatcher.or(itemInfoMatcher: ItemInfoMatcher): ItemInfoMatcher {
    return { launcherItem: LauncherItem, componentName: ComponentName ->
        itemInfoMatcher(launcherItem, componentName) || this(launcherItem, componentName)
    }
}

fun ItemInfoMatcher.and(itemInfoMatcher: ItemInfoMatcher): ItemInfoMatcher {
    return { launcherItem: LauncherItem, componentName: ComponentName ->
        itemInfoMatcher(launcherItem, componentName) && this(launcherItem, componentName)
    }
}

fun ItemInfoMatcher.not(itemInfoMatcher: ItemInfoMatcher): ItemInfoMatcher {
    return { launcherItem: LauncherItem, componentName: ComponentName ->
        !itemInfoMatcher(launcherItem, componentName)
    }
}

class Matcher {
    companion object {
        fun ofPackages(packageNames: HashSet<String>, user: UserHandle): ItemInfoMatcher =
            { launcherItem: LauncherItem, componentName: ComponentName ->
                packageNames.contains(componentName.packageName) && launcherItem.user == user
            }

        fun ofComponents(components: HashSet<ComponentName>, user: UserHandle): ItemInfoMatcher =
            { launcherItem: LauncherItem, componentName: ComponentName ->
                components.contains(componentName) && launcherItem.user == user
            }

        fun ofItemIds(ids: LongArrayMap<Boolean>, matchDefault: Boolean): ItemInfoMatcher =
            { launcherItem: LauncherItem, _: ComponentName ->
                ids.get(launcherItem.id, matchDefault)
            }

        fun ofUser(user: UserHandle): ItemInfoMatcher =
            { launcherItem: LauncherItem, componentName: ComponentName ->
                launcherItem.user == user
            }
    }
}

typealias ApplyFlag = (flag: Int) -> (oldFlags: Int) -> Int

val addFlag: ApplyFlag = { flag -> { oldFlags -> oldFlags or flag } }

val removeFlag: ApplyFlag = { flag -> { oldFlags -> oldFlags and flag.inv() } }