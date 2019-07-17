package sp.it.pl.audio

import java.io.File
import java.net.URI

/** Simplest [Song] implementation. Wraps [java.net.URI]. */
class SimpleSong(resource: URI): Song() {

   override val uri: URI = resource

   constructor(resource: File): this(resource.toURI())

   override fun toSimple() = this

   override fun toString() = "$javaClass[$uri]"

}