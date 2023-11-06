package sp.it.pl.main

import sp.it.util.async.VT as VTe
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.utils.io.core.toByteArray
import java.net.InetSocketAddress
import java.util.function.Consumer
import mu.KLogging
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.launch
import sp.it.util.async.runFX
import sp.it.util.file.json.toCompactS
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Handler1
import sp.it.util.text.lengthInBytes
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.HttpTimeout
import sp.it.util.async.runVT
import sp.it.util.async.sleep
import sp.it.util.conf.Config
import sp.it.util.dev.printThreads

/** Application http server & http client. */
class AppHttp(
   private val port: Int
) {
   private val ser = lazy { HttpServer.create(InetSocketAddress(port), 0).apply {
   } }
   private val cli = lazy { HttpClient(Java).config { expectSuccess = true; install(HttpTimeout) } }
   private val j = Config.json

   /** Url of this application */
   val url = "http://127.0.0.1:$port"
   /** Http sever of this application, does not support https. */
   val server get() = ser.value
   /** Http server request handlers. */
   val serverHandlers = mutableListOf<Handler>()
   /** Http client of this application. */
   val client get() = cli.value

   fun init() {
      server.createContext("/", HttpHandler { r -> serverHandlers.find { it.matcher(r) }?.block?.invoke(r) })
      server.executor = VTe
      server.start()
   }

   fun stop() {
      runTry { cli.orNull()?.close() }.ifError { logger.error(it) { "Failed to close http client" } }
      runTry { ser.orNull()?.stop(0) }.ifError { logger.error(it) { "Failed to close http server" } }
   }

   class Handler(val matcher: (HttpExchange) -> Boolean, val block: (HttpExchange) -> Unit)

   class JsContent(o: Any?): OutgoingContent.ByteArrayContent() {
      private val bytes = APP.serializerJson.json.toJsonValue(o).toCompactS().toByteArray()
      override val status = null
      override val contentType = ContentType.Application.Json
      override val contentLength = bytes.size.toLong()
      override fun bytes() = bytes
      override fun toString() = "JsContent(byte[...])"
   }

   companion object: KLogging()
}