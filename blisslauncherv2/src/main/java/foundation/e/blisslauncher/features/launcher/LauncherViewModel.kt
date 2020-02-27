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
) : BaseViewModel<LauncherState>(
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

    fun loadLauncher() {
        process(loadLauncherIntent())
    }

    private fun loadLauncherIntent(): BaseIntent<LauncherState> {
        return intent {
            loadLauncher(
                onSuccess = { list ->
                    process(intent {
                        val mutableAllItems = allItems.toMutableList()
                        mutableAllItems.addAll(list)
                        copy(data = list, allItems = mutableAllItems)
                    })
                },
                onError = {
                    it.printStackTrace()
                    process(intent { copy(data = emptyList()) })
                }
            )
            copy()
        }
    }

    fun terminate() {
        launcherStateInteractor(LauncherStateInteractor.Command.TERMINATE)
        disposable.dispose()
        observeAddedApps.dispose()
    }
}