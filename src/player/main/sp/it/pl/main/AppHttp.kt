package sp.it.pl.main

import sp.it.util.async.VT as VTe
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.statement.HttpResponse
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import org.jetbrains.annotations.Range
import sp.it.pl.core.Core
import sp.it.pl.core.NameUi
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.butElement
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.type.mimeType
import sp.it.util.functional.getOr
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Subscription
import sp.it.util.units.FileSize.Companion.sizeInBytes
import sp.it.util.units.uri

/** Application http server & http client. */
class AppHttp(
   private val port: @Range(from = 0, to = 65535) Int
): Core, GlobalSubConfigDelegator("Http") {
   /** Lazy [server]. */
   private val serverLazy = lazy { buildServer() }
   /** Lazy [client]. */
   private val clientLazy = lazy { buildClient() }
   /** Speech handlers called when user has spoken. */
   private val serverHandlers by cList<Handler>().readOnly().noPersist().butElement { uiConverter { it.nameUi } }
      .def(name = "API", info = "API of this application. Be default empty.", editable = EditMode.APP)
   /** Url of this application (where the server will be started) */
   val url by c(uri("http://0.0.0.0:$port")).readOnly().noPersist()
      .def(name = "Url", info = "Url address of this application. 0.0.0.0 means the application is reachable from local network and potentially also the internet.", editable = EditMode.NONE)
   /** Url of this application (where the server can be reached locally, i.e. 0.0.0.0 replaced by localhost) */
   val urlLocal = uri(url.toString().replace("0.0.0.0", "localhost"))
   /** Url of this application (where the server can be reached externally, i.e. 0.0.0.0 replaced by host address) */
   val urlExternal = uri(url.toString().replace("0.0.0.0", InetAddress.getLocalHost().hostAddress))
   /** Http sever of this application, does not support https. Thread-safe. */
   val server get() = serverLazy.value
   /** Http server routes, i.e., request handlers. Thread-safe. */
   val serverRoutes = Handlers()
   /** Http client of this application. Thread-safe. */
   val client get() = clientLazy.value

   fun buildServer(): HttpServer =
      try {
         HttpServer.create(InetSocketAddress(url.host, port), 0).apply {
            runTry {
               executor = VTe("Http")
               start()
               createContext("/", buildServerHandler())
               logger.info { "Http server url=$url started" }
            }.ifError {
               logger.error(it) {
                  if (it !is BindException) "Http server url=$url failed to start"
                  else "Http server url=$url failed to start, port in use. Available port: ${findAvailablePort(50000..60000)}"
               }
            }
         }
      } catch (e: Throwable) {
         fail {
            if (e !is BindException) "Http server url=$url failed to create"
            else "Http server url=$url failed to create, port in use. Available port: ${findAvailablePort(50000..60000)}"
         }
      }

   fun buildClient(): HttpClient =
      HttpClient(Java).apply {
         config {
            expectSuccess = true
            install(HttpTimeout)
         }

         logger.info { "Http client started" }
      }

   override fun dispose() {
      runTry { clientLazy.orNull()?.close() }.ifError { logger.error(it) { "Failed to close http client" } }
      runTry { serverLazy.orNull()?.stop(0) }.ifError { logger.error(it) { "Failed to close http server" } }
   }

   private fun buildServerHandler() = HttpHandler { e ->
      runTry {
         logger.info { "Req ${e.requestMethod} ${e.requestURI}" }
         val r = serverRoutes.find(e)
         // 404 if no match
         r ?: throw Exception404("No handler for ${e.requestURI.path}")
         // 405 if no method match
         r.takeIf { it.method==null || it.method==e.requestMethod } ?: throw Exception405("Method not allowed for ${e.requestURI.path}")
         r.block(e)
      }.map {
         when (it) {
            is Fut<*> -> it.blockAndGetOrThrow()
            else -> it
         }
      }.ifError { x ->
         if (x is Exception404)
            e.respond(404, 0) { it.writer().write(x.message ?: "") }
         if (x is Exception405)
            e.respond(405, 0) { it.writer().write(x.message ?: "") }
         else {
            logger.error(x) { "Failed to handle http request ${e.requestURI}" }
            e.respond(500, 0) { it.writer().write(x.message ?: "") }
         }
      }.ifOk {
         val bs = DEFAULT_BUFFER_SIZE
         when (it) {
            null ->
               e.respond(200, 0) {}
            is Unit ->
               e.respond(200, 0) {}
            is InputStream ->
               e.respond(200, 0) { o -> it.buffered(bs).use { i -> i.copyTo(o, bs) } }
            is File -> {
               val fileSize = it.sizeInBytes()
               val isRange = e.requestHeaders["Range"]!=null
               val range = e.requestHeaders["Range"]?.first()?.split("-")?.net { parts ->
                  parts[0].toLong()..when {
                     parts.size>1 && parts[1].isNotEmpty() -> parts[1].toLong()
                     else -> it.sizeInBytes() - 1
                  }
               } ?: 0..it.sizeInBytes() - 1
               val contentLength = range.endInclusive - range.start + 1
               e.responseHeaders["Content-Type"] = it.mimeType().name
               e.responseHeaders["Content-Length"] = contentLength.toString()
               e.responseHeaders["Accept-Ranges"] = "bytes"
               if (isRange) e.responseHeaders["Content-Range"] = "bytes ${range.start}-${range.endInclusive}/$fileSize"

               e.respond(if (isRange) 206 else 200, contentLength) { o ->
                  val buffer = ByteArray(bs)
                  var bytesRead: Int
                  var bytesRemaining = contentLength
                  var i = it.inputStream().buffered(bs)
                  while (i.read(buffer).also { bytesRead = it }>=0 && bytesRemaining>0) {
                     val bytesToWrite = if (bytesRead<=bytesRemaining) bytesRead else bytesRemaining
                     o.write(buffer, 0, bytesToWrite.toInt())
                     bytesRemaining -= bytesRead
                  }
                  println("bytes read $bytesRead")
               }
            }
            else -> {
               e.responseHeaders["Content-Type"] = "application/json"
               val s = Config.json.toJsonValue(it).toPrettyS().toByteArray()
               e.respond(200, s.size.toLong()) { o ->
                  o.write(s)
               }
            }
         }
      }
   }

   class Handler(override val nameUi: String, val method: String? = "GET", val exactMatch: Boolean = false, block: (HttpExchange) -> Any?): NameUi {
      /** Path to match against in raw form. May contain single `%` path variable at the end. May contain `?` and quary parameters. */
      val matcher = nameUi
      /** Path to match against */
      val path = matcher.substringBeforeLast("?").substringBeforeLast("%")
      /** Number of parts of [path] */
      val pathParts = path.split("/").size
      /** Request handler */
      val block = block
   }

   inner class Handlers() {
      /** Routes ordered by [pathParts] desc */
      protected val routes = CopyOnWriteArrayList<Handler>()

      infix fun route(handler: Handler): Subscription {
         routes.add(routes.indexOfFirst { it.pathParts<handler.pathParts }.coerceAtLeast(0), handler)
         runFX { serverHandlers += handler }
         if (routes.isNotEmpty() && !serverLazy.isInitialized()) server
         return Subscription {
            routes -= handler
            runFX { serverHandlers -= handler }
         }
      }

      fun routes(): List<Handler> = routes

      fun find(request: HttpExchange): Handler? =
         routes.find {
            if (it.exactMatch) request.requestURI.path == it.path
            else request.requestURI.path.startsWith(it.path)
         }
   }

   private class Exception404(message: String): RuntimeException(message)
   private class Exception405(message: String): RuntimeException(message)

   companion object {
      private val logger = KotlinLogging.logger { }

      /** Whether `Server` header is this application. */
      fun HttpExchange.isFromSpitPlayer() =
         responseHeaders["Server"]?.firstOrNull()?.startsWith(APP.name)==true

      /** Whether `Server` header is this application. */
      fun HttpResponse.isFromSpitPlayer() =
         headers["Server"]?.startsWith(APP.name)==true

      /** Set `Server` header to this application. */
      fun HttpExchange.setFromSpitPlayer() {
         responseHeaders["Server"] = "${APP.name}/${APP.version}"
      }

      private fun findAvailablePort(range: IntRange): Int? {
         val localhost = InetAddress.getByName("localhost")
         return range.first { runTry { Socket(localhost, it).use { true } }.getOr(false) }
      }

      private fun HttpExchange.respond(status: Int, contentLength: Long, writer: (OutputStream) -> Unit) {
         setFromSpitPlayer()
         if (requestMethod=="HEAD") {
            sendResponseHeaders(status, -1)
            responseBody.close()
         } else {
            sendResponseHeaders(status, contentLength)
            runTry {
               responseBody.buffered(DEFAULT_BUFFER_SIZE).use { writer(it); it.flush() }
            }
         }
      }

   }
}