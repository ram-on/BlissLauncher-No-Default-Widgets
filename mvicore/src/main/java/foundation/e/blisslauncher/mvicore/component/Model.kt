package foundation.e.blisslauncher.mvicore.component

import io.reactivex.ObservableSource
import io.reactivex.functions.Consumer

interface Model<Intent: Any, State: Any> : Consumer<Intent>,
    ObservableSource<State> {
    val state: State
}