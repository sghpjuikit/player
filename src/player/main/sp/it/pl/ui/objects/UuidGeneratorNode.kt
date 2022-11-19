package sp.it.pl.ui.objects


import com.github.f4b6a3.uuid.UuidCreator
import javafx.geometry.Insets
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Pos.CENTER
import javafx.scene.control.TextArea
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import sp.it.pl.main.IconFA
import sp.it.pl.main.emScaled
import sp.it.pl.ui.item_node.ConfigEditor
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.v
import sp.it.util.collections.tabulate0
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.PropertyConfig
import sp.it.util.conf.min
import sp.it.util.conf.uiInfoConverter
import sp.it.util.conf.values
import sp.it.util.type.type
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupSiblingDown
import sp.it.util.ui.separator
import sp.it.util.ui.textAlignment
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox

class UuidGeneratorNode: StackPane() {
   init {
      val alg = ConfigEditor.create(PropertyConfig(type<String>(), "Type", ConfigDef(), setOf(), v("UUIDv4"), "UUIDv4", "").constrain { values(listOf("UUIDv1", "UUIDv4")); uiInfoConverter { if (it=="UUIDv1") "Time-based unique identifier" else "Random-based unique identifier" } })
      val count = ConfigEditor.create(PropertyConfig(type<Int>(), "Count", ConfigDef(), setOf(), v(1), 1, "").constrain { min(1) })
      lay += vBox {
         lay += separator(HORIZONTAL) { padding = Insets(12.emScaled) }
         lay += label("Generates UUID") { padding = Insets(12.emScaled) }
         lay += count.buildNode(false).apply { children.add(0, label("Count")) }
         lay += alg.buildNode(false).apply { children.add(0, label("Type")) }
         lay += vBox(null, CENTER) {
            padding = Insets(12.emScaled)
            lay += Icon(IconFA.PLAY).onClickDo {
               val uuids = tabulate0(count.config.value) { if (alg.config.value=="UUIDv1") { UuidCreator.getTimeBased() } else { UuidCreator.getRandomBased() } }
               it.lookupSiblingDown<TextArea>().text = uuids.joinToString("\n")
            }
            lay += textArea { padding = Insets(12.emScaled); isEditable = false; textAlignment = TextAlignment.CENTER }
         }
         lay += separator(HORIZONTAL) { padding = Insets(12.emScaled) }
      }
   }
}