package sp.it.pl.web

import sp.it.util.parsing.StringParseStrategy as Parse
import de.jensd.fx.glyphs.GlyphIcons
import java.net.URI
import sp.it.pl.main.IconMD
import sp.it.util.Util.urlEncodeUtf8
import sp.it.util.functional.Functors.F1
import sp.it.util.parsing.StringParseStrategy.From.SINGLETON
import sp.it.util.parsing.StringParseStrategy.To.CONSTANT
import sp.it.util.units.uri

/** [java.net.URI] builder for searching a resource with a string. */
interface SearchUriBuilder: F1<String, URI> {
   /** Name of this search builder */
   val name: String
   /** Icon of this search builder */
   val icon: GlyphIcons?
   /** Builds search URI */
   fun build(q: String): URI
   /** Builds search URI */
   override fun apply(queryParam: String): URI = build(urlEncodeUtf8(queryParam))
}

interface ImageSearchUriBuilder: SearchUriBuilder

interface WebSearchUriBuilder: SearchUriBuilder

@Parse(from = SINGLETON, to = CONSTANT, constant = "Bing")
object BingSearchQBuilder: ImageSearchUriBuilder {
   override val name = "Bing"
   override val icon = IconMD.BING
   override fun build(q: String) = uri("https://www.bing.com/search?q=$q")
}
@Parse(from = SINGLETON, to = CONSTANT, constant = "Bing Image")
object BingImageSearchQBuilder: ImageSearchUriBuilder {
   override val name = "Bing Image"
   override val icon = IconMD.BING
   override fun build(q: String) = uri("https://www.bing.com/images/search?q=$q")
}

@Parse(from = SINGLETON, to = CONSTANT, constant = "DuckDuckGo")
object DuckDuckGoQBuilder: WebSearchUriBuilder {
   override val name = "DuckDuckGo"
   override val icon = IconMD.DUCK
   override fun build(q: String) = uri("https://duckduckgo.com/?q=$q")
}

@Parse(from = SINGLETON, to = CONSTANT, constant = "DuckDuckGo Image")
object DuckDuckGoImageQBuilder: ImageSearchUriBuilder {
   override val name = "DuckDuckGo Image"
   override val icon = IconMD.DUCK
   override fun build(q: String) = uri("https://duckduckgo.com/?q=$q&iax=images&ia=images")
}

@Parse(from = SINGLETON, to = CONSTANT, constant = "Google")
object GoogleQBuilder: ImageSearchUriBuilder {
   override val name = "Google"
   override val icon = IconMD.GOOGLE
   override fun build(q: String) = uri("https://www.google.com/search?q=q")
}

@Parse(from = SINGLETON, to = CONSTANT, constant = "Google Image")
object GoogleImageQBuilder: ImageSearchUriBuilder {
   override val name = "Google Image"
   override val icon = IconMD.GOOGLE
   override fun build(q: String) = uri("https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=$q")
}

@Parse(from = SINGLETON, to = CONSTANT, constant = "Wikipedia")
object WikipediaQBuilder: WebSearchUriBuilder {
   override val name = "Wikipedia"
   override val icon = IconMD.WIKIPEDIA
   override fun build(q: String) = uri("https://en.wikipedia.org/wiki/Special:Search/$q")
}

object WebBarInterpreter {

   fun toUrlString(text: String, searchEngine: SearchUriBuilder): String {
      val isUrl = text.contains(".")
      return if (isUrl) addHttpWwwPrefix(urlEncodeUtf8(text)) else searchEngine(text).toASCIIString()
   }

   private fun addHttpWwwPrefix(url: String) =
      if (url.startsWithHttp()) url
      else "http://" + (if (url.startsWith("www.")) url else "www.$url")

   private fun String.startsWithHttp() = startsWith("http://") || startsWith("https://")

}