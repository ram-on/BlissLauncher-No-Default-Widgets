package foundation.e.blisslauncher.mvicore.component

import io.reactivex.Observable
import io.reactivex.functions.Consumer

interface MviView<in ViewModel : Any, ViewEvent : Any> {

    val events: Observable<ViewEvent>

    fun render(model: ViewModel)
}