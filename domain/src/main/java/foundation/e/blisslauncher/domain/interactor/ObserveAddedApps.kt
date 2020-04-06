package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import io.reactivex.Flowable
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveAddedApps @Inject constructor(
    appExecutors: AppExecutors,
    private val launcherItemRepository: LauncherItemRepository
) : PublishSubjectInteractor<List<ApplicationItem>, List<ApplicationItem>>() {

    override val subscribeExecutor: Executor = appExecutors.io

    override val observeExecutor: Executor = appExecutors.main

    override fun createObservable(params: List<ApplicationItem>): Flowable<List<ApplicationItem>> =
        Flowable.just(params)
}