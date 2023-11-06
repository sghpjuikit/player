@file:Suppress("EnumEntryName", "SpellCheckingInspection")

package sp.it.pl.plugin

import de.jensd.fx.glyphs.GlyphIcons
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogging
import sp.it.pl.core.InfoUi
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.IconWH
import sp.it.pl.main.emScaled
import sp.it.pl.main.showFloating
import sp.it.pl.main.toUi
import sp.it.pl.plugin.HueSceneType.GroupScene
import sp.it.pl.plugin.HueSceneType.LightScene
import sp.it.pl.plugin.impl.SpeechRecognition
import sp.it.pl.plugin.impl.SpeechRecognition.SpeakHandler
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.password
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.div
import sp.it.util.file.json.toPrettyS
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.Subscription
import sp.it.util.text.equalsNc
import sp.it.util.text.split3
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupId
import sp.it.util.ui.prefSize
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.seconds

/*
 * Hue plugin. See:
 * https://developers.meethue.com/develop/hue-api/
 * https://developers.meethue.com/develop/hue-api/supported-devices/
 */
class Hue: PluginBase() {

   private val scope: CoroutineScope = MainScope()
   private val onClose = Disposer()
   private val speechHandlers = mutableListOf<SpeakHandler>()
   private val client = APP.http.client

   private val hueBridgeApiKey by cv("").password()
      .def(name = "Hue bridge API key", info = "API key of the Phillips Hue bridge. Use linking and button press to pair the application")

   private val hueBridgeIp by cv("")
      .def(name = "Hue bridge IP", info = "IP of the Phillips Hue bridge. Use linking and button press to pair the application")

   val refreshes = Handler1<Unit>()
   val hueBridge = HueBridge()

   override fun start() {
      onClose += {
         APP.plugins.use<SpeechRecognition> { it.handlers -= speechHandlers }
      }
      onClose += APP.plugins.plugin<SpeechRecognition>().attachWhile {
         it.handlers += speechHandlers
         Subscription { it.handlers -= speechHandlers }
      }

      scope.launch(FX) {
         hueBridge.init()
         hueBridge.bulbsAndGroups().net { (_, groups) ->
            APP.plugins.use<SpeechRecognition> {
               it.handlers -= speechHandlers
               speechHandlers.clear()
               speechHandlers += SpeakHandler("Turn all lights on/off", "lights on|off") { text, _ ->
                  if ("command lights on"==text || "command lights off"==text)
                     hueBridge.toggleBulbGroup("0").ui { refreshes(Unit) }
               }
               speechHandlers += SpeakHandler("Turn group lights on/off", "lights \$group-name on|off") { text, _ ->
                  if (text.startsWith("command lights ")) {
                     val gName = text.substringAfter("command lights ").removeSuffix(" on").removeSuffix(" off")
                     val g = groups.find { it.name.lowercase() equalsNc gName }
                     if (g!=null) hueBridge.toggleBulbGroup(g.id).ui { refreshes(Unit) }
                  }
               }
               it.handlers += speechHandlers
            }
         }
      }
   }

   override fun stop() {
      onClose()
      scope.cancel()
   }

