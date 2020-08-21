@file:Suppress("SpellCheckingInspection")

package hue

import hue.HueGroupType.LightGroup
import hue.HueGroupType.Lightsource
import hue.HueGroupType.Luminaire
import hue.HueGroupType.Zone
import hue.HueSceneType.GroupScene
import hue.HueSceneType.LightScene
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.SwitchingProtocols
import javafx.event.EventHandler
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Side.RIGHT
import javafx.scene.control.ContextMenu
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.control.TitledPane
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KLogging
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.VISUALISATION
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.appProperty
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconUN
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.showFloating
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.form.Validated
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.async.IO
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyIf
import sp.it.util.conf.uiConverterElement
import sp.it.util.conf.uiInfoConverter
import sp.it.util.dev.Blocks
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.printIt
import sp.it.util.file.div
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.div
import sp.it.util.file.json.toPrettyS
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressingAlways
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.type.atomic
import sp.it.util.ui.dsl
import sp.it.util.ui.flowPane
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.scrollPane
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year
import kotlin.coroutines.CoroutineContext

class Hue(widget: Widget): SimpleController(widget) {

   val client = HttpClient(CIO)
   var selectedGroup: HueGroup? = null
   var selectedGroupIcon: Icon? = null
   var selectedBulb: HueBulb? = null
   var selectedBulbIcon: Icon? = null
   val bulbsPane = flowPane(10.emScaled, 10.emScaled)
   val groupsPane = flowPane(10.emScaled, 10.emScaled)
   val scenesPane = flowPane(10.emScaled, 10.emScaled)

