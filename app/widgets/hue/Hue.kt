@file:Suppress("SpellCheckingInspection")

package hue

import de.jensd.fx.glyphs.GlyphIcons
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
import java.awt.Color.HSBtoRGB
import javafx.event.EventHandler
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Side.RIGHT
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.control.TitledPane
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode.F5
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.TextAlignment
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
import sp.it.util.conf.uiConverterElement
import sp.it.util.conf.uiInfoConverter
import sp.it.util.dev.Blocks
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
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
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.text.keys
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.IconWH
import sp.it.pl.main.textColon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.pyth
import sp.it.util.conf.EditMode.NONE
import sp.it.util.conf.cvn
import sp.it.util.conf.lengthMax
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.math.clip
import sp.it.util.math.min
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.text.capitalLower
import sp.it.util.text.nameUi
import sp.it.util.ui.centre
import sp.it.util.ui.label
import sp.it.util.ui.lookupId
import sp.it.util.ui.stackPane

class Hue(widget: Widget): SimpleController(widget) {

   val client = HttpClient(CIO)
   val hueBulbCells = HashMap<HueBulbId, HueCell<HueBulb>>()
   val hueBulbGroupCells = HashMap<HueGroupId, HueCell<HueGroup>>()
   var selectedGroupId: HueGroupId? = null
   var selectedGroupIcon: HueIcon<HueGroup>? = null
   var selectedBulbId: HueBulbId? = null
   var selectedBulbIcon: HueIcon<HueBulb>? = null
   val bulbsPane = flowPane(10.emScaled, 10.emScaled)
   val groupsPane = flowPane(10.emScaled, 10.emScaled)
   val scenesPane = flowPane(10.emScaled, 10.emScaled)
   val sensorsPane = flowPane(10.emScaled, 10.emScaled)

   private val hueBridgeUserDevice = "spit-player"
   private var hueBridgeUrl = "http://ip/api/user"
   private val hueBridge = object {

      fun init() =
         runFX { hueBridgeIp }
            .then(IO) { ip ->
               ip.validIpOrNull() ?: ip()
               ?: fail { "Unable to obtain Phillips Hue bridge ip. Make sure it is turned on and connected to the network." }
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
         (response.parseToJson().asJsArray()/0/"internalipaddress")?.asJsStringValue()
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
         val response = client.get<String>("$hueBridgeUrl/groups")
         val groups = response.parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueGroup>().copy(id = id) }
         groups + HueGroup("0", "All", listOf(), HueGroupState(false, false))
      }