   inner class HueBridge: CoroutineScope by scope {
      val hueBridgeUserDevice = "spit-player"
      lateinit var ip: String
      lateinit var apiKey: String
      lateinit var apiVersion: KotlinVersion
      lateinit var url: String

      suspend fun init() {
         ip = hueBridgeIp.value.validIpOrNull() ?: ip() ?: fail { "Unable to obtain Phillips Hue bridge ip. Make sure it is turned on and connected to the network." }
         hueBridgeIp.value = ip
         apiKey = if (isAuthorizedApiKey(ip, hueBridgeApiKey.value)) hueBridgeApiKey.value else createApiKey(ip)
         hueBridgeApiKey.value = apiKey
         apiVersion = apiVersion("http://$ip/api/$apiKey").split3(".").net { (major, minor, patch) -> KotlinVersion(major.toInt(), minor.toInt(), patch.toIntOrNull() ?: 0) }
         url = "http://$ip/api/$apiKey"
      }

      private suspend fun String.validIpOrNull(): String? {
         val isValid = runTry { client.get("http://${this@validIpOrNull}").status }.orNull()==HttpStatusCode.OK
         return this@validIpOrNull.takeIf { isValid }
      }

      private suspend fun ip(): String? {
         val response = client.get("https://discovery.meethue.com/").bodyAsText()
         return (response.parseToJson().asJsArray()/0/"internalipaddress")?.asJsStringValue()
      }

      private suspend fun isAuthorizedApiKey(ip: String, apiKey: String): Boolean {
         val response = client.get("http://$ip/api/$apiKey/lights")
         failIf(response.status!=HttpStatusCode.OK) { "Failed to check if hue bridge user $apiKey exists" }
         return runTry { (response.bodyAsText().parseToJson()/0/"error") }.isError
      }

      private suspend fun createApiKey(ip: String): String {
         logger.info { "Creating Phillips Hue bridge user" }

         val pressCheckDuration = 30.seconds
         var pressed = false
         val popup = showFloating("Hue bridge linking") { popup ->
            vBox {
               lay += text("Please press the Phillips Hue bridge link button within ${pressCheckDuration.toUi()}")
               lay += Icon(IconFA.CHECK).run {
                  onClickDo {
                     popup.hide()
                     pressed = true
                  }
                  withText(Side.RIGHT, "I pressed it, continue...")
               }
            }
         }

         repeat(pressCheckDuration.toSeconds().toInt() + 5) {
            if (!pressed) delay(1000)
         }

         val userCreated = client.post("http://$ip/api") {
            setBody(JsObject("devicetype" to JsString(hueBridgeUserDevice)).toPrettyS())
         }

             if(userCreated.status==HttpStatusCode.SwitchingProtocols) { popup.content.value = hBox { lay += text("Failed to create Phillips Hue bridge user: Link button not pressed") } }
         failIf(userCreated.status==HttpStatusCode.SwitchingProtocols) { "Failed to create Phillips Hue bridge user: Link button not pressed" }
             if(userCreated.status!=HttpStatusCode.OK) { popup.content.value = hBox { lay += text("Failed to create Phillips Hue bridge user") } }
         failIf(userCreated.status!=HttpStatusCode.OK) { "Failed to create Phillips Hue bridge user" }

         val apiKey = runTry { (userCreated.bodyAsText().parseToJson()/0/"success"/"username")?.asJsStringValue() }.orThrow
         return apiKey ?: fail { "Failed to obtain user Phillips Hue bridge api key" }
      }

      private suspend fun apiVersion(url: String): String {
         val response = client.getText("$url/config")
         return response.parseToJson().asJsObject().value["apiversion"]?.asJsString()?.value ?: fail { "Could not obtain api version" }
      }

      suspend fun bulbs(): List<HueBulb> = client.getText("$url/lights")
         .parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueBulb>().copy(id = id) }

