@file:Suppress("SpellCheckingInspection", "EnumEntryName")

package hue

import de.jensd.fx.glyphs.GlyphIcons
import hue.HueBulbConfPowerOn.*
import hue.HueGroupType.LightGroup
import hue.HueGroupType.Lightsource
import hue.HueGroupType.Luminaire
import hue.HueGroupType.Zone
import hue.HueSceneType.GroupScene
import hue.HueSceneType.LightScene
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.shape.Circle
import javafx.scene.text.TextAlignment
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import mu.KLogging
import sp.it.pl.core.InfoUi
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.appProperty
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.IconWH
import sp.it.pl.main.Key
import sp.it.pl.main.WidgetTags.IOT
import sp.it.pl.main.WidgetTags.VISUALISATION
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.showFloating
import sp.it.pl.main.textColon
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.form.Validated
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.pyth
import sp.it.util.access.v
import sp.it.util.async.coroutine.await
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.EditMode.NONE
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.lengthMax
import sp.it.util.conf.uiConverterElement
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
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.clip
import sp.it.util.math.min
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.notNull
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.system.browse
import sp.it.util.text.capitalLower
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.text.split3
import sp.it.util.type.atomic
import sp.it.util.ui.alpha
import sp.it.util.ui.center
import sp.it.util.ui.centre
import sp.it.util.ui.dsl
import sp.it.util.ui.flowPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupId
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.scrollPane
import sp.it.util.ui.size
import sp.it.util.ui.text
import sp.it.util.ui.unitCircleDegP
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.xy
import sp.it.util.units.seconds
import sp.it.util.units.uri
import sp.it.util.units.version
import sp.it.util.units.year

class Hue(widget: Widget): SimpleController(widget) {

   val client = HttpClient(CIO).apply { onClose += ::close }
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

   private val hueBridge = object: CoroutineScope by scope {
      val hueBridgeUserDevice = "spit-player"
      lateinit var ip: String
      lateinit var apiKey: String
      lateinit var apiVersion: KotlinVersion
      lateinit var url: String

      fun init() = runSuspendingFx {
         ip = hueBridgeIp.validIpOrNull() ?: ip() ?: fail { "Unable to obtain Phillips Hue bridge ip. Make sure it is turned on and connected to the network." }
         hueBridgeIp = ip
         apiKey = if (isAuthorizedApiKey(ip, hueBridgeApiKey)) hueBridgeApiKey else createApiKey(ip)
         hueBridgeApiKey = apiKey
         apiVersion = apiVersion("http://$ip/api/$apiKey").split3(".").net { (major, minor, patch) -> KotlinVersion(major.toInt(), minor.toInt(), patch.toIntOrNull() ?: 0) }
         url = "http://$ip/api/$apiKey"
      }

      private suspend fun String.validIpOrNull(): String? {
         val isValid = runTry { client.get("http://${this@validIpOrNull}").status }.orNull()==OK
         return this@validIpOrNull.takeIf { isValid }
      }

      private suspend fun ip(): String? {
         val response = client.get("https://discovery.meethue.com/").bodyAsText()
         return (response.parseToJson().asJsArray()/0/"internalipaddress")?.asJsStringValue()
      }

      private suspend fun isAuthorizedApiKey(ip: String, apiKey: String): Boolean {
         val response = client.get("http://$ip/api/$apiKey/lights")
         failIf(response.status!=OK) { "Failed to check if hue bridge user $apiKey exists" }
         return runTry { (response.bodyAsText().parseToJson()/0/"error") }.isError
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

         val userCreated = client.post("http://$ip/api") {
            setBody(JsObject("devicetype" to JsString(hueBridgeUserDevice)).toPrettyS())
         }
         failIf(userCreated.status==SwitchingProtocols) { "Failed to create Phillips Hue bridge user: Link button not pressed" }
         failIf(userCreated.status!=OK) { "Failed to create Phillips Hue bridge user" }
         val apiKey = runTry { (userCreated.bodyAsText().parseToJson()/0/"success"/"username")?.asJsStringValue() }.orThrow
         return apiKey ?: fail { "Failed to obtain user Phillips Hue bridge api key" }
      }

      private suspend fun apiVersion(url: String): String {
         val response = client.getText("$url/config")
         return response.parseToJson().asJsObject().value["apiversion"]?.asJsString()?.value ?: fail { "Could not obtain api version" }
      }

      fun bulbs() = runSuspendingFx {
         val response = client.getText("$url/lights")
         response.parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueBulb>().copy(id = id) }
      }

