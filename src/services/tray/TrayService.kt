package services.tray

import audio.Player
import audio.playback.PLAYBACK
import gui.Gui
import javafx.application.Platform.runLater
import javafx.event.EventHandler
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton.MIDDLE
import javafx.scene.input.MouseButton.NONE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.stage.Stage
import main.App
import main.App.APP
import services.ServiceBase
import util.access.v
import util.conf.IsConfig
import util.conf.IsConfig.EditMode
import util.conf.IsConfigurable
import util.functional.Functors.Ƒ1
import util.functional.Try
import util.graphics.Util.menuItem
import util.graphics.image.loadBufferedImage
import util.reactive.Disposer
import util.reactive.syncFalse
import java.awt.EventQueue
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.io.File
import java.io.IOException

/** Provides tray facilities, such as tray icon, tray tooltip, tray click actions or tray bubble notification. */
@Suppress("unused")
@IsConfigurable("Tray")
class TrayService : ServiceBase(true) {

    private var tooltipText = APP.name
    @IsConfig(name = "Show tooltip", info = "Enables tooltip displayed when mouse hovers tray icon.")
    val tooltipShow = v(true) { setTooltipText(tooltipText) }
    @IsConfig(name = "Show playing in tooltip", info = "Shows playing song title in tray tooltip.")
    val showPlayingInTooltip = v(true)
    @IsConfig(name = "Is supported", info = "Shows playing song title in tray tooltip.", editable = EditMode.NONE)
    private val supported = SystemTray.isSupported()
    private var running = false
    private val onEnd = Disposer()

    private var tray: SystemTray? = null
    private val trayIconImageDefault = File(APP.DIR_APP, "icon24.png")
    private var trayIconImage = trayIconImageDefault
    private var trayIcon: TrayIcon? = null
    private val onClickDefault = EventHandler<MouseEvent> { Gui.toggleMinimize() }
    private var onClick = onClickDefault
    private var contextMenu: ContextMenu? = null
    private var contextMenuOwner: Stage? = null
    private val contextMenuItemsDefault = listOf(
            menuItem("New window", Runnable { APP.windowManager.createWindow() }),
            menuItem("Play/pause", Runnable { PLAYBACK.pause_resume() }),
            menuItem("Exit", Runnable { APP.close() })
    )
    private var contextMenuItems: MutableList<MenuItem> = ArrayList(contextMenuItemsDefault)

    override fun start() {
        if (!supported) return

        val cm = ContextMenu().apply {
            items += contextMenuItems
            isAutoFix = true
            consumeAutoHidingEvents = false
            // setOnShown(e -> run(3000, cm::hide));
        }
        val cmOwner = App.APP.windowManager.createStageOwner().apply {
            hide()
            focusedProperty() syncFalse {
                if (cm.isShowing) cm.hide()
                if (isShowing) hide()
            }
        }
        contextMenu = cm
        contextMenuOwner = cmOwner

        // build tray
        EventQueue.invokeLater {
            tray = SystemTray.getSystemTray().apply {
                val image = loadBufferedImage(trayIconImage).getOr(null)!!  // TODO: avoid !!
                        .getScaledInstance(this.trayIconSize.width, -1, Image.SCALE_SMOOTH)
                trayIcon = TrayIcon(image).apply {
                    toolTip = APP.name
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent) {
                            // transform to javaFX MouseEvent
                            val bi = e.button
                            val b = if (bi == 1) PRIMARY else if (bi == 3) SECONDARY else if (bi == 2) MIDDLE else NONE
                            val me = MouseEvent(MOUSE_CLICKED, -1.0, -1.0,
                                    e.xOnScreen.toDouble(), e.yOnScreen.toDouble(), b, e.clickCount,
                                    e.isShiftDown, e.isControlDown, e.isAltDown, e.isMetaDown,
                                    b == PRIMARY, false, b == SECONDARY, false, true, true, null)

                            // show menu on right click
                            when (me.button) {
                                PRIMARY -> runLater { onClick.handle(me) }
                                SECONDARY -> runLater {
                                    cmOwner.show()
                                    cmOwner.requestFocus()
                                    cm.show(cmOwner, me.screenX, me.screenY - 40)
                                }
                                else -> {}
                            }
                        }
                    })
                }
                add(trayIcon!!)
            }
        }

        onEnd += Player.playingItem.onUpdate { m ->
            if (showPlayingInTooltip.value)
                setTooltipText(m.getTitle()?.let { "${APP.name} - $it" })
        }

        running = true
    }

    override fun isRunning() = running

    override fun stop() {
        running = false

        contextMenu = null
        contextMenuOwner?.close()
        contextMenuOwner = null

        onEnd()

        EventQueue.invokeLater {
            tray?.remove(trayIcon)
            tray = null
        }
    }

    override fun isSupported() = supported

    /**
     * Sets the tooltip string for this tray icon. The tooltip is displayed
     * automatically when the mouse hovers over the icon.
     *
     * Null or empty text shows application name.
     */
    fun setTooltipText(text: String?) {
        if (!running || !supported) return

        tooltipText = text?.takeIf { it.isNotBlank() } ?: APP.name
        val t = text.takeIf { tooltipShow.value }
        EventQueue.invokeLater { trayIcon!!.toolTip = t }
    }

    /** Equivalent to: `showNotification(caption,text,NONE)`.  */
    fun showNotification(caption: String, text: String) {
        if (!running || !supported) return

        EventQueue.invokeLater { trayIcon?.displayMessage(caption, text, TrayIcon.MessageType.NONE) }
    }

    /**
     * Shows an OS tray bubble message notification.

     * @param caption - the caption displayed above the text, usually in bold
     * @param text - the text displayed for the particular message
     * @param type - an enum indicating the message type
     */
    fun showNotification(caption: String, text: String, type: TrayIcon.MessageType) {
        if (!running || !supported) return

        EventQueue.invokeLater { trayIcon?.displayMessage(caption, text, type) }
    }

    /** Set tray icon. Null sets default icon. */
    fun setIcon(img: File?): Try<Void, IOException> {
        trayIconImage = img ?: trayIconImageDefault

        if (!running || !supported) return Try.ok()

        return if (trayIcon != null) {
            loadBufferedImage(trayIconImage)
                    .ifOk {
                        trayIcon?.image?.flush()
                        trayIcon?.image = it
                    }
                    .map { null }
        } else Try.ok()
    }

    /** Set action on left mouse tray click. Null sets default behavior. */
    fun setOnTrayClick(action: EventHandler<MouseEvent>?) {
        onClick = action ?: onClickDefault
    }

    /** Adjust tray right mouse click context menu items. Null sets default context menu. */
    fun adjustContextMenuItems(action: Ƒ1<in MutableList<MenuItem>, out MutableList<MenuItem>>?) {
        contextMenuItems = action?.apply(contextMenuItems) ?: ArrayList(contextMenuItemsDefault)
        contextMenu?.items?.setAll(contextMenuItems)
    }

}