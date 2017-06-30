package web

import util.parsing.StringParseStrategy
import util.parsing.StringParseStrategy.From
import util.parsing.StringParseStrategy.To
import java.net.URI

@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Google Image")
object GoogleImageQBuilder: ImageSearchUriBuilder {

    override fun doApply(q: String) = URI.create("https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=$q")!!

}