package foundation.e.blisslauncher.base.presentation

import io.reactivex.Observable

interface Model<S> {

    /**
     * Model will receive intents to be processed via this function
     *
     * Model State is immutable. Processed intents will copy and create a new modified state.
     */
    fun process(intent: BaseIntent<S>)

    /**
     * Observable stream of changes to the Model state
     *
     * Every time a model state is replaced by a new one, this observable will emit that.
     *
     * Views should only subscribe to this.
     */
    fun states(): Observable<S>
}