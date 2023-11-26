package sp.it.pl.audio

import java.io.File
import java.net.URI

/** Simplest [Song] implementation. Wraps [java.net.URI]. */
class SimpleSong(uri: URI): Song() {

   override val uri: URI = uri

   constructor(resource: File): this(resource.toURI())

   override fun toSimple() = this

   override fun toString() = "$javaClass[$uri]"

}