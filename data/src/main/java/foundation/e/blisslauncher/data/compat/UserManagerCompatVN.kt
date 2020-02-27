package foundation.e.blisslauncher.data.compat

import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.ArrayMap
import foundation.e.blisslauncher.common.util.LongArrayMap
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import java.util.ArrayList

open class UserManagerCompatVN(context: Context) : UserManagerRepository {

    protected val userManager: UserManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val pm: PackageManager = context.packageManager

    private lateinit var users: LongArrayMap<UserHandle>
    // Create a separate reverse map as LongArrayMap.indexOfValue checks if objects are same
    // and not {@link Object#equals}
    private lateinit var userToSerialMap: ArrayMap<UserHandle, Long>

    override fun enableAndResetCache() {
        synchronized(this) {
            users = LongArrayMap()
            userToSerialMap = ArrayMap()
            val _users: List<UserHandle> = userManager.userProfiles
            _users.forEach {
                val serial = userManager.getSerialNumberForUser(it)
                users.put(serial, it)
                userToSerialMap[it] = serial
            }
        }
    }

    override val userProfiles: List<UserHandle>
        get() {
            synchronized(this) {
                if (::users.isInitialized) {
                    return ArrayList<UserHandle>(userToSerialMap.keys)
                }
            }
            return userManager.userProfiles
        }

    override fun getSerialNumberForUser(user: UserHandle): Long {
        synchronized(this) {
            if (::userToSerialMap.isInitialized) {
                return userToSerialMap[user] ?: 0
            }
        }
        return userManager.getSerialNumberForUser(user)
    }

    override fun getUserForSerialNumber(serialNumber: Long): UserHandle? {
        synchronized(this) {
            if (::users.isInitialized) {
                users[serialNumber]
            }
        }
        return userManager.getUserForSerialNumber(serialNumber)
    }

    override fun getBadgedLabelForUser(label: CharSequence, user: UserHandle?): CharSequence =
        when (user) {
            null -> label
            else -> pm.getUserBadgedLabel(label, user)
        }

    override fun isQuietModeEnabled(user: UserHandle): Boolean =
        userManager.isQuietModeEnabled(user)

    override fun isUserUnlocked(user: UserHandle): Boolean = userManager.isUserUnlocked(user)

    override val isDemoUser: Boolean
        get() = false

    override fun requestQuietModeEnabled(enableQuietMode: Boolean, user: UserHandle?): Boolean =
        false

    override val isAnyProfileQuietModeEnabled: Boolean
        get() {
            for (userProfile in userProfiles) {
                if (Process.myUserHandle().equals(userProfile)) continue

                if (isQuietModeEnabled(userProfile)) return true
            }
            return false
        }
}