   private val hueBridgeUserDevice = "spit-player"
   private var hueBridgeUrl = "http://ip/api/user"
   private val hueBridge = object {

      fun init() =
         runFX { hueBridgeIp }
            .then(IO) { ip ->
               ip.validIpOrNull() ?: ip() ?: fail { "Unable to obtain Phillips Hue bridge ip. Make sure it is turned on and connected to the network." }
            }
            .ui { ip ->
               hueBridgeIp = ip
               ip to hueBridgeApiKey
            }
            .then(IO) { (ip, apiKey) ->
               runBlocking {
                  if (isAuthorizedApiKey(ip, apiKey)) ip to apiKey
                  else ip to createApiKey(ip)
               }
            }.ui { (ip, apiKey) ->
               hueBridgeApiKey = apiKey
               hueBridgeUrl = "http://$ip/api/$apiKey"
            }

      @Blocks
      private fun String.validIpOrNull() = runBlocking {
         val isValid = runTry { client.get<HttpResponse>("http://${this@validIpOrNull}").status }.orNull()==OK
         this@validIpOrNull.takeIf { isValid }
      }

      @Blocks
      private fun ip() = runBlocking {
         val response = client.get<String>("https://discovery.meethue.com/")
         (response.parseToJson().asJsArray()/0/"internalipaddress")?.asJsStringValue().printIt()
      }

      private suspend fun isAuthorizedApiKey(ip: String, apiKey: String): Boolean {
         val response = client.get<HttpResponse>("http://$ip/api/$apiKey/lights")
         failIf(response.status!=OK) { "Failed to check if hue bridge user $apiKey exists" }
         return runTry { (response.readText().parseToJson()/0/"error") }.isError
      }

      private suspend fun createApiKey(ip: String): String {
         logger.info { "Creating Phillips Hue bridge user" }

         val pressCheckDuration = 30.seconds
         var pressed by atomic(false)
         runFX {
            showFloating("Hue bridge linking") { popup ->
               vBox {
                  lay += text("Please press the Phillips Hue bridge link button within ${pressCheckDuration.toUi()}")
                  lay += Icon(IconFA.CHECK).run {
                     onClickDo {
                        popup.hide()
                        pressed = true
                     }
                     withText(RIGHT, "I pressed it, continue...")
                  }
               }
            }
         }
         repeat(pressCheckDuration.toSeconds().toInt() + 5) {
            if (!pressed) delay(1000)
         }

         val userCreated = client.post<HttpResponse>("http://$ip/api") {
            body = JsObject("devicetype" to JsString(hueBridgeUserDevice)).toPrettyS()
         }
         failIf(userCreated.status==SwitchingProtocols) { "Failed to create Phillips Hue bridge user: Link button not pressed" }
         failIf(userCreated.status!=OK) { "Failed to create Phillips Hue bridge user" }
         val apiKey = runTry { (userCreated.readText().parseToJson()/0/"success"/"username")?.asJsStringValue() }.orThrow
         return apiKey ?: fail { "Failed to obtain user Phillips Hue bridge api key" }
      }

      fun bulbs() = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/lights")
         response.parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueBulb>().copy(id = id) }
      }

      fun groups() = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/groups").printIt()
         val groups = response.parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueGroup>().copy(id = id) }
         groups + HueGroup("0", "All", listOf(), HueGroupState(false, false))
      }

      fun scenes() = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/scenes")
         response.parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueScene>().copy(id = id) }
      }

      fun toggle(bulb: HueBulb) = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/lights/${bulb.id}")
         val on = response.parseToJson().to<HueBulb>().state.on
         client.put<String>("$hueBridgeUrl/lights/${bulb.id}/state") {
            body = HueBulbStateEditOn(!on).toJson().toPrettyS()
         }.printIt()
      }

      fun toggle(group: HueGroup) = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/groups/${group.id}")
         val allOn = response.parseToJson().to<HueGroup>().copy(id = group.id).state.all_on
         client.put<String>("$hueBridgeUrl/groups/${group.id}/action") {
            body = HueBulbStateEditOn(!allOn).toJson().toPrettyS()
         }.printIt()
      }

      fun applyLight(bulb: HueBulb, state: HueBulbStateEditLight) = runSuspending {
         client.put<String>("$hueBridgeUrl/lights/${bulb.id}/state") {
            body = state.toJson().asJsObject().withoutNullValues().toPrettyS()
         }.printIt()
      }

      fun applyLight(group: HueGroup, state: HueBulbStateEditLight) = runSuspending {
         client.put<String>("$hueBridgeUrl/groups/${group.id}/action") {
            body = state.toJson().asJsObject().withoutNullValues().toPrettyS()
         }.printIt()
      }

      fun applyScene(scene: HueScene) = runSuspending {
         client.put<String>("$hueBridgeUrl/groups/0/action") {
            body = JsObject("scene" to JsString(scene.id)).toPrettyS()
         }.printIt()
      }

      fun createGroup(group: HueGroupCreate) = runSuspending {
         client.post<String>("$hueBridgeUrl/groups") {
            body = group.toJson().toPrettyS()
         }.printIt()
      }

      fun createScene(scene: HueSceneCreate) = runSuspending {
         client.post<String>("$hueBridgeUrl/scenes") {
            body = scene.toJson().asJsObject().withoutNullValues().toPrettyS()
         }.printIt()
      }

      fun deleteGroup(group: HueGroupId) = runSuspending {
         client.delete<String>("$hueBridgeUrl/groups/$group").printIt()
      }

      fun deleteScene(group: HueSceneId) = runSuspending {
         client.delete<String>("$hueBridgeUrl/scenes/$group").printIt()
      }

   }

   private val color = object: ConfigurableBase<Any?>() {
      var avoidApplying = Suppressor(false)
      val readOnly = v(true)
      val hue by cv(0).readOnlyIf(readOnly).between(0, 65535).def(name = "Hue") attach { applyToSelected(null, it, null) }
      val bri by cv(1).readOnlyIf(readOnly).between(1, 254).def(name = "Brightness") attach { applyToSelected(it, null, null) }
      val sat by cv(0).readOnlyIf(readOnly).between(0, 254).def(name = "Saturation") attach { applyToSelected(null, null, it) }

      fun changeToBulb(bulb: HueBulb) {
         avoidApplying.suppressingAlways {
            bri.value = bulb.state.bri
            hue.value = bulb.state.hue
            sat.value = bulb.state.sat
         }
      }

      fun applyToSelected(bri: Int?, hue: Int?, sat: Int?) {
         avoidApplying.suppressed {
            val state = HueBulbStateEditLight(bri, hue, sat)
            selectedBulb.ifNotNull { hueBridge.applyLight(it, state) }
            selectedGroup.ifNotNull { hueBridge.applyLight(it, state) }
         }
      }
   }

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.lay += vBox(10.emScaled, TOP_LEFT) {
         lay += hBox {
            lay += Icon(IconMA.ROUTER).run {
               tooltip("Link bridge")
               onClickDo { linkBridge() }
               withText(RIGHT, "Bridge")
            }
            lay += Icon(IconFA.REFRESH).run {
               tooltip("Refresh state from bridge")
               onClickDo { refresh() }
               withText(RIGHT, "Refresh")
            }
         }
         lay(ALWAYS) += hBox(30.emScaled, TOP_LEFT) {
            lay(ALWAYS) += scrollPane {
               isFitToWidth = true
               hbarPolicy = NEVER
               vbarPolicy = AS_NEEDED
               content = vBox {
                  lay += TitledPane("Bulbs", bulbsPane)
                  lay += TitledPane("Groups", groupsPane)
                  lay += TitledPane("Scenes", scenesPane)
               }
            }
            lay += form(color)
         }
      }

      root.sync1IfInScene { refresh() } on onClose
      onClose += { client.close() }
   }

   fun Fut<*>.thenRefresh() = ui { refresh() }

   fun linkBridge() {

   }

   fun refresh(): Fut<Any> = hueBridge.init() ui {
      hueBridge.groups() ui { groups ->
         groupsPane.children setTo groups.map { group ->
            Icon(IconFA.LIGHTBULB_ALT, 40.0).run {
               styleclass("hue-group-icon")
               pseudoClassChanged("on", group.state.any_on)

               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) {
                     selectedGroupIcon?.select(false)
                     selectedGroupIcon = this
                     select(true)
                     selectedGroup = group
                     selectedBulb = null
                     selectedBulbIcon = null
                     color.readOnly.value = false
                  }
                  if (it.clickCount==2) {
                     hueBridge.toggle(group)
                     refresh()
                  }
               }
               onContextMenuRequested = EventHandler {
                  if (group.id!="0")
                     ContextMenu().dsl {
                        item("delete") {
                           hueBridge.deleteGroup(group.id).thenRefresh()
                        }
                     }.show(this, RIGHT, 0.0, 0.0)
               }

               withText(group.name)
            }
         }
         groupsPane.children += Icon(IconFA.PLUS).onClickDo {
            hueBridge.bulbs() ui { bulbsAll ->
               object: ConfigurableBase<Any?>(), Validated {
                  var name by c("")
                  var type by c(Zone).uiInfoConverter { it.description }
                  val bulbs by cCheckList(*bulbsAll.toTypedArray()).uiConverterElement { it.name }

                  fun materialize() = HueGroupCreate(name, type.value, this.bulbs.selected(true).map { it.id })

                  override fun isValid() = when {
                     name.isEmpty() -> Try.error("Name can not be empty")
                     bulbs.selected(true).isEmpty() && type in setOf(Luminaire, LightGroup, Lightsource) -> Try.error("$type can not have no bulbs")
                     else -> Try.ok()
                  }
               }.configure("Create group") {
                  hueBridge.createGroup(it.materialize()).thenRefresh()
               }
            }
         }
      }
      hueBridge.bulbs() ui { bulbs ->
         bulbsPane.children setTo bulbs.map { bulb ->
            Icon(IconFA.LIGHTBULB_ALT, 40.0).run {
               styleclass("hue-bulb-icon")
               pseudoClassChanged("on", bulb.state.on)

               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) {
                     selectedBulbIcon?.select(false)
                     selectedBulbIcon = this
                     select(true)
                     selectedBulb = bulb
                     selectedGroup = null
                     selectedGroupIcon = null
                     color.readOnly.value = false
                     color.changeToBulb(bulb)
                  }
                  if (it.clickCount==2) {
                     hueBridge.toggle(bulb).thenRefresh()
                  }
               }
               withText(bulb.name)
            }
         }
      }
      hueBridge.scenes() ui { scenes ->
         scenesPane.children setTo scenes.map { scene ->
            Icon(IconFA.LIGHTBULB_ALT, 40.0).run {
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==2)
                     hueBridge.applyScene(scene)
               }
               onContextMenuRequested = EventHandler {
                  ContextMenu().dsl {
                     item("delete") {
                        hueBridge.deleteScene(scene.id).thenRefresh()
                     }
                  }.show(this, RIGHT, 0.0, 0.0)
               }
               withText(scene.name)
            }
         }
         scenesPane.children += Icon(IconFA.PLUS).onClickDo {
            hueBridge.bulbs() ui { bulbsAll ->
               object: ConfigurableBase<Any?>(), Validated {
                  var name by c("")
                  val bulbs by cCheckList(*bulbsAll.toTypedArray()).uiConverterElement { it.name }

                  fun materialize() = HueSceneCreate(name, this.bulbs.selected(true))

                  override fun isValid() = when {
                     name.isEmpty() -> Try.error("Name can not be empty")
                     this.bulbs.selected(true).isEmpty() -> Try.error("At least one bulb must be selected")
                     else -> Try.ok()
                  }
               }.configure("Create scene") {
                  hueBridge.createScene(it.materialize()).thenRefresh()
               }
            }
         }
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Hue Scenes"
      override val description = "Manages Phillips Hue bulbs, groups & scenes"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(0, 0, 5)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf<ShortcutPane.Entry>()
      override val group = VISUALISATION

      private var hueBridgeApiKey by appProperty("")
      private var hueBridgeIp by appProperty("")

      fun <T> runSuspending(block: suspend CoroutineScope.() -> T) = runIO { runBlocking(block = block) }
      fun <T> runSuspending(context: CoroutineContext, block: suspend CoroutineScope.() -> T) = runIO { runBlocking(context, block) }
      fun JsObject.withoutNullValues() = JsObject(value.filter { it.value !is JsNull })
      fun JsObject.withoutType() = JsObject(value.filter { it.key!="_type" })
      fun Any?.toJson() = APP.serializerJson.json.toJsonValue(this).let {
         when (it) {
            is JsObject -> it.withoutType()
            else -> it
         }
      }

      fun String.parseToJson() = APP.serializerJson.json.ast(this).orThrow
      inline fun <reified T> JsValue.to() = APP.serializerJson.json.fromJsonValue<T>(this).orThrow!!
   }
}

typealias HueBulbId = String
typealias HueGroupId = String
typealias HueSceneId = String

data class HueBridge(val id: String, val name: String)
data class HueBulbStateEditOn(val on: Boolean)
data class HueBulbStateEditLight(val bri: Int?, val hue: Int?, val sat: Int?)
data class HueBulbState(val on: Boolean, val bri: Int, val hue: Int, val sat: Int)
data class HueBulb(val id: HueBulbId = "", val name: String, val productname: String, val state: HueBulbState)
data class HueGroupState(val all_on: Boolean, val any_on: Boolean)
data class HueGroup(val id: HueGroupId = "", val name: String, val lights: List<HueBulbId>, val state: HueGroupState)
data class HueGroupCreate(val name: String, val type: String, val lights: List<HueBulbId>)
data class HueScene(val id: HueSceneId = "", val name: String, val lights: List<HueBulbId>)
data class HueSceneCreate(val name: String, val type: HueSceneType, val group: HueGroupId?, val lights: List<HueBulbId>?, val recycle: Boolean = false) {
   constructor(name: String, lights: List<HueBulb>): this(name, LightScene, null, lights.map { it.id })
   constructor(name: String, group: HueGroupId): this(name, GroupScene, group, null)
}

enum class HueSceneType { LightScene, GroupScene }
enum class HueGroupType(val value: String, val since: Double, val description: String) {
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