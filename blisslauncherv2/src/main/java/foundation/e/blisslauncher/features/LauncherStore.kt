package foundation.e.blisslauncher.features

import foundation.e.blisslauncher.features.LauncherStore.Intent
import foundation.e.blisslauncher.features.LauncherStore.Action
import foundation.e.blisslauncher.features.LauncherStore.Effect
import foundation.e.blisslauncher.features.LauncherStore.State
import foundation.e.blisslauncher.features.LauncherStore.News
import foundation.e.blisslauncher.mvicore.component.BaseStore

class LauncherStore :
    BaseStore<Intent, Action, Effect, State, News>() {

    sealed class Intent {

    }

    sealed class Effect {

    }

    sealed class News {

    }

    sealed class Action {

    }

    data class State(val name: String) {

    }
}