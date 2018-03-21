package sp.it.pl.gui

import javafx.scene.Parent
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tooltip
import javafx.scene.text.Font
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.pl.gui.Gui.getSkins
import sp.it.pl.gui.Gui.reloadSkin
import sp.it.pl.gui.Gui.skin
import sp.it.pl.gui.Gui.skins
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.file.FileMonitor
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.isParentOf
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.setFontAsStyle
import sp.it.pl.util.reactive.doOnceIfNonNull
import sp.it.pl.util.reactive.listChangeHandlerEach
import sp.it.pl.util.reactive.sync
import java.net.MalformedURLException
import java.util.function.Consumer

private val logger = KotlinLogging.logger { }
private const val skinKey = "skin_old_url"
private const val skinInitMarker = "HAS_BEEN_INITIALIZED"

fun initSkins() {
    skins.clear()
    Gui.skins += getSkins()
    monitorSkinFiles()
    observeWindowsAndApplySkin()
}

private fun monitorSkinFiles() {
    FileMonitor.monitorDirectory(APP.DIR_SKINS, true) { type, file ->
        logger.info { "Change=$type detected in skin directory for $file" }

        skins.clear()
        skins += getSkins()

        val refreshAlways = true    // skins may import each other hence it is more convenient to refresh always
        val currentSkinDir = APP.DIR_SKINS.childOf(skin.get())
        val isActive = currentSkinDir.isParentOf(file)
        if (isActive || refreshAlways) reloadSkin()
    }
}

private fun observeWindowsAndApplySkin() {
    fun Parent.initializeFontAndSkin() {
        if (properties.containsKey(skinInitMarker)) return
        properties[skinInitMarker] = skinInitMarker

        Gui.skin sync { applySkinGui(it) }
        Gui.font sync { applyFontGui(it) }
    }

    fun Window.initializeFontAndSkin() {
        doOnceIfNonNull(sceneProperty(), Consumer { doOnceIfNonNull(it.rootProperty(), Consumer { it.initializeFontAndSkin() }) })
    }

    seqOf(Popup.getWindows(), Stage.getWindows(), ContextMenu.getWindows(), PopOver.active_popups)
            .forEach { windows ->
                windows.forEach { it.initializeFontAndSkin() }
                windows.addListener(listChangeHandlerEach(Consumer { it.initializeFontAndSkin() }))
            }
    Tooltip.getWindows().addListener(listChangeHandlerEach(Consumer { (it as? Tooltip)?.font = Gui.font.value }))
}

fun applySkin(skin: String) {
    seqOf(Popup.getWindows(), Stage.getWindows(), ContextMenu.getWindows(), PopOver.active_popups)
            .flatMap { it.asSequence() }
            .mapNotNull { it?.scene?.root }
            .forEach { it.applySkinGui(skin) }
}

private fun Parent.applyFontGui(font: Font) {
    setFontAsStyle(font)
}

private fun Parent.applySkinGui(skin: String) {
    val skinFile = APP.DIR_SKINS.childOf(skin, "$skin.css")
    val urlOld = properties[skinKey] as String?
    val urlNew = try {
        skinFile.toURI().toURL().toExternalForm()
    } catch (e: MalformedURLException) {
        logger.error(e) { "Could not load skin $skinFile" }
        null
    }
    if (urlOld!=null) stylesheets -= urlOld
    if (urlNew!=null) stylesheets += urlNew
    properties[skinKey] = urlNew
}