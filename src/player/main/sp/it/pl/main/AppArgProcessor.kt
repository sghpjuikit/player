package sp.it.pl.main

import sp.it.util.file.toFileOrNull
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.util.ArrayList

class AppArgProcessor: ArgProcessor {
    private val processors = ArrayList<ArgProcessor>()

    override fun process(args: Collection<String>) {
        processors.forEach { it.process(args) }
    }

    operator fun plusAssign(p: ArgProcessor) {
        processors += p
    }

}

interface ArgProcessor {
    fun process(args: Collection<String>)
}

abstract class ArgProcessorBase: ArgProcessor {
    abstract val needsApp: Boolean

    override fun process(args: Collection<String>) {
        if (needsApp) processImpl(args)
        else APP.onStarted += { processImpl(args) }
    }

    protected abstract fun processImpl(args: Collection<String>)
}

class StringArgProcessor(private val isProcessible: (String) -> Boolean, override val needsApp: Boolean, private val processor: (List<String>) -> Unit): ArgProcessorBase() {

    override fun processImpl(args: Collection<String>) {
        val strings = args.filter(isProcessible)
        if (strings.isNotEmpty()) processor(strings)
    }

}

class URIArgProcessor(private val isProcessible: (URI) -> Boolean, override val needsApp: Boolean, private val processor: (List<URI>) -> Unit): ArgProcessorBase() {

    override fun processImpl(args: Collection<String>) {
        val uris = args.asSequence()
                .mapNotNull {
                    try {
                        if (it.length>=2 && it[1]==':')
                            URI.create("file:///"+URLEncoder.encode(it, "UTF-8").replace("+", "%20"))
                        else
                            URI.create(URLEncoder.encode(it, "UTF-8").replace("+", "%20"))
                    } catch (e: IllegalArgumentException) {
                        null
                    } catch (e: java.io.UnsupportedEncodingException) {
                        null
                    }
                }
                .filter(isProcessible)
                .toList()
        if (uris.isNotEmpty()) processor(uris)
    }

}

class FileArgProcessor(private val isProcessible: (File) -> Boolean, override val needsApp: Boolean, private val processor: (List<File>) -> Unit): ArgProcessorBase() {

    override fun processImpl(args: Collection<String>) {
        val files = args.asSequence()
                .mapNotNull {
                    try {
                        if (it.length>=2 && it[1]==':')
                            URI.create("file:///"+URLEncoder.encode(it, "UTF-8").replace("+", "%20"))
                        else
                            URI.create(URLEncoder.encode(it, "UTF-8").replace("+", "%20"))
                    } catch (e: IllegalArgumentException) {
                        null
                    } catch (e: java.io.UnsupportedEncodingException) {
                        null
                    }
                }
                .mapNotNull { it.toFileOrNull() }
                .filter(isProcessible)
                .toList()
        if (files.isNotEmpty()) processor(files)
    }

}