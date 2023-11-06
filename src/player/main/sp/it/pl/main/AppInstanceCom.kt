package sp.it.pl.main

import sp.it.util.async.VT as VTe
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
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
import sp.it.util.conf.Config
import sp.it.util.file.json.toCompactS
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Handler1
import sp.it.util.text.lengthInBytes

class AppInstanceCom() {
   private val j = Config.json

   fun start() {
      APP.http.serverHandlers += AppHttp.Handler({ it.requestURI.path=="/instance-launched" }) {
         newInstanceLaunched(j.fromJson<List<String>>(it.requestBody).orThrow)
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
         APP.http.client.post("${APP.http.url}/instance-launched") { setBody(AppHttp.JsContent(args)) }
      }
   }

   fun newInstanceLaunched(args: List<String>) {
      AppHttp.logger.info { "New app instance event received" }
      runFX { onNewInstanceHandlers(args) }
   }

}