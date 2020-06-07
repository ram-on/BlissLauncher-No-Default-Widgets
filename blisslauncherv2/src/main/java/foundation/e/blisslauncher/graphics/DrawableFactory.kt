/*
 * Copyright (C) 2016 The Android Open Source Project
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
package foundation.e.blisslauncher.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.ArrayMap
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating new drawables.
 */
@Singleton
open class DrawableFactory @Inject constructor(context: Context){
    private val myUser: UserHandle = Process.myUserHandle()
    private val userBadges = ArrayMap<UserHandle, Bitmap?>()

    /**
     * Returns a FastBitmapDrawable with the icon.
     */
    fun newIcon(info: LauncherItemWithIcon): FastBitmapDrawable {
        val drawable = FastBitmapDrawable(info)
        drawable.setIsDisabled(info.isDisabled())
        return drawable
    }

    /**
     * Returns a drawable that can be used as a badge for the user or null.
     */
    fun getBadgeForUser(user: UserHandle, context: Context): Drawable? {
        if (myUser == user) {
            return null
        }
        val badgeBitmap = getUserBadge(user, context)
        val d = FastBitmapDrawable(badgeBitmap)
        d.isFilterBitmap = true
        d.setBounds(0, 0, badgeBitmap!!.width, badgeBitmap.height)
        return d
    }

    @Synchronized
    fun getUserBadge(
        user: UserHandle,
        context: Context
    ): Bitmap? {
        var badgeBitmap = userBadges[user]
        if (badgeBitmap != null) {
            return badgeBitmap
        }
        val res = context.applicationContext.resources
        val badgeSize =
            res.getDimensionPixelSize(R.dimen.profile_badge_size)
        badgeBitmap = Bitmap.createBitmap(
            badgeSize,
            badgeSize,
            Bitmap.Config.ARGB_8888
        )
        val drawable = context.packageManager.getUserBadgedDrawableForDensity(
            BitmapDrawable(res, badgeBitmap),
            user,
            Rect(0, 0, badgeSize, badgeSize),
            0
        )
        if (drawable is BitmapDrawable) {
            badgeBitmap = drawable.bitmap
        } else {
            badgeBitmap.eraseColor(Color.TRANSPARENT)
            val c = Canvas(badgeBitmap)
            drawable.setBounds(0, 0, badgeSize, badgeSize)
            drawable.draw(c)
            c.setBitmap(null)
        }
        userBadges[user] = badgeBitmap
        return badgeBitmap
    }
}