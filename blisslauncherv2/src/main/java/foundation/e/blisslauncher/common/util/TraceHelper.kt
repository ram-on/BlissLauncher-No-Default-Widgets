package foundation.e.blisslauncher.common.util

import android.os.SystemClock
import android.os.Trace
import android.util.ArrayMap
import android.util.Log
import android.util.Log.VERBOSE

class TraceHelper {
    companion object {
        private const val SYSTEM_TRACE = false
        private val upTimes = ArrayMap<String, Long>()

        fun beginSection(sectionName: String) {
            var time = upTimes[sectionName]
            if (time == null) {
                time = if (Log.isLoggable(sectionName, VERBOSE)) 0 else -1
                upTimes.put(sectionName, time)
            }

            if (time >= 0) {
                if (SYSTEM_TRACE) {
                    Trace.beginSection(sectionName)
                }
                time = SystemClock.uptimeMillis()
            }
        }

        fun partitionSection(sectionName: String, partition: String) {
            var time = upTimes[sectionName]
            if (time != null && time >= 0) {
                if (SYSTEM_TRACE) {
                    Trace.endSection()
                    Trace.beginSection(sectionName)
                }

                val now = SystemClock.uptimeMillis()
                Log.d(sectionName, "${partition} : ${now - time}")
                time = now
            }
        }

        fun endSection(sectionName: String) {
            endSection(sectionName, "End")
        }

        fun endSection(sectionName: String, msg: String) {
            val time = upTimes[sectionName]
            if (time != null && time >= 0) {
                if (SYSTEM_TRACE) {
                    Trace.endSection()
                }
                Log.d(
                    sectionName,
                    "${msg} : ${(SystemClock.uptimeMillis() - time)}"
                )
            }
        }
    }
}