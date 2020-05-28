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

    override fun doWork(params: Unit?): Single<WorkspaceModel> {
        Timber.d("This is invoked")

        return Single.just(WorkspaceModel())
            .map { workspaceModel ->

                Timber.d("Current working thread is ${Thread.currentThread().name}")

                /*workspaceModel.workspaceScreens.addAll(
                    workspaceScreenRepository.findAllOrderedByScreenRank()
                )

                val launcherItems = launcherItemRepository.findAll()
                var count = 0
                launcherItems.forEach { count++ }

                // Remove any empty screens
                val unusedScreens: ArrayList<Long> =
                    ArrayList<Long>(workspaceModel.workspaceScreens)
                for (item in workspaceModel.itemsIdMap) {
                    val screenId: Long = item.screenId
                    if (item.container == CONTAINER_DESKTOP &&
                        unusedScreens.contains(screenId)
                    ) {
                        unusedScreens.remove(screenId)
                    }
                }

                // If there are any empty screens remove them, and update.
                if (unusedScreens.size != 0) {
                    workspaceModel.workspaceScreens.removeAll(unusedScreens)
                    //TODO: update workspace screen order in database.
                }
                val sortedList = sortWorkspaceItems(launcherItems)*/
                workspaceRepository.loadWorkspace()
            }
            .subscribeOn(Schedulers.from(subscribeExecutor))
    }
}