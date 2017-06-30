package web

import util.parsing.StringParseStrategy
import util.parsing.StringParseStrategy.From
import util.parsing.StringParseStrategy.To
import java.net.URI

@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Wikipedia")
object WikipediaQBuilder: WebSearchUriBuilder {

    override fun doApply(q: String) = URI.create("https://en.wikipedia.org/wiki/$q")!!

}