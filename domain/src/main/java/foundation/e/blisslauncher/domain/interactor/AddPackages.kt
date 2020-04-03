package foundation.e.blisslauncher.domain.interactor

import android.os.UserHandle
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import io.reactivex.Completable
import java.util.concurrent.Executor
import javax.inject.Inject

class AddPackages @Inject constructor(
    appExecutors: AppExecutors,
    private val launcherRepository: LauncherRepository,
    private val userManager: UserManagerRepository,
    private val observeAddedApps: ObserveAddedApps
) : CompletableInteractor<AddPackages.Params>() {

    class Params(val user: UserHandle, vararg val packages: String)

    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: Params): Completable = Completable.fromAction {
        params.packages.forEach {
            //TODO: Update icons cache
            launcherRepository.add(it, params.user, userManager.isQuietModeEnabled(params.user))
            //TODO: Add SessionCommitReceiver for below O devices
        }
    } /*.doOnComplete {
        observeAddedApps(Unit)
    }*/
}