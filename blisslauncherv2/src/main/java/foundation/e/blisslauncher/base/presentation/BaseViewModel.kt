package foundation.e.blisslauncher.base.presentation

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

abstract class BaseViewModel<E : BaseViewEvent, S : BaseViewState>(initialState: S) :
    Model<S> {

    /**
     * Used to process events and state reducers
     */
    private val intents = PublishSubject.create<BaseIntent<S>>()

    private val store = intents
        .observeOn(AndroidSchedulers.mainThread())
        .scan(initialState) { oldState, intent -> intent.reduce(oldState) }
        .replay(1)
        .apply { connect() }

    private val internalLogger = store.subscribe({ Timber.i("$it") }, { throw it })

    override fun process(intent: BaseIntent<S>) = intents.onNext(intent)

    fun newState(onStateUpdate: S.() -> S) {
        process(intent(onStateUpdate))
    }

    override fun states(): Observable<S> = store

    abstract fun toIntent(event: E): BaseIntent<S>
}