      fun scenes() = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/scenes")
         response.parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueScene>().copy(id = id) }
      }

      fun sensors() = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/sensors")
         response.parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueSensor>().copy(id = id) }
      }

      fun renameBulb(bulb: HueBulbId, name: String) = runSuspending {
         client.put<String>("$hueBridgeUrl/lights/$bulb") {
            body = """{"name": "$name"}"""
         }
      }

      fun toggleBulb(bulb: HueBulbId) = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/lights/$bulb")
         val on = response.parseToJson().to<HueBulb>().state.on
         client.put<String>("$hueBridgeUrl/lights/$bulb/state") {
            body = HueBulbStateEditOn(!on).toJson().toPrettyS()
         }
      }

      fun toggleBulbGroup(group: HueGroupId) = runSuspending {
         val response = client.get<String>("$hueBridgeUrl/groups/$group")
         val allOn = response.parseToJson().to<HueGroup>().copy(id = group).state.all_on
         client.put<String>("$hueBridgeUrl/groups/$group/action") {
            body = HueBulbStateEditOn(!allOn).toJson().toPrettyS()
         }
      }

      fun applyBulbLight(bulb: HueBulbId, state: HueBulbStateEditLight) = runSuspending {
         client.put<String>("$hueBridgeUrl/lights/$bulb/state") {
            body = state.toJson().asJsObject().withoutNullValues().toPrettyS()
         }
      }

      fun applyBulbGroupLight(group: HueGroupId, state: HueBulbStateEditLight) = runSuspending {
         client.put<String>("$hueBridgeUrl/groups/$group/action") {
            body = state.toJson().asJsObject().withoutNullValues().toPrettyS()
         }
      }

      fun applyScene(scene: HueScene) = runSuspending {
         client.put<String>("$hueBridgeUrl/groups/0/action") {
            body = JsObject("scene" to JsString(scene.id)).toPrettyS()
         }
      }

      fun createGroup(group: HueGroupCreate) = runSuspending {
         client.post<String>("$hueBridgeUrl/groups") {
            body = group.toJson().toPrettyS()
         }
      }

      fun createScene(scene: HueSceneCreate) = runSuspending {
         client.post<String>("$hueBridgeUrl/scenes") {
            body = scene.toJson().asJsObject().withoutNullValues().toPrettyS()
         }
      }

      fun deleteGroup(group: HueGroupId) = runSuspending {
         client.delete<String>("$hueBridgeUrl/groups/$group")
      }

      fun deleteScene(group: HueSceneId) = runSuspending {
         client.delete<String>("$hueBridgeUrl/scenes/$group")
      }

   }

   private val infoPane = vBox(30.emScaled)
   private val devicePane = vBox(0, TOP_LEFT)
   private val color = object: ConfigurableBase<Any?>() {
      val avoidApplying = Suppressor()
      val readOnly = v(true)
      val color by cvn<Color>(null).def(name = "Color", editable = NONE)
      val hue by cv(0).between(0, 65535).def(name = "Color Hue", editable = NONE)
      val sat by cv(0).between(0, 254).def(name = "Color Saturation", editable = NONE)
      val bri by cv(1).between(1, 254).def(name = "Color Brightness", editable = NONE)
      val configurable = this
      var isDragged = false
      val selectorRadius = 75.emScaled
      val selector = Circle(5.emScaled).apply {
         isManaged = false
         isMouseTransparent = true
         isVisible = false
         stroke = Color.BLACK
         strokeWidth = 1.0
      }
      val node = vBox {
         alignment = TOP_CENTER
         lay += form(configurable)
         lay += stackPane {
            this.lay += Thumbnail(selectorRadius*2, selectorRadius*2).run {
               loadImage(drawGradientCir(selectorRadius.toInt()))

               fun updateFromMouse(it: MouseEvent) {
                  if (!readOnly.value) {
                     val d = pane.layoutBounds.centre.distance(it.x x it.y)
                     if (d < selectorRadius) {
                        val o = 2-(d/selectorRadius).clip(0.5, 1.0)*2
                        val c = image.value!!.pixelReader.getColor(it.x.toInt().clip(0, selectorRadius.toInt()*2), it.y.toInt().clip(0, selectorRadius.toInt()*2)).deriveColor(0.0, 1.0, 1.0, o)
                        val cBri = 1 + ((c.opacity) * 253).toInt()
                        val cHue = (c.hue/360.0*65535).toInt()
                        val cSat = (c.saturation*245).toInt()
                        changeToBulb(HueBulb("", "", "", HueBulbState(true, cBri, cHue, cSat, true)))

                        if (!isDragged) applyToSelected(cBri, cHue, cSat)
                     }
                  }
               }
               pane.onEventDown(MOUSE_PRESSED, PRIMARY) { isDragged = true }
               pane.onEventDown(MOUSE_RELEASED, PRIMARY) { isDragged = false }
               pane.onEventDown(MOUSE_RELEASED, PRIMARY) { updateFromMouse(it) }
               pane.onEventDown(MOUSE_DRAGGED, PRIMARY) { if (isDragged) updateFromMouse(it) }

               pane
            }
            this.lay += selector
         }
      }

      @Suppress("UNUSED_PARAMETER")
      fun changeToBulbGroup(group: HueGroup) {
         avoidApplying.suppressing {
            color.value = null
            hue.value = 0
            sat.value = 0
            bri.value = 1
            selector.isVisible = false
         }
      }
      fun changeToBulb(bulb: HueBulb) {
         avoidApplying.suppressing {
            val c = Color.hsb(bulb.state.hue.toDouble()*360.0/65535.0, bulb.state.sat.toDouble()/254.0, 1.0, bulb.state.bri.minus(1).toDouble()/253.0)
            color.value = c
            hue.value = bulb.state.hue
            sat.value = bulb.state.sat
            bri.value = bulb.state.bri
            selector.isVisible = true
            selector.centerX = selector.parent.layoutBounds.centerX + cos(c.hue/360*2*PI + PI)*(selectorRadius*(if (c.opacity==1.0) 0.5*c.saturation else 1 - 0.5*c.opacity))
            selector.centerY = selector.parent.layoutBounds.centerY + sin(c.hue/360*2*PI + PI)*(selectorRadius*(if (c.opacity==1.0) 0.5*c.saturation else 1 - 0.5*c.opacity))
            selector.fill = c
         }
      }

      fun applyToSelected(bri: Int?, hue: Int?, sat: Int?) {
         avoidApplying.suppressed {
            val state = HueBulbStateEditLight(bri, hue, sat)
            selectedBulbId.ifNotNull { hueBridge.applyBulbLight(it, state) }
            selectedGroupId.ifNotNull { hueBridge.applyBulbGroupLight(it, state) }
         }
      }
   }

   init {
      root.prefSize = 900.emScaled x 600.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()
      root.onEventDown(KEY_PRESSED, F5) { refresh() }
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
                  lay += TitledPane("Sensors", sensorsPane)
               }
            }
            lay += infoPane.apply {
               prefWidth = 200.emScaled
            }
         }
      }

      root.sync1IfInScene { refresh() } on onClose
      onClose += { client.close() }
   }

   fun Fut<*>.thenRefresh() = ui { refresh() }

   fun linkBridge() {

   }

   fun refresh(): Fut<Any> = hueBridge.init() ui {
      fun unfocusBulb() {
         selectedBulbIcon?.pseudoClassChanged("edited", false)
         selectedBulbIcon = null
         selectedBulbId = null
         color.readOnly.value = true
         infoPane.lay -= color.node
      }
      fun unfocusBulbGroup() {
         selectedGroupIcon?.hue?.lights?.forEach { hueBulbCells[it]?.icon?.pseudoClassChanged("edited-group", false) }
         selectedGroupIcon?.pseudoClassChanged("edited", false)
         selectedGroupIcon = null
         selectedGroupId = null
         color.readOnly.value = true
         infoPane.lay -= color.node
      }
      fun unfocusSensor() {
         infoPane.lay -= devicePane
      }

      hueBridge.groups() ui { groups ->
         groupsPane.children setTo groups.map { group ->
            hueBulbGroupCells.getOrPut(group.id) {
               HueIcon(IconFA.LIGHTBULB_ALT, 40.0, group).run {
                  styleclass("hue-group-icon")

                  fun toggleBulbGrouo() = hueBridge.toggleBulbGroup(group.id).thenRefresh()
                  fun deleteBulbGrouo() = hueBridge.deleteGroup(group.id).thenRefresh()
                  fun focusBulbGroup() {
                     unfocusSensor()
                     unfocusBulb()
                     selectedGroupIcon?.hue?.lights?.forEach { hueBulbCells[it]?.icon?.pseudoClassChanged("edited-group", false) }
                     selectedGroupIcon?.pseudoClassChanged("edited", false)
                     selectedGroupIcon = this
                     selectedGroupId = group.id
                     pseudoClassChanged("edited", true)
                     hue.lights.forEach { hueBulbCells[it]?.icon?.pseudoClassChanged("edited-group", true) }
                     color.readOnly.value = false
                     infoPane.lay += color.node
                     color.changeToBulbGroup(hue)
                  }

                  focusedProperty() attach { focusBulbGroup() }

                  onEventDown(KEY_PRESSED, SPACE) { toggleBulbGrouo() }
                  onEventDown(MOUSE_CLICKED, PRIMARY) {
                     if (it.clickCount==1) focusBulbGroup()
                     if (it.clickCount==2) toggleBulbGrouo()
                  }
                  onContextMenuRequested = EventHandler {
                     if (hue.id!="0")
                        ContextMenu().dsl {
                           item("Toggle bulbs on/off    (${keys("SPACE")})") { toggleBulbGrouo() }
                           item("Delete (${keys("DELETE")})") { deleteBulbGrouo() }
                        }.show(this, RIGHT, 0.0, 0.0)
                  }

                  HueCell(HueCellNode(this, group.name), this)
               }
            }.run {
               icon.hue = group
               icon.pseudoClassChanged("on", group.state.any_on)

               node
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
            hueBulbCells.getOrPut(bulb.id) {
               HueIcon(IconFA.LIGHTBULB_ALT, 40.0, bulb).run {
                  styleclass("hue-bulb-icon")

                  fun rename() {
                     object: ConfigurableBase<Any?>() {
                        val name by cv(hue.name).lengthMax(32)
                     }.configure("Rename bulb") {
                        hueBridge.renameBulb(bulb.id, it.name.value).thenRefresh()
                     }
                  }
                  fun toggleBulb() = hueBridge.toggleBulb(bulb.id).thenRefresh()
                  fun focusBulb() {
                     unfocusSensor()
                     unfocusBulbGroup()
                     selectedBulbIcon?.pseudoClassChanged("edited", false)
                     selectedBulbIcon = this
                     selectedBulbId = bulb.id
                     pseudoClassChanged("edited", true)
                     color.readOnly.value = false
                     infoPane.lay += color.node
                     color.changeToBulb(hue)
                  }

                  focusedProperty() attach { focusBulb() }
                  onEventDown(KEY_PRESSED, SPACE) { toggleBulb() }
                  onEventDown(MOUSE_CLICKED, PRIMARY) {
                     if (it.clickCount==1) focusBulb()
                     if (it.clickCount==2) toggleBulb()
                  }
                  onContextMenuRequested = EventHandler {
                     ContextMenu().dsl {
                        item("Rename") { rename() }
                        item("Toggle on/off (${keys("SPACE")})") { toggleBulb() }
                     }.show(this, RIGHT, 0.0, 0.0)
                  }

                  HueCell(HueCellNode(this, bulb.name), this)
               }
            }.run {
               icon.hue = bulb
               icon.pseudoClassChanged("unreachable", !bulb.state.reachable)
               icon.pseudoClassChanged("on", bulb.state.on)
               icon.isDisable = !bulb.state.reachable
               node.asIs<HueCellNode>().name = bulb.name

               node
            }
         }
      }
      hueBridge.scenes() ui { scenes ->
         scenesPane.children setTo scenes.map { scene ->
            Icon(IconFA.LIGHTBULB_ALT, 40.0).run {
               fun focusScene() {
                  unfocusSensor()
                  unfocusBulbGroup()
                  unfocusBulb()
               }

               focusedProperty() attach { focusScene() }
               onEventDown(KEY_PRESSED, SPACE) { focusScene() }
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) focusScene()
                  if (it.clickCount==2) hueBridge.applyScene(scene)
               }
               onContextMenuRequested = EventHandler {
                  ContextMenu().dsl {
                     item("delete") {
                        hueBridge.deleteScene(scene.id).thenRefresh()
                     }
                  }.show(this, RIGHT, 0.0, 0.0)
               }
               HueCellNode(this, scene.name)
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
      hueBridge.sensors() ui { scenes ->
         sensorsPane.children setTo scenes.map { sensor ->
            val icon = when (sensor.type) {
               "ZLLTemperature" -> IconMD.THERMOMETER
               "ZLLPresence" -> IconOC.BROADCAST
               "ZLLLightLevel" -> IconWH.WU_CLEAR
               "Daylight" -> IconWH.MOON_ALT_WAXING_GIBBOUS_2
               else -> IconMA.SETTINGS_INPUT_ANTENNA
            }
            fun focusSensor() {
               unfocusSensor()
               unfocusBulbGroup()
               unfocusBulb()
               infoPane.lay += devicePane

               devicePane.lay.clear()
               when (sensor.type) {
                  "ZLLTemperature" -> {
                     sensor.state["temperature"]?.asIf<Number>().ifNotNull { devicePane.lay += textColon("Temperature", "${it.toDouble()/100}°C") }
                  }
                  "ZLLPresence" -> {
                     sensor.state["presence"]?.asIf<Boolean>().ifNotNull { devicePane.lay += textColon("Presence", it) }
                  }
                  "Daylight" -> {
                     sensor.state["daylight"]?.asIf<Boolean>().ifNotNull { devicePane.lay += textColon("Is daylight", it) }
                  }
                  else -> IconMA.SETTINGS_INPUT_ANTENNA
               }
               if (devicePane.lay.children.isNotEmpty()) devicePane.lay += label()
               sensor.config.entries.sortedBy { it.key }.forEach { (name, value) ->
                  devicePane.lay += textColon(name.capitalLower(), value)
               }
            }

            Icon(icon, 40.0).run {
               styleclass("hue-sensor-icon")
               pseudoClassChanged("unreachable", sensor.config["reachable"]?.asIf<Boolean>() ?: false)
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) focusSensor()
               }
               HueCellNode(this, sensor.name)
            }
         }
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Hue Scenes"
      override val description = "Manages Phillips Hue bulbs, groups & scenes"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(0, 0, 6)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         Entry("Data", "Refresh", F5.nameUi),
      )
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


      fun drawGradientCir(radius: Int): Image {
         return WritableImage(2*radius, 2*radius).apply {
            val c = (radius).toDouble()
            val opacityMask = (255 shl 16) or (255 shl 8) or 255
            val pixels = IntArray(2*radius*2*radius) { i ->
               val x = i.rem(2*radius)
               val y = i.floorDiv(2*radius)
               val dx = x-c
               val dy = y-c
               val d = pyth(dx, dy).min(c)/c
               val a = atan2(dy, dx).plus(PI)/2/PI
               if (d<=0.5) HSBtoRGB(a.toFloat(), (d*2).toFloat(), 1f)
               else HSBtoRGB(a.toFloat(), 1f, 1f) and ((((2f-2*d)*255 + .5).toInt() shl 24) or opacityMask)
            }
            pixelWriter.setPixels(0, 0, 2*radius, 2*radius, PixelFormat.getIntArgbInstance(), pixels, 0, 2*radius)
         }
      }
      fun drawGradientRec(radius: Int): Image {
         return WritableImage(2*radius, 2*radius).apply {
            val c = (radius).toDouble()
            val opacityMask = (255 shl 16) or (255 shl 8) or 255
            val pixels = IntArray(2*radius*2*radius) { i ->
               val x = i.rem(2*radius)
               val y = i.floorDiv(2*radius)
               if (y<=radius) HSBtoRGB((x.toDouble()/2.0/c).toFloat(), (y.toDouble()/c).toFloat(), 1f)
               else HSBtoRGB((x.toDouble()/2.0/c).toFloat(), 1f, 1f) and (((((2*radius-y).toFloat()/radius)*255 + .5).toInt() shl 24) or opacityMask)
            }
            pixelWriter.setPixels(0, 0, 2*radius, 2*radius, PixelFormat.getIntArgbInstance(), pixels, 0, 2*radius)
         }
      }
   }
}

