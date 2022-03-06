package sp.it.pl.layout

import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.layout.Pane
import javafx.stage.Stage
import sp.it.pl.layout.ComponentLoaderProcess.NEW_PROCESS
import sp.it.pl.layout.ComponentLoaderProcess.NORMAL
import sp.it.pl.layout.ComponentLoaderStrategy.DOCK
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.main.APP
import sp.it.pl.main.configure
import sp.it.pl.main.runAsAppProgram
import sp.it.util.async.FX
import sp.it.util.async.launch
import sp.it.util.collections.materialize
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.div
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.traverse
import sp.it.util.system.Os
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.pickTopMostAt
import sp.it.util.ui.toP
import sp.it.util.ui.xy

var Widget.forceLoading: Boolean
   get() = "forceLoading" in properties
   set(value) {
      if (value) properties["forceLoading"] = Any()
      else properties -= "forceLoading"
   }

fun Widget.focusAndTraverseFromToRoot() {
   traverse<Component> { it.parent }
      .windowed(2, 1, false) { it[1].asIs<Container<*>>() to it[0] }
      .forEach { (c, w) -> c.ui?.focusTraverse(w, this) }
}

fun Parent.widgetFocused(): Widget? = APP.widgetManager.widgets.findAll(OPEN).find { it.focused.value && it.window?.scene?.root == this }

fun Stage.widgetAtMousePos(): Widget? {
   val pos = APP.mouse.mousePosition.toP() - xy
   val node = scene?.root?.pickTopMostAt(pos.x, pos.y) { it.isVisible }
   return widgetContainingNode(node)
}

private fun widgetContainingNode(node: Node?): Widget? {
   if (node==null) return null
   val window = node.scene?.window
   return APP.widgetManager.widgets
      .findAll(OPEN).filter { it.window===window }
      .find { it.ui?.root?.isAnyParentOf(node) ?: false }
}

fun ComponentFactory<*>.loadIn(strategy: ComponentLoaderStrategy, process: ComponentLoaderProcess = NORMAL) {
   val c = this
   val cId = if (c is WidgetFactory<*>) c.id else c.name
   when (process) {
      NORMAL -> {
         FX.launch {
            strategy.loader(c.create())
            APP.widgetManager.widgets.componentLastOpenStrategiesMap(cId, strategy)
         }
      }
      NEW_PROCESS -> {
         val f = if (Os.WINDOWS.isCurrent) APP.location.spitplayerc_exe else APP.location.spitplayer_sh
         val fArgs = (APP.location/"SpitPlayer.l4j.ini").readLines().filter { it.isNotBlank() && !it.startsWith("#") }.toTypedArray()
         f.runAsAppProgram(
            "Launching component ${c.name} in new process",
            *fArgs, "--singleton=false", "--stateless=true", "open-component", c.name
         )
      }
   }
}

fun Component.openIn(strategy: ComponentLoaderStrategy, process: ComponentLoaderProcess = NORMAL) {
   val c = this
   when (process) {
      NORMAL -> {
         FX.launch {
            strategy.loader(c)
            if (c is Widget)  APP.widgetManager.widgets.componentLastOpenStrategiesMap(c.factory.id, strategy)
         }
      }
      NEW_PROCESS -> {
         val f = if (Os.WINDOWS.isCurrent) APP.location.spitplayerc_exe else APP.location.spitplayer_sh
         val fArgs = (APP.location/"SpitPlayer.l4j.ini").readLines().filter { it.isNotBlank() && !it.startsWith("#") }.toTypedArray()
         f.runAsAppProgram(
            "Launching component ${c.name} in new process",
            *fArgs, "--singleton=false", "--stateless=true", "open-component", c.name
         )
      }
   }
}

/** [configure] [Component.openIn] */
fun Component.openInConfigured() {
   val w = this
   object: ConfigurableBase<Any?>() {
      val strategy by cv<ComponentLoaderStrategy>(w.asIf<WidgetFactory<*>>()?.let { APP.widgetManager.widgets.componentLastOpenStrategiesMap[it.id] } ?: DOCK)
      val process by cv<ComponentLoaderProcess>(NORMAL)
   }.configure("Open") {
      w.toDb().toDomain()?.openIn(it.strategy.value, it.process.value)
   }
}

fun WidgetFactory<*>.reloadAllOpen() = also { widgetFactory ->
   failIfNotFxThread()
   WidgetManager.logger.info("Reloading all open widgets of {}", widgetFactory)

   APP.widgetManager.widgets.findAll(OPEN).asSequence()
      .filter { it.factory.id==widgetFactory.id }
      .filter { it.isLoaded }
      .materialize()
      .forEach {
         val widgetOld = it
         val widgetNew = widgetFactory.createRecompiled(widgetOld.id).apply {
            setStateFrom(widgetOld)
            forceLoading = true
         }
         val wasFocused = widgetOld.focused.value
         val widgetOldInputs = widgetOld.controller!!.io.i.getInputs().associate { it.name to it.value }
         fun Widget.restoreAuxiliaryState() {
            forceLoading = false
            failIf(controller==null)
            controller!!.io.i.getInputs().forEach { i -> widgetOldInputs[i.name].ifNotNull { i.valueAny = it } }
            if (wasFocused) focus()
         }

         val p = widgetOld.parent
         if (p!=null) {
            val i = widgetOld.indexInParent()

            val loadNotification = "reloading=" + widgetNew.id
            p.properties[loadNotification] = loadNotification // in some situations, container needs to know that after remove, add will come
            p.removeChild(i)
            p.addChild(i, widgetNew)
            p.properties -= loadNotification
            widgetNew.restoreAuxiliaryState()
         } else {
            val parent = widgetOld.graphics!!.parent
            val i = parent.childrenUnmodifiable.indexOf(widgetOld.graphics!!)
            widgetOld.close()
            parent.asIf<Pane?>()?.children?.add(i, widgetNew.load())
            widgetNew.restoreAuxiliaryState()
         }
      }
}