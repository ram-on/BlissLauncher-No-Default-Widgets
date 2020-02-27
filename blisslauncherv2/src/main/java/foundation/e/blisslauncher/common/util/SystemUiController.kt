package foundation.e.blisslauncher.common.util

import android.view.View
import android.view.Window
import foundation.e.blisslauncher.common.Utilities
import javax.inject.Inject

class SystemUiController @Inject constructor(private val window: Window) {
    private val states = IntArray(5)

    fun updateUiState(uiState: Int, isLight: Boolean) {
        updateUiState(
            uiState,
            if (isLight) FLAG_LIGHT_NAV or FLAG_LIGHT_STATUS else FLAG_DARK_NAV or FLAG_DARK_STATUS
        )
    }

    fun updateUiState(uiState: Int, flags: Int) {
        if (states[uiState] == flags) {
            return
        }
        states[uiState] = flags
        val oldFlags = window.decorView.systemUiVisibility
        // Apply the state flags in priority order
        var newFlags = oldFlags
        for (stateFlag in states) {
            if (Utilities.ATLEAST_OREO) {
                if (stateFlag and FLAG_LIGHT_NAV != 0) {
                    newFlags = newFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else if (stateFlag and FLAG_DARK_NAV != 0) {
                    newFlags =
                        newFlags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
            if (stateFlag and FLAG_LIGHT_STATUS != 0) {
                newFlags = newFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else if (stateFlag and FLAG_DARK_STATUS != 0) {
                newFlags = newFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
        if (newFlags != oldFlags) {
            window.decorView.systemUiVisibility = newFlags
        }
    }

    override fun toString(): String {
        return "states=${states.contentToString()}"
    }

    companion object {
        // Various UI states in increasing order of priority
        const val UI_STATE_BASE_WINDOW = 0
        const val UI_STATE_ALL_APPS = 1
        const val UI_STATE_WIDGET_BOTTOM_SHEET = 2
        const val UI_STATE_ROOT_VIEW = 3
        const val UI_STATE_OVERVIEW = 4

        const val FLAG_LIGHT_NAV = 1 shl 0
        const val FLAG_DARK_NAV = 1 shl 1
        const val FLAG_LIGHT_STATUS = 1 shl 2
        const val FLAG_DARK_STATUS = 1 shl 3

    }
}