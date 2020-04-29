package foundation.e.blisslauncher.mvicore.component

/**
 * Reducer function which takes current state, applies an effect to it and produce a new state.
 */
typealias Reducer<State, Effect> = (state: State, effect: Effect) -> State