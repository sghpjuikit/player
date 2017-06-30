package web

import util.parsing.StringParseStrategy
import util.parsing.StringParseStrategy.From
import util.parsing.StringParseStrategy.To
import java.net.URI

@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "DuckDuckGo Image")
object DuckDuckGoImageQBuilder: ImageSearchUriBuilder {

    override fun doApply(q: String): URI {
        return URI.create("https://duckduckgo.com/?q=$q")!!
    }

}