package foundation.e.blisslauncher.mvicore.component

import io.reactivex.Observable

interface MviView<in State : Any, ViewEvent : Any> {

    val events: Observable<ViewEvent>

    fun render(state: State)
}