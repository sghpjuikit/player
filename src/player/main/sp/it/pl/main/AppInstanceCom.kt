package sp.it.pl.main

import io.ktor.client.request.post
import sp.it.pl.core.bodyJs
import sp.it.pl.core.requestBodyAsJs
import sp.it.pl.core.to
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.launch
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.reactive.Handler1

class AppInstanceCom() {
   private val j = Config.json

   fun start() {
      APP.http.serverRoutes route AppHttp.Handler({ it.requestURI.path=="/instance-launched" }) {
         newInstanceLaunched(it.requestBodyAsJs().to<List<String>>())
      }
   }

   fun stop() = Unit

   val onNewInstanceHandlers = Handler1<List<String>>()

   /**
    * Fires new app instance event. Any instance of this application listening
    * will receive it. Run when application starts.
    */
   fun fireNewInstanceEvent(args: List<String>) {
      AppHttp.logger.info { "New app instance event sent" }
      launch(VT) {
         APP.http.client.post("${APP.http.url}/instance-launched") { bodyJs(args) }
      }
   }

   fun newInstanceLaunched(args: List<String>) {
      AppHttp.logger.info { "New app instance event received" }
      runFX { onNewInstanceHandlers(args) }
   }

}