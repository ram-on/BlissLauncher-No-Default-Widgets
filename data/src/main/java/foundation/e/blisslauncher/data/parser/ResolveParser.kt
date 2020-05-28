package foundation.e.blisslauncher.data.parser

import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException

/**
 * Contains a list of <favorite> nodes, and accepts the first successfully parsed node.
</favorite> */
class ResolveParser(context: Context) : TagParser {
    private val mChildParser: AppShortcutWithUriParser = AppShortcutWithUriParser(context)

    @Throws(XmlPullParserException::class, IOException::class)
    override fun parseAndAdd(parser: XmlResourceParser): ParseResult {
        val groupDepth = parser.depth
        var type: Int
        var parseResult = ParseResult(-1)
        while (parser.next().also { type = it } != XmlPullParser.END_TAG ||
            parser.depth > groupDepth
        ) {
            Timber.d("Parsing Type is $type")
            if (type != XmlPullParser.START_TAG || parseResult.result > -1) {
                continue
            }
            val fallbackItemName = parser.name
            Timber.d("FallbackItem $fallbackItemName")
            if (DefaultHotseatParser.TAG_FAVORITE == fallbackItemName) {
                parseResult = mChildParser.parseAndAdd(parser)
                return parseResult
            } else {
                Timber.e("Fallback groups can contain only favorites, found " +
                    "$fallbackItemName")
            }
        }
        return parseResult
    }
}