typealias HueBulbId = String
typealias HueGroupId = String
typealias HueSceneId = String
typealias HueSensorId = String

class HueCellNode(icon: Icon, name: String): VBox(5.emScaled) {
   var name: String = name
      set(value) {
         field = value
         lookupId<Label>("nameLabel").text = value
      }

   init {
      prefSize = 90.emScaled x 80.emScaled
      alignment = TOP_CENTER
      lay += icon
      lay += label(name) {
         id = "nameLabel"
         isWrapText = true
         textAlignment = TextAlignment.CENTER
      }
   }
}
class HueCell<T>(val node: Node, val icon: HueIcon<T>)
class HueIcon<T>(i: GlyphIcons, size: Double, var hue: T): Icon(i, size)

data class HueBridge(val id: String, val name: String)
data class HueBulbStateEditOn(val on: Boolean)
data class HueBulbStateEditLight(val bri: Int?, val hue: Int?, val sat: Int?)
data class HueBulbState(val on: Boolean, val bri: Int, val hue: Int, val sat: Int, val reachable: Boolean)
data class HueBulb(val id: HueBulbId = "", val name: String, val productname: String, val state: HueBulbState)
data class HueGroupState(val all_on: Boolean, val any_on: Boolean)
data class HueGroup(val id: HueGroupId = "", val name: String, val lights: List<HueBulbId>, val state: HueGroupState)
data class HueGroupCreate(val name: String, val type: String, val lights: List<HueBulbId>)
data class HueScene(val id: HueSceneId = "", val name: String, val lights: List<HueBulbId>)
data class HueSceneCreate(val name: String, val type: HueSceneType, val group: HueGroupId?, val lights: List<HueBulbId>?, val recycle: Boolean = false) {
   constructor(name: String, lights: List<HueBulb>): this(name, LightScene, null, lights.map { it.id })
   constructor(name: String, group: HueGroupId): this(name, GroupScene, group, null)
}
data class HueSensor(val id: HueSensorId = "", val name: String, val type: String, val state: Map<String, Any?>, val config: Map<String, Any?>)

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