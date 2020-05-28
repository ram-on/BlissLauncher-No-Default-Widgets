package foundation.e.blisslauncher.data.parser

import android.content.Intent

/**
 * Result return after parsing the node.
 * If [result] == 1, the xml node is parsed successfully and it contains additional data in
 * [dataTriplet]
 *
 * If their is any error or no package/shortcut is found [result] is set to -1.
 */
data class ParseResult(val result: Int, val dataTriplet: Triple<String, Intent, Int>? = null)