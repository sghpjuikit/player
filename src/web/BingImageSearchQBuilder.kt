package web

import util.parsing.StringParseStrategy
import util.parsing.StringParseStrategy.From
import util.parsing.StringParseStrategy.To
import java.net.URI

@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Bing Image")
object BingImageSearchQBuilder: ImageSearchUriBuilder {

    override fun doApply(q: String) = URI.create("http://www.bing.com/images/search?q=$q&qs=n&form=QBIR&pq=ggg&sc=8-3&sp=-1")!!

}