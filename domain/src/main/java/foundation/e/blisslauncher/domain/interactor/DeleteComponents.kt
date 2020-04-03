package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.domain.ItemInfoMatcher
import io.reactivex.Completable
import java.util.concurrent.Executor

class DeleteComponents(appExecutors: AppExecutors) : CompletableInteractor<ItemInfoMatcher>() {
    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: ItemInfoMatcher): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}