package foundation.e.blisslauncher.data.util

import android.os.Process
import foundation.e.blisslauncher.common.util.LabelComparator
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import javax.inject.Inject

class LauncherItemComparator
@Inject constructor(
    private val userManager: UserManagerRepository
) :
    Comparator<LauncherItem> {

    private val myUser = Process.myUserHandle()
    private val labelComparator = LabelComparator()

    override fun compare(itemA: LauncherItem, itemB: LauncherItem): Int {
        // Order by the title in the current locale
        var result: Int = labelComparator.compare(itemA.title.toString(), itemB.title.toString())
        if (result != 0) {
            return result
        }

        return if (myUser == itemA.user) {
            -1
        } else {
            val aUserSerial: Long = userManager.getSerialNumberForUser(itemA.user)
            val bUserSerial: Long = userManager.getSerialNumberForUser(itemB.user)
            aUserSerial.compareTo(bUserSerial)
        }
    }
}