package foundation.e.blisslauncher.base.presentation

import io.reactivex.Observable

interface BaseView<State : BaseViewState> {

    fun intents(): Observable<BaseIntent<State>>

    fun render(state: State)
}