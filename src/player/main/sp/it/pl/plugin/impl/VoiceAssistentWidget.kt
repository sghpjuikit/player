package sp.it.pl.plugin.impl

import io.ktor.client.request.get
import javafx.geometry.Pos.CENTER
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.scene.layout.VBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import sp.it.pl.core.bodyAsJs
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.loadIn
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.ValueToggleButtonGroup
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.visible
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.launch
import sp.it.util.collections.mapset.MapSet
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.getDelegateConfig
import sp.it.util.dev.fail
import sp.it.util.file.json.JsArray
import sp.it.util.file.json.JsFalse
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsNumber
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsTrue
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachWhileTrue
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.map
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.text.nameUi
import sp.it.util.type.atomic
import sp.it.util.ui.appendTextSmart
import sp.it.util.ui.hBox
import sp.it.util.ui.insertNewline
import sp.it.util.ui.isNewlineOnShiftEnter
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.singLineProperty
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class VoiceAssistentWidget(widget: Widget): SimpleController(widget) {

   init {
      val plugin = APP.plugins.plugin<VoiceAssistant>().asValue(onClose)
      var run = {}
      val mode = v("Chat")
      val chatSettings = v(false)

      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.lay += vBox(null, CENTER) {
         lay += hBox(null, CENTER) {
            lay += Icon(IconMD.AV_TIMER).tooltip("HW utilization").apply {
               onClickDo { APP.instances.factoryGpuNvidiaInfo.loadIn() }
            }
            lay += Icon(IconFA.COG).tooltip("Settings").apply {
               disableProperty() syncFrom plugin.map { it==null }
               onClickDo { APP.actions.app.openSettings(plugin.value?.configurableGroupPrefix) }
            }
            lay += CheckIcon().icons(IconMD.FILTER, IconMD.FILTER_REMOVE_OUTLINE).apply {
               disableProperty() syncFrom plugin.map { it==null }
               selected syncFrom plugin.flatMap { it!!.pythonStdOutDebug }.orElse(true)
               selected attach { plugin.value?.pythonStdOutDebug?.value = it }
               tooltip("Hide debug and raw output")
            }
            lay += Icon(IconFA.REFRESH).tooltip("Restart voice assistent")
               .onClickDo { plugin.value?.restart() }
            lay += label("   ")
            lay += CheckIcon().icons(IconMD.TEXT_TO_SPEECH, IconMD.TEXT_TO_SPEECH_OFF).apply {
               disableProperty() syncFrom plugin.map { it==null }
               selected syncFrom plugin.flatMap { it!!.speechOn }.orElse(false)
               selected attach { plugin.value?.speechOn?.value = it }
               tooltip("Enable/disable voice")
            }
            lay += CheckIcon().icons(IconMA.MIC, IconMA.MIC_OFF).apply {
               disableProperty() syncFrom plugin.map { it==null }
               selected syncFrom plugin.flatMap { it!!.micEnabled }.orElse(false)
               selected attach { plugin.value?.micEnabled?.value = it }
               tooltip("Enable/disable microphone")
            }
            lay += label {
               plugin.sync { text = if (it!=null) "Active" else "Inactive" }
            }
            lay += label("   ")
            lay += ValueToggleButtonGroup.ofObservableValue(mode, listOf("Raw", "Speak", "Chat", "Hw")) {
               tooltip = appTooltip(
                  when (it) {
                     "Raw" -> "Send the text to the Voice Assestant as if user wrote it in console"
                     "Speak" -> "Narrates the specified text using synthesized voice"
                     "Chat" -> "Send the text to the Voice Assestant as if user spoke it"
                     "Hw" -> "Show actor system state"
                     else -> fail { "Illegal value" }
                  }
               )
            }.apply {
               alignment = CENTER
            }
         }

         lay(ALWAYS) += stackPane {
            lay += scrollPane {
               isFitToWidth = true
               isFitToHeight = false
               prefSize = -1 x -1
               vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
               hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
               content = vBox(10.emScaled) {
                  id = "hw"
                  visible syncFrom mode.map { it == "Hw" }

                  val actorStates = MapSet { a: ActorState -> a.type }

                  visibleProperty() attachWhileTrue {
                     var a by atomic(true)
                     launch(VT) {
                        while (a) {
                           runTry {
                              val url = plugin.value?.speechServerUrl?.value?.net { "http://$it/actor"}
                              if (url==null) return@runTry
                              val actors = APP.http.client.get(url).bodyAsJs().asJsObject().value
                              FX {
                                 actors.forEach { (type, actor) ->
                                    actorStates.getOrPut(type) { ActorState(type).apply { this@vBox.lay += this } }.update(actor.asJsObject())
                                 }
                              }
                           }.ifError {
                              it.printStackTrace()
                           }
                           delay(1000)
                        }
                     }
                     Subscription { a = false }
                  }
               }
            }
            lay += hBox(5.emScaled, CENTER) {
               visible syncFrom mode.map { it != "Hw" }

               lay(ALWAYS) += vBox(5.emScaled, CENTER) {
                  lay(ALWAYS) += textArea {
                     id = "output"
                     isEditable = false
                     isFocusTraversable = false
                     isWrapText = true
                     prefColumnCount = 100
                     text = plugin.value?.pythonStdOut?.value ?: ""

                     onEventDown(KEY_PRESSED, ENTER) { appendText("\n") }
                     plugin.syncNonNullWhile { it.onLocalInput attach ::appendTextSmart }
                  }
                  lay += stackPane {
                     lay(CENTER) += hBox(null, CENTER) {
                        lay(ALWAYS) += textArea("") {
                           id = "input"
                           isWrapText = true
                           isNewlineOnShiftEnter = true
                           prefColumnCount = 100
                           promptText = "${ENTER.nameUi} to send, ${SHIFT.nameUi} + ${ENTER.nameUi} for new line"
                           singLineProperty() sync {
                              styleclassToggle("text-area-singlelined", !it)
                              prefRowCount = if (it) 10 else 1
                           }

                           run = {
                              when (mode.value) {
                                 "Raw" -> {
                                    plugin.value?.raw(text)
                                 }
                                 "Speak" -> {
                                    plugin.value?.speak(text); clear()
                                 }
                                 "Chat" -> {
                                    plugin.value?.chat(text); clear()
                                 }
                              }
                           }
                           onEventDown(KEY_PRESSED, ENTER) { if (it.isShiftDown) insertNewline() else run() }
                        }
                        lay(NEVER) += CheckIcon(chatSettings).icons(IconFA.COG, IconFA.COG)
                        lay(NEVER) += Icon(IconFA.SEND).onClickDo { run() }
                     }
                  }
               }
               lay += stackPane {
                  chatSettings zip plugin zip2 mode map { (showSettings, active, mode) -> showSettings && active!=null && mode=="Chat" } sync { show ->
                     lay.clear()
                     lay += supplyIf(show) {
                        scrollPane {
                           isFitToWidth = true
                           isFitToHeight = false
                           prefSize = -1 x -1
                           vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                           hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                           minWidth = 250.emScaled
                           content = ConfigPane(
                              ListConfigurable.Companion.heterogeneous(
                                 plugin.value?.net {
                                    listOf(
                                       it::llmChatSysPrompt.getDelegateConfig(),
                                       it::llmChatMaxTokens.getDelegateConfig(),
                                       it::llmChatTemp.getDelegateConfig(),
                                       it::llmChatTopP.getDelegateConfig(),
                                       it::llmChatTopK.getDelegateConfig(),
                                    )
                                 }.orEmpty()
                              )
                           )
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private class ActorState(val type: String): VBox() {
      init {
         lay += label(type)
      }
      fun labelOf(key: String) = label {
         id = key
         onEventDown(MOUSE_CLICKED, PRIMARY) { if (userData!=null) APP.ui.actionPane.show(userData) }
         lay += this
      }
      fun update(state: JsObject) {
         state.value.forEach { (key, value) ->
            val label = children.findIsInstanceAnd<Label> { it.id==key } ?: labelOf(key)
            label.userData = value.takeIf { it is JsArray }
            label.text = when (value) {
               is JsArray -> "  " + key + ": " + value.value.size + " 🔍"
               is JsObject -> "  " + key + ": " + value.value.size + " 🔍"
               is JsString -> "  " + key + ": " + value.value.toUi()
               is JsNumber -> "  " + key + ": " + value.value.toUi()
               is JsNull -> "  " + key + ": " + null.toUi()
               is JsTrue -> "  " + key + ": " + true.toUi()
               is JsFalse -> "  " + key + ": " + false.toUi()
            }
         }
      }
   }

   companion object: WidgetCompanion {
      override val id = "VoiceAssistant"
      override val name = "Voice Assistant"
      override val description = "Voice Assistant plugin UI"
      override val descriptionLong = "$description."
      override val icon = IconMA.MIC
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2023)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(WidgetTags.UTILITY)
      override val summaryActions
         get() = APP.plugins.get<VoiceAssistant>()?.handlers.orEmpty().map {
            ShortcutPane.Entry("Voice", it.commandUi, it.name)
         }
   }
}