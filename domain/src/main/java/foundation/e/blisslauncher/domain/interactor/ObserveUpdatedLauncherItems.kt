package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.entity.LauncherItem
import io.reactivex.Flowable
import java.util.concurrent.Executor

class ObserveUpdatedLauncherItems(appExecutors: AppExecutors) :
    PublishSubjectInteractor<List<LauncherItem>, List<LauncherItem>>() {
    override val subscribeExecutor: Executor = appExecutors.io
    override val observeExecutor: Executor = appExecutors.main

    override fun createObservable(params: List<LauncherItem>): Flowable<List<LauncherItem>> =
        Flowable.just(params)
}