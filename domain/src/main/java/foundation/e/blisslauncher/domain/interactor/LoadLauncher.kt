package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.dto.WorkspaceModel
import foundation.e.blisslauncher.domain.repository.WorkspaceRepository
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadLauncher @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    appExecutors: AppExecutors
) : ResultInteractor<Unit, WorkspaceModel>() {

    override val subscribeExecutor: Executor = appExecutors.io
    override val observeExecutor: Executor = appExecutors.main

    override fun doWork(params: Unit?): Single<WorkspaceModel> =
        Single.fromCallable { workspaceRepository.loadWorkspace() }
            .subscribeOn(Schedulers.from(subscribeExecutor))
}