package foundation.e.blisslauncher.domain.interactor

import android.content.ComponentName
import android.os.UserHandle
import android.util.ArrayMap
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.common.util.LongArrayMap
import foundation.e.blisslauncher.domain.ItemInfoMatcher
import foundation.e.blisslauncher.domain.Matcher
import foundation.e.blisslauncher.domain.and
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.WorkspaceItem
import foundation.e.blisslauncher.domain.or
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import io.reactivex.Completable
import java.util.concurrent.Executor

class UpdateLauncher(
    appExecutors: AppExecutors,
    private val launcherRepository: LauncherRepository,
    private val launcherAppsCompat: LauncherAppsCompat,
    private val deleteComponents: DeleteComponents
) : CompletableInteractor<UpdateLauncher.Params>() {

    override val subscribeExecutor: Executor = appExecutors.io

    override fun doWork(params: Params): Completable = Completable.fromAction {
        val addedOrUpdated = ArrayList<ApplicationItem>()
        /* addedOrUpdated.addAll(appsRepository.getModifiedApps())
         addedOrUpdated.addAll(appsRepository.getAddedApps())

         val removedApps = ArrayList<ApplicationItem>(appsRepository.getRemovedApps())
         appsRepository.clear()*/

        val addedOrUpdatedApps = ArrayMap<ComponentName, ApplicationItem>()
        if (addedOrUpdated.isNotEmpty()) {
            // TODO: Push updated apps here
            addedOrUpdated.forEach {
                addedOrUpdatedApps[it.componentName] = it
            }
        }

        // LauncherItems that are about to be removed
        val removedItems = LongArrayMap<Boolean>()

        val isNewApkAvailable = params.command == Command.ADD || params.command == Command.UPDATE
        val updatedItems = ArrayList<WorkspaceItem>()
        //TODO: Uncomment it after successful presentation test.
        //val map = launcherRepository.allItemsMap()
        /*map.forEach {
            if (it is WorkspaceItem && params.user == it.user) it.let { workspaceItem ->
                var itemUpdated = false
                var shortcutUpdated = false
                if (workspaceItem.iconResource != null && params.packages.contains(workspaceItem.iconResource!!.packageName)) {
                    //TODO: Update shortcut icon here
                    itemUpdated = true
                }

                val cn = workspaceItem.getTargetComponent()
                if (cn != null && params.matcher(workspaceItem, cn)) {
                    val applicationItem = addedOrUpdatedApps[cn]
                    if (workspaceItem.hasStatusFlag(WorkspaceItem.FLAG_SUPPORTS_WEB_UI)) {
                        removedItems.put(it.id, false)
                        if (params.command == Command.REMOVE) {
                            return@forEach
                        }
                    }

                    if (workspaceItem.isPromise() && isNewApkAvailable) {
                        if(workspaceItem.hasStatusFlag(WorkspaceItem.FLAG_AUTOINSTALL_ICON)) {
                            if(launcherAppsCompat.isActivityEnabledForProfile(cn, params.user)) {

                            }
                        }
                    }

                    if (isNewApkAvailable &&
                        workspaceItem.itemType == LauncherConstants.ItemType.APPLICATION) {
                        // update icon cache from tile
                        itemUpdated = true
                    }

                    val oldRuntimeFlags = workspaceItem.runtimeStatusFlags
                    workspaceItem.runtimeStatusFlags =
                        params.flagOp(workspaceItem.runtimeStatusFlags)
                    if (oldRuntimeFlags != workspaceItem.runtimeStatusFlags) {
                        shortcutUpdated = true
                    }
                }

                if (itemUpdated || shortcutUpdated) {
                    updatedItems.add(workspaceItem)
                }

                if (itemUpdated) {
                    //TODO: Updated item in database here
                }

            }
            //TODO: Update launcher widgets here
        }*/

        // TODO: Update Shortcut here
        if (!removedItems.isEmpty) {
            deleteComponents(Matcher.ofItemIds(removedItems, false))
        }
        // TODO: Update or Add new apps here
        // TODO: Update widgets here

        val removedPackages = HashSet<String>()
        val removedComponents = HashSet<ComponentName>()

        if (params.command == Command.REMOVE) {
            removedPackages.addAll(params.packages)
        } else if (params.command == Command.UPDATE) {
            params.packages.forEach {
                if (!launcherAppsCompat.isPackageEnabledForProfile(it, params.user)) {
                    removedPackages.add(it)
                }
            }
            //TODO
            //Update removedComponents because some packages can get removed during package update
            /*removedApps.forEach {
                removedComponents.add(it.componentName)
            }*/
        }

        if (removedPackages.isNotEmpty() || removedComponents.isNotEmpty()) {
            val removeMatch = Matcher.ofPackages(removedPackages, params.user)
                .or(Matcher.ofComponents(removedComponents, params.user))
                .and(Matcher.ofItemIds(removedItems, true))
            deleteComponents(removeMatch)

            //TODO:  Remove packages from InstallQueue
        }

        if (Utilities.ATLEAST_OREO && params.command == Command.ADD) {
            // Load widgets for the new package. Changes due to app updates are handled through
            // AppWidgetHost events, this is just to initialize the long-press options.
            /*for (i in 0 until N) {
                dataModel.widgetsModel.update(app, PackageUserKey(packages.get(i), mUser))
            }
            bindUpdatedWidgets(dataModel)*/
            //TODO: Update widget model here
        }
    }

    data class Params(
        val command: Command,
        val packages: HashSet<String>,
        val user: UserHandle,
        val matcher: ItemInfoMatcher,
        val flagOp: (flag: Int) -> Int
    )

    enum class Command {
        ADD, UPDATE, REMOVE, UNAVAILABLE, SUSPEND, UNSUSPEND, USER_AVAILABILITY_CHANGE
    }
}