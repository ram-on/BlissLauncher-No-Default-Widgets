package foundation.e.blisslauncher.mvicore.component

import io.reactivex.Observable

typealias Actor<State, Action, Effect> = (State, Action) -> Observable<out Effect>