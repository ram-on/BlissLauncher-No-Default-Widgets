package foundation.e.blisslauncher.domain.interactor

import android.os.Looper
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * Interactor to execute tasks synchronously on main thread.
 */
abstract class SynchronousInteractor<in P> {
    abstract fun doWork(params: P)

    operator fun invoke(params: P, block: () -> Unit = {}) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("Can't be executed from thread other than main")
        }
        doWork(params)
        block() // To do any additional work
    }
}

abstract class AsyncInteractor<in P> : Disposable {
    abstract val subscribeExecutor: Executor

    protected val disposables: CompositeDisposable = CompositeDisposable()

    override fun dispose() = disposables.dispose()

    override fun isDisposed(): Boolean = disposables.isDisposed
}

abstract class CompletableInteractor<in P> : AsyncInteractor<P>() {

    protected abstract fun doWork(params: P): Completable

    operator fun invoke(params: P, onComplete: () -> Unit = {}) {
        this.disposables += doWork(params)
            .subscribeOn(Schedulers.from(subscribeExecutor))
            .subscribe(onComplete, Timber::w)
    }

    fun executeSync(params: P) {
        doWork(params)
    }
}

abstract class ObservableInteractor<in P> : AsyncInteractor<P>() {
    abstract val observeExecutor: Executor
}

abstract class ResultInteractor<in P, T> : ObservableInteractor<P>() {

    abstract fun doWork(params: P? = null): Single<T>

    operator fun invoke(
        params: P? = null
    ): Single<T> {
        return this.doWork(params)
    }
}

abstract class SubjectInteractor<P, T> : ObservableInteractor<P>() {
    abstract val subject: Subject<P>

    protected abstract fun createObservable(params: P): Flowable<T>

    operator fun invoke(params: P) = subject.onNext(params)

    fun observe(onNext: (result: T) -> Unit = {}) {
        disposables += subject.toFlowable(BackpressureStrategy.BUFFER)
            .flatMap { createObservable(it) }
            .subscribeOn(Schedulers.from(subscribeExecutor))
            .observeOn(Schedulers.from(observeExecutor))
            .subscribe(onNext)
    }
}

abstract class PublishSubjectInteractor<P, T> : SubjectInteractor<P, T>() {
    override val subject: Subject<P> = PublishSubject.create()
}

abstract class BehaviourSubjectInteractor<P, T> : SubjectInteractor<P, T>() {
    override val subject: Subject<P> = BehaviorSubject.create()
}

abstract class FlowableInteractor<in P, T> : ObservableInteractor<P>() {

    protected abstract fun buildObservable(params: P? = null): Flowable<T>

    operator fun invoke(
        params: P,
        onNext: (next: T) -> Unit = {},
        onError: (e: Throwable) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        disposables += this.buildObservable(params)
            .subscribeOn(Schedulers.from(subscribeExecutor))
            .observeOn(Schedulers.from(observeExecutor))
            .subscribe(onNext, onError, onComplete)
    }
}