package foundation.e.blisslauncher.common

import foundation.e.blisslauncher.base.presentation.BaseViewState
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

fun <S : BaseViewState> Observable<S>.subscribeToState(onNext: (state: S) -> Unit): Disposable =
    this.subscribe(onNext)