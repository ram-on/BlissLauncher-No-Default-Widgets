package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.domain.manager.LauncherStateManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherStateInteractor @Inject constructor(private val launcherStateManager: LauncherStateManager) :
    SynchronousInteractor<LauncherStateInteractor.Command>() {

    override fun doWork(command: Command) {
        if (command == Command.INIT)
            launcherStateManager.init()
        else if (command == Command.TERMINATE)
            launcherStateManager.terminate()
    }

    enum class Command { INIT, TERMINATE }
}