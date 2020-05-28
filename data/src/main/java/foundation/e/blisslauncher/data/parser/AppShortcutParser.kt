package foundation.e.blisslauncher.data.parser

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import timber.log.Timber

open class AppShortcutParser(context: Context) : TagParser {

    val packageManager = context.packageManager

    override fun parseAndAdd(parser: XmlResourceParser): ParseResult {
        val packageName: String? = DefaultHotseatParser.getAttributeValue(
            parser,
            DefaultHotseatParser.ATTR_PACKAGE_NAME
        )
        val className: String? = DefaultHotseatParser.getAttributeValue(
            parser,
            DefaultHotseatParser.ATTR_CLASS_NAME
        )

        return if (!packageName.isNullOrEmpty() && !className.isNullOrEmpty()) {
            var info: ActivityInfo
            try {
                var cn: ComponentName?
                try {
                    cn = ComponentName(packageName, className)
                    info = packageManager.getActivityInfo(cn, 0)
                } catch (nnfe: PackageManager.NameNotFoundException) {
                    val packages: Array<String> =
                        packageManager.currentToCanonicalPackageNames(
                            arrayOf(packageName)
                        )
                    cn = ComponentName(packages[0], className)
                    info = packageManager.getActivityInfo(cn, 0)
                }
                val intent =
                    Intent(Intent.ACTION_MAIN, null)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(cn)
                        .setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        )
                return ParseResult(
                    1, Triple(
                        info.loadLabel(packageManager).toString(),
                        intent, LauncherConstants.ItemType.APPLICATION
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e("Favorite not found: $packageName/$className")
                return ParseResult(-1)
            }
        } else {
            return invalidPackageOrClass(parser)
        }
    }

    /**
     * Helper method to allow extending the parser capabilities
     */
    protected open fun invalidPackageOrClass(parser: XmlResourceParser): ParseResult {
        Timber.w("Skipping invalid <favorite> with no component")
        return ParseResult(-1)
    }
}