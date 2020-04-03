package foundation.e.blisslauncher.features.launcher

import foundation.e.blisslauncher.base.presentation.BaseIntent
import foundation.e.blisslauncher.base.presentation.BaseViewModel
import foundation.e.blisslauncher.base.presentation.intent
import foundation.e.blisslauncher.common.util.LongArrayMap
import foundation.e.blisslauncher.domain.interactor.LauncherStateInteractor
import foundation.e.blisslauncher.domain.interactor.LoadLauncher
import foundation.e.blisslauncher.domain.interactor.ObserveAddedApps
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class LauncherViewModel @Inject constructor(
    private val launcherStateInteractor: LauncherStateInteractor,
    private val observeAddedApps: ObserveAddedApps,
    private val loadLauncher: LoadLauncher
) : BaseViewModel<LauncherViewEvent, LauncherState>(
    LauncherState(
        itemsIdMap = LongArrayMap(),
        allItems = emptyList(),
        folders = LongArrayMap(),
        workspaceScreen = emptyList(),
        data = emptyList()
    )
) {
    private val disposable = CompositeDisposable()

    init {
        launcherStateInteractor(LauncherStateInteractor.Command.INIT)

        observeAddedApps.observe {
        }
    }

    fun terminate() {
        launcherStateInteractor(LauncherStateInteractor.Command.TERMINATE)
        disposable.dispose()
        observeAddedApps.dispose()
    }

    override fun toIntent(event: LauncherViewEvent): BaseIntent<LauncherState> {
        return when (event) {
            is LauncherViewEvent.LoadLauncher -> intent {
                loadLauncher(
                    onSuccess = {
                        newState { copy(data = emptyList()) }
                    },
                    onError = {
                        it.printStackTrace()
                        copy(data = emptyList())
                    }
                )
                copy()
            }
        }
    }
}