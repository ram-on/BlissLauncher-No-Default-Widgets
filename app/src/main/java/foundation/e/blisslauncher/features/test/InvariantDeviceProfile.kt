package foundation.e.blisslauncher.features.test
/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Log
import android.util.Xml
import android.view.WindowManager
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.Utilities
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.ArrayList
import kotlin.math.hypot
import kotlin.math.pow

open class InvariantDeviceProfile {
    // Profile-defining invariant properties
    var name: String? = null
    var minWidthDps = 0f
    var minHeightDps = 0f

    /**
     * Number of icons per row and column in the workspace.
     */
    var numRows = 0
    var numColumns = 0

    /**
     * Number of icons per row and column in the folder.
     */
    var numFolderRows = 0
    var numFolderColumns = 0

    var iconSize = 0f
    var landscapeIconSize = 0f
    var iconBitmapSize = 0
    var fillResIconDpi = 0
    var iconTextSize = 0f

    /**
     * Number of icons inside the hotseat area.
     */
    var numHotseatIcons = 0
    var defaultLayoutId = 0
    var demoModeLayoutId = 0
    lateinit var landscapeProfile: VariantDeviceProfile
    lateinit var portraitProfile: VariantDeviceProfile
    lateinit var defaultWallpaperSize: Point

    constructor()

    constructor(context: Context) {
        val wm =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        val smallestSize = Point()
        val largestSize = Point()
        display.getCurrentSizeRange(smallestSize, largestSize)
        // This guarantees that width < height
        minWidthDps = Utilities.dpiFromPx(
            smallestSize.x.coerceAtMost(smallestSize.y), dm
        )
        minHeightDps = Utilities.dpiFromPx(
            largestSize.x.coerceAtMost(largestSize.y), dm
        )

        Log.d("ClosestProfiles", "[$minWidthDps], [$minHeightDps]")
        val closestProfiles =
            findClosestDeviceProfiles(
                minWidthDps, minHeightDps, getPredefinedDeviceProfiles(context)
            )
        closestProfiles.forEach {
            Log.d("InvariantDeviceProfile", it.name)
        }
        val interpolatedDeviceProfileOut =
            invDistWeightedInterpolate(minWidthDps, minHeightDps, closestProfiles)
        val closestProfile = closestProfiles[0]
        Log.d(
            "InvariantDevice",
            "rows and col: ${closestProfile.numRows} * ${closestProfile.numColumns}"
        )
        numRows = closestProfile.numRows
        numColumns = closestProfile.numColumns
        numHotseatIcons = closestProfile.numHotseatIcons
        defaultLayoutId = closestProfile.defaultLayoutId
        demoModeLayoutId = closestProfile.demoModeLayoutId
        numFolderRows = closestProfile.numFolderRows
        numFolderColumns = closestProfile.numFolderColumns
        iconSize = interpolatedDeviceProfileOut.iconSize
        landscapeIconSize = interpolatedDeviceProfileOut.landscapeIconSize
        iconBitmapSize =
            Utilities.pxFromDp(iconSize, dm)
        iconTextSize = interpolatedDeviceProfileOut.iconTextSize
        fillResIconDpi = getLauncherIconDensity(iconBitmapSize)
        // If the partner customization apk contains any grid overrides, apply them
        // Supported overrides: numRows, numColumns, iconSize
        //applyPartnerDeviceProfileOverrides(context, dm);
        val realSize = Point()
        display.getRealSize(realSize)
        // The real size never changes. smallSide and largeSide will remain the
        // same in any orientation.
        val smallSide = Math.min(realSize.x, realSize.y)
        val largeSide = Math.max(realSize.x, realSize.y)
        landscapeProfile = VariantDeviceProfile(
            context, this, smallestSize, largestSize,
            largeSide, smallSide
        )
        portraitProfile = VariantDeviceProfile(
            context, this, smallestSize, largestSize,
            smallSide, largeSide
        )
        // We need to ensure that there is enough extra space in the wallpaper
        // for the intended parallax effects
        defaultWallpaperSize = Point(smallSide, largeSide)
    }

