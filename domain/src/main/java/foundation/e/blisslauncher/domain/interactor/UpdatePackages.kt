package foundation.e.blisslauncher.domain.interactor

import android.content.Context
import android.os.UserHandle
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.executors.AppExecutors
import io.reactivex.Completable
import java.util.concurrent.Executor
import javax.inject.Inject

class UpdatePackages @Inject constructor(
    appExecutors: AppExecutors,
    private val context: Context,
    private val observeAddedApps: ObserveAddedApps,
    private val launcherAppsCompat: LauncherAppsCompat
) : CompletableInteractor<UpdatePackages.Params>() {

    class Params(val user: UserHandle, vararg val packages: String)

    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: Params): Completable = Completable.fromAction {
        params.packages.forEach {
            // TODO: update icon cache
            //appsRepository.updateApp(context, it, params.user)
            //TODO: Remove from widget cache
        }
    }.doOnComplete {
        //observeAddedApps(Unit)
    }
}