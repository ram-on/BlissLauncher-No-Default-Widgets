package foundation.e.blisslauncher.features.launcher

import foundation.e.blisslauncher.base.presentation.BaseViewEvent

sealed class LauncherViewEvent: BaseViewEvent {
    object LoadLauncher: LauncherViewEvent()
}