    private constructor(p: InvariantDeviceProfile) : this(
        p.name, p.minWidthDps, p.minHeightDps, p.numRows, p.numColumns,
        p.numFolderRows, p.numFolderColumns,
        p.iconSize, p.landscapeIconSize, p.iconTextSize, p.numHotseatIcons,
        p.defaultLayoutId, p.demoModeLayoutId
    )

    private constructor(
        n: String?,
        w: Float,
        h: Float,
        r: Int,
        c: Int,
        fr: Int,
        fc: Int,
        `is`: Float,
        lis: Float,
        its: Float,
        hs: Int,
        dlId: Int,
        dmlId: Int
    ) {
        name = n
        minWidthDps = w
        minHeightDps = h
        numRows = r
        numColumns = c
        numFolderRows = fr
        numFolderColumns = fc
        iconSize = `is`
        landscapeIconSize = lis
        iconTextSize = its
        numHotseatIcons = hs
        defaultLayoutId = dlId
        demoModeLayoutId = dmlId
    }

    private fun getPredefinedDeviceProfiles(context: Context): ArrayList<InvariantDeviceProfile> {
        val profiles =
            ArrayList<InvariantDeviceProfile>()
        try {
            context.resources.getXml(R.xml.device_profiles).use { parser ->
                val depth = parser.depth
                var type: Int
                while ((parser.next().also {
                        type = it
                    } != XmlPullParser.END_TAG ||
                        parser.depth > depth) && type != XmlPullParser.END_DOCUMENT
                ) {
                    if (type == XmlPullParser.START_TAG && "profile" == parser.name) {
                        val a = context.obtainStyledAttributes(
                            Xml.asAttributeSet(parser),
                            R.styleable.InvariantDeviceProfile
                        )
                        val numRows = a.getInt(
                            R.styleable.InvariantDeviceProfile_numRows,
                            0
                        )
                        val numColumns = a.getInt(
                            R.styleable.InvariantDeviceProfile_numColumns,
                            5
                        )
                        Log.d("Invariant", "Num columns here:  $numColumns")
                        val iconSize = a.getFloat(
                            R.styleable.InvariantDeviceProfile_iconSize,
                            0f
                        )
                        val name = a.getString(R.styleable.InvariantDeviceProfile_name)
                        Log.d("Invariant", "Parsing profile name:  $name")
                        profiles.add(
                            InvariantDeviceProfile(
                                a.getString(R.styleable.InvariantDeviceProfile_name),
                                a.getFloat(
                                    R.styleable.InvariantDeviceProfile_minWidthDps,
                                    0f
                                ),
                                a.getFloat(
                                    R.styleable.InvariantDeviceProfile_minHeightDps,
                                    0f
                                ),
                                numRows,
                                numColumns,
                                a.getInt(
                                    R.styleable.InvariantDeviceProfile_numFolderRows,
                                    numRows
                                ),
                                a.getInt(
                                    R.styleable.InvariantDeviceProfile_numFolderColumns,
                                    numColumns
                                ),
                                iconSize,
                                a.getFloat(
                                    R.styleable.InvariantDeviceProfile_landscapeIconSize,
                                    iconSize
                                ),
                                a.getFloat(
                                    R.styleable.InvariantDeviceProfile_iconTextSize,
                                    0f
                                ),
                                a.getInt(
                                    R.styleable.InvariantDeviceProfile_numHotseatIcons,
                                    numColumns
                                ),
                                a.getResourceId(
                                    R.styleable.InvariantDeviceProfile_defaultLayoutId,
                                    0
                                ),
                                a.getResourceId(
                                    R.styleable.InvariantDeviceProfile_demoModeLayoutId,
                                    0
                                )
                            )
                        )
                        a.recycle()
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: XmlPullParserException) {
            throw RuntimeException(e)
        }
        return profiles
    }

    private fun getLauncherIconDensity(requiredSize: Int): Int { // Densities typically defined by an app.
        val densityBuckets = intArrayOf(
            DisplayMetrics.DENSITY_LOW,
            DisplayMetrics.DENSITY_MEDIUM,
            DisplayMetrics.DENSITY_TV,
            DisplayMetrics.DENSITY_HIGH,
            DisplayMetrics.DENSITY_XHIGH,
            DisplayMetrics.DENSITY_XXHIGH,
            DisplayMetrics.DENSITY_XXXHIGH
        )
        var density = DisplayMetrics.DENSITY_XXXHIGH
        for (i in densityBuckets.indices.reversed()) {
            val expectedSize =
                (ICON_SIZE_DEFINED_IN_APP_DP * densityBuckets[i] /
                    DisplayMetrics.DENSITY_DEFAULT)
            if (expectedSize >= requiredSize) {
                density = densityBuckets[i]
            }
        }
        return density
    }

    fun dist(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        return hypot(x1 - x0, y1 - y0)
    }

    /**
     * Returns the closest device profiles ordered by closeness to the specified width and height
     */
    fun findClosestDeviceProfiles(
        width: Float,
        height: Float,
        points: ArrayList<InvariantDeviceProfile>
    ): ArrayList<InvariantDeviceProfile> {
        // Sort the profiles by their closeness to the dimensions
        var pointsByNearness = points
        pointsByNearness.sortWith(Comparator { a, b ->
            java.lang.Float.compare(
                dist(width, height, a.minWidthDps, a.minHeightDps), dist(
                    width,
                    height,
                    b.minWidthDps,
                    b.minHeightDps
                )
            )
        })
        return pointsByNearness
    }

    // Package private visibility for testing.
    fun invDistWeightedInterpolate(
        width: Float,
        height: Float,
        points: ArrayList<InvariantDeviceProfile>
    ): InvariantDeviceProfile {
        var weights = 0f
        var p = points[0]
        if (dist(width, height, p.minWidthDps, p.minHeightDps) == 0f) {
            return p
        }
        val out = InvariantDeviceProfile()
        var i = 0
        while (i < points.size && i < KNEARESTNEIGHBOR) {
            p = InvariantDeviceProfile(points[i])
            val w = weight(
                width,
                height,
                p.minWidthDps,
                p.minHeightDps,
                WEIGHT_POWER
            )
            weights += w
            out.add(p.multiply(w))
            ++i
        }
        return out.multiply(1.0f / weights)
    }

    private fun add(p: InvariantDeviceProfile) {
        iconSize += p.iconSize
        landscapeIconSize += p.landscapeIconSize
        iconTextSize += p.iconTextSize
    }

    private fun multiply(w: Float): InvariantDeviceProfile {
        iconSize *= w
        landscapeIconSize *= w
        iconTextSize *= w
        return this
    }

    val allAppsButtonRank: Int
        get() = numHotseatIcons / 2

    fun isAllAppsButtonRank(rank: Int): Boolean {
        return rank == allAppsButtonRank
    }

    fun getDeviceProfile(context: Context): VariantDeviceProfile {
        return if (context.resources.configuration.orientation
            == Configuration.ORIENTATION_LANDSCAPE
        ) landscapeProfile else portraitProfile
    }

    private fun weight(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        pow: Float
    ): Float {
        val d = dist(x0, y0, x1, y1)
        return if (d.compareTo(0f) == 0) {
            Float.POSITIVE_INFINITY
        } else (WEIGHT_EFFICIENT / d.toDouble().pow(pow.toDouble())).toFloat()
    }

    companion object {
        // This is a static that we use for the default icon size on a 4/5-inch phone
        private const val DEFAULT_ICON_SIZE_DP = 60f
        private const val ICON_SIZE_DEFINED_IN_APP_DP = 48f

        // Constants that affects the interpolation curve between statically defined device profile
        // buckets.
        private const val KNEARESTNEIGHBOR = 3f
        private const val WEIGHT_POWER = 5f

        // used to offset float not being able to express extremely small weights in extreme cases.
        private const val WEIGHT_EFFICIENT = 100000f
    }
}
