package foundation.e.blisslauncher.domain.interactor

import android.os.UserHandle
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import io.reactivex.Completable
import java.util.concurrent.Executor
import javax.inject.Inject

class MakePackageUnavailable @Inject constructor(
    appExecutors: AppExecutors,
    private val launcherRepository: LauncherRepository,
    private val observeUpdatedLauncherItems: ObserveUpdatedLauncherItems
) : CompletableInteractor<MakePackageUnavailable.Params>() {

    class Params(val user: UserHandle, vararg val packages: String)

    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: Params): Completable = Completable.fromAction {
        observeUpdatedLauncherItems(
            launcherRepository.makePackagesUnavailable(
                params.packages,
                params.user
            )
        )
    }
}