      fun groups() = runSuspendingFx {
         val response = client.getText("$url/groups")
         response.parseToJson().asJsObject().value.map { (id, bulbJs) -> bulbJs.to<HueGroup>().copy(id = id) }
      }

      fun bulbsAndGroups() = runSuspendingFx {
         val bulbs = bulbs().await()
         val groups = groups().await()
         bulbs to groups + HueGroup("0", "All", listOf(), HueGroupState(bulbs.all { it.state.on }, bulbs.any { it.state.on }))
      }

      fun scenes() = runSuspendingFx {
         val response = client.getText("$url/scenes")
         response.parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueScene>().copy(id = id) }
      }

      fun sensors() = runSuspendingFx {
         val response = client.getText("$url/sensors")
         response.parseToJson().asJsObject().value.map { (id, sceneJs) -> sceneJs.to<HueSensor>().copy(id = id) }
      }

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

      fun createScene(scene: HueSceneCreate) = runSuspendingFx {
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
         fillProperty() syncFrom color.notNull(TRANSPARENT).map { it.alpha(0.5+0.5*it.opacity) }
      }
      val node = vBox {
         alignment = TOP_CENTER
         lay += form(configurable)
         lay += object: StackPane() {
            override fun layoutChildren() {
               super.layoutChildren()
               val c = color.value ?: TRANSPARENT
               selector.center = size/2.0 + (c.hue+180).unitCircleDegP * (selectorRadius*(if (c.opacity==1.0) 0.5*c.saturation else 1 - 0.5*c.opacity))
            }
         }.apply {
            lay += Thumbnail(selectorRadius*2, selectorRadius*2).run {
               loadImage(drawGradientCir(selectorRadius.toInt()))

               fun updateFromMouse(it: MouseEvent) {
                  if (!readOnly.value) {
                     val c = image.value!!.pixelReader.getColor(it.x.toInt().clip(0, selectorRadius.toInt()*2-1), it.y.toInt().clip(0, selectorRadius.toInt()*2-1))
                     val isOuter = c==TRANSPARENT
                     val cBriRaw = if (isOuter) 0.0 else c.opacity
                     val cBri = (1 + cBriRaw*253).toInt()
                     val cHueRaw = (((pane.layoutBounds.centre - it.xy).atan2())/2/PI).mod(1.0)
                     val cHue = (cHueRaw*65535).toInt()
                     val cSatRaw = if (isOuter) 1.0 else c.saturation
                     val cSat = (cSatRaw*245).toInt()
                     changeToBulb(HueBulb("", "", "", HueBulbState(true, cBri, cHue, cSat, true), mapOf()))
                     if (!isDragged) applyToSelected(cBri, cHue, cSat)
                  }
               }
               pane.onEventDown(MOUSE_PRESSED, PRIMARY) { isDragged = true }
               pane.onEventDown(MOUSE_RELEASED, PRIMARY) { isDragged = false }
               pane.onEventDown(MOUSE_RELEASED, PRIMARY) { updateFromMouse(it) }
               pane.onEventDown(MOUSE_DRAGGED, PRIMARY) { if (isDragged) updateFromMouse(it) }

               pane
            }
            lay += selector
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
            selector.parent?.requestLayout()
         }
      }
      fun changeToBulb(bulb: HueBulb) {
         avoidApplying.suppressing {
            color.value = Color.hsb(bulb.state.hue.toDouble()*360.0/65535.0, bulb.state.sat.toDouble()/254.0, 1.0, bulb.state.bri.minus(1).toDouble()/253.0)
            hue.value = bulb.state.hue
            sat.value = bulb.state.sat
            bri.value = bulb.state.bri
            selector.isVisible = true
            selector.parent?.requestLayout()
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
            lay += Icon(IconFA.PLAY).run {
               tooltip("Run commandsto bridge manually")
               onClickDo { uri("https://$hueBridgeIp/debug/clip.html").browse() }
               withText(RIGHT, "Commands")
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

   fun linkBridge() = hueBridge.init() ui { }

   fun refresh(): Fut<Any> = hueBridge.init() ui {
      fun unfocusBulb() {
         selectedBulbIcon?.pseudoClassChanged("edited", false)
         selectedBulbIcon = null
         selectedBulbId = null
         color.readOnly.value = true
         infoPane.lay -= color.node
      }
      fun unfocusBulbGroup() {
         hueBulbCells.values.map { it.icon.hue.id }.forEach { hueBulbCells[it]?.icon?.pseudoClassChanged("edited-group", false) }
         selectedGroupIcon?.pseudoClassChanged("edited", false)
         selectedGroupIcon = null
         selectedGroupId = null
         color.readOnly.value = true
         infoPane.lay -= color.node
      }
      fun unfocusSensor() {
         infoPane.lay -= devicePane
      }

      hueBridge.bulbsAndGroups() ui { (bulbs, groups) ->
         groupsPane.children setTo groups.map { group ->
            hueBulbGroupCells.getOrPut(group.id) {
               HueIcon(IconFA.LIGHTBULB_ALT, 40.0, group).run {
                  styleclass("hue-group-icon")

                  fun toggleBulbGrouo() = hueBridge.toggleBulbGroup(group.id).thenRefresh()
                  fun deleteBulbGrouo() = hueBridge.deleteGroup(group.id).thenRefresh()
                  fun focusBulbGroup() {
                     unfocusSensor()
                     unfocusBulb()
                     hueBulbCells.values.map { it.icon.hue.id }.forEach { hueBulbCells[it]?.icon?.pseudoClassChanged("edited-group", false) }
                     selectedGroupIcon?.pseudoClassChanged("edited", false)
                     selectedGroupIcon = this
                     selectedGroupId = group.id
                     pseudoClassChanged("edited", true)
                     (if (group.id=="0") hueBulbCells.values.map { it.icon.hue.id } else hue.lights).forEach { hueBulbCells[it]?.icon?.pseudoClassChanged("edited-group", true) }
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
                           item("Toggle bulbs on/off", keys = keys(Key.SPACE)) { toggleBulbGrouo() }
                           item("Delete", keys = keys(Key.DELETE)) { deleteBulbGrouo() }
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
                  var type by c(Zone)
                  val bulbs by cCheckList(*bulbsAll.toTypedArray()).uiConverterElement { it.name }

                  fun materialize() = HueGroupCreate(name, type.value, this.bulbs.selected(true).map { it.id })

                  override fun isValid() = when {
                     name.isEmpty() -> Try.error("Name can not be empty")
                     this.bulbs.selected(true).isEmpty() && type in setOf(Luminaire, LightGroup, Lightsource) -> Try.error("$type can not have no bulbs")
                     else -> Try.ok()
                  }
               }.configure("Create group") {
                  hueBridge.createGroup(it.materialize()).thenRefresh()
               }
            }
         }

         bulbsPane.children setTo bulbs.map { bulb ->
            hueBulbCells.getOrPut(bulb.id) {
               HueIcon(null, 40.0, bulb).run {
                  styleclass("hue-bulb-icon")

                  fun rename() {
                     object: ConfigurableBase<Any?>() {
                        val name by cv(hue.name).lengthMax(32).def("Name")
                     }.configure("Rename bulb") {
                        hueBridge.renameBulb(bulb.id, it.name.value).thenRefresh()
                     }
                  }
                  fun changePowerOn() {
                     object: ConfigurableBase<Any?>(), Validated {
                        val powerOn by cv(hue.confPowerOn ?: unknown).def("Behavior")
                        override fun isValid() = when {
                           !hueBridge.apiVersion.isAtLeast(1, 28) -> Try.error("Unsupported before apiVersion 1.28. Please update bridge.")
                           powerOn.value == custom || powerOn.value == unknown -> Try.error("Value not supported")
                           else -> Try.ok()
                        }
                     }.configure("Change power on behavior") {
                        hueBridge.changePowerOn(bulb.id, it.powerOn.value).thenRefresh()
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
                        item("Toggle on/off", keys = keys(Key.SPACE)) { toggleBulb() }
                        if (hue.confPowerOn!=null) item("Power on behavior") { changePowerOn() }
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
            Icon(null, 40.0).run {
               styleclass("hue-scene-icon")

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
            Icon(sensor.icon, 40.0).run {
               styleclass("hue-sensor-icon")

               fun focusSensor() {
                  unfocusSensor()
                  unfocusBulbGroup()
                  unfocusBulb()
                  infoPane.lay += devicePane

                  devicePane.lay.clear()
                  devicePane.lay += textColon("Type", sensor.type)
                  when (sensor.type) {
                     "ZLLTemperature" -> sensor.stateTemperature.ifNotNull { devicePane.lay += textColon("Temperature", "$it°C") }
                     "ZLLPresence" -> sensor.statePresence.ifNotNull { devicePane.lay += textColon("Presence", it) }
                     "Daylight" -> sensor.stateDaylight.ifNotNull { devicePane.lay += textColon("Is daylight", it) }
                     else -> IconMA.SETTINGS_INPUT_ANTENNA
                  }
                  if (devicePane.lay.children.isNotEmpty()) devicePane.lay += label()
                  sensor.config.entries.sortedBy { it.key }.forEach { (name, value) ->
                     devicePane.lay += textColon(name.capitalLower(), value)
                  }
               }
               pseudoClassChanged("unreachable", sensor.config["reachable"]?.asIf<Boolean>() ?: false)
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) focusSensor()
               }
               HueCellNode(this, sensor.name)
            }
         }
      }
   }

   /*
    * https://developers.meethue.com/develop/hue-api/
    * https://developers.meethue.com/develop/hue-api/supported-devices/
    */
   companion object: WidgetCompanion, KLogging() {
      override val name = "Hue Scenes"
      override val description = "Manages Phillips Hue bulbs, groups & scenes"
      override val descriptionLong = "$description."
      override val icon = IconMD.LIGHTBULB_OUTLINE
      override val version = version(0, 0, 6)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(VISUALISATION, IOT, "Hue")
      override val summaryActions = listOf(
         Entry("Data", "Refresh", F5.nameUi),
      )

      private var hueBridgeApiKey by appProperty("")
      private var hueBridgeIp by appProperty("")

      fun JsObject.withoutNullValues() = JsObject(value.filter { it.value !is JsNull })
      fun JsObject.withoutType() = JsObject(value.filter { it.key!="_type" })
      fun Any?.toJson() = APP.serializerJson.json.toJsonValue(this).let {
         when (it) {
            is JsObject -> it.withoutType()
            else -> it
         }
      }

      fun String.parseToJson() = APP.serializerJson.json.ast(this).orThrow
      inline fun <reified T> JsValue.to() = APP.serializerJson.json.fromJsonValue<T>(this).orThrow

      suspend fun HttpClient.getText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = get(url, block).bodyAsText()
      suspend fun HttpClient.putText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = put(url, block).bodyAsText()
      suspend fun HttpClient.postText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = post(url, block).bodyAsText()
      suspend fun HttpClient.deleteText(url: String, block: HttpRequestBuilder.() -> Unit = {}) = delete(url, block).bodyAsText()

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
typealias HueMap = Map<String,Any?>

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
      alignment = TOP_CENTER
      lay += icon
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
data class HueGroup(val id: HueGroupId = "", val name: String, val lights: List<HueBulbId>, val state: HueGroupState)
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
   val icon: GlyphIcons get() = when (type) {
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