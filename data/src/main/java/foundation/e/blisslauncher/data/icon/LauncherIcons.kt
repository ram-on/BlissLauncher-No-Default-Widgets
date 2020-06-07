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
package foundation.e.blisslauncher.data.icon

import android.content.Context
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.os.Process
import android.os.UserHandle
import foundation.e.blisslauncher.common.BitmapRenderer
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import foundation.e.blisslauncher.data.R
import foundation.e.blisslauncher.data.shortcuts.PinnedShortcutManager
import foundation.e.blisslauncher.domain.entity.ApplicationItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.entity.PackageItem
import javax.inject.Inject

/**
 * Helper methods for generating various launcher icons
 */
class LauncherIcons @Inject constructor(
    context: Context,
    idp: InvariantDeviceProfile,
    val pinnedShortcutManager: PinnedShortcutManager
) : AutoCloseable {

    override fun close() {
    }

    private val mOldBounds = Rect()
    private val mContext: Context = context.applicationContext
    private val mCanvas: Canvas
    private val mPm: PackageManager
    private val mFillResIconDpi: Int
    private val mIconBitmapSize: Int
    private var mWrapperIcon: Drawable? = null
    private var mWrapperBackgroundColor = DEFAULT_WRAPPER_BACKGROUND

    // sometimes we store linked lists of these things
    private var next: LauncherIcons? = null

    /**
     * Returns a bitmap suitable for the all apps view. If the package or the resource do not
     * exist, it returns null.
     */
    fun createIconBitmap(iconRes: ShortcutIconResource): Bitmap? {
        try {
            val resources =
                mPm.getResourcesForApplication(iconRes.packageName)
            if (resources != null) {
                val id = resources.getIdentifier(iconRes.resourceName, null, null)
                // do not stamp old legacy shortcuts as the app may have already forgotten about it
                return createBadgedIconBitmap(
                    resources.getDrawableForDensity(id, mFillResIconDpi),
                    Process.myUserHandle() /* only available on primary user */,
                    0 /* do not apply legacy treatment */
                )
            }
        } catch (e: Exception) {
            // Icon not found.
        }
        return null
    }

    /**
     * Returns a bitmap which is of the appropriate size to be displayed as an icon
     */
    fun createIconBitmap(icon: Bitmap): Bitmap {
        return if (mIconBitmapSize == icon.width && mIconBitmapSize == icon.height) {
            icon
        } else createIconBitmap(BitmapDrawable(mContext.resources, icon))
    }

    /**
     * Returns a bitmap suitable for displaying as an icon at various launcher UIs like all apps
     * view or workspace. The icon is badged for {@param user}.
     * The bitmap is also visually normalized with other icons.
     */
    @JvmOverloads
    fun createBadgedIconBitmap(
        icon: Drawable?, user: UserHandle?, iconAppTargetSdk: Int
    ): Bitmap {
        var icon = icon
        icon = normalizeAndWrapToAdaptiveIcon(icon!!, iconAppTargetSdk, null)
        val bitmap = createIconBitmap(icon!!)
        if (Utilities.ATLEAST_OREO && icon is AdaptiveIconDrawable) {
            mCanvas.setBitmap(bitmap)
            mCanvas.setBitmap(null)
        }
        val result: Bitmap
        result = if (user != null && Process.myUserHandle() != user) {
            val drawable: BitmapDrawable = FixedSizeBitmapDrawable(bitmap)
            val badged = mPm.getUserBadgedIcon(drawable, user)
            if (badged is BitmapDrawable) {
                badged.bitmap
            } else {
                createIconBitmap(badged)
            }
        } else {
            bitmap
        }
        return result
    }

    /**
     * Creates a normalized bitmap suitable for the all apps view. The bitmap is also visually
     * normalized with other icons and has enough spacing to add shadow.
     */
    fun createBitmapWithoutShadow(
        icon: Drawable?,
        iconAppTargetSdk: Int
    ): Bitmap {
        var icon = icon
        val iconBounds = RectF()
        icon = normalizeAndWrapToAdaptiveIcon(icon!!, iconAppTargetSdk, iconBounds)
        return createIconBitmap(icon)
    }

    /**
     * Sets the background color used for wrapped adaptive icon
     */
    fun setWrapperBackgroundColor(color: Int) {
        mWrapperBackgroundColor =
            if (Color.alpha(color) < 255) DEFAULT_WRAPPER_BACKGROUND else color
    }

    private fun normalizeAndWrapToAdaptiveIcon(
        icon: Drawable, iconAppTargetSdk: Int,
        outIconBounds: RectF?
    ): Drawable {
        // Ignore icon processing as of now.
        return icon
    }

    /**
     * Adds the {@param badge} on top of {@param target} using the badge dimensions.
     */
    fun badgeWithDrawable(target: Bitmap?, badge: Drawable) {
        mCanvas.setBitmap(target)
        badgeWithDrawable(mCanvas, badge)
        mCanvas.setBitmap(null)
    }

    /**
     * Adds the {@param badge} on top of {@param target} using the badge dimensions.
     */
    private fun badgeWithDrawable(target: Canvas, badge: Drawable) {
        val badgeSize = mContext.resources
            .getDimensionPixelSize(R.dimen.profile_badge_size)
        badge.setBounds(
            mIconBitmapSize - badgeSize, mIconBitmapSize - badgeSize,
            mIconBitmapSize, mIconBitmapSize
        )
        badge.draw(target)
    }

    private fun createIconBitmap(icon: Drawable): Bitmap {
        var width = mIconBitmapSize
        var height = mIconBitmapSize
        if (icon is PaintDrawable) {
            val painter = icon
            painter.intrinsicWidth = width
            painter.intrinsicHeight = height
        } else if (icon is BitmapDrawable) {
            // Ensure the bitmap has a density.
            val bitmapDrawable = icon
            val bitmap = bitmapDrawable.bitmap
            if (bitmap != null && bitmap.density == Bitmap.DENSITY_NONE) {
                bitmapDrawable.setTargetDensity(mContext.resources.displayMetrics)
            }
        }
        val sourceWidth = icon!!.intrinsicWidth
        val sourceHeight = icon.intrinsicHeight
        if (sourceWidth > 0 && sourceHeight > 0) {
            // Scale the icon proportionally to the icon dimensions
            val ratio = sourceWidth.toFloat() / sourceHeight
            if (sourceWidth > sourceHeight) {
                height = (width / ratio).toInt()
            } else if (sourceHeight > sourceWidth) {
                width = (height * ratio).toInt()
            }
        }
        // no intrinsic size --> use default size
        val textureWidth = mIconBitmapSize
        val textureHeight = mIconBitmapSize
        val bitmap = Bitmap.createBitmap(
            textureWidth, textureHeight,
            Bitmap.Config.ARGB_8888
        )
        mCanvas.setBitmap(bitmap)
        val left = (textureWidth - width) / 2
        val top = (textureHeight - height) / 2
        mOldBounds.set(icon.bounds)
        icon.setBounds(left, top, left + width, top + height)
        mCanvas.save()
        icon.draw(mCanvas)
        mCanvas.restore()
        icon.bounds = mOldBounds
        mCanvas.setBitmap(null)
        return bitmap
    }

    @JvmOverloads
    fun createShortcutIcon(
        shortcutInfo: ShortcutInfoCompat,
        badged: Boolean = true
    ): Bitmap {
        val unbadgedDrawable: Drawable? =
            pinnedShortcutManager.getShortcutIconDrawable(shortcutInfo, mFillResIconDpi)
        val unbadgedBitmap: Bitmap
        unbadgedBitmap = createBitmapWithoutShadow(unbadgedDrawable, 0)
        if (!badged) {
            return unbadgedBitmap
        }
        val badge = getShortcutInfoBadge(shortcutInfo)
        return BitmapRenderer.createHardwareBitmap(
            mIconBitmapSize,
            mIconBitmapSize,
            object : BitmapRenderer.Renderer {
                override fun draw(out: Canvas) {
                    badgeWithDrawable(out, BitmapDrawable(badge.iconBitmap))
                }
            })
    }

    private fun getShortcutInfoBadge(
        shortcutInfo: ShortcutInfoCompat
    ): LauncherItemWithIcon {
        val cn = shortcutInfo.getActivity()
        val badgePkg = shortcutInfo.getBadgePackage(mContext)
        val hasBadgePkgSet = badgePkg != shortcutInfo.getPackage()
        return if (cn != null && !hasBadgePkgSet) {
            // Get the app info for the source activity.
            val appItem = ApplicationItem()
            appItem.user = shortcutInfo.getUserHandle()
            appItem.componentName = cn
            appItem.intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)

            appItem
        } else {
            val pkgInfo = PackageItem(badgePkg)
            pkgInfo
        }
    }

    /**
     * An extension of [BitmapDrawable] which returns the bitmap pixel size as intrinsic size.
     * This allows the badging to be done based on the action bitmap size rather than
     * the scaled bitmap size.
     */
    private class FixedSizeBitmapDrawable(bitmap: Bitmap) :
        BitmapDrawable(null, bitmap) {
        override fun getIntrinsicHeight(): Int {
            return bitmap.width
        }

        override fun getIntrinsicWidth(): Int {
            return bitmap.width
        }
    }

    companion object {
        private const val DEFAULT_WRAPPER_BACKGROUND = Color.WHITE
    }

    init {
        mPm = mContext.packageManager
        mFillResIconDpi = idp.fillResIconDpi
        mIconBitmapSize = idp.iconBitmapSize
        mCanvas = Canvas()
        mCanvas.drawFilter = PaintFlagsDrawFilter(
            Paint.DITHER_FLAG,
            Paint.FILTER_BITMAP_FLAG
        )
    }
}