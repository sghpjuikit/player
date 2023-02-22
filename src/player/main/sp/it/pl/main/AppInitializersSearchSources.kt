package sp.it.pl.main

import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.text.TextAlignment
import sp.it.pl.conf.Command
import sp.it.pl.layout.ComponentLoaderProcess
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.loadIn
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry.SimpleEntry
import sp.it.pl.ui.pane.ActContext
import sp.it.pl.ui.pane.ActionData
import sp.it.util.access.focused
import sp.it.util.action.Action
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfList
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.Constraint
import sp.it.util.conf.EditMode
import sp.it.util.conf.ListConfig
import sp.it.util.conf.ValueConfig
import sp.it.util.conf.but
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.reactive.sync1If
import sp.it.util.system.Windows
import sp.it.util.type.VType
import sp.it.util.type.isObject
import sp.it.util.type.raw
import sp.it.util.type.typeNothingNullable
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupSiblingUp
import sp.it.util.ui.textField

fun AppSearch.initApp() = APP.apply {

   data class OsAction(val name: String, val icon: Glyph, val action: () -> Unit)
   val osActions = sequenceOf(
      OsAction("Sleep",     IconWH.MOON_27)            { Windows.sleep().ifErrorDefault() },
      OsAction("Hibernate", IconWH.MOON_THIRD_QUARTER) { Windows.hibernate().ifErrorDefault() },
      OsAction("Shutdown",  IconWH.MOON_14)            { Windows.shutdown().ifErrorDefault() },
      OsAction("Restart",   IconWH.MOON_0)             { Windows.restart().ifErrorDefault() },
      OsAction("Lock",      IconFA.LOCK)               { Windows.lock().ifErrorDefault() },
      OsAction("Log off",   IconMD.LOGOUT)             { Windows.logOff().ifErrorDefault() },
   )
   sources += AppSearch.Source("Os actions") { osActions } by { it.name } toSource {
      Entry.of(
         name = "Os: ${it.name}",
         icon = it.icon,
         run = { it.action() }
      )
   }

   data class Radix(val radix: Int, val name: String)
   val radixes = mapOf("bin" to 2, "oct" to 8, "dec" to 10, "hex" to 16)
   sources += AppSearch.Source("Math number conversions") { radixes.keys.asSequence() } byAny { "bin oct dec hex" } toSource {
      Entry.of(
         name = "Math: convert to $it",
         icon = IconFA.ARROW_RIGHT,
      ) {
         val v = term.trim()
         val radixFromS = v.dropWhile { it.isDigit() }.trim().substringBefore(" ").trim().lowercase().ifEmpty { "dec" }
         val radixFrom = radixes[radixFromS] ?: 10
         val radixToS = it
         val radixTo = radixes[radixToS] ?: 10
         val nFrom = v.takeWhile { it.isDigit() }.ifEmpty { "0" }
         val nTo = nFrom.toBigInteger(radixFrom).toString(radixTo)
         showFloating("Convert $it") {
            hBox(5.emScaled, Pos.CENTER) {
               lay += label(radixFromS)
               lay += textField(nFrom).apply { isFocusTraversable = false }
               lay += label("=")
               lay += label(radixToS)
               lay += textField(nTo).apply { focused.sync1If({ it }) { lookupSiblingUp<TextField>(3).isFocusTraversable = true } }
            }
         }
      }
   }

   sources += AppSearch.Source("Settings") {
      configuration.getConfigs().flatMap {
         it.group.traverse { it.substringBeforeLast(".", "").takeIf { it.isNotEmpty() } }.asIterable()
      }.toSet().asSequence()
   } by { it.substringAfterLast(".") } toSource {
      Entry.of(
         name = "Open settings: ${it.substringAfterLast(".")}",
         icon = IconFA.COG,
         graphics = label("Settings > " + it.replace(".", " > ")) {
            styleClass += Css.DESCRIPTION
            textAlignment = TextAlignment.RIGHT
         }
      ) {
         actions.app.openSettings(it)
      }
   }

   sources += AppSearch.Source("Actions") {
      configuration.getConfigs().asSequence().filterIsInstance<Action>().filter { it.isEditableByUserRightNow() }
   } by { it.name + it.keys } toSource {
      Entry.of(it)
   }

   sources += AppSearch.Source("Actions (parametric)") {
      ActionsPaneGenericActions.actionsAll.values.asSequence().flatten()
   } by { it.name } toSource {
      Entry.of(
         name = it.nameWithDots,
         icon = it.icon,
         graphics = null
      ) {

         fun <T1, TN> ActionData<T1, TN>.invokeWithForm() {
            val context = ActContext(null, null, null, null)
            when {
               type.raw.isObject && !type.isNullable -> invokeFutAndProcess(context, type.raw.objectInstance.asIs())
               type.raw == Unit::class -> invokeFutAndProcess(context, Unit.asIs())
               type == typeNothingNullable() -> invokeFutAndProcess(context, null.asIs())
               else -> {
                  val receiver = when {
                     type1!=typeN -> {
                        val t1: VType<T1> = when (type1.raw) {
                           Any::class -> VType(String::class.java, type1.isNullable).asIs()  // Any::class does not have an editor, but String editor is still plenty useful
                           else -> type1
                        }
                        val confList = ConfList(t1, null, { Config.forValue(t1, "Item", it).constrain { but(buildConstraint1()); but(Constraint.ObjectNonNull) } })
                        ListConfig("Input", ConfigDef("Input", "Input", "", EditMode.USER), confList, "", setOf(), setOf()).constrain {
                           addConstraint(buildConstraintN().asIs())
                           addConstraint(Constraint.CollectionSize(1, null))
                           but()
                        }
                     }
                     else -> {
                        val tn: VType<TN> = when (type.raw) {
                           Any::class -> VType(String::class.java, type.isNullable).asIs()  // Any::class does not have an editor, but String editor is still plenty useful
                           else -> type
                        }
                        ValueConfig(tn, "Input", "Input", null, "", description, EditMode.USER).constrain { but(buildConstraintN()) }
                     }
                  }

                  receiver.configure(it.nameWithDots) { invokeFutAndProcess(context, it.value.asIs()) }
               }
            }
         }
         it.invokeWithForm()
      }
   }

   sources += AppSearch.Source("Commands") {
      configuration.getConfigs().asSequence().filter { it.type.type.raw==Command::class }
         .mapNotNull { c -> c.value?.asIs<Command>()?.net { c to it } }
   } by { (config, _) -> config.nameUi } toSource { (config, command) ->
      Entry.of(
         name = "Run command: ${config.nameUi} = ${command.toUi()}",
         icon = IconMA.PLAY_ARROW,
         graphics = null
      ) {
         command()
      }
   }

   sources += AppSearch.Source("Skins") {
      ui.skins.asSequence()
   } by { "Open skin: ${it.name}" } toSource {
      Entry.of(
         name = "Open skin: ${it.name}",
         icon = IconMA.BRUSH,
         graphics = null
      ) {
         ui.skin.value = it.name
      }
   }

   sources += AppSearch.Source("Components - open") {
      widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() }
   } by { "Open widget ${it.name}" } toSource { c ->
      val id = if (c is WidgetFactory<*>) c.id else c.name
      val strategyCB = SpitComboBox<ComponentLoaderStrategy>({ it.toUi() }).apply {
         items setTo ComponentLoaderStrategy.values()
         value = widgetManager.widgets.componentLastOpenStrategiesMap[id] ?: ComponentLoaderStrategy.DOCK
      }
      val processCB = SpitComboBox<ComponentLoaderProcess>({ it.toUi() }).apply {
         items setTo ComponentLoaderProcess.values()
         value = ComponentLoaderProcess.NORMAL
      }
      Entry.of(
         name = "Open widget ${c.name}",
         icon = IconFA.TH_LARGE,
         infoΛ = { "Open widget ${c.name}" },
         graphics = hBox { lay += strategyCB; lay += processCB }
      ) {
         c.loadIn(strategyCB.value, processCB.value)
      }
   }

   sources += AppSearch.Source("Components - recompile") {
      widgetManager.factories.getFactories().filter { it.isUsableByUser() }
   } by { "Recompile widget ${it.name}" } toSource { c ->
      SimpleEntry(
         name = "Recompile widget ${c.name}",
         icon = IconFA.TH_LARGE,
         infoΛ = { "Recompile widget ${c.name} and reload all of its instances upon success" }
      ) {
         widgetManager.factories.recompile(c)
      }
   }
}