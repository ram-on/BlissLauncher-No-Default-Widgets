package foundation.e.blisslauncher.mvicore.component

import io.reactivex.ObservableSource
import io.reactivex.functions.Consumer

/**
 * Store manages the state of the application, similar to the Model
 * and are bound to a particular domain.
 */
interface Store<Intent: Any, State: Any> : Consumer<Intent>,
    ObservableSource<State> {
    val state: State
}