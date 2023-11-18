package sp.it.pl.main

import io.ktor.client.request.post
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.jetbrains.annotations.Blocking
import sp.it.pl.core.bodyJs
import sp.it.pl.core.requestBodyAsJs
import sp.it.pl.core.to
import sp.it.util.async.coroutine.NEW
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.asFut
import sp.it.util.async.coroutine.launch
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.reactive.Handler1

class AppInstanceCom() {
   private val j = Config.json
   private val api = "/instance-launched"
   val onNewInstanceHandlers = Handler1<List<String>>()

   fun start() {
      APP.http.serverRoutes route AppHttp.Handler(api) {
         newInstanceLaunched(it.requestBodyAsJs().to<List<String>>())
      }
   }

   fun stop() = Unit

   /** Fires new app instance event on localhost and blocks thread until response */
   @Blocking
   fun fireNewInstanceEvent(args: List<String>): Unit =
      runBlocking(NEW) {
         logger.info { "Sending NewAppInstance($args)" }
         APP.http.client.post("127.0.0.1:${APP.http.url.port}$api") { bodyJs(args) }
      }

   /** Handles received new app instance event */
   fun newInstanceLaunched(args: List<String>) {
      logger.info { "NewAppInstance($args)" }
      runFX { onNewInstanceHandlers(args) }
   }

   companion object: KLogging()
}