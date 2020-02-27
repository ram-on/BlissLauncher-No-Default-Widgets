package foundation.e.blisslauncher.domain.keys

import android.content.ComponentName
import android.os.UserHandle

open class ComponentKey(val componentName: ComponentName, val user: UserHandle) {
    private val hashCode = arrayOf(componentName, user).contentHashCode()

    override fun hashCode(): Int = hashCode

    override fun equals(any: Any?): Boolean {
        val other = any as ComponentKey
        return (other.componentName == componentName) and (other.user == user)
    }

    override fun toString(): String {
        return "${componentName.flattenToString()}#$user"
    }
}