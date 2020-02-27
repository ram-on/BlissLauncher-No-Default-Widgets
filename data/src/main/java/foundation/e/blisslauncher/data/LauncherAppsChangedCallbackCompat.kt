package foundation.e.blisslauncher.data

import android.os.UserHandle
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import foundation.e.blisslauncher.domain.interactor.AddPackages
import foundation.e.blisslauncher.domain.interactor.MakePackageUnavailable
import foundation.e.blisslauncher.domain.interactor.RemovePackages
import foundation.e.blisslauncher.domain.interactor.SuspendPackages
import foundation.e.blisslauncher.domain.interactor.UnsuspendPackages
import foundation.e.blisslauncher.domain.interactor.UpdatePackages
import javax.inject.Inject

class LauncherAppsChangedCallbackCompat
@Inject constructor(
    private val updatePackages: UpdatePackages,
    private val addPackages: AddPackages,
    private val removePackages: RemovePackages,
    private val makePackageUnavailable: MakePackageUnavailable,
    private val suspendPackages: SuspendPackages,
    private val unsuspendPackages: UnsuspendPackages
) :
    LauncherAppsCompat.OnAppsChangedCallbackCompat {
    override fun onPackageRemoved(packageName: String, user: UserHandle) {
        removePackages(
            RemovePackages.Params(user, packageName)
        )
    }

    override fun onPackageAdded(packageName: String, user: UserHandle) {
        addPackages(
            AddPackages.Params(user, packageName)
        )
    }

    override fun onPackageChanged(packageName: String, user: UserHandle) {
        updatePackages(
            UpdatePackages.Params(user, packageName)
        )
    }

    override fun onPackagesAvailable(
        packageNames: Array<String>,
        user: UserHandle,
        replacing: Boolean
    ) {
        updatePackages(
            UpdatePackages.Params(user, *packageNames)
        )
    }

    override fun onPackagesUnavailable(
        packageNames: Array<String>,
        user: UserHandle,
        replacing: Boolean
    ) {
        if (!replacing) {
            makePackageUnavailable(
                MakePackageUnavailable.Params(user, *packageNames)
            )
        }
    }

    override fun onPackagesSuspended(packageNames: Array<String>, user: UserHandle) {
        suspendPackages(
            SuspendPackages.Params(user, *packageNames)
        )
    }

    override fun onPackagesUnsuspended(packageNames: Array<String>, user: UserHandle) {
        unsuspendPackages(
            UnsuspendPackages.Params(user, *packageNames)
        )
    }

    override fun onShortcutsChanged(
        packageName: String,
        shortcuts: List<ShortcutInfoCompat>,
        user: UserHandle
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}