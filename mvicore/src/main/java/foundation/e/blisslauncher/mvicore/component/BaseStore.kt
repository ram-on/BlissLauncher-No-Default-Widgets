package foundation.e.blisslauncher.mvicore.component

import foundation.e.blisslauncher.mvicore.util.SameThreadVerifier
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

open class BaseStore<Intent : Any, Action : Any, Effect : Any, State : Any, News : Any>(
    initialState: State,
    private val intentToAction: IntentToAction<Intent, Action>,
    private val actor: Actor<State, Action, Effect>,
    private val reducer: Reducer<State, Effect>,
    private val newsPublisher: NewsPublisher<Action, Effect, State, News>? = null
) : Store<Intent, State>, Disposable {

    private val threadVerifier = SameThreadVerifier()
    private val actionSubject = PublishSubject.create<Action>()
    private val stateSubject = BehaviorSubject.createDefault(initialState)
    private val newsSubject = PublishSubject.create<News>()

    private val disposable = CompositeDisposable()

    private val news: ObservableSource<News>
        get() = newsSubject

    override val state: State
        get() = stateSubject.value!!

    init {
        disposable += actionSubject.subscribe { invokeActor(state, it) }
    }

    override fun accept(intent: Intent) {
        val action = intentToAction(intent)
        actionSubject.onNext(action)
    }

    override fun subscribe(observer: Observer<in State>) {
        stateSubject.subscribe(observer)
    }

    override fun isDisposed(): Boolean = disposable.isDisposed

    override fun dispose() = disposable.dispose()

    private fun invokeActor(state: State, action: Action) {
        if (isDisposed) return

        disposable += actor(state, action)
            .subscribe { invokeReducer(stateSubject.value!!, action, it) }
    }

    private fun invokeReducer(state: State, action: Action, effect: Effect) {
        if(isDisposed) return

        threadVerifier.verify()

        val newState = reducer.invoke(state, effect)
        stateSubject.onNext(newState)
        newsPublisher?.invoke(action, effect, state)?.let {
            newsSubject.onNext(it)
        }
    }
}