      suspend fun groups(): List<HueGroup> = client.getText("$url/groups")
         .parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueGroup>().copy(id = id) }

      suspend fun bulbsAndGroups(): Pair<List<HueBulb>, List<HueGroup>> = (bulbs() to groups()).net { (bulbs, groups) ->
         bulbs to groups + HueGroup("0", "All", listOf(), HueGroupState(bulbs.all { it.state.on }, bulbs.any { it.state.on }), null)
      }

      suspend fun scenes(): List<HueScene> = client.getText("$url/scenes")
         .parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueScene>().copy(id = id) }

      suspend fun sensors(): List<HueSensor> =client.getText("$url/sensors")
         .parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueSensor>().copy(id = id) }

      fun renameBulb(bulb: HueBulbId, name: String) = runSuspendingFx {
         client.put("$url/lights/$bulb") {
            setBody("""{"name": "$name"}""")
         }
      }

      fun changePowerOn(bulb: HueBulbId, powerOn: HueBulbConfPowerOn) = runSuspendingFx {
         client.putText("$url/lights/$bulb/config") {
            setBody("""{ "startup": {"mode": "$powerOn"} }""")
         }
      }

      fun toggleBulb(bulb: HueBulbId) = runSuspendingFx {
         val response = client.getText("$url/lights/$bulb")
         val on = response.parseToJson().to<HueBulb>().state.on
         client.putText("$url/lights/$bulb/state") {
            setBody(HueBulbStateEditOn(!on).toJson().toPrettyS())
         }
      }

      fun toggleBulbGroup(group: HueGroupId) = runSuspendingFx {
         val response = client.getText("$url/groups/$group")
         val allOn = response.parseToJson().to<HueGroup>().copy(id = group).state.all_on
         client.putText("$url/groups/$group/action") {
            setBody(HueBulbStateEditOn(!allOn).toJson().toPrettyS())
         }
      }

      fun applyBulbLight(bulb: HueBulbId, state: HueBulbStateEditLight) = runSuspendingFx {
         client.putText("$url/lights/$bulb/state") {
            setBody(state.toJson().asJsObject().withoutNullValues().toPrettyS())
         }
      }

      fun applyBulbGroupLight(group: HueGroupId, state: HueBulbStateEditLight) = runSuspendingFx {
         client.putText("$url/groups/$group/action") {
            setBody(state.toJson().asJsObject().withoutNullValues().toPrettyS())
         }
      }

      fun applyScene(scene: HueScene) = runSuspendingFx {
         client.putText("$url/groups/0/action") {
            setBody(JsObject("scene" to JsString(scene.id)).toPrettyS())
         }
      }

      fun createGroup(group: HueGroupCreate) = runSuspendingFx {
         client.postText("$url/groups") {
            setBody(group.toJson().toPrettyS())
         }
      }

      fun createScene(scene: HueSceneCreate) = async {
         client.postText("$url/scenes") {
            setBody(scene.toJson().asJsObject().withoutNullValues().toPrettyS())
         }
      }

      fun deleteGroup(group: HueGroupId) = runSuspendingFx {
         client.deleteText("$url/groups/$group")
      }

      fun deleteScene(group: HueSceneId) = runSuspendingFx {
         client.deleteText("$url/scenes/$group")
      }

   }

   companion object: PluginInfo, KLogging() {
      override val name = "Phillips Hue"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false
      override val description =
         "Provides Phillips Hue system integration." +
            "\nManages Phillips Hue bulbs, groups & scenes." +
            "\nAdds voice commands to ${SpeechRecognition.name} plugin." +
            "\nSee https://www.philips-hue.com"

      private fun JsObject.withoutNullValues() = JsObject(value.filter { it.value !is JsNull })

      private fun JsObject.withoutType() = JsObject(value.filter { it.key!="_type" })

      private fun Any?.toJson() = APP.serializerJson.json.toJsonValue(this).let {
         when (it) {
            is JsObject -> it.withoutType()
            else -> it
         }
      }

      private fun String.parseToJson() = APP.serializerJson.json.ast(this).orThrow

      private inline fun <reified T> JsValue.to() = APP.serializerJson.json.fromJsonValue<T>(this).orThrow

      private suspend fun HttpClient.getText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = get(url, block).bodyAsText()
      private suspend fun HttpClient.putText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = put(url, block).bodyAsText()
      private suspend fun HttpClient.postText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = post(url, block).bodyAsText()
      private suspend fun HttpClient.deleteText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = delete(url, block).bodyAsText()

   }

}

typealias HueBulbId = String
typealias HueGroupId = String
typealias HueSceneId = String
typealias HueSensorId = String
typealias HueMap = Map<String, Any?>

class HueIcon<T>(i: GlyphIcons?, size: Double, var hue: T): Icon(i, size)

class HueCell<T>(val node: Node, val icon: HueIcon<T>)

class HueCellNode(icon: Icon, name: String): VBox(5.emScaled) {
   var name: String = name
      set(value) {
         field = value
         lookupId<Label>("nameLabel").text = value
      }

   init {
      prefSize = 90.emScaled x 80.emScaled
      alignment = Pos.TOP_CENTER
      lay += icon.apply {
         focusOwner.value = this@HueCellNode
      }
      lay += label(name) {
         id = "nameLabel"
         isWrapText = true
         textAlignment = TextAlignment.CENTER
      }
   }
}

data class HueBulbStateEditOn(val on: Boolean)

data class HueBulbStateEditLight(val bri: Int?, val hue: Int?, val sat: Int?)

data class HueBulbState(val on: Boolean, val bri: Int, val hue: Int, val sat: Int, val reachable: Boolean)

data class HueBulb(val id: HueBulbId = "", val name: String, val productname: String, val state: HueBulbState, val config: HueMap) {
   val confPowerOn get() = config["startup"]?.asIf<HueMap>()?.get("mode")?.asIs<String>()?.net(HueBulbConfPowerOn::valueOf)
}

