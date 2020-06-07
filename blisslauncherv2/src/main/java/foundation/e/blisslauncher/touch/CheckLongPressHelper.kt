/*
 * Copyright (C) 2012 The Android Open Source Project
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
package foundation.e.blisslauncher.touch

import android.view.View
import android.view.View.OnLongClickListener

class CheckLongPressHelper {
    var mView: View
    var mListener: OnLongClickListener? = null
    var mHasPerformedLongPress = false
    private var mLongPressTimeout =
        DEFAULT_LONG_PRESS_TIMEOUT
    private var mPendingCheckForLongPress: CheckForLongPress? =
        null

    internal inner class CheckForLongPress : Runnable {
        override fun run() {
            if (mView.parent != null && mView.hasWindowFocus()
                && !mHasPerformedLongPress
            ) {
                val handled: Boolean = if (mListener != null) {
                    mListener!!.onLongClick(mView)
                } else {
                    mView.performLongClick()
                }
                if (handled) {
                    mView.isPressed = false
                    mHasPerformedLongPress = true
                }
            }
        }
    }

    constructor(v: View) {
        mView = v
    }

    constructor(v: View, listener: OnLongClickListener?) {
        mView = v
        mListener = listener
    }

    /**
     * Overrides the default long press timeout.
     */
    fun setLongPressTimeout(longPressTimeout: Int) {
        mLongPressTimeout = longPressTimeout
    }

    fun postCheckForLongPress() {
        mHasPerformedLongPress = false
        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress =
                CheckForLongPress()
        }
        mView.postDelayed(mPendingCheckForLongPress, mLongPressTimeout.toLong())
    }

    fun cancelLongPress() {
        mHasPerformedLongPress = false
        if (mPendingCheckForLongPress != null) {
            mView.removeCallbacks(mPendingCheckForLongPress)
            mPendingCheckForLongPress = null
        }
    }

    fun hasPerformedLongPress(): Boolean {
        return mHasPerformedLongPress
    }

    companion object {
        const val DEFAULT_LONG_PRESS_TIMEOUT = 300
    }
}