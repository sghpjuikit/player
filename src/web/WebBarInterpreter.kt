package web

import util.Util.urlEncodeUtf8

object WebBarInterpreter {

    fun toUrlString(text: String, searchEngine: SearchUriBuilder): String {
        val isUrl = text.contains(".")
        return if (isUrl) addHttpWwwPrefix(urlEncodeUtf8(text)) else searchEngine(text).toASCIIString()
    }

    private fun addHttpWwwPrefix(url: String) =
        if (url.startsWithHttp()) url
        else "http://" + (if (url.startsWith("www.")) url else "www.") + url

    fun String.startsWithHttp() = startsWith("http://") || startsWith("https://")

}