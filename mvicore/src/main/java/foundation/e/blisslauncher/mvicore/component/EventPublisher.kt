package foundation.e.blisslauncher.mvicore.component

typealias EventPublisher<Action, Effect, State, Event> =
        (action: Action, effect: Effect, state: State) -> Event?