package foundation.e.blisslauncher.data.database

/**
 * File names of all the file BlissLauncher uses to store data.
 */
class BlissLauncherFiles {
    companion object {
        private val XML = ".xml"

        const val LAUNCHER_DB = "launcher.db"
        const val SHARED_PREFERENCES_KEY = "foundation.e.blisslauncher.prefs"
        const val DEVICE_PREFERENCES_KEY = "foundation.e.blisslauncher.device.prefs"

        const val WIDGET_PREVIEWS_DB = "widgetpreviews.db"
        const val APP_ICONS_DB = "app_icons.db"

        val ALL_FILES = listOf(
            LAUNCHER_DB,
            SHARED_PREFERENCES_KEY + XML,
            WIDGET_PREVIEWS_DB,
            DEVICE_PREFERENCES_KEY + XML,
            APP_ICONS_DB
        )
    }
}