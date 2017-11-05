package audio

import java.io.File
import java.net.URI

/** Simplest [Item] implementation. Wraps [java.net.URI]. Immutable. */
class SimpleItem: Item {

    override val uri: URI

    constructor(resource: URI) {
        uri = resource
    }

    constructor(resource: File) {
        uri = resource.toURI()
    }

    override fun toSimple() = this

}