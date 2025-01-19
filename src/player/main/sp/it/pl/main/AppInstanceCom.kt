package sp.it.pl.main

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.Blocking
import sp.it.pl.core.bodyJs
import sp.it.pl.core.requestBodyAsJs
import sp.it.pl.core.to
import sp.it.util.async.coroutine.NEW
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.reactive.Handler1

class AppInstanceCom() {
   private val logger = KotlinLogging.logger { }
   private val j = Config.json
   private val api = "/instance-launched"
   val onNewInstanceHandlers = Handler1<List<String>>()

   fun start() {
      APP.http.serverRoutes route AppHttp.Handler(api, method = "POST") {
         newInstanceLaunched(it.requestBodyAsJs().to<List<String>>())
      }
   }

   fun stop() = Unit

   /** Fires new app instance event on localhost and blocks thread until response */
   @Blocking
   fun fireNewInstanceEvent(args: List<String>): Unit =
      runBlocking(NEW) {
         logger.info { "Sending NewAppInstance(args=$args)" }
         APP.http.client.post("http://127.0.0.1:${APP.http.url.port}$api") { bodyJs(args) }
      }

   /** Handles received new app instance event */
   fun newInstanceLaunched(args: List<String>) {
      logger.info { "NewAppInstance(args=$args)" }
      runFX { onNewInstanceHandlers(args) }
   }

}