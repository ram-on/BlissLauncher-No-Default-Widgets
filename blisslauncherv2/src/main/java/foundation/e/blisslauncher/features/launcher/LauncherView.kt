package foundation.e.blisslauncher.features.launcher

import foundation.e.blisslauncher.mvicore.component.MviView

interface LauncherView : MviView<LauncherView.LauncherViewModel, LauncherView.LauncherViewEvent> {
    data class LauncherViewModel(private val name: String)

    sealed class LauncherViewEvent {

    }
}