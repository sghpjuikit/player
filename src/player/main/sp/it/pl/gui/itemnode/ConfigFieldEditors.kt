package sp.it.pl.gui.itemnode

import javafx.beans.Observable
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.event.EventHandler
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.ANY
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.gui.itemnode.textfield.FileTextField
import sp.it.pl.gui.objects.combobox.ImprovedComboBox
import sp.it.pl.main.APP
import sp.it.util.Util.enumToHuman
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.functional.Try
import sp.it.util.functional.Util.by
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.validation.Constraint
import java.io.File
import java.util.function.BiConsumer

open class EnumerableCF<T>: ConfigField<T> {
   protected val n: ComboBox<T>
   protected var suppressChanges = false

   @JvmOverloads
   constructor(c: Config<T>, enumeration: Collection<T> = c.enumerateValues()): super(c) {
      val converter: (T) -> String = c.findConstraint<Constraint.UiConverter<T>>()?.converter
         ?: { enumToHuman(APP.converter.ui.toS(it)) }

      n = ImprovedComboBox(converter)
      n.styleClass += "combobox-field-config"

      val isSortable = c.constraints.none { it is Constraint.PreserveOrder }
      val observable = getObservableValue(c)
      val isObservable = observable!=null
      if (isObservable)
         observable attach { refreshItem() }

      when (enumeration) {
         is ObservableList<*> -> {
            val source = enumeration as ObservableList<T>
            val list = observableArrayList<T>()
            val listSorted = if (isSortable) list.sorted(by(converter)) else list
            n.setItems(listSorted)

            list setTo enumeration
            source.onChange {
               suppressChanges = true
               list setTo enumeration
               n.value = null   // prevents JavaFX incorrectly applying next line
               n.value = c.value
               suppressChanges = false
            }
         }
         is ObservableSet<*> -> {
            val source = enumeration as ObservableSet<T>
            val list = observableArrayList<T>()
            val listSorted = if (isSortable) list.sorted(by(converter)) else list
            n.setItems(listSorted)

            list setTo enumeration
            source.onChange {
               suppressChanges = true
               list setTo enumeration
               n.value = null   // prevents JavaFX incorrectly applying next line
               n.value = c.value
               suppressChanges = false
            }
         }
         is Observable -> {
            val source = enumeration as Observable
            val list = observableArrayList<T>()
            val listSorted = if (isSortable) list.sorted(by(converter)) else list
            n.setItems(listSorted)

            list setTo enumeration
            source.addListener {
               suppressChanges = true
               list setTo enumeration
               n.value = null   // prevents JavaFX incorrectly applying next line
               n.value = c.value
               suppressChanges = false
            }
         }
         else -> {
            n.items setTo if (isSortable) enumeration.sortedBy(converter) else enumeration
         }
      }

      n.value = c.value
      n.valueProperty() attach {
         if (!suppressChanges)
            apply(false)
      }
   }

   public override fun get() = Try.ok(n.value)

   override fun refreshItem() {
      n.value = config.value
      // TODO: update warn icon since remove operation may render value illegal
   }

   override fun getEditor() = n
}

private class FileCF(c: Config<File>): ConfigField<File>(c) {
   internal var editor: FileTextField
   internal var isObservable: Boolean = false

   init {
      val v = getObservableValue(c)
      isObservable = v!=null
      val fileType = c.findConstraint<Constraint.FileActor>() ?: Constraint.FileActor.ANY
      val relativeTo = c.findConstraint<Constraint.FileRelative>()?.to

      editor = FileTextField(fileType, relativeTo)
      editor.styleClass += STYLECLASS_TEXT_CONFIG_FIELD
      editor.onEventDown(KEY_PRESSED, ENTER) { it.consume() }
      editor.value = config.value

      v?.attach { editor.value = it }
      editor.onValueChange = BiConsumer { _, _ -> apply(false) }
   }

   override fun getEditor() = editor

   public override fun get() = Try.ok(editor.value)   // TODO: return error when nullable not allowed

   override fun refreshItem() {
      if (!isObservable)
         editor.value = config.value
   }
}

private class KeyCodeCF: EnumerableCF<KeyCode> {

   constructor(c: Config<KeyCode>): super(c) {
      n.onKeyPressed = EventHandler { it.consume() }
      n.onKeyReleased = EventHandler { it.consume() }
      n.onKeyTyped = EventHandler { it.consume() }
      n.onEventUp(ANY) {
         // Note that in case of UP, DOWN, LEFT, RIGHT arrow keys and potentially others (any
         // which cause selection change) the KEY_PRESSED event will not get fired!
         //
         // Hence we set the value in case of key event of any type. This causes the value to
         // be set twice, but should be all right since the value is the same anyway.
         n.value = it.code

         it.consume()
      }
   }

}