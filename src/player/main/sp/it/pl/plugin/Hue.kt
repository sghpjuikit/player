@file:Suppress("EnumEntryName", "SpellCheckingInspection")

package sp.it.pl.plugin

import de.jensd.fx.glyphs.GlyphIcons
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.SwitchingProtocols
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javax.jmdns.JmDNS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import mu.KLogging
import sp.it.pl.core.InfoUi
import sp.it.pl.core.bodyAsJs
import sp.it.pl.core.bodyJs
import sp.it.pl.core.to
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
import sp.it.pl.plugin.impl.VoiceAssistant
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler
import sp.it.pl.plugin.impl.availableWidgets
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.IO
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.password
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.div
import sp.it.util.file.json.toPrettyS
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.addRem
import sp.it.util.reactive.on
import sp.it.util.text.equalsNcs
import sp.it.util.text.split3
import sp.it.util.text.words
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
   private val client = APP.http.client

   private val hueBridgeApiKey by cv("").password()
      .def(name = "Hue bridge API key", info = "API key of the Phillips Hue bridge. Use linking and button press to pair the application")

   private val hueBridgeIp by cv("")
      .def(name = "Hue bridge IP", info = "IP of the Phillips Hue bridge. Use linking and button press to pair the application")

   val refreshes = Handler1<Unit>()
   val hueBridge = HueBridge()

   override fun start() {
      installSpeechHandlers()
   }

   override fun stop() {
      onClose()
      scope.cancel()
   }

   private fun installSpeechHandlers() {
      val speechHandlers = listOf(
         SpeakHandler("Turn hue lights on/off", "turn? lights on|off?") { text ->
            if (matches(text)) {
               val s = when { text.endsWith("on") -> true; text.endsWith("off") -> false; else -> null }
               hueBridge.init().toggleBulbGroup("0", s).ui { refreshes(Unit) }
               Ok("Ok")
            } else
               null
         },
         SpeakHandler("List hue light bulbs", "list light bulbs") { text ->
            if (matches(text)) Ok("The available light bulbs are: " + hueBridge.init().bulbs().joinToString(", ") { it.name })
            else null
         },
         SpeakHandler("Turn hue light bulb on/off", "turn? light bulb \$bulb-name on|off?") { text ->
            if (matches(text)) {
               val bName = text.substringAfter("light bulb ")
                  .replace("on ", "").replace(" on", "")
                  .replace("off ", "").replace(" off", "")
                  .replace("_", " ")
               val s = when { text.endsWith("on") -> true; text.endsWith("off") -> false; else -> null }
               val bulbs = hueBridge.init().bulbs()
               val b = bulbs.find { it.name.lowercase() equalsNcs bName }
               if (b!=null) hueBridge.toggleBulb(b.id, s).ui { refreshes(Unit) }
               if (b!=null) Ok("Ok")
               else Error("No Light Bulb $bName available")
            } else
               null
         },
         SpeakHandler("List hue light scenes", "list light scenes") { text ->
            if (text == "list light scenes") Ok("The available light scenes are: " + hueBridge.init().scenes().joinToString(", ") { it.name })
            else null
         },
         SpeakHandler("Set hue lights scene", "lights scene \$scene-name") { text ->
            if (text.startsWith("lights scene ")) {
               val sName = text.substringAfter("lights scene ").removeSuffix(" on").removeSuffix(" off").replace("_", " ")
               val scenes = hueBridge.init().scenes()
               val s = scenes.find { it.name.lowercase() equalsNcs sName }
               if (s!=null) hueBridge.applyScene(s).ui { refreshes(Unit) }
               if (s!=null) Ok("Ok")
               else Error("No Light Scene $sName available")
            } else
               null
         },
         SpeakHandler("List hue light groups", "list light groups") { text ->
            if (matches(text)) Ok("The available light groups are: " + hueBridge.init().bulbsAndGroups().second.joinToString(", ") { it.name })
            else null
         },
         SpeakHandler("Turn hue light group on/off", "turn? lights group? \$group-name on|off?") { text ->
            if (matches(text)) {
               val gName = text.substringAfter("lights ")
                  .replace("group ", "").replace("in ", "")
                  .replace("on ", "").replace(" on", "")
                  .replace("off ", "").replace(" off", "")
                  .replace("_", " ")
               val (s, ss) = when { text.endsWith("on") -> true to "on"; text.endsWith("off") -> false to "off"; else -> null to "" }
               val (_, groups) = hueBridge.init().bulbsAndGroups()
               val groupsCsv = groups.map { "- ${it.name.lowercase()}" }.joinToString("\n")
               val g = groups.find { it.name.lowercase() equalsNcs gName }
               if (g!=null) hueBridge.toggleBulbGroup(g.id, s).ui { refreshes(Unit) }
               if (g!=null) Ok("Ok")
               else if (intent) intent(text, "$groupsCsv\n- unidentified // no recognized function", gName) { this("lights group $it $ss") }
               else confirming("No Light Group $gName available. Please repeat the group name", "\$group") { gName2 ->
                  var groupIsValid = groups.find { it.name.lowercase() equalsNcs gName2.lowercase() }!=null
                  if (groupIsValid) this@SpeakHandler.copy(handler, plugin)("turn lights group $gName2 $ss")!!
                  else confirming("No such group. Would you like me to list available groups?", "yes") {
                     Ok("The available light groups are: " + groups.joinToString(", ") { it.name })
                  }
               }
            } else
               if (!intent) Error("No such Light Group available.")
               else null
         },
      )

      APP.plugins.plugin<VoiceAssistant>().syncWhile { it.handlers addRem speechHandlers } on onClose
   }

   inner class HueBridge: CoroutineScope by scope {
      val hueBridgeUserDevice = "spit-player"
      lateinit var ip: String
      lateinit var apiKey: String
      lateinit var apiVersion: KotlinVersion
      lateinit var url: String

      suspend fun init(): HueBridge {
         ip = hueBridgeIp.value.validIpOrNull() ?: ip() ?: fail { "Unable to obtain Phillips Hue bridge ip. Make sure it is turned on and connected to the network." }
         hueBridgeIp.value = ip
         apiKey = if (isAuthorizedApiKey(ip, hueBridgeApiKey.value)) hueBridgeApiKey.value else createApiKey(ip)
         hueBridgeApiKey.value = apiKey
         apiVersion = apiVersion("http://$ip/api/$apiKey").split3(".").net { (major, minor, patch) -> KotlinVersion(major.toInt(), minor.toInt(), patch.toIntOrNull() ?: 0) }
         url = "http://$ip/api/$apiKey"
         return this
      }

      private suspend fun String.validIpOrNull(): String? {
         val isValid = runTry { client.get("http://${this@validIpOrNull}").status }.orNull()==OK
         return this@validIpOrNull.takeIf { isValid }
      }

      private suspend fun ip(): String? {
         return null
            // use mDNS
            ?: IO {
               JmDNS.create().use { it.list("_hue._tcp.local.").firstOrNull()?.hostAddresses?.firstOrNull() }
            }
            // use meethue cloud
            ?: (client.get("https://discovery.meethue.com/").bodyAsJs().asJsArray()/0/"internalipaddress")?.asJsStringValue()
      }

      private suspend fun isAuthorizedApiKey(ip: String, apiKey: String): Boolean {
         val response = client.get("http://$ip/api/$apiKey/lights")
         failIf(response.status!=OK) { "Failed to check if hue bridge user $apiKey exists" }
         return runTry { (response.bodyAsJs()/0/"error") }.isError
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

             if(userCreated.status==SwitchingProtocols) { popup.content.value = hBox { lay += text("Failed to create Phillips Hue bridge user: Link button not pressed") } }
         failIf(userCreated.status==SwitchingProtocols) { "Failed to create Phillips Hue bridge user: Link button not pressed" }
             if(userCreated.status!=OK) { popup.content.value = hBox { lay += text("Failed to create Phillips Hue bridge user") } }
         failIf(userCreated.status!=OK) { "Failed to create Phillips Hue bridge user" }

         val apiKey = runTry { (userCreated.bodyAsJs()/0/"success"/"username")?.asJsStringValue() }.orThrow
         return apiKey ?: fail { "Failed to obtain user Phillips Hue bridge api key" }
      }

      private suspend fun apiVersion(url: String): String =
         client.get("$url/config").bodyAsJs().asJsObject().value["apiversion"]?.asJsString()?.value ?: fail { "Could not obtain api version" }

      suspend fun api(): HueApi =
         client.get("$url/config").bodyAsJs().to<HueApi>()

      suspend fun bulbs(): List<HueBulb> =
         client.get("$url/lights").bodyAsJs().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueBulb>().copy(id = id) }

      suspend fun groups(): List<HueGroup> =
         client.get("$url/groups").bodyAsJs().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueGroup>().copy(id = id) }

      suspend fun bulbsAndGroups(): Pair<List<HueBulb>, List<HueGroup>> =
         (bulbs() to groups()).net { (bulbs, groups) ->
            bulbs to groups + HueGroup("0", "All", listOf(), HueGroupState(bulbs.all { it.state.on }, bulbs.any { it.state.on }), null)
         }

      suspend fun scenes(): List<HueScene> =
         client.get("$url/scenes").bodyAsJs().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueScene>().copy(id = id) }

      suspend fun sensors(): List<HueSensor> =
         client.get("$url/sensors").bodyAsJs().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueSensor>().copy(id = id) }

      fun renameBulb(bulb: HueBulbId, name: String) = runSuspendingFx {
         client.put("$url/lights/$bulb") {
            setBody("""{"name": "$name"}""")
         }
      }

      fun changePowerOn(bulb: HueBulbId, powerOn: HueBulbConfPowerOn) = runSuspendingFx {
         client.put("$url/lights/$bulb/config") {
            setBody("""{ "startup": {"mode": "$powerOn"} }""")
         }
      }

      fun flashBulb(bulb: HueBulbId) = runSuspendingFx {
         client.put("$url/lights/$bulb/state") {
            setBody("""{ "alert": "select" }""")
         }
      }

      fun toggleBulb(bulb: HueBulbId, onlyTo: Boolean? = null) = runSuspendingFx {
         val on = client.get("$url/lights/$bulb").bodyAsJs().to<HueBulb>().state.on
         if (onlyTo==on) return@runSuspendingFx
         client.put("$url/lights/$bulb/state") {
            bodyJs(HueBulbStateEditOn(onlyTo ?: !on).toJson())
         }
      }

      fun toggleBulbGroup(group: HueGroupId, onlyTo: Boolean? = null) = runSuspendingFx {
         val (allOn, anyOn) = client.get("$url/groups/$group").bodyAsJs().to<HueGroup>().copy(id = group).state.net { it.all_on to it.any_on }
         if (onlyTo==true && allOn) return@runSuspendingFx
         if (onlyTo==false && !anyOn) return@runSuspendingFx
         client.put("$url/groups/$group/action") {
            bodyJs(HueBulbStateEditOn(onlyTo ?: !allOn).toJson())
         }
      }

      fun applyBulbLight(bulb: HueBulbId, state: HueBulbStateEditLight) = runSuspendingFx {
         client.put("$url/lights/$bulb/state") {
            bodyJs(state.toJson().asJsObject().withoutNullValues())
         }
      }

      fun applyBulbGroupLight(group: HueGroupId, state: HueBulbStateEditLight) = runSuspendingFx {
         client.put("$url/groups/$group/action") {
            bodyJs(state.toJson().asJsObject().withoutNullValues())
         }
      }

      fun applyScene(scene: HueScene) = runSuspendingFx {
         client.put("$url/groups/0/action") {
            bodyJs(JsObject("scene" to JsString(scene.id)))
         }
      }

      fun createGroup(group: HueGroupCreate) = runSuspendingFx {
         client.post("$url/groups") {
            bodyJs(group.toJson())
         }
      }

      fun createScene(scene: HueSceneCreate) = async {
         client.post("$url/scenes") {
            bodyJs(scene.toJson().asJsObject().withoutNullValues())
         }
      }

      fun deleteGroup(group: HueGroupId) = runSuspendingFx {
         client.delete("$url/groups/$group")
      }

      fun deleteScene(group: HueSceneId) = runSuspendingFx {
         client.delete("$url/scenes/$group")
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
            "\nAdds voice commands to ${VoiceAssistant.name} plugin." +
            "\nSee https://www.philips-hue.com"

      private fun JsObject.withoutNullValues() = JsObject(value.filter { it.value !is JsNull })

      private fun JsObject.withoutType() = JsObject(value.filter { it.key!="_type" })

      private fun Any?.toJson() = APP.serializerJson.json.toJsonValue(this).let {
         when (it) {
            is JsObject -> it.withoutType()
            else -> it
         }
      }

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
data class HueApi(
   val name: String,
   val datastoreversion: String,
   val swversion: String,
   val apiversion: String,
   val mac: String,
   val bridgeid: String,
   val factorynew: Boolean,
   val replacesbridgeid: String?,
   val modelid: String?,
   val starterkitid: String?,
)

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
