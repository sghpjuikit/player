package sp.it.pl.web

import sp.it.util.Util
import sp.it.util.functional.Functors.Ƒ1
import java.net.URI
import sp.it.util.parsing.StringParseStrategy as Parse

/** [java.net.URI] builder for searching a resource with a string. */
interface SearchUriBuilder: Ƒ1<String, URI> {

    val name: String

    fun uri(q: String): URI

    override fun apply(queryParam: String): URI = uri(Util.urlEncodeUtf8(queryParam))

}

interface ImageSearchUriBuilder: SearchUriBuilder

interface WebSearchUriBuilder: SearchUriBuilder


@Parse(from = Parse.From.SINGLETON, to = Parse.To.CONSTANT, constant = "Bing Image")
object BingImageSearchQBuilder: ImageSearchUriBuilder {
    override val name = "Bing Image"
    override fun uri(q: String) = URI.create("http://www.bing.com/images/search?q=$q&qs=n&form=QBIR&pq=ggg&sc=8-3&sp=-1")!!
}

@Parse(from = Parse.From.SINGLETON, to = Parse.To.CONSTANT, constant = "DuckDuckGo Image")
object DuckDuckGoImageQBuilder: ImageSearchUriBuilder {
    override val name = "DuckDuckGo Image"
    override fun uri(q: String) = URI.create("https://duckduckgo.com/?q=$q&iax=images&ia=images")!!
}

@Parse(from = Parse.From.SINGLETON, to = Parse.To.CONSTANT, constant = "DuckDuckGo")
object DuckDuckGoQBuilder: WebSearchUriBuilder {
    override val name = "DuckDuckGo"
    override fun uri(q: String) = URI.create("https://duckduckgo.com/?q=$q")!!
}

@Parse(from = Parse.From.SINGLETON, to = Parse.To.CONSTANT, constant = "Google Image")
object GoogleImageQBuilder: ImageSearchUriBuilder {
    override val name = "Google Image"
    override fun uri(q: String) = URI.create("https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=$q")!!
}

@Parse(from = Parse.From.SINGLETON, to = Parse.To.CONSTANT, constant = "Wikipedia")
object WikipediaQBuilder: WebSearchUriBuilder {
    override val name = "Wikipedia"
    override fun uri(q: String) = URI.create("https://en.wikipedia.org/wiki/Special:Search/$q")!!
}

object WebBarInterpreter {

    fun toUrlString(text: String, searchEngine: SearchUriBuilder): String {
        val isUrl = text.contains(".")
        return if (isUrl) addHttpWwwPrefix(Util.urlEncodeUtf8(text)) else searchEngine(text).toASCIIString()
    }

    private fun addHttpWwwPrefix(url: String) =
        if (url.startsWithHttp()) url
        else "http://" + (if (url.startsWith("www.")) url else "www.$url")

    private fun String.startsWithHttp() = startsWith("http://") || startsWith("https://")

}