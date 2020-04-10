package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.dto.WorkspaceModel
import foundation.e.blisslauncher.domain.entity.LauncherConstants.ContainerType.CONTAINER_DESKTOP
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import foundation.e.blisslauncher.domain.repository.WorkspaceScreenRepository
import io.reactivex.Single
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadLauncher @Inject constructor(
    private val launcherItemRepository: LauncherItemRepository,
    private val workspaceScreenRepository: WorkspaceScreenRepository,
    appExecutors: AppExecutors
) : ResultInteractor<Unit, WorkspaceModel>() {

    override val subscribeExecutor: Executor = appExecutors.io
    override val observeExecutor: Executor = appExecutors.main

    override fun doWork(params: Unit?): Single<WorkspaceModel> {
        return Single.just(WorkspaceModel())
            .map { workspaceModel ->
                var clearDb = false

                //TODO: GridSize Migration Task
                /*if (!clearDb && GridSizeMigrationTask.ENABLED &&
                    !GridSizeMigrationTask.migrateGridIfNeeded(context)
                ) {
                    // Migration failed. Clear workspace.
                    clearDb = true
                }*/

                if (clearDb) {
                    Timber.d("loadLauncher: resetting launcher database")
                    clearAllDbs()
                }

                workspaceModel.workspaceScreens.addAll(
                    workspaceScreenRepository.findAllOrderedByScreenRank()
                        .map { it.id })

                val launcherItems = launcherItemRepository.findAll()

                // Remove any empty screens
                val unusedScreens: ArrayList<Long> =
                    ArrayList<Long>(workspaceModel.workspaceScreens)
                for (item in workspaceModel.itemsIdMap) {
                    val screenId: Long = item.screenId
                    if (item.container == CONTAINER_DESKTOP.toLong() &&
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
                workspaceModel
            }
    }

    private fun clearAllDbs() {
        workspaceScreenRepository.deleteAll()
        launcherItemRepository.deleteAll()
    }
}