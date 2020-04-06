package foundation.e.blisslauncher.domain.interactor

import android.os.UserHandle
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.repository.LauncherItemRepository
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import io.reactivex.Completable
import java.util.concurrent.Executor
import javax.inject.Inject

class ChangeUserAvailability @Inject constructor(
    appExecutors: AppExecutors,
    private val launcherItemRepository: LauncherItemRepository,
    private val userManager: UserManagerRepository,
    private val observeUpdatedLauncherItems: ObserveUpdatedLauncherItems
) : CompletableInteractor<UserHandle>() {

    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: UserHandle): Completable = Completable.fromAction {
        observeUpdatedLauncherItems(
            launcherItemRepository.updateUserAvailability(
                params,
                userManager.isQuietModeEnabled(params)
            )
        )
    }
}