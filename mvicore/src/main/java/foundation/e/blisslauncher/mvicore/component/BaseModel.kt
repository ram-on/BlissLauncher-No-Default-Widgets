package foundation.e.blisslauncher.mvicore.component

import foundation.e.blisslauncher.mvicore.util.SameThreadVerifier
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

open class BaseModel<Intent : Any, Action : Any, Effect : Any, State : Any, Event : Any>(
    initialState: State,
    private val intentToAction: IntentToAction<Intent, Action>,
    actor: Actor<State, Action, Effect>,
    reducer: Reducer<State, Effect>,
    eventPublisher: EventPublisher<Action, Effect, State, Event>? = null
) : Model<Intent, State>, Disposable {

    private val threadVerifier = SameThreadVerifier()
    private val actionSubject = PublishSubject.create<Action>()
    private val stateSubject = PublishSubject.create<State>()
    private val eventSubject = PublishSubject.create<Event>()

    private val disposable = CompositeDisposable()

    private val eventPublisherWrapper = eventPublisher?.let {
        EventPublisherWrapper(eventPublisher, eventSubject)
    }

    init {
        disposable += eventPublisherWrapper
    }

    override fun accept(t: Intent) {
        TODO("Not yet implemented")
    }

    override fun subscribe(observer: Observer<in State>) {
        TODO("Not yet implemented")
    }

    override val state: State
        get() = TODO("Not yet implemented")

    override fun isDisposed(): Boolean = disposable.isDisposed

    override fun dispose() = disposable.dispose()

    private class EventPublisherWrapper<Action : Any, Effect : Any, State : Any, Event : Any>(
        private val eventPublisher: EventPublisher<Action, Effect, State, Event>,
        private val events: Subject<Event>
    ) : Consumer<Triple<Action, Effect, State>>, Disposable {

        override fun accept(t: Triple<Action, Effect, State>) {
            val (action, effect, state) = t
            eventPublisher.invoke(action, effect, state)?.let {
                events.onNext(it)
            }
        }
    }
}