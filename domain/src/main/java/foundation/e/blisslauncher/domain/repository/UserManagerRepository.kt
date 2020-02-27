package foundation.e.blisslauncher.domain.repository

import android.os.UserHandle

interface UserManagerRepository {

    val userProfiles: List<UserHandle>
    val isDemoUser: Boolean
    val isAnyProfileQuietModeEnabled: Boolean

    /**
     * Creates a cache for users.
     */
    fun enableAndResetCache()

    fun getSerialNumberForUser(user: UserHandle): Long
    fun getUserForSerialNumber(serialNumber: Long): UserHandle?
    fun getBadgedLabelForUser(
        label: CharSequence,
        user: UserHandle?
    ): CharSequence

    fun isQuietModeEnabled(user: UserHandle): Boolean
    fun isUserUnlocked(user: UserHandle): Boolean
    fun requestQuietModeEnabled(
        enableQuietMode: Boolean,
        user: UserHandle?
    ): Boolean
}
