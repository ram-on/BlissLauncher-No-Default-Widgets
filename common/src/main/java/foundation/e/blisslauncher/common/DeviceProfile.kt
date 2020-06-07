/*
 * Copyright (C) 2008 The Android Open Source Project
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
package foundation.e.blisslauncher.common

import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log

class DeviceProfile(
    context: Context,
    inv: InvariantDeviceProfile,
    minSize: Point,
    maxSize: Point,
    width: Int,
    height: Int
) {
    val inv: InvariantDeviceProfile

    // Device properties
    val isTablet: Boolean
    val isLargeTablet: Boolean
    val isPhone: Boolean
    val transposeLayoutWithOrientation: Boolean

    val widthPx: Int
    val heightPx: Int
    var availableWidthPx = 0
    var availableHeightPx = 0

    // Workspace
    val desiredWorkspaceLeftRightMarginPx: Int
    val cellLayoutPaddingLeftRightPx: Int
    val cellLayoutBottomPaddingPx: Int
    val edgeMarginPx: Int
    val defaultWidgetPadding: Rect
    val defaultPageSpacingPx: Int
    private val topWorkspacePadding: Int

    // Workspace icons
    var iconSizePx = 0
    var iconTextSizePx = 0
    var iconDrawablePaddingPx = 0
    var iconDrawablePaddingOriginalPx: Int
    var cellWidthPx = 0
    var cellHeightPx = 0
    var workspaceCellPaddingXPx: Int

    // Folder
    var folderIconSizePx = 0
    var folderIconOffsetYPx = 0

    // Folder cell
    var folderCellWidthPx = 0
    var folderCellHeightPx = 0

    // Folder child
    var folderChildIconSizePx = 0
    var folderChildTextSizePx = 0
    var folderChildDrawablePaddingPx = 0

    // Hotseat
    var hotseatCellHeightPx = 0

    // In portrait: size = height, in landscape: size = width
    var hotseatBarSizePx: Int
    val hotseatBarTopPaddingPx: Int
    val hotseatBarBottomPaddingPx: Int
    val hotseatBarSidePaddingPx: Int

    // Widgets
    val appWidgetScale = PointF(1.0f, 1.0f)

    // Drop Target
    var dropTargetBarSizePx: Int

    private val TAG = "DeviceProfile"

    // Insets
    val insets = Rect()
    val workspacePadding = Rect()
    private val mHotseatPadding = Rect()
    private var mIsSeascape = false

    fun copy(context: Context): DeviceProfile {
        val size =
            Point(availableWidthPx, availableHeightPx)
        return DeviceProfile(
            context, inv, size, size, widthPx, heightPx
        )
    }

    /**
     * Inverse of [.getMultiWindowProfile]
     * @return device profile corresponding to the current orientation in non multi-window mode.
     */
    var fullScreenProfile: DeviceProfile? = null
        get() = inv.portraitProfile

    private fun updateAvailableDimensions(
        dm: DisplayMetrics,
        res: Resources
    ) {
        updateIconSize(1f, res, dm)
        // Check to see if the icons fit within the available height.  If not, then scale down.
        val usedHeight = (cellHeightPx * inv.numRows).toFloat()
        val maxHeight = availableHeightPx - totalWorkspacePadding.y
        if (usedHeight > maxHeight) {
            val scale = maxHeight / usedHeight
            updateIconSize(scale, res, dm)
        }
        updateAvailableFolderCellDimensions(dm, res)
    }

    private fun updateIconSize(
        scale: Float,
        res: Resources,
        dm: DisplayMetrics
    ) {
        val invIconSizePx = inv.iconSize
        iconSizePx =
            (Utilities.pxFromDp(
                invIconSizePx,
                dm
            ) * scale).toInt()
        iconTextSizePx = (Utilities.pxFromSp(
            inv.iconTextSize,
            dm
        ) * scale).toInt()
        iconDrawablePaddingPx =
            (availableWidthPx - iconSizePx * inv.numColumns) / (inv.numColumns + 1)
        cellHeightPx = (iconSizePx + iconDrawablePaddingPx
            + Utilities.calculateTextHeight(iconTextSizePx.toFloat()))

        val cellYPadding = (cellSize.y - cellHeightPx) / 2
        if (iconDrawablePaddingPx > cellYPadding) {
            cellHeightPx -= iconDrawablePaddingPx - cellYPadding
            iconDrawablePaddingPx = cellYPadding
        }
        cellWidthPx = iconSizePx + iconDrawablePaddingPx
        hotseatCellHeightPx = iconSizePx

        // Folder icon
        folderIconSizePx = iconSizePx
        folderIconOffsetYPx = (iconSizePx - folderIconSizePx) / 2
    }

    private fun updateAvailableFolderCellDimensions(
        dm: DisplayMetrics,
        res: Resources
    ) {
        val folderBottomPanelSize =
            (res.getDimensionPixelSize(R.dimen.folder_label_padding_top) +
                res.getDimensionPixelSize(R.dimen.folder_label_padding_bottom) +
                Utilities.calculateTextHeight(
                    res.getDimension(
                        R.dimen.folder_label_text_size
                    )
                ))
        updateFolderCellSize(1f, dm, res)
        // Don't let the folder get too close to the edges of the screen.
        val folderMargin = edgeMarginPx
        val totalWorkspacePadding = totalWorkspacePadding
        // Check if the icons fit within the available height.
        val usedHeight =
            folderCellHeightPx * inv.numFolderRows + folderBottomPanelSize.toFloat()
        val maxHeight = availableHeightPx - totalWorkspacePadding.y - folderMargin
        val scaleY = maxHeight / usedHeight
        // Check if the icons fit within the available width.
        val usedWidth = folderCellWidthPx * inv.numFolderColumns.toFloat()
        val maxWidth = availableWidthPx - totalWorkspacePadding.x - folderMargin
        val scaleX = maxWidth / usedWidth
        val scale = Math.min(scaleX, scaleY)
        if (scale < 1f) {
            updateFolderCellSize(scale, dm, res)
        }
    }

    private fun updateFolderCellSize(
        scale: Float,
        dm: DisplayMetrics,
        res: Resources
    ) {
        folderChildIconSizePx =
            (Utilities.pxFromDp(
                inv.iconSize,
                dm
            ) * scale).toInt()
        folderChildTextSizePx =
            (res.getDimensionPixelSize(R.dimen.folder_child_text_size) * scale).toInt()
        val textHeight =
            Utilities.calculateTextHeight(
                folderChildTextSizePx.toFloat()
            )
        val cellPaddingX =
            (res.getDimensionPixelSize(R.dimen.folder_cell_x_padding) * scale).toInt()
        val cellPaddingY =
            (res.getDimensionPixelSize(R.dimen.folder_cell_y_padding) * scale).toInt()
        folderCellWidthPx = folderChildIconSizePx + 2 * cellPaddingX
        folderCellHeightPx = folderChildIconSizePx + 2 * cellPaddingY + textHeight
        folderChildDrawablePaddingPx = Math.max(
            0,
            (folderCellHeightPx - folderChildIconSizePx - textHeight) / 3
        )
    }

    fun updateInsets(insets: Rect?) {
        insets!!.set(insets)
        updateWorkspacePadding()
    }

    // Since we are only concerned with the overall padding, layout direction does not matter.
    val cellSize: Point
        get() {
            val result = Point()
            val padding = totalWorkspacePadding
            result.x =
                calculateCellWidth(
                    availableWidthPx - padding.x -
                        cellLayoutPaddingLeftRightPx * 2, inv.numColumns
                )
            result.y =
                calculateCellHeight(
                    availableHeightPx - padding.y -
                        cellLayoutBottomPaddingPx, inv.numRows
                )
            return result
        }

    val totalWorkspacePadding: Point
        get() {
            updateWorkspacePadding()
            return Point(
                workspacePadding.left + workspacePadding.right,
                workspacePadding.top + workspacePadding.bottom
            )
        }

    /**
     * Updates [.workspacePadding] as a result of any internal value change to reflect the
     * new workspace padding
     */
    private fun updateWorkspacePadding() {
        val padding = workspacePadding
        val paddingBottom = hotseatBarSizePx
        if (isTablet) { // Pad the left and right of the workspace to ensure consistent spacing
            // between all icons
            // The amount of screen space available for left/right padding.
            var availablePaddingX = Math.max(
                0, widthPx - (inv.numColumns * cellWidthPx +
                    (inv.numColumns - 1) * cellWidthPx)
            )
            availablePaddingX = Math.min(
                availablePaddingX.toFloat(),
                widthPx * MAX_HORIZONTAL_PADDING_PERCENT
            ).toInt()
            val availablePaddingY = Math.max(
                0, heightPx - topWorkspacePadding - paddingBottom -
                    2 * inv.numRows * cellHeightPx - hotseatBarTopPaddingPx -
                    hotseatBarBottomPaddingPx
            )
            padding[availablePaddingX / 2, topWorkspacePadding + availablePaddingY / 2, availablePaddingX / 2] =
                paddingBottom + availablePaddingY / 2
        } else { // Pad the top and bottom of the workspace with search/hotseat bar sizes
            padding[desiredWorkspaceLeftRightMarginPx, topWorkspacePadding, desiredWorkspaceLeftRightMarginPx] =
                paddingBottom
        }
    }

    val hotseatLayoutPadding: Rect
        get() {
            // We want the edges of the hotseat to line up with the edges of the workspace, but the
            // icons in the hotseat are a different size, and so don't line up perfectly. To account
            // for this, we pad the left and right of the hotseat with half of the difference of a
            // workspace cell vs a hotseat cell.
            val workspaceCellWidth = widthPx.toFloat() / inv.numColumns
            val hotseatCellWidth = widthPx.toFloat() / inv.numHotseatIcons
            val hotseatAdjustment =
                Math.round((workspaceCellWidth - hotseatCellWidth) / 2)
            mHotseatPadding[hotseatAdjustment + workspacePadding.left + cellLayoutPaddingLeftRightPx, hotseatBarTopPaddingPx, hotseatAdjustment + workspacePadding.right + cellLayoutPaddingLeftRightPx] =
                hotseatBarBottomPaddingPx + insets.bottom + cellLayoutBottomPaddingPx
            return mHotseatPadding
        } // Folders should only appear below the drop target bar and above the hotseat// Folders should only appear right of the drop target bar and left of the hotseat

    /**
     * @return the bounds for which the open folders should be contained within
     */
    val absoluteOpenFolderBounds: Rect
        get() = Rect(
            insets.left + edgeMarginPx,
            insets.top + dropTargetBarSizePx + edgeMarginPx,
            insets.left + availableWidthPx - edgeMarginPx,
            insets.top + availableHeightPx - hotseatBarSizePx - -edgeMarginPx
        )

    fun getCellHeight(containerType: Long): Int {
        return when (containerType) {
            LauncherConstants.ContainerType.CONTAINER_DESKTOP -> cellHeightPx
            LauncherConstants.ContainerType.CONTAINER_HOTSEAT -> hotseatCellHeightPx
            else -> 0
        }
    }

    /**
     * Callback when a component changes the DeviceProfile associated with it, as a result of
     * configuration change
     */
    interface OnDeviceProfileChangeListener {
        /**
         * Called when the device profile is reassigned. Note that for layout and measurements, it
         * is sufficient to listen for inset changes. Use this callback when you need to perform
         * a one time operation.
         */
        fun onDeviceProfileChanged(dp: DeviceProfile?)
    }

    companion object {
        /**
         * The maximum amount of left/right workspace padding as a percentage of the screen width.
         * To be clear, this means that up to 7% of the screen width can be used as left padding, and
         * 7% of the screen width can be used as right padding.
         */
        private const val MAX_HORIZONTAL_PADDING_PERCENT = 0.14f
        private const val TALL_DEVICE_ASPECT_RATIO_THRESHOLD = 2.0f

        fun calculateCellWidth(width: Int, countX: Int): Int {
            return width / countX
        }

        fun calculateCellHeight(height: Int, countY: Int): Int {
            return height / countY
        }

        private fun getContext(
            c: Context,
            orientation: Int
        ): Context {
            val context =
                Configuration(c.resources.configuration)
            context.orientation = orientation
            return c.createConfigurationContext(context)
        }
    }

    init {
        var context = context
        this.inv = inv
        var res = context.resources
        val dm = res.displayMetrics
        // Constants from resources
        isTablet = res.getBoolean(
            R.bool.is_tablet
        )
        isLargeTablet = res.getBoolean(R.bool.is_large_tablet)
        isPhone = !isTablet && !isLargeTablet
        // Some more constants
        transposeLayoutWithOrientation =
            res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation)
        context =
            getContext(
                context, Configuration.ORIENTATION_PORTRAIT
            )
        res = context.resources
        val cn = ComponentName(
            context.packageName,
            this.javaClass.name
        )
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null)
        edgeMarginPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin)
        desiredWorkspaceLeftRightMarginPx = edgeMarginPx
        cellLayoutPaddingLeftRightPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding)
        cellLayoutBottomPaddingPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_bottom_padding)
        defaultPageSpacingPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing)
        topWorkspacePadding =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_top_padding)
        iconDrawablePaddingOriginalPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding)
        dropTargetBarSizePx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_drop_target_size)
        workspaceCellPaddingXPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_padding_x)
        hotseatBarTopPaddingPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_top_padding)
        hotseatBarBottomPaddingPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_bottom_padding)
        hotseatBarSidePaddingPx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_side_padding)
        hotseatBarSizePx =
            res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_size) + hotseatBarTopPaddingPx + hotseatBarBottomPaddingPx
        // Determine sizes.
        widthPx = width
        heightPx = height
        availableWidthPx = minSize.x
        availableHeightPx = maxSize.y
        // Calculate all of the remaining variables.
        updateAvailableDimensions(dm, res)
        // Now that we have all of the variables calculated, we can tune certain sizes.
        val aspectRatio =
            Math.max(widthPx, heightPx).toFloat() / Math.min(
                widthPx,
                heightPx
            )
        val isTallDevice = aspectRatio.compareTo(TALL_DEVICE_ASPECT_RATIO_THRESHOLD) >= 0
        if (isPhone && isTallDevice) {
            // We increase the hotseat size when there is extra space.
            // ie. For a display with a large aspect ratio, we can keep the icons on the workspace
            // in portrait mode closer together by adding more height to the hotseat.
            // Note: This calculation was created after noticing a pattern in the design spec.
            val extraSpace = cellSize.y - iconSizePx - iconDrawablePaddingPx
            hotseatBarSizePx += extraSpace
            // Recalculate the available dimensions using the new hotseat size.
            updateAvailableDimensions(dm, res)
        }
        updateWorkspacePadding()
        // This is done last, after iconSizePx is calculated above.
        //TODO: mBadgeRenderer = BadgeRenderer(iconSizePx)
    }
}