data class HueGroupState(val all_on: Boolean, val any_on: Boolean)

data class HueGroup(val id: HueGroupId = "", val name: String, val lights: List<HueBulbId>, val state: HueGroupState, val type: String?)

data class HueGroupCreate(val name: String, val type: String, val lights: List<HueBulbId>)

data class HueScene(val id: HueSceneId = "", val name: String, val lights: List<HueBulbId>)

data class HueSceneCreate(val name: String, val type: HueSceneType, val group: HueGroupId?, val lights: List<HueBulbId>?, val recycle: Boolean = false) {
   constructor(name: String, lights: List<HueBulb>): this(name, LightScene, null, lights.map { it.id })
   constructor(name: String, group: HueGroupId): this(name, GroupScene, group, null)
}

data class HueSensor(val id: HueSensorId = "", val name: String, val type: String, val state: HueMap, val config: HueMap) {
   val stateTemperature get() = state["temperature"]?.asIf<Number>()?.net { it.toDouble()/100 }
   val statePresence get() = state["presence"]?.asIf<Boolean>()
   val stateDaylight get() = state["daylight"]?.asIf<Boolean>()
   val icon: GlyphIcons
      get() = when (type) {
         "ZLLTemperature" -> IconMD.THERMOMETER
         "ZLLPresence" -> IconOC.BROADCAST
         "ZLLLightLevel" -> IconWH.WU_CLEAR
         "Daylight" -> IconMA.TIMELAPSE
         else -> IconMA.SETTINGS_INPUT_ANTENNA
      }
}

enum class HueBulbConfPowerOn(val value: String, override val infoUi: String): InfoUi {
   custom("Custom", "Custom settings defined in custom settings. Will be automatically set when providing “customsettings”. Not available for “On/Off Light”."),
   lastonstate("Last on state", "Light keeps the setting when power failed. If light was off it returns to the last on state."),
   powerfail("Powerfail", "Light keeps the setting when power failed. If light was off it stays off."),
   safety("Safety", "Lights go back to Philips “bright light” safety setting (100% brightness @ 2700K)."),
   unknown("Unknown", "Custom setting is not supported.")
}

enum class HueSceneType { LightScene, GroupScene }

enum class HueGroupType(val value: String, val since: Double, override val infoUi: String): InfoUi {
   Luminaire(
      "Luminaire",
      1.4,
      "" +
         "Multisource luminaire group.\n\nA lighting installation of default groupings of hue " +
         "lights. The bridge will pre-install these groups for ease of use. This type cannot be created manually. " +
         "Also, a light can only be in a maximum of one luminaire group. See multisource luminaires for more info."
   ),
   Lightsource(
      "Lightsource",
      1.4,
      "" +
         "LightSource group.\n\nA group of lights which is created by the bridge based on " +
         "multisource luminaire attributes of Zigbee light resource."
   ),
   LightGroup(
      "LightGroup",
      1.4,
      "" +
         "LightGroup group.\n\nA group of lights that can be controlled together. This is the " +
         "default group type that the bridge generates for user created groups. Default type when no type is given on creation."
   ),
   Room(
      "Room",
      1.11,
      "" +
         "Room.\n\nA group of lights that are physically located in the same place in the house. Rooms " +
         "behave similar as light groups, except: (1) A room can be empty and contain 0 lights, (2) a light is only " +
         "allowed in one room and (3) a room isn’t automatically deleted when all lights in that room are deleted."
   ),
   Entertainment(
      "Entertainment",
      1.22,
      "" +
         "Represents an entertainment setup.\n\n" +
         "Entertainment group describe a group of lights that are used in an entertainment setup. Locations describe " +
         "the relative position of the lights in an entertainment setup. E.g. for TV the position is relative to the " +
         "TV. Can be used to configure streaming sessions.\n\n" +
         "Entertainment group behave in a similar way as light groups, with the exception: it can be empty and contain " +
         "0 lights. The group is also not automatically recycled when lights are deleted. The group of lights can be " +
         "controlled together as in LightGroup."
   ),
   Zone(
      "Zone",
      1.30,
      "" +
         "Zone.\n\nZones describe a group of lights that can be controlled together. Zones can be empty " +
         "and contain 0 lights. A light is allowed to be in multiple zones."
   )
}
