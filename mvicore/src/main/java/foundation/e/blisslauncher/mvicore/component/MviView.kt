package foundation.e.blisslauncher.mvicore.component

import io.reactivex.Observable

interface MviView<in ViewModel : Any, Event : Any> {

    val events: Observable<Event>

    fun render(model: ViewModel)
}