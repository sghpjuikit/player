package sp.it.util.text

import org.apache.commons.text.StringEscapeUtils.escapeCsv
import org.apache.commons.text.StringEscapeUtils.escapeEcmaScript
import org.apache.commons.text.StringEscapeUtils.escapeHtml3
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.apache.commons.text.StringEscapeUtils.escapeJava
import org.apache.commons.text.StringEscapeUtils.escapeJson
import org.apache.commons.text.StringEscapeUtils.escapeXSI
import org.apache.commons.text.StringEscapeUtils.escapeXml10
import org.apache.commons.text.StringEscapeUtils.escapeXml11
import org.apache.commons.text.StringEscapeUtils.unescapeCsv
import org.apache.commons.text.StringEscapeUtils.unescapeEcmaScript
import org.apache.commons.text.StringEscapeUtils.unescapeHtml3
import org.apache.commons.text.StringEscapeUtils.unescapeHtml4
import org.apache.commons.text.StringEscapeUtils.unescapeJava
import org.apache.commons.text.StringEscapeUtils.unescapeJson
import org.apache.commons.text.StringEscapeUtils.unescapeXSI
import org.apache.commons.text.StringEscapeUtils.unescapeXml

/** [org.apache.commons.text.StringEscapeUtils.escapeJava] */
fun String.escapeJava(): String = escapeJava(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeEcmaScript] */
fun String.escapeEcmaScript(): String = escapeEcmaScript(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeJson] */
fun String.escapeJson(): String = escapeJson(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeJava] */
fun String.unescapeJava(): String = unescapeJava(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeEcmaScript] */
fun String.unescapeEcmaScript(): String = unescapeEcmaScript(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeJson] */
fun String.unescapeJson(): String = unescapeJson(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeHtml4] */
fun String.escapeHtml4(): String = escapeHtml4(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeHtml3] */
fun String.escapeHtml3(): String = escapeHtml3(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeHtml4] */
fun String.unescapeHtml4(): String = unescapeHtml4(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeHtml3] */
fun String.unescapeHtml3(): String = unescapeHtml3(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeXml10] */
fun String.escapeXml10(): String? = escapeXml10(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeXml11] */
fun String.escapeXml11(): String? = escapeXml11(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeXml] */
fun String.unescapeXml(): String = unescapeXml(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeCsv] */
fun String.escapeCsv(): String = escapeCsv(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeCsv] */
fun String.unescapeCsv(): String = unescapeCsv(this)

/** [org.apache.commons.text.StringEscapeUtils.escapeCsv] */
fun String.escapeMd(): String {
   val mdChars = """*#()[]_-\+`<>&|""".chars32().toSet()
   return chars32().map { if (it in mdChars) "\\" + it else "" + it }.joinToString("")
}

/** [org.apache.commons.text.StringEscapeUtils.escapeXSI] */
fun String.escapeXSI(): String = escapeXSI(this)

/** [org.apache.commons.text.StringEscapeUtils.unescapeXSI] */
fun String.unescapeXSI(): String = unescapeXSI(this)

