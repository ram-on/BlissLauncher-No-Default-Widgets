package foundation.e.blisslauncher.data.parser

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.XmlResourceParser
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import timber.log.Timber
import java.net.URISyntaxException

class AppShortcutWithUriParser(context: Context) : AppShortcutParser(context) {

    override fun invalidPackageOrClass(parser: XmlResourceParser): ParseResult {
        val uri: String? =
            DefaultHotseatParser.getAttributeValue(parser, DefaultHotseatParser.ATTR_URI)
        if (uri.isNullOrEmpty()) {
            Timber.e("Skipping invalid <favorite> with no component or uri")
            return ParseResult(-1)
        }
        val metaIntent: Intent
        metaIntent = try {
            Intent.parseUri(uri, 0)
        } catch (e: URISyntaxException) {
            Timber.e("Unable to add meta-favorite: $uri")
            return ParseResult(-1)
        }
        var resolved: ResolveInfo = packageManager.resolveActivity(
            metaIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val appList: List<ResolveInfo> = packageManager.queryIntentActivities(
            metaIntent, PackageManager.MATCH_DEFAULT_ONLY
        )

        // Verify that the result is an app and not just the resolver dialog asking which
        // app to use.
        if (wouldLaunchResolverActivity(resolved, appList)) {
            // If only one of the results is a system app then choose that as the default.
            val systemApp = getSingleSystemActivity(appList)
            if (systemApp == null) {
                // There is no logical choice for this meta-favorite, so rather than making
                // a bad choice just add nothing.
                Timber.w(
                    "No preference or single system activity found for "
                        + metaIntent.toString()
                )
                return ParseResult(-1)
            }
            resolved = systemApp
        }
        val info = resolved.activityInfo
        val intent: Intent =
            packageManager.getLaunchIntentForPackage(info.packageName)
                ?: return ParseResult(-1)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        return ParseResult(
            1, Triple(
                info.loadLabel(packageManager).toString(), intent,
                LauncherConstants.ItemType.APPLICATION
            )
        )
    }

    private fun getSingleSystemActivity(appList: List<ResolveInfo>): ResolveInfo? {
        var systemResolve: ResolveInfo? = null
        val N = appList.size
        for (i in 0 until N) {
            try {
                val info: ApplicationInfo = packageManager.getApplicationInfo(
                    appList[i].activityInfo.packageName, 0
                )
                Timber.d("$info")
                if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    Timber.d("True for $info")
                    return appList[i]
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.w(e, "Unable to get info about resolve results")
                return null
            }
        }
        return systemResolve
    }

    private fun wouldLaunchResolverActivity(
        resolved: ResolveInfo,
        appList: List<ResolveInfo>
    ): Boolean {
        // If the list contains the above resolved activity, then it can't be
        // ResolverActivity itself.
        for (i in appList.indices) {
            val tmp = appList[i]
            if (tmp.activityInfo.name == resolved.activityInfo.name && tmp.activityInfo.packageName == resolved.activityInfo.packageName) {
                return false
            }
        }
        return true
    }
}