package sp.it.pl.main

import sp.it.util.async.VT as VTe
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
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
import mu.KLogging
import org.jetbrains.annotations.Range
import sp.it.pl.core.Core
import sp.it.pl.core.NameUi
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.collections.toStringPretty
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
   /** Url of this application */
   val url by c(uri("http://0.0.0.0:$port")).readOnly().noPersist()
      .def(name = "Url", info = "Url address of this application. 0.0.0.0 means the application is reachable from local network and potentially also the internet.", editable = EditMode.NONE)
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
         serverRoutes.find(e).ifNull { fail { "No handler for ${e.requestURI.path}" } }!!.block(e)
      }.map {
         when (it) {
            is Fut<*> -> it.blockAndGetOrThrow()
            else -> it
         }
      }.ifError { x ->
         logger.error(x) { "Failed to handle http request ${e.requestURI}" }
         e.respond(500, 0) { it.writer().write(x.message ?: "") }
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
               e.requestHeaders.toStringPretty().printIt()
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
            else ->
               e.respond(200, 0) {
                  it.writer().write(Config.json.toJsonValue(it).toPrettyS())
               }
         }
      }
   }

   class Handler(override val nameUi: String, block: (HttpExchange) -> Any?): NameUi {
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

      fun find(request: HttpExchange): Handler? =
         routes.find { request.requestURI.path.startsWith(it.path) }
   }

   companion object: KLogging() {

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
            responseBody.buffered(DEFAULT_BUFFER_SIZE).use { writer(it); it.flush() }
         }
      }

   }
}