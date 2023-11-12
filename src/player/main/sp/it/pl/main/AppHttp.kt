package sp.it.pl.main

import sp.it.util.async.VT as VTe
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import mu.KLogging
import sp.it.pl.core.Core
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Subscription

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
   /** Http server routes, i.e., request handlers. */
   val serverRoutes = Handlers()
   /** Http client of this application. */
   val client get() = cli.value

   fun buildServer(): HttpServer =
      try {
      HttpServer.create(InetSocketAddress(InetAddress.getLocalHost(), 53705), 0).apply {
         runTry {
            server.createContext("/", HttpHandler { r -> serverRoutes.find(r)?.block?.invoke(r) })
            server.executor = VTe
            server.start()
            logger.info { "Http server url=$url started" }
         }.ifError {
            logger.error(it) { "Http server url=$url failed to start" }
         }
      }
      } catch (e: Throwable) {
         e.printStackTrace()
         throw RuntimeException(e)
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
      runTry { cli.orNull()?.close() }.ifError { logger.error(it) { "Failed to close http client" } }
      runTry { ser.orNull()?.stop(0) }.ifError { logger.error(it) { "Failed to close http server" } }
   }

   class Handler(val matcher: (HttpExchange) -> Boolean, val block: (HttpExchange) -> Unit)

   inner class Handlers() {
      protected val routes = CopyOnWriteArrayList<Handler>()

      infix fun route(handler: Handler): Subscription {
         routes += handler
         if (routes.isNotEmpty() && !ser.isInitialized()) server
         return Subscription { routes -= handler }
      }

      fun find(request: HttpExchange): Handler? =
         routes.find { it.matcher(request) }
   }

   companion object: KLogging()
}