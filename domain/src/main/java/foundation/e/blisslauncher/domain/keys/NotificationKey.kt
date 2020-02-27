/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package foundation.e.blisslauncher.domain.keys

import android.service.notification.StatusBarNotification
import java.util.ArrayList

/**
 * The key data associated with the notification, used to determine what to include
 * in badges and dummy popup views before they are populated.
 *
 * @see NotificationInfo for the full data used when populating the dummy views.
 */
class NotificationKey private constructor(
    val notificationKey: String,
    val shortcutId: String,
    count: Int
) {
    var count: Int
    override fun equals(obj: Any?): Boolean {
        return if (obj !is NotificationKey) {
            false
        } else obj.notificationKey == notificationKey
        // Only compare the keys.
    }

    companion object {
        fun fromNotification(sbn: StatusBarNotification): NotificationKey {
            val notif = sbn.notification
            return NotificationKey(sbn.key, notif.shortcutId, notif.number)
        }

        fun extractKeysOnly(notificationKeys: List<NotificationKey>): List<String> {
            val keysOnly: MutableList<String> =
                ArrayList(notificationKeys.size)
            for (notificationKey in notificationKeys) {
                keysOnly.add(notificationKey.notificationKey)
            }
            return keysOnly
        }
    }

    init {
        this.count = Math.max(1, count)
    }
}