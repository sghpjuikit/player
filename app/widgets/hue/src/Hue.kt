@file:Suppress("SpellCheckingInspection", "EnumEntryName")

package hue

import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Color.HSBtoRGB
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Side.RIGHT
import javafx.scene.control.ContextMenu
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
import javafx.scene.paint.Color
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.shape.Circle
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Key
import sp.it.pl.main.WidgetTags.IOT
import sp.it.pl.main.WidgetTags.VISUALISATION
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.showFloating
import sp.it.pl.main.textColon
import sp.it.pl.plugin.Hue
import sp.it.pl.plugin.HueBulb
import sp.it.pl.plugin.HueBulbConfPowerOn.custom
import sp.it.pl.plugin.HueBulbConfPowerOn.unknown
import sp.it.pl.plugin.HueBulbId
import sp.it.pl.plugin.HueBulbState
import sp.it.pl.plugin.HueBulbStateEditLight
import sp.it.pl.plugin.HueCell
import sp.it.pl.plugin.HueCellNode
import sp.it.pl.plugin.HueGroup
import sp.it.pl.plugin.HueGroupCreate
import sp.it.pl.plugin.HueGroupId
import sp.it.pl.plugin.HueGroupType.LightGroup
import sp.it.pl.plugin.HueGroupType.Lightsource
import sp.it.pl.plugin.HueGroupType.Luminaire
import sp.it.pl.plugin.HueGroupType.Room
import sp.it.pl.plugin.HueGroupType.Zone
import sp.it.pl.plugin.HueIcon
import sp.it.pl.plugin.HueSceneCreate
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.form.Validated
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.pyth
import sp.it.util.access.v
import sp.it.util.async.FX_LATER
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.asFut
import sp.it.util.async.future.Fut
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
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
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.math.clip
import sp.it.util.math.min
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.attachWhile
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.map
import sp.it.util.reactive.notNull
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.syncWhile
import sp.it.util.system.browse
import sp.it.util.text.capitalLower
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.alpha
import sp.it.util.ui.center
import sp.it.util.ui.centre
import sp.it.util.ui.dsl
import sp.it.util.ui.flowPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.scrollPane
import sp.it.util.ui.size
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.text
import sp.it.util.ui.unitCircleDegP
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.xy
import sp.it.util.units.uri
import sp.it.util.units.version
import sp.it.util.units.year

class Hue(widget: Widget): SimpleController(widget) {

   val hueBulbCells = HashMap<HueBulbId, HueCell<HueBulb>>()
   val hueBulbGroupCells = HashMap<HueGroupId, HueCell<HueGroup>>()
   var selectedGroupId: HueGroupId? = null
   var selectedGroupIcon: HueIcon<HueGroup>? = null
   var selectedBulbId: HueBulbId? = null
   var selectedBulbIcon: HueIcon<HueBulb>? = null
   val bridgePane = flowPane(10.emScaled, 10.emScaled).apply { padding = Insets(0.0, 0.0, 5.emScaled, 0.0) }
   val bulbsPane = flowPane(10.emScaled, 10.emScaled).apply { padding = Insets(0.0, 0.0, 5.emScaled, 0.0) }
   val groupsPane = flowPane(10.emScaled, 10.emScaled).apply { padding = Insets(0.0, 0.0, 5.emScaled, 0.0) }
   val scenesPane = flowPane(10.emScaled, 10.emScaled).apply { padding = Insets(0.0, 0.0, 5.emScaled, 0.0) }
   val sensorsPane = flowPane(10.emScaled, 10.emScaled).apply { padding = Insets(0.0, 0.0, 5.emScaled, 0.0) }
   val huePlugin = APP.plugins.plugin<Hue>().asValue(onClose)
   val hueBridge: Hue.HueBridge?
      get() {
         huePlugin.value.ifNull {
            showFloating("Hue plugin") { popup ->
               vBox {
                  val hue = APP.plugins.getRaw<Hue>()
                  if (hue==null) {
                     lay += text("Hue plugin not available. Check whether the plugin is installed.")
                  } else {
                     lay += text("Hue plugin not running.")
                     lay += Icon(IconMD.RUN).run {
                        onClickDo {
                           popup.hide()
                           hue.start()
                        }
                        withText(RIGHT, "Start plugin...")
                     }
                  }
               }
            }
         }
         return huePlugin.value?.hueBridge
      }

   private val infoPane = vBox(30.emScaled)
   private val devicePane = vBox(0, TOP_LEFT)
   private val color = ColorPane()

   init {
      root.prefSize = 900.emScaled x 600.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()
      root.onEventDown(KEY_PRESSED, F5) { refresh() }
      root.lay += vBox(10.emScaled, TOP_LEFT) {
         lay += hBox(5.emScaled, CENTER_LEFT) {
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
               disableProperty() syncFrom huePlugin.map { it==null } on onClose
               tooltip("Run commands to bridge manually")
               onClickDo { huePlugin.value?.ifNotNull { uri("https://${it.hueBridge.ip}/debug/clip.html").browse() } }
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
                  lay += TitledPane("Bridge", bridgePane)
               }
            }
            lay += infoPane.apply {
               prefWidth = 210.emScaled
            }
         }
      }

      huePlugin syncNonNullWhile { it.refreshes attach { refresh() } } on onClose
      root.sync1IfInScene { refresh() } on onClose
   }

   fun Fut<*>?.thenRefresh() = this?.then(FX_LATER) { refresh().toUnit() }.toUnit()

   fun linkBridge(): Job = scope.launch(FX) { hueBridge?.init() }

   fun refresh(): Job = scope.launch(FX) {

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
      fun unfocusSensorOrBridge() {
         infoPane.lay -= devicePane
      }

      hueBridge?.init()
      hueBridge?.bulbsAndGroups().net { it?.first.orEmpty() to it?.second.orEmpty() }.net { (bulbs, groups) ->
         groupsPane.children setTo groups.map { group ->
            hueBulbGroupCells.getOrPut(group.id) {
               HueIcon(IconFA.LIGHTBULB_ALT, 40.0, group).run {
                  styleclass("hue-group-icon")

                  fun toggleBulbGrouo() = hueBridge?.toggleBulbGroup(group.id).thenRefresh()
                  fun deleteBulbGrouo() = hueBridge?.deleteGroup(group.id).thenRefresh()
                  fun focusBulbGroup() {
                     unfocusSensorOrBridge()
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

                  HueCell(HueCellNode(this, group.name), this).apply {
                     icon.focusedProperty() attachTrue { focusBulbGroup() }
                     node.onEventDown(KEY_PRESSED, SPACE) { toggleBulbGrouo() }
                     node.onEventDown(MOUSE_CLICKED, PRIMARY) {
                        if (it.clickCount==1) focusBulbGroup()
                        if (it.clickCount==2) toggleBulbGrouo()
                     }
                     node.onContextMenuRequested = EventHandler {
                        if (hue.id!="0")
                           ContextMenu().dsl {
                              item("Toggle bulbs on/off", keys = keys(Key.SPACE)) { toggleBulbGrouo() }
                              item("Delete", keys = keys(Key.DELETE)) { deleteBulbGrouo() }
                           }.show(node, RIGHT, 0.0, 0.0)
                     }
                  }
               }
            }.run {
               icon.hue = group
               icon.pseudoClassChanged("on", group.state.any_on)

               node
            }
         }
         groupsPane.children += Icon(IconFA.PLUS).onClickDo {
            scope.launch(FX) {
               val bulbsAll = hueBridge?.bulbs().orEmpty()
               object: ConfigurableBase<Any?>(), Validated {
                  var name by c("")
                  var type by c(Zone)
                  val bulbs by cCheckList(*bulbsAll.toTypedArray()).uiConverterElement { it.name }

                  fun materialize() = HueGroupCreate(name, type.value, this.bulbs.selected(true).map { it.id })

                  override fun isValid() = when {
                     name.isEmpty() -> Try.error("Name can not be empty")
                     type==Room && this.bulbs.selected(true).any { it.id in groups.asSequence().filter { it.type==Room.value }.flatMap { it.lights } } -> Try.error("Some bulbs are already in other group of Room type")
                     this.bulbs.selected(true).isEmpty() && type in setOf(Luminaire, LightGroup, Lightsource) -> Try.error("$type can not have no bulbs")
                     else -> Try.ok()
                  }
               }.configure("Create group") {
                  hueBridge?.createGroup(it.materialize()).thenRefresh()
               }
            }
         }

         bulbsPane.children setTo bulbs.map { bulb ->
            hueBulbCells.getOrPut(bulb.id) {
               HueIcon(null, 40.0, bulb).run {
                  styleclass("hue-bulb-icon")
                  styleclassToggle("hue-plug-plug", hue.isPlug())

                  fun rename() {
                     object: ConfigurableBase<Any?>() {
                        val name by cv(hue.name).lengthMax(32).def("Name")
                     }.configure("Rename bulb") {
                        hueBridge?.renameBulb(bulb.id, it.name.value).thenRefresh()
                     }
                  }
                  fun changePowerOn() {
                     object: ConfigurableBase<Any?>(), Validated {
                        val powerOn by cv(hue.confPowerOn ?: unknown).def("Behavior")
                        override fun isValid() = when {
                           !hueBridge!!.apiVersion.isAtLeast(1, 28) -> Try.error("Unsupported before apiVersion 1.28. Please update bridge.")
                           powerOn.value == custom || powerOn.value == unknown -> Try.error("Value not supported")
                           else -> Try.ok()
                        }
                     }.configure("Change power on behavior") {
                        hueBridge?.changePowerOn(bulb.id, it.powerOn.value).thenRefresh()
                     }
                  }
                  fun flashBulb() =
                     hueBridge?.flashBulb(bulb.id)
                  fun toggleBulb() =
                     hueBridge?.toggleBulb(bulb.id).thenRefresh()
                  fun focusBulb() {
                     unfocusSensorOrBridge()
                     unfocusBulbGroup()
                     selectedBulbIcon?.pseudoClassChanged("edited", false)
                     selectedBulbIcon = this
                     selectedBulbId = bulb.id
                     pseudoClassChanged("edited", true)
                     color.readOnly.value = false
                     if (hue.isBulb()) infoPane.lay += color.node
                     if (hue.isBulb()) color.changeToBulb(hue)
                  }


                  HueCell(HueCellNode(this, bulb.name), this).apply {
                     icon.focusedProperty() attachTrue { focusBulb() }
                     node.onEventDown(KEY_PRESSED, SPACE) { toggleBulb() }
                     node.onEventDown(MOUSE_CLICKED, PRIMARY) {
                        if (it.clickCount==1) focusBulb()
                        if (it.clickCount==2) toggleBulb()
                     }
                     node.onContextMenuRequested = EventHandler {
                        ContextMenu().dsl {
                           item("Rename") { rename() }
                           item("Flash") { flashBulb() }
                           item("Toggle on/off", keys = keys(Key.SPACE)) { toggleBulb() }
                           if (hue.confPowerOn!=null) item("Power on behavior") { changePowerOn() }
                        }.show(node, RIGHT, 0.0, 0.0)
                     }
                  }
               }
            }.run {
               icon.hue = bulb
               icon.styleclassToggle("hue-plug-plug", bulb.isPlug())
               icon.pseudoClassChanged("unreachable", !bulb.state.reachable)
               icon.pseudoClassChanged("on", bulb.state.on)
               icon.isDisable = !bulb.state.reachable
               node.asIs<HueCellNode>().name = bulb.name

               node
            }
         }
      }
      hueBridge?.scenes().orEmpty().net { scenes ->
         scenesPane.children setTo scenes.map { scene ->
            fun focusScene() {
               unfocusSensorOrBridge()
               unfocusBulbGroup()
               unfocusBulb()
            }

            val icon = Icon(null, 40.0).apply {
               styleclass("hue-scene-icon")
            }
            HueCellNode(icon, scene.name).apply {
               icon.focusedProperty() attachTrue { focusScene() }
               onEventDown(KEY_PRESSED, SPACE) { focusScene() }
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) focusScene()
                  if (it.clickCount==2) hueBridge?.applyScene(scene)
               }
               onContextMenuRequested = EventHandler {
                  ContextMenu().dsl {
                     item("delete") {
                        hueBridge?.deleteScene(scene.id).thenRefresh()
                     }
                  }.show(this, RIGHT, 0.0, 0.0)
               }
            }
         }
         scenesPane.children += Icon(IconFA.PLUS).onClickDo {
            scope.launch(FX) {
               val bulbsAll = hueBridge?.bulbs().orEmpty()
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
                  hueBridge?.createScene(it.materialize())?.asFut().thenRefresh()
               }
            }
         }
      }
      hueBridge?.sensors().orEmpty().net { scenes ->
         sensorsPane.children setTo scenes.map { sensor ->
            fun focusSensor() {
               unfocusSensorOrBridge()
               unfocusBulbGroup()
               unfocusBulb()
               infoPane.lay += devicePane.apply {
                  lay.clear()
                  lay += textColon("Name", sensor.name)
                  lay += textColon("Type", sensor.type)
                  when (sensor.type) {
                     "ZLLTemperature" -> sensor.stateTemperature.ifNotNull { lay += textColon("Temperature", "$it°C") }
                     "ZLLPresence" -> sensor.statePresence.ifNotNull { lay += textColon("Presence", it) }
                     "Daylight" -> sensor.stateDaylight.ifNotNull { lay += textColon("Is daylight", it) }
                     else -> IconMA.SETTINGS_INPUT_ANTENNA
                  }
                  if (lay.children.isNotEmpty()) lay += label()
                  sensor.config.entries.sortedBy { it.key }.forEach { (name, value) ->
                     lay += textColon(name.capitalLower(), value)
                  }
               }
            }
            val icon = Icon(sensor.icon, 40.0).apply {
               styleclass("hue-sensor-icon")
               pseudoClassChanged("unreachable", sensor.config["reachable"]?.asIf<Boolean>() ?: false)
            }
            HueCellNode(icon, sensor.name).apply {
               icon.focusedProperty() attachTrue { focusSensor() }
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) focusSensor()
               }
            }
         }
      }
      hueBridge?.api().net { listOfNotNull(it) }.net { apis ->
         bridgePane.children setTo apis.map { api ->
            fun focusBridge() {
               unfocusSensorOrBridge()
               unfocusBulbGroup()
               unfocusBulb()
               infoPane.lay += devicePane.apply {
                  lay.clear()
                  lay += textColon("Name", api.name)
                  lay += label()
                  lay += textColon("Datastore version", api.datastoreversion)
                  lay += textColon("Sw version", api.swversion)
                  lay += textColon("Api version", api.apiversion)
                  lay += textColon("Mac", api.mac)
                  lay += textColon("Bridge id", api.bridgeid)
                  lay += textColon("Factory new", api.factorynew)
                  lay += textColon("Replaces bridge", api.replacesbridgeid)
                  lay += textColon("Model id", api.modelid)
                  lay += textColon("Starter kit id", api.starterkitid)
               }
            }
            val icon = HueIcon(IconFA.SQUARE, 40.0, null).apply {
               styleclass("hue-bridge-icon")
            }

            HueCellNode(icon, "Bridge").apply {
               icon.focusedProperty() attachTrue { focusBridge() }
               onEventDown(MOUSE_CLICKED, PRIMARY) {
                  if (it.clickCount==1) focusBridge()
               }
            }
         }
      }
   }

   inner class ColorPane: ConfigurableBase<Any?>() {
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
               selector.center = size/2.0 + (c.hue + 180).unitCircleDegP*(selectorRadius*(if (c.opacity==1.0) 0.5*c.saturation else 1 - 0.5*c.opacity))
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
            color.value = Color.hsb(bulb.state.hue!!.toDouble()*360.0/65535.0, bulb.state.sat!!.toDouble()/254.0, 1.0, bulb.state.bri!!.minus(1).toDouble()/253.0)
            hue.value = bulb.state.hue!!
            sat.value = bulb.state.sat!!
            bri.value = bulb.state.bri!!
            selector.isVisible = true
            selector.parent?.requestLayout()
         }
      }

      fun applyToSelected(bri: Int?, hue: Int?, sat: Int?) {
         avoidApplying.suppressed {
            val state = HueBulbStateEditLight(bri, hue, sat)
            selectedBulbId.ifNotNull { hueBridge?.applyBulbLight(it, state) }
            selectedGroupId.ifNotNull { hueBridge?.applyBulbGroupLight(it, state) }
         }
      }
   }

   companion object: WidgetCompanion {
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

      val logger = KotlinLogging.logger { }

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