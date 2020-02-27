package foundation.e.blisslauncher.data.compat

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionInfo
import android.os.Process
import android.util.SparseArray
import androidx.annotation.NonNull

class PackageInstallerCompat(private val context: Context) {

    val activeSessions = SparseArray<String>()
    val installer = context.packageManager.packageInstaller
    val appContext = context.applicationContext
    val sessionVerifiedMap = HashMap<String, Boolean>()

    fun updateAndGetActiveSessionCache(): HashMap<String, PackageInstaller.SessionInfo> {
        val activePackages = HashMap<String, SessionInfo>()
        val user = Process.myUserHandle()
        getAllVerifiedSessions().forEach {
            if (it.appPackageName != null) {
                //TODO: Cache to IconCache
                activePackages[it.appPackageName] = it
                activeSessions.run { put(it.sessionId, it.appPackageName) }
            }
        }
        return activePackages
    }

    fun onStop() {
    }

    fun getAllVerifiedSessions(): List<PackageInstaller.SessionInfo> {
        val list = ArrayList<SessionInfo>(installer.allSessions)
        val iterator = list.iterator()
        iterator.forEachRemaining {
            if (verify(it) == null) {
                iterator.remove()
            }
        }

        return list
    }

    private fun verify(sessionInfo: SessionInfo?): SessionInfo? {
        if (sessionInfo == null || sessionInfo.installerPackageName == null ||
            sessionInfo.appPackageName.isNullOrEmpty()
        ) {
            return null
        }
        val pkg = sessionInfo.installerPackageName
        synchronized(sessionVerifiedMap) {
            if (!sessionVerifiedMap.containsKey(pkg)) {
                val launcherApps = LauncherAppsCompatVO(appContext)
                val hasSystemFlag = launcherApps.getApplicationInfo(
                    pkg,
                    ApplicationInfo.FLAG_SYSTEM, Process.myUserHandle()
                ) != null
                sessionVerifiedMap[pkg] = hasSystemFlag
            }
        }
        return if (sessionVerifiedMap[pkg] == true) sessionInfo else null
    }

    companion object {
        const val STATUS_INSTALLED = 0
        const val STATUS_INSTALLING = 1
        const val STATUS_FAILED = 2
    }

    class PackageInstallInfo {
        val componentName: ComponentName
        val packageName: String
        val state: Int
        val progress: Int

        private constructor(@NonNull info: SessionInfo) {
            state = STATUS_INSTALLING
            packageName = info.appPackageName
            componentName = ComponentName(packageName, "")
            progress = (info.progress * 100f).toInt()
        }

        constructor(packageName: String, state: Int, progress: Int) {
            this.state = state
            this.packageName = packageName
            componentName = ComponentName(packageName, "")
            this.progress = progress
        }

        companion object {
            fun fromInstallingState(info: SessionInfo): PackageInstallInfo {
                return PackageInstallInfo(info)
            }

            fun fromState(state: Int, packageName: String): PackageInstallInfo {
                return PackageInstallInfo(packageName, state, 0 /* progress */)
            }
        }
    }
}