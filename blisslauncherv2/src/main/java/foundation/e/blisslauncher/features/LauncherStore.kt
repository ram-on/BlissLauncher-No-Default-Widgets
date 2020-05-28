package foundation.e.blisslauncher.features

import foundation.e.blisslauncher.domain.dto.WorkspaceModel
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.interactor.LoadLauncher
import foundation.e.blisslauncher.features.LauncherStore.LauncherIntent
import foundation.e.blisslauncher.features.LauncherStore.Action
import foundation.e.blisslauncher.features.LauncherStore.Effect
import foundation.e.blisslauncher.features.LauncherStore.News
import foundation.e.blisslauncher.features.launcher.LauncherState
import foundation.e.blisslauncher.mvicore.component.Actor
import foundation.e.blisslauncher.mvicore.component.BaseStore
import foundation.e.blisslauncher.mvicore.component.IntentToAction
import foundation.e.blisslauncher.mvicore.component.Reducer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

class LauncherStore @Inject constructor(loadLauncher: LoadLauncher) :
    BaseStore<LauncherIntent, Action, Effect, LauncherState, News>(
        LauncherState.Loading,
        IntentToActionImpl(),
        ActorImpl(loadLauncher),
        ReducerImpl()
    ) {

    sealed class LauncherIntent {
        object InitialIntent : LauncherIntent()
    }

    sealed class Effect {
        object Loading : Effect()
        object ErrorLoading : Effect()

        data class LoadedResponse(val workspaceModel: WorkspaceModel) : Effect()
    }

    sealed class News

    sealed class Action {
        object LoadLauncher : Action()
    }

    class IntentToActionImpl : IntentToAction<LauncherIntent, Action> {
        override fun invoke(intent: LauncherIntent): Action = when (intent) {
            is LauncherIntent.InitialIntent -> Action.LoadLauncher
        }
    }

    class ActorImpl(private val loadLauncher: LoadLauncher) : Actor<LauncherState, Action, Effect> {
        override fun invoke(state: LauncherState, action: Action): Observable<out Effect> {
            return loadLauncher().toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .map { Effect.LoadedResponse(it) as Effect }
                .startWith(Effect.Loading)
                .onErrorReturn { Effect.ErrorLoading }
        }
    }

    class ReducerImpl : Reducer<LauncherState, Effect> {
        override fun invoke(state: LauncherState, effect: Effect): LauncherState {
            return when(effect) {
                Effect.Loading -> LauncherState.Loading
                Effect.ErrorLoading -> LauncherState.Error
                is Effect.LoadedResponse -> LauncherState.Loaded(effect.workspaceModel)
            }
        }
    }
}