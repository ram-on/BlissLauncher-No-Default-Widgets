package foundation.e.blisslauncher.mvicore.component

import io.reactivex.Observable

/**
 * Function which maps Intent to Action.
 */
typealias IntentToAction<Intent, Action> = (intent: Intent) -> Action

/**
 * Actor function which takes current state, action and returns a stream of effects.
 */
typealias Actor<State, Action, Effect> = (State, Action) -> Observable<out Effect>

/**
 * Reducer function which takes current state, applies an effect to it and returns a new state.
 */
typealias Reducer<State, Effect> = (state: State, effect: Effect) -> State

/**
 * Publisher used to publish Single Events (aka News)
 */
typealias NewsPublisher<Action, Effect, State, News> =
        (action: Action, effect: Effect, state: State) -> News?
