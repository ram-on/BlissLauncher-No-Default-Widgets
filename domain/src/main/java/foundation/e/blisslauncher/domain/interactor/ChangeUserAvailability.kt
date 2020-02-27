package foundation.e.blisslauncher.domain.interactor

import android.os.UserHandle
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import io.reactivex.Completable
import java.util.concurrent.Executor
import javax.inject.Inject

class ChangeUserAvailability @Inject constructor(
    appExecutors: AppExecutors,
    private val launcherRepository: LauncherRepository,
    private val userManager: UserManagerRepository,
    private val observeUpdatedLauncherItems: ObserveUpdatedLauncherItems
) : CompletableInteractor<UserHandle>() {

    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: UserHandle): Completable = Completable.fromAction {
        observeUpdatedLauncherItems(
            launcherRepository.updateUserAvailability(
                params,
                userManager.isQuietModeEnabled(params)
            )
        )
    }
}