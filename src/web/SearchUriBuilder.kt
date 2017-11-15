package web

import util.Util
import util.functional.Functors.Ƒ1
import util.parsing.StringParseStrategy
import java.net.URI

private typealias Parse = StringParseStrategy
private typealias ParseFrom = StringParseStrategy.From
private typealias ParseTo = StringParseStrategy.To

/** [java.net.URI] builder for searching a resource with a string. */
interface SearchUriBuilder: Ƒ1<String, URI> {

    override fun apply(queryParam: String): URI = uri(Util.urlEncodeUtf8(queryParam))

    fun uri(q: String): URI

}

interface ImageSearchUriBuilder: SearchUriBuilder

interface WebSearchUriBuilder: SearchUriBuilder


@Parse(from = ParseFrom.SINGLETON, to = ParseTo.CONSTANT, constant = "Bing Image")
object BingImageSearchQBuilder: ImageSearchUriBuilder {
    override fun uri(q: String) = URI.create("http://www.bing.com/images/search?q=$q&qs=n&form=QBIR&pq=ggg&sc=8-3&sp=-1")!!
}

@Parse(from = ParseFrom.SINGLETON, to = ParseTo.CONSTANT, constant = "DuckDuckGo Image")
object DuckDuckGoImageQBuilder: ImageSearchUriBuilder {
    override fun uri(q: String) = URI.create("https://duckduckgo.com/?q=$q&iax=images&ia=images")!!
}

@Parse(from = ParseFrom.SINGLETON, to = ParseTo.CONSTANT, constant = "DuckDuckGo")
object DuckDuckGoQBuilder: WebSearchUriBuilder {
    override fun uri(q: String) = URI.create("https://duckduckgo.com/?q=$q")!!
}

@Parse(from = ParseFrom.SINGLETON, to = ParseTo.CONSTANT, constant = "Google Image")
object GoogleImageQBuilder: ImageSearchUriBuilder {
    override fun uri(q: String) = URI.create("https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=$q")!!
}

@Parse(from = ParseFrom.SINGLETON, to = ParseTo.CONSTANT, constant = "Wikipedia")
object WikipediaQBuilder: WebSearchUriBuilder {
    override fun uri(q: String) = URI.create("https://en.wikipedia.org/wiki/$q")!!
}


object WebBarInterpreter {

    fun toUrlString(text: String, searchEngine: SearchUriBuilder): String {
        val isUrl = text.contains(".")
        return if (isUrl) addHttpWwwPrefix(Util.urlEncodeUtf8(text)) else searchEngine(text).toASCIIString()
    }

    private fun addHttpWwwPrefix(url: String) =
            if (url.startsWithHttp()) url
            else "http://" + (if (url.startsWith("www.")) url else "www.") + url

    fun String.startsWithHttp() = startsWith("http://") || startsWith("https://")

}