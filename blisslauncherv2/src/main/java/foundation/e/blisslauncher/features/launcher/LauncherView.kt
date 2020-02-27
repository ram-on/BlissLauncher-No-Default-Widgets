package foundation.e.blisslauncher.features.launcher

import foundation.e.blisslauncher.base.presentation.BaseView
import foundation.e.blisslauncher.domain.keys.PackageUserKey

interface LauncherView:
    BaseView<LauncherState> {

    fun updateIconBadges(updatedBadges: Set<PackageUserKey>)
}