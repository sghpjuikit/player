package web

import util.Util
import util.functional.Functors.Ƒ1
import java.net.URI

/**
 * [java.net.URI] builder for searching a resource with a string.
 */
interface SearchUriBuilder: Ƒ1<String, URI> {

    override fun apply(queryParam: String): URI = doApply(Util.urlEncodeUtf8(queryParam))

    fun doApply(q: String): URI

}

interface ImageSearchUriBuilder: SearchUriBuilder

interface WebSearchUriBuilder: SearchUriBuilder