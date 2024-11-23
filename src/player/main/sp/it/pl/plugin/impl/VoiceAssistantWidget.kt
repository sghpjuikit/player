package sp.it.pl.plugin.impl

import io.ktor.client.request.get
import java.time.Instant
import javafx.animation.Interpolator.LINEAR
import javafx.animation.Transition.INDEFINITE
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.Cursor.HAND
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.util.Duration
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.core.bodyAsJs
import sp.it.pl.core.to
import sp.it.pl.layout.ComponentLoader.Ctx
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.WidgetUse
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.loadIn
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.errorLabel
import sp.it.pl.main.showFloating
import sp.it.pl.main.toUi
import sp.it.pl.plugin.impl.VoiceAssistant.LlmEngine
import sp.it.pl.plugin.impl.VoiceAssistantWidgetTimeline.Event
import sp.it.pl.plugin.impl.VoiceAssistantWidgetTimeline.Line
import sp.it.pl.plugin.impl.VoiceAssistantWidgetTimeline.View
import sp.it.pl.ui.ValueToggleButtonGroup
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.ui.pane.ActContext
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.ui.pane.ConfigPane.Layout.MINI
import sp.it.pl.ui.pane.ConfigPaneScrolPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.vAlways
import sp.it.util.access.visible
import sp.it.util.access.vn
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.invokeTry
import sp.it.util.async.coroutine.launch
import sp.it.util.async.coroutine.toSubscription
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateConfig
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.file.json.JsArray
import sp.it.util.file.json.JsFalse
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsNumber
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsTable
import sp.it.util.file.json.JsTrue
import sp.it.util.file.json.div
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.abs
import sp.it.util.math.min
import sp.it.util.reactive.Subscribed.Companion.subBetween
import sp.it.util.reactive.Subscribed.Companion.subscribedIff
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.flatMap
import sp.it.util.reactive.map
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.syncTo
import sp.it.util.reactive.syncWhile
import sp.it.util.reactive.zip
import sp.it.util.text.capitalLower
import sp.it.util.text.nameUi
import sp.it.util.ui.appendTextSmart
import sp.it.util.ui.flowPane
import sp.it.util.ui.hBox
import sp.it.util.ui.insertNewline
import sp.it.util.ui.isNewlineOnShiftEnter
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.setTextSmart
import sp.it.util.ui.singLineProperty
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year

class VoiceAssistantWidget(widget: Widget): SimpleController(widget) {
   private val textArea = textArea("")
   private var run = {}
   private val chatSettings = v(false)
   private val plugin = APP.plugins.plugin<VoiceAssistant>().asValue(onClose)

   private val mode by cv(Out.DEBUG)
      .def(name = "Tab", info = "Type of shown output.")
   private val submit by cv(Submit.CHAT)
      .def(name = "Submit", info = "Type of input to send.")
   private val speaker by cv(VoiceAssistant.mainSpeakerInitial)
      .valuesUnsealed { VoiceAssistant.obtainSpeakers() }
      .uiNoOrder()
      .def(name = "Speaker", info = "Speaker.")

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.lay += vBox(null, CENTER) {
         lay += hBox(null, CENTER) {
            lay += Icon(IconMD.AV_TIMER).tooltip("HW utilization").apply {
               onClickDo { APP.instances.factoryGpuNvidiaInfo.loadIn() }
            }
            lay += Icon(IconFA.COG).tooltip("All Settings").apply {
               disableProperty() syncFrom plugin.map { it==null }
               onClickDo { APP.actions.app.openSettings(plugin.value?.configurableGroupPrefix, ActContext(it)) }
            }
            lay += Icon(IconFA.REFRESH).tooltip("Restart voice assistent").apply {
               onClickDo { plugin.value?.restart() }
            }
            lay += label("   ")

            lay += Icon(IconMA.PERSON).apply {
               tooltip("Manage personas")
               onClickDo {
                  APP.widgetManager.widgets.find(VoiceAssistant.voiceAssistantPersonasWidgetFactory, WidgetUse.ANY(Ctx(ActContext(it))))
               }
            }
            lay += CheckIcon().icons(IconMD.SERVER, IconMD.SERVER_OFF).apply {
               selected syncFrom plugin.flatMap { it!!.llmEngine }.map { it!=LlmEngine.NONE }.orElse(false)
               disableProperty() syncFrom plugin.map { it==null }
               tooltip("Llm settings")
               onClickDo {
                  showFloating("Llm settings", DOWN_CENTER(this)) {
                     it.isAutohide.value = true
                     vBox {
                        lay += ConfigPaneScrolPane(
                           ConfigPane(
                              ListConfigurable.heterogeneous(
                                 plugin.value
                                    ?.net {
                                       listOf(
                                          it::llmEngine, it::llmEngineDetails,
                                          it::llmChatSysPromptFile, it::llmChatSysPrompt, it::llmChatTemp, it::llmChatTopK, it::llmChatTopP, it::llmChatMaxTokens,
                                          it::llmOpenAiServerStartCommand, it::llmOpenAiServerStopCommand
                                       )
                                    }
                                    .orEmpty()
                                    .map { it.getDelegateConfig() }
                              )
                           ).apply {
                              ui.value = MINI
                              editorOrder = ConfigPane.compareByDeclaration
                           }
                        )
                     }
                  }
               }
            }
            lay += CheckIcon().icons(IconMD.TEXT_TO_SPEECH, IconMD.TEXT_TO_SPEECH_OFF).apply {
               selected syncFrom plugin.flatMap { it!!.ttsOn }.orElse(false)
               disableProperty() syncFrom plugin.map { it==null }
               tooltip("Voice output settings")
               onClickDo {
                  showFloating("Voice output settings", DOWN_CENTER(this)) {
                     it.isAutohide.value = true
                     vBox {
                        lay += ConfigPaneScrolPane(
                           ConfigPane(
                              ListConfigurable.heterogeneous(
                                 plugin.value
                                    ?.net { listOf(it::ttsOn, it::ttsEngine, it::ttsEngineDetails, it::audioOuts) }
                                    .orEmpty()
                                    .map { it.getDelegateConfig() }
                              )
                           ).apply {
                              ui.value = MINI
                              editorOrder = ConfigPane.compareByDeclaration
                           }
                        )
                     }
                  }
               }
            }
            lay += CheckIcon().icons(IconMA.MIC, IconMA.MIC_OFF).apply {
               selected syncFrom plugin.flatMap { it!!.micEnabled }.orElse(false)
               disableProperty() syncFrom plugin.map { it==null }
               tooltip("Voice input settings")
               onClickDo {
                  showFloating("Voice input settings", DOWN_CENTER(this)) {
                     it.isAutohide.value = true
                     vBox {
                        lay += ConfigPaneScrolPane(
                           ConfigPane(
                              ListConfigurable.heterogeneous(
                                 plugin.value
                                    ?.net { listOf(it::micEnabled, it::mics, it::micVoiceDetectDetails, it::sttEngine, it::sttEngineDetails) }
                                    .orEmpty()
                                    .map { it.getDelegateConfig() }
                              )
                           ).apply {
                              ui.value = MINI
                              editorOrder = ConfigPane.compareByDeclaration
                           }
                        )

                        subBetween(it.onShown, it.onHiding) {
                           launch(VT) {
                              while (true) {
                                 runTry {
                                    val url = plugin.value?.httpUrl?.value?.net { "$it/mic/state"} ?: fail { "Voice Assistant not running" }
                                    APP.http.client.get(url).bodyAsJs().asJsObject().value.mapValues { it.value.to<Short>() }
                                 }.apply {
                                    FX {
                                       ifOk { mics -> mics.forEach { name, energy -> plugin.value?.mics?.find { it.name.value==name }?.energyCurrent?.value = Ok(energy min 32767) } }
                                       ifError { e -> plugin.value?.mics?.forEach { it.energyCurrent.value = Error(e) } }
                                    }
                                 }
                                 delay(125)
                              }
                           }.toSubscription()
                        }
                     }
                  }
               }
            }
            lay += label {
               plugin.sync { text = if (it!=null) "Active" else "Inactive" }
            }
            lay += label("   ")
            lay += ValueToggleButtonGroup.ofObservableValue(mode, Out.entries).apply {
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
               visible syncFrom mode.map { it == Out.HW }

               val errorProperty = vn<Throwable>(null)
               val actorStates = MapSet { a: ActorState -> a.type }
               val contentEr = stackPane {
                  id = "hw-er"
                  lay += label {
                     isWrapText = true
                     cursor = HAND
                     textProperty() syncFrom (errorProperty map { it?.message })
                     onEventDown(MOUSE_CLICKED, PRIMARY) { APP.ui.actionPane.show(errorProperty.value) }
                  }
               }
               val contentOk = flowPane(2.em.emScaled, 2.em.emScaled) {
                  id = "hw-ok"
                  alignment = CENTER;
                  rowValignment = VPos.CENTER
                  columnHalignment = HPos.CENTER

                  subscribedIff(this@scrollPane.visibleProperty()) {
                     launch(VT) {
                        while (true) {
                           runTry {
                              val url = plugin.value?.httpUrl?.value?.net { "$it/actor"} ?: fail { "Voice Assistant not running" }
                              APP.http.client.get(url).bodyAsJs().asJsObject().value
                           }.ifOk { actors ->
                              FX {
                                 errorProperty.value = null
                                 actors.forEach { type, actor ->
                                    actorStates.getOrPut(type) { ActorState(type).also { lay += it } }.update(actor.asJsObject())
                                 }
                              }
                           }.ifError {
                              FX {
                                 errorProperty.value = if (it is java.net.ConnectException) RuntimeException("Unable to connect") else it
                              }
                           }
                           delay(1000)
                        }
                     }.toSubscription()
                  }
               }

               errorProperty sync { content = if (it != null) contentEr else contentOk }
               errorProperty sync { isFitToHeight = it != null }
            }

            lay += stackPane {
               visible syncFrom mode.map { it == Out.EVENTS }

               val errorProperty = vn<Throwable>(null)
               val contentEr = stackPane {
                  id = "hw-er"
                  lay += errorLabel(errorProperty)
               }
               val contentOk = VoiceAssistantWidgetTimeline().apply {
                  mode.sync {
                     if (it==Out.EVENTS)
                        launch(FX) {
                           runTry {
                              val x = VT.invokeTry {
                                 val r = plugin.value?.events() ?: fail { "Voice Assistant not running" }
                                 val rFrom = Instant.ofEpochMilli((r / "started time")!!.to<Double>().toLong()*1000)
                                 val rTo = Instant.ofEpochMilli((r / "now")!!.to<Double>().toLong()*1000)
                                 val rEvents = APP.serializerJson.json.fromJsonValue<Map<String, List<EventProcessedRaw>>>((r / "events")!!).orThrow
                                 (rFrom to rTo) to rEvents.map { Line(it.key, it.value.map { it.asEvent() }) }
                              }.orThrow
                              view.value = View.between(x.first.first, x.first.second)
                              viewSpanMin.value = x.first.first
                              viewSpanMax.value = x.first.second
                              lines setTo x.second
                              errorProperty.value = null
                           }.ifError {
                              errorProperty.value = if (it is java.net.ConnectException) RuntimeException("Unable to connect") else it
                           }
                        }
                  }
               }

               errorProperty sync { children setToOne if (it != null) contentEr else contentOk }
            }

            lay += vBox(5.emScaled, CENTER) {
               visible syncFrom mode.map { it != Out.HW && it != Out.EVENTS }
               val textAreaPadding = v(Insets.EMPTY)

               lay(ALWAYS) += textArea.apply {
                  id = "output"
                  isEditable = false
                  isFocusTraversable = false
                  wrapTextProperty() syncFrom mode.map { it==Out.SPEAK }
                  paddingProperty() syncTo textAreaPadding
                  prefColumnCount = 100

                  onEventDown(KEY_PRESSED, ENTER) { appendText("\n") }
                  mode syncWhile { m ->
                     setTextSmart(plugin.value?.net(m.initText(this@VoiceAssistantWidget)) ?: "")
                     plugin.syncNonNullWhile { p ->
                        p.onLocalInput attach { (it, state) ->
                           when (m!!) {
                              Out.DEBUG -> appendTextSmart(it)
                              Out.INFO -> if (state!=null && state!="") appendTextSmart(it)
                              Out.SPEAK -> if (state=="SYS" || state=="USER") appendTextSmart(it)
                              Out.HW -> Unit
                              Out.EVENTS -> Unit
                           }
                        }
                     }
                  }
               }
               lay += stackPane {
                  id = "user-input-pane"
                  val userModeWidthcccWidth = v(0.0)
                  val promptVisible = v(true)
                  installActivityIndicator()

                  lay += textArea {
                     userModeWidthcccWidth attach { style = "-fx-padding: ${textAreaPadding.value.top} ${textAreaPadding.value.right + it} ${textAreaPadding.value.bottom} ${textAreaPadding.value.left};" }
                     id = "user-input"
                     isWrapText = true
                     isNewlineOnShiftEnter = true
                     prefColumnCount = 100
                     textProperty() sync { promptVisible.value = it.orEmpty().isEmpty() }

                     // reactive height
                     singLineProperty() sync {
                        styleclassToggle("text-area-singlelined", !it)
                        prefRowCount = if (it) 10 else 1
                     }

                     // action
                     run = { plugin.value.ifNotNull { submit.value.run(it, speaker.value, text) } }
                     onEventDown(KEY_PRESSED, ENTER) { if (it.isShiftDown) insertNewline() else run() }
                  }

                  lay += label {
                     id = "user-input-prompt-label"
                     style = "-fx-text-fill: -skin-prompt-font-color;"
                     textAlignment = TextAlignment.CENTER
                     this.isMouseTransparent = true
                     visibleProperty() syncFrom promptVisible
                     textProperty() syncFrom (plugin flatMap {
                        val p = "${ENTER.nameUi} to send, ${SHIFT.nameUi} + ${ENTER.nameUi} for new line"
                        if (it==null) vAlways(p)
                        else it.wakeUpWord.zip(it.llmOn).map { (_, chat) -> "Speak ${if (chat) "normally" else "`" + it.wakeUpWordPrimary.capitalLower() + "`"} or $p" }
                     })
                  }

                  lay(CENTER_RIGHT) += hBox(null, CENTER_RIGHT) {
                     id = "user-mode-pane"
                     isPickOnBounds = false

                     lay += label {
                        cursor = HAND
                        textProperty() syncFrom speaker
                        onEventDown(MOUSE_CLICKED) {
                           ::speaker.getDelegateConfig().configure("Edit speaker") { }
                        }
                     }
                     lay += Icon(IconFA.SEND).onClickDo { run() }.apply {
                        mode sync { tooltip(it.runDesc) }
                     }
                     lay += label {
                        cursor = HAND
                        textProperty() syncFrom submit.map { it.nameUi }
                        onEventDown(MOUSE_CLICKED) {
                           ::submit.getDelegateConfig().configure("Edit submit mode") { }
                        }
                     }
                     val labels = children.takeLast(3)
                     for (l in labels) l.boundsInLocalProperty() attach { userModeWidthcccWidth.value = labels.sumOf { it.boundsInLocal.width } + 2*spacing }
                  }
               }
            }
         }
      }
   }

   override fun focus() = textArea.requestFocus()

   private fun Pane.installActivityIndicator() {
      val a = anim(8.seconds) { at ->
         val a = at*2*PI
         val s = 100 + 100*((1 - at).abs min at.abs)
         val f = (cos(a) x sin(a))*s
         val t = (cos(a + PI) x sin(a + PI))*s
         style =
            "-fx-border-color: linear-gradient(from ${(width/2 + f.x).toInt()}px ${height/2 + f.y.toInt()}px to ${width/2 + t.x.toInt()}px ${height/2 + t.y.toInt()}px, transparent 25%, -fx-focus-color 50%, transparent 75%); -fx-border-width:2;"
      }.apply {
         interpolator = LINEAR
         cycleCount = INDEFINITE
      }

      plugin flatMap { it?.isProgress ?: vAlways(false) } attach { if (it) a.play() else a.pause() }
   }

   private inner class ActorState(val type: String): VBox() {
      init {
         lay += stackPane {
            lay += label(type) { styleClass += listOf("h4", "h4p") }
         }
      }
      fun labelOf(key: String) = label {
         id = key
         onEventDown(MOUSE_CLICKED, PRIMARY) {
            userData.ifNotNull { eType ->
               launch(FX) {
                  val events = VT.invokeTry { plugin.value?.state(type, eType as String) ?: fail { "Voice Assistant not running" } }
                  APP.ui.actionPane.show(events.map {
                     if (eType=="PROCESSED") APP.serializerJson.json.fromJsonValue<List<EventProcessedRaw>>(it).orNull()?.map { it.typed() } ?: it
                     else JsTable.of(it) ?: it
                  })
               }
            }
         }
         lay += this
      }
      fun update(state: JsObject) {
         state.value.forEach { (key, value) ->
            val label = children.findIsInstanceAnd<Label> { it.id==key } ?: labelOf(key)
            label.userData = when (key) { "events processing" -> "PROCESSING"; "events queued" -> "QUEUED"; "events processed" -> "PROCESSED"; else -> null }
            label.text = when (value) {
               is JsArray -> key + ": " + value.value.size.toUi()
               is JsObject -> key + ": " + value.value.size.toUi()
               is JsString -> key + ": " + value.value.toUi()
               is JsNumber -> key + ": " + value.value.toUi()
               is JsNull -> key + ": " + null.toUi()
               is JsTrue -> key + ": " + true.toUi()
               is JsFalse -> key + ": " + false.toUi()
            } + (label.userData?.net { " 🔍" } ?: "")
         }
      }
   }

   private data class EventProcessed(val `processed from`: Instant?, val `processed to`: Instant?, val `processed in`: Duration, val event: String)
   private data class EventProcessedRaw(val `processed from`: Double?, val `processed to`: Double?, val `processed in`: Double, val event: String) {
      fun typed() = EventProcessed(`processed from`?.net { Instant.ofEpochMilli((it*1000).toLong()) }, `processed to`?.net { Instant.ofEpochMilli((it*1000).toLong()) }, `processed in`.seconds, event)
      fun asEvent() = Event(event, `processed from`?.net { it*1000 } ?: NEGATIVE_INFINITY, `processed to`?.net { it*1000 } ?: POSITIVE_INFINITY)
   }
   private enum class Submit(
      override val nameUi: String,
      override val infoUi: String,
      val run: VoiceAssistant.(String, String) -> Unit,
   ): NameUi, InfoUi {
      CHAT("Chat", "Send the text to the Voice Assistant as if user spoke it", { speaker, text -> writeChat(speaker, text) }),
      SPEAK("Speak", "Narrates the specified text using synthesized voice", { _, text -> speak(text) }),
      COM("Command", "Send command and execute it", { _, text -> writeCom(text) }),
      PYT("Python", "Send python code and execute it", { speaker, text -> writeComPyt(speaker, null, text) }),
   }
   enum class Out(
      override val nameUi: String,
      val initText: VoiceAssistantWidget.(VoiceAssistant) -> String,
      val runDesc: String
   ): NameUi {
      DEBUG(
         "Trace",
         { it.pythonOutStd.value },
         "Show debug console output",
      ),
      INFO(
         "Info",
         { it.pythonOutEvent.value },
         "Show info console output",
      ),
      SPEAK(
         "Speak",
         { it.pythonOutSpeak.value },
         "Show user & system speech",
      ),
      HW(
         "Hw",
         { "" },
         "Show system state",
      ),
      EVENTS(
         "Events",
         { "" },
         "Show system event timeline",
      ),
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


