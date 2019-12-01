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
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.main.APP
import sp.it.util.Util.enumToHuman
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigImpl
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.conf.Constraint.FileRelative
import sp.it.util.conf.Constraint.HasNonNullElements
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.functional.Try
import sp.it.util.functional.Util.by
import sp.it.util.functional.invoke
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.onItemSync
import sp.it.util.reactive.syncFrom
import sp.it.util.type.isSuperclassOf
import java.io.File
import java.util.function.BiConsumer

open class EnumerableCF<T>: ConfigField<T> {
   protected val n: ComboBox<T>
   protected var suppressChanges = false

   @JvmOverloads
   constructor(c: Config<T>, enumeration: Collection<T> = c.enumerateValues()): super(c) {
      val converter: (T) -> String = c.findConstraint<UiConverter<T>>()?.converter
         ?: { enumToHuman(APP.converter.ui.toS(it)) }

      n = ImprovedComboBox(converter)
      n.styleClass += "combobox-field-config"

      val isSortable = c.constraints.none { it is PreserveOrder }
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
   private var editor: FileTextField
   private var isObservable: Boolean = false
   private val isNullable = c.findConstraint<ObjectNonNull>()==null

   init {
      val v = getObservableValue(c)
      isObservable = v!=null
      val fileType = c.findConstraint<FileActor>() ?: FileActor.ANY
      val relativeTo = c.findConstraint<FileRelative>()?.to

      editor = FileTextField(fileType, relativeTo)
      editor.styleClass += STYLECLASS_TEXT_CONFIG_FIELD
      editor.onEventDown(KEY_PRESSED, ENTER) { it.consume() }
      editor.value = config.value

      v?.attach { editor.value = it }
      editor.onValueChange = BiConsumer { _, _ -> apply(false) }
   }

   override fun getEditor() = editor

   override fun get() = if (isNullable || editor.value!=null) Try.ok(editor.value) else Try.error(ObjectNonNull.message())

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

private class ObservableListCF<T>(c: Config<ObservableList<T>>): ConfigField<ObservableList<T>>(c) {

   private val lc = c as ConfigImpl.ListConfig<T>
   private val chain: ChainValueNode.ListConfigField<T, ConfigurableField>
   private var isSyntheticLinkEvent = false
   private var isSyntheticListEvent = false
   private var isSyntheticSetEvent = false
   private val isNullable = c.findConstraint<HasNonNullElements>()==null
   private val list = lc.a.list

   init {

      chain = ChainValueNode.ListConfigField<T, ConfigurableField>(0) { ConfigurableField(lc.a.itemType, lc.a.factory()) }
      chain.editable syncFrom !chain.getNode().disableProperty()

      // bind list to chain
      chain.onUserItemAdded += {
         isSyntheticLinkEvent = true
         if (isNullableOk(it.chained.getVal())) list += it.chained.getVal()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemRemoved += {
         isSyntheticLinkEvent = true
         list -= it.chained.getVal()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemEnabled += {
         isSyntheticLinkEvent = true
         if (isNullableOk(it.chained.getVal())) list += it.chained.getVal()
         isSyntheticLinkEvent = false
      }
      chain.onUserItemDisabled += {
         isSyntheticLinkEvent = true
         list -= it.chained.getVal()
         isSyntheticLinkEvent = false
      }

      // bind chain to list
      list.onItemSync {
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.addChained(ConfigurableField(lc.a.itemType, it))
      }
      list.onItemRemoved {
         if (!isSyntheticLinkEvent && !isSyntheticSetEvent)
            chain.chain.find { it.chained.getVal()==it }?.let { chain.chain.remove(it) }
      }

      chain.growTo1()
   }

   private fun isNullableOk(it: T?) = isNullable || it!=null

   override fun getEditor() = chain.getNode()

   override fun get() = Try.ok(config.value)

   override fun refreshItem() {}

   private inner class ConfigurableField(private val type: Class<T>, value: T?): ValueNode<T?>(value) {
      private val pane = ConfigPane<T>()

      // TODO: improve, as is it can return wrong value
      val isAddableType: Boolean
         get() {
            if (Configurable::class.isSuperclassOf(type)) return false
            val configs = pane.getConfigFields()
            if (configs.size!=1) return false
            val config1Type = configs[0].config.type
            return config1Type==type
         }

      init {
         pane.onChange = Runnable {
            if (isAddableType && !isSyntheticListEvent && !isSyntheticLinkEvent) {
               isSyntheticSetEvent = true
               list setTo chain.chain.map { it.chained.getVal() }.filter { isNullableOk(it) }
               isSyntheticSetEvent = false
            }
         }
         pane.configure(lc.toConfigurable(this.value))
      }

      override fun getNode() = pane

      override fun getVal() = if (isAddableType) pane.getConfigFields()[0].configValue else value

   }
}