package sp.it.pl.web

import sp.it.util.parsing.StringParseStrategy as Parse
import de.jensd.fx.glyphs.GlyphIcons
import java.net.URI
import sp.it.pl.main.IconFA
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

abstract class ImageSearchUriBuilderBase(override val name: String, override val icon: GlyphIcons?, val uri: (String) -> URI): ImageSearchUriBuilder { override fun build(q: String) = uri(q) }

abstract class WebSearchUriBuilderBase(override val name: String, override val icon: GlyphIcons?, val uri: (String) -> URI): WebSearchUriBuilder { override fun build(q: String) = uri(q) }

@Parse(from = SINGLETON, to = CONSTANT, constant = "Bing")
object BingSearchQBuilder: WebSearchUriBuilderBase("Bing", IconMD.BING, { uri("https://www.bing.com/search?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Bing Image")
object BingImageSearchQBuilder: ImageSearchUriBuilderBase("Bing Image", IconMD.BING, { uri("https://www.bing.com/images/search?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "DuckDuckGo")
object DuckDuckGoQBuilder: WebSearchUriBuilderBase("DuckDuckGo", IconMD.DUCK, { uri("https://duckduckgo.com/?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "DuckDuckGo Image")
object DuckDuckGoImageQBuilder: ImageSearchUriBuilderBase("DuckDuckGo Image", IconMD.DUCK, { uri("https://duckduckgo.com/?q=$it&iax=images&ia=images") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Google")
object GoogleQBuilder: WebSearchUriBuilderBase("Google", IconMD.GOOGLE, { uri("https://www.google.com/search?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Google Image")
object GoogleImageQBuilder: ImageSearchUriBuilderBase("Google Image", IconMD.GOOGLE, { uri("https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Wikipedia")
object WikipediaQBuilder: WebSearchUriBuilderBase("Wikipedia", IconMD.WIKIPEDIA, { uri("https://en.wikipedia.org/wiki/Special:Search/$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Yahoo")
object YahooWebSearchUriQBuilder: WebSearchUriBuilderBase("Yahoo", IconFA.YAHOO, { uri("https://search.yahoo.com/search?p=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "AOL")
object AOLWebSearchUriQBuilder: WebSearchUriBuilderBase("AOL", null, { uri("https://search.aol.com/aol/search?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Baidu")
object BaiduWebSearchUriQBuilder: WebSearchUriBuilderBase("Baidu", null, { uri("https://www.baidu.com/s?wd=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Yandex")
object YandexWebSearchUriQBuilder: WebSearchUriBuilderBase("Yandex", null, { uri("https://yandex.ru/search/?text=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Ask")
object AskWebSearchUriQBuilder: WebSearchUriBuilderBase("Ask", null, { uri("https://www.ask.com/web?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "WolframAlpha")
object WolframAlphaWebSearchUriQBuilder: WebSearchUriBuilderBase("WolframAlpha", null, { uri("https://www.wolframalpha.com/input?i=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "WebCrawler")
object WebSearchUriQBuilder: WebSearchUriBuilderBase("WebCrawler", null, { uri("https://www.webcrawler.com/serp?q=$it") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "Qwant")
object QwantWebSearchUriQBuilder: WebSearchUriBuilderBase("Qwant", null, { uri("https://www.qwant.com/?q=$it&t=web") })

@Parse(from = SINGLETON, to = CONSTANT, constant = "StartPage")
object StartPageWebSearchUriQBuilder: WebSearchUriBuilderBase("StartPage", null, { uri("https://www.startpage.com/do/search?q=$it") })

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