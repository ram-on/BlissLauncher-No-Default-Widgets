package foundation.e.blisslauncher.data.parser

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.os.Process
import android.util.ArrayMap
import foundation.e.blisslauncher.data.LauncherDatabaseGateway
import foundation.e.blisslauncher.data.database.roomentity.WorkspaceItem
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException

class DefaultHotseatParser (
    private val dbGateway: LauncherDatabaseGateway,
    private val defaultHotseatId: Int,
    private val context: Context,
    private val userManagerRepository: UserManagerRepository
) {

    private val resources: Resources = context.resources

    /**
     * Loads the default hotseat layout and returns number of entries added to the desktop
     */
    fun loadDefaultLayout(): Int = try {
        parseLayout(defaultHotseatId)
    } catch (e: Exception) {
        Timber.e(e, "Error parsing layout")
        -1
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseLayout(defaultHotseatId: Int): Int {
        val parser = resources.getXml(defaultHotseatId)
        beginDocument(parser, ROOT_TAG)
        val depth = parser.depth
        Timber.d("Depth of parser is: $depth")
        var type: Int
        val tagParserMap = getLayoutElementsMap()
        var count = 0
        while ((parser.next().also { type = it } != XmlPullParser.END_TAG || parser.depth > depth)
            && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue
            }
            count += parseAndAddNode(parser, tagParserMap)
        }
        return count
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseAndAddNode(
        parser: XmlResourceParser,
        tagParserMap: ArrayMap<String, TagParser>
    ): Int {
        Timber.d("Parser is ${parser.name}")
        val container = getAttributeValue(parser, ATTR_CONTAINER)?.toLong()
        val screenId = getAttributeValue(parser, ATTR_SCREEN)?.toLong()
        val rank = screenId?.toInt()
        val x = getAttributeValue(parser, ATTR_X)?.toInt()
        val y = getAttributeValue(parser, ATTR_Y)?.toInt()
        val tagParser = tagParserMap.get(parser.name)
        if (tagParser == null) {
            Timber.d("Ignoring unknown element tag: ${parser.name}" )
            return 0
        }

        val parsedResult = tagParser.parseAndAdd(parser)
        return if (parsedResult.result >= 0 && parsedResult.dataTriplet != null) {
            val triple = parsedResult.dataTriplet
            WorkspaceItem(
                _id = dbGateway.generateNewItemId(), title = triple.first,
                intentStr = triple.second.toUri(0), container = container!!, screen = screenId!!,
                cellX = x!!, cellY = y!!, itemType = triple.third, rank = rank!!,
                profileId = userManagerRepository.getSerialNumberForUser(Process.myUserHandle())
            ).let {
                Timber.d("Parsed Item: $it")
                dbGateway.insertAndCheck(it)
                1
            }
        } else 0
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun beginDocument(
        parser: XmlPullParser,
        firstElementName: String
    ) {
        var type: Int
        while (parser.next().also { type = it } != XmlPullParser.START_TAG
            && type != XmlPullParser.END_DOCUMENT
        );
        if (type != XmlPullParser.START_TAG) {
            throw XmlPullParserException("No start tag found")
        }
        if (parser.name != firstElementName) {
            throw XmlPullParserException(
                "Unexpected start tag: found " + parser.name +
                    ", expected " + firstElementName
            )
        }
    }

    private fun getLayoutElementsMap(): ArrayMap<String, TagParser> {
        val parsers = ArrayMap<String, TagParser>()
        parsers[TAG_RESOLVE] = ResolveParser(context)
        return parsers
    }

    companion object {
        const val TAG_RESOLVE = "resolve"
        const val TAG_FAVORITE = "favorite"
        const val ATTR_URI = "uri"
        const val ATTR_CONTAINER = "container"
        const val ATTR_SCREEN = "screen"
        const val ATTR_PACKAGE_NAME = "packageName"
        const val ATTR_CLASS_NAME = "className"

        const val ROOT_TAG = "hotseat"

        const val ATTR_X = "x"
        const val ATTR_Y = "y"

        /**
         * Return attribute value, attempting launcher-specific namespace first
         * before falling back to anonymous attribute.
         */
        fun getAttributeValue(
            parser: XmlResourceParser,
            attribute: String
        ): String? {
            Timber.d("Attribute is $attribute")
            var value = parser.getAttributeValue(
                "http://schemas.android.com/apk/res-auto", attribute
            )
            if (value == null) {
                value = parser.getAttributeValue(null, attribute)
            }
            Timber.d("Value is $value")

            return value
        }

        /**
         * Return attribute resource value, attempting launcher-specific namespace
         * first before falling back to anonymous attribute.
         */
        fun getAttributeResourceValue(
            parser: XmlResourceParser, attribute: String?,
            defaultValue: Int
        ): Int {
            var value = parser.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto/foundation.e.blisslauncher", attribute,
                defaultValue
            )
            if (value == defaultValue) {
                value = parser.getAttributeResourceValue(null, attribute, defaultValue)
            }
            return value
        }
    }
}