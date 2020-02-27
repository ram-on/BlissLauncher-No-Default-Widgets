package foundation.e.blisslauncher.domain.interactor

import android.os.UserHandle
import foundation.e.blisslauncher.common.executors.AppExecutors
import io.reactivex.Flowable
import javax.inject.Inject

class ChangeUserLockState @Inject constructor(appExecutors: AppExecutors) :
    PublishSubjectInteractor<UserHandle, Any>() {
    override val subscribeExecutor = appExecutors.io
    override val observeExecutor = appExecutors.main

    override fun createObservable(params: UserHandle): Flowable<Any> {
        return Flowable.just("")
    }
}