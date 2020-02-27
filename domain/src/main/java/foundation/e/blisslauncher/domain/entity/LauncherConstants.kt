package foundation.e.blisslauncher.domain.entity

class LauncherConstants {

    object ItemType {

        const val APPLICATION = 0
        const val SHORTCUT = 1
        const val FOLDER = 2
        const val APPWIDGET = 4
        const val CUSTOM_APPWIDGET = 5
        const val DEEP_SHORTCUT = 6

        fun itemTypeToString(type: Int): String = when (type) {
            APPLICATION -> "APP"
            SHORTCUT -> "SHORTCUT"
            FOLDER -> "FOLDER"
            APPWIDGET -> "WIDGET"
            CUSTOM_APPWIDGET -> "CUSTOMWIDGET"
            DEEP_SHORTCUT -> "DEEPSHORTCUT"
            else -> type.toString()
        }
    }

    object ContainerType {

        const val CONTAINER_DESKTOP = -100
        const val CONTAINER_HOTSEAT = -101

        fun containerToString(container: Int): String = when (container) {
            CONTAINER_DESKTOP -> "desktop"
            CONTAINER_HOTSEAT -> "hotseat"
            else -> container.toString()
        }
    }
}
