package sp.it.pl.main

import io.ktor.client.request.post
import kotlinx.coroutines.job
import mu.KLogging
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
   val onNewInstanceHandlers = Handler1<List<String>>()

   fun start() {
      APP.http.serverRoutes route AppHttp.Handler("/instance-launched") {
         newInstanceLaunched(it.requestBodyAsJs().to<List<String>>())
      }
   }

   fun stop() = Unit

   /**
    * Fires new app instance event. Any instance of this application listening
    * will receive it. Run when application starts.
    */
   fun fireNewInstanceEvent(args: List<String>): Fut<Unit> {
      logger.info { "Sending NewAppInstance($args)" }
      return launch(NEW) {
         APP.http.client.post("127.0.0.1:${APP.http.url.port}/instance-launched") { bodyJs(args) }
      }.job.asFut()
   }

   fun newInstanceLaunched(args: List<String>) {
      logger.info { "NewAppInstance($args)" }
      runFX { onNewInstanceHandlers(args) }
   }

   companion object: KLogging()
}