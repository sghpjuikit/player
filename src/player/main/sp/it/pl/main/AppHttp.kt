package sp.it.pl.main

import sp.it.util.async.VT as VTe
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import java.net.InetSocketAddress
import mu.KLogging
import sp.it.pl.core.Core
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry

/** Application http server & http client. */
class AppHttp(
   private val port: Int
): Core {
   private val ser = lazy { buildServer() }
   private val cli = lazy { buildClient() }

   /** Url of this application */
   val url = "http://127.0.0.1:$port"
   /** Http sever of this application, does not support https. */
   val server get() = ser.value
   /** Http server request handlers. */
   val serverHandlers = mutableListOf<Handler>()
   /** Http client of this application. */
   val client get() = cli.value

   fun buildServer(): HttpServer =
      HttpServer.create(InetSocketAddress(port), 0).apply {
         server.createContext("/", HttpHandler { r -> serverHandlers.find { it.matcher(r) }?.block?.invoke(r) })
         server.executor = VTe
         server.start()
      }

   fun buildClient(): HttpClient =
      HttpClient(Java).apply {
         config {
            expectSuccess = true
            install(HttpTimeout)
         }
      }

   override fun dispose() {
      runTry { cli.orNull()?.close() }.ifError { logger.error(it) { "Failed to close http client" } }
      runTry { ser.orNull()?.stop(0) }.ifError { logger.error(it) { "Failed to close http server" } }
   }

   class Handler(val matcher: (HttpExchange) -> Boolean, val block: (HttpExchange) -> Unit)

   companion object: KLogging()
}