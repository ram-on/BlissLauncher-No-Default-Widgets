package foundation.e.blisslauncher.features.launcher

import foundation.e.blisslauncher.features.LauncherStore
import foundation.e.blisslauncher.mvicore.component.MviView

interface LauncherView : MviView<LauncherState, LauncherStore.LauncherIntent>