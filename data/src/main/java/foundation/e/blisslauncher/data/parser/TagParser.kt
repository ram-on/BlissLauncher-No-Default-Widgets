package foundation.e.blisslauncher.data.parser

import android.content.Intent
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

interface TagParser {
    /**
     * Parses the tag and adds to the db
     * @return the id of the row added or -1;
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseAndAdd(parser: XmlResourceParser): ParseResult
}