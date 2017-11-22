package sp.it.pl.gui

import javafx.scene.Parent
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tooltip
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.main.App.APP
import sp.it.pl.util.file.childOf
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.setFontAsStyle
import sp.it.pl.util.reactive.doOnceIfNonNull
import sp.it.pl.util.reactive.listChangeHandlerEach
import sp.it.pl.util.reactive.sync
import java.net.MalformedURLException
import java.util.function.Consumer

private val logger = KotlinLogging.logger { }

fun observeSkin() {

    fun Parent.applyFont() {
        setFontAsStyle(Gui.font.get())
    }

    fun Parent.applySkin() {
        val skinFile = APP.DIR_SKINS.childOf(Gui.skin.value, "${Gui.skin.value}.css")
        try {
            stylesheets?.add(skinFile.toURI().toURL().toExternalForm())
        } catch (e: MalformedURLException) {
            logger.error(e) { "Could not load skin $skinFile" }
        }
    }

    fun Parent.initializeFontAndSkin() {
        val marker = "HAS_BEEN_INITIALIZED"
        if (properties.containsKey(marker)) return
        properties.put(marker, marker)

        Gui.skin sync { applySkin() }
        Gui.font sync { applyFont() }
    }

    fun Window.initializeFontAndSkin() {
        doOnceIfNonNull(sceneProperty(), Consumer { doOnceIfNonNull(it.rootProperty(), Consumer { it.initializeFontAndSkin()} ) })
    }

    seqOf(Popup.getWindows(), Stage.getWindows(), ContextMenu.getWindows(), PopOver.active_popups)
            .forEach { windows ->
                windows.forEach { it.initializeFontAndSkin() }
                windows.addListener(listChangeHandlerEach(Consumer { it.initializeFontAndSkin() }))
            }
    Tooltip.getWindows().addListener(listChangeHandlerEach(Consumer { (it as? Tooltip)?.font = Gui.font.value }))
}