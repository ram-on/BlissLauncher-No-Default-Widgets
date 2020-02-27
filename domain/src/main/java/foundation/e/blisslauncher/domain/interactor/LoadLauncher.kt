package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import io.reactivex.Single
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadLauncher @Inject constructor(
    private val launcherApps: LauncherAppsCompat,
    private val userManager: UserManagerRepository,
    appExecutors: AppExecutors
) : ResultInteractor<Unit, List<ApplicationItem>>() {

    override val subscribeExecutor: Executor = appExecutors.io
    override val observeExecutor: Executor = appExecutors.main

    override fun doWork(params: Unit?): Single<List<ApplicationItem>> {
        return Single.just(
            userManager.userProfiles.map { user ->
                launcherApps.getActivityList(null, user)
                    .map { ApplicationItem(it, user, userManager.isQuietModeEnabled(user)) }
            }.flatten()
        )
    }
}