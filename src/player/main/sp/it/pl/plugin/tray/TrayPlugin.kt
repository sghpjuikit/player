package sp.it.pl.plugin.tray

import javafx.scene.control.ContextMenu
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.MIDDLE
import javafx.scene.input.MouseButton.NONE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.stage.Stage
import mu.KLogging
import sp.it.pl.audio.Player
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.util.async.runAwt
import sp.it.util.async.runFX
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.orNull
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.syncFalse
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.createImageBlack
import sp.it.util.ui.image.loadBufferedImage
import sp.it.util.ui.menuItem
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.io.File
import java.io.IOException

/** Provides tray facilities, such as tray icon, tray tooltip, tray click actions or tray bubble notification. */
class TrayPlugin: PluginBase("Tray", true) {

   private var tooltipText = APP.name
   @IsConfig(name = "Show tooltip", info = "Enables tooltip displayed when mouse hovers tray icon.")
   val tooltipShow by cv(true)
   @IsConfig(name = "Show playing in tooltip", info = "Shows playing song title in tray tooltip.")
   val showPlayingInTooltip by cv(true)
   @IsConfig(name = "Is supported", editable = EditMode.NONE)
   private val supported by c(SystemTray.isSupported())
   private var running = false
   private val onEnd = Disposer()

   private var tray: SystemTray? = null
   private val trayIconImageDefault = APP.DIR_RESOURCES/"icons"/"icon24.png"
   private var trayIconImage = trayIconImageDefault
   private var trayIcon: TrayIcon? = null
   private val onClickDefault: (MouseEvent) -> Unit = { APP.ui.toggleMinimize() }
   private var onClick = onClickDefault
   private var contextMenu: ContextMenu? = null
   private var contextMenuOwner: Stage? = null

   /** Right mouse click context menu items. */
   var contextMenuItemsBuilder = {
      listOf(
         menuItem("Show actions") { APP.actions.openOpen() },
         menuItem("Settings") { APP.actions.openSettings() },
         menuItem("New window") { APP.windowManager.createWindow() },
         menuItem("Play/pause") { Player.pause_resume() },
         menuItem("Disable tray") { stop() },
         menuItem("Exit") { APP.close() }
      )
   }

   override fun start() {
      if (!supported) return

      val cm = ContextMenu().apply {
         isAutoFix = true
         consumeAutoHidingEvents = false
      }
      val cmOwner = APP.windowManager.createStageOwner().apply {
         hide()
         focusedProperty() syncFalse {
            if (cm.isShowing) cm.hide()
            if (isShowing) hide()
            cm.items.clear()
         }
      }
      contextMenuOwner = cmOwner
      contextMenu = cm

      // build tray
      runAwt {
         tray = SystemTray.getSystemTray().apply {
            val image = loadBufferedImage(trayIconImage)
               .ifError { logger.warn { "Failed to load tray icon" } }
               .orNull()
               ?.getScaledInstance(trayIconSize.width, -1, Image.SCALE_SMOOTH)
               ?: createImageBlack(ImageSize(trayIconSize.size))
            val trayIconTmp = TrayIcon(image).apply {
               toolTip = tooltipText
               addMouseListener(object: MouseAdapter() {
                  override fun mouseClicked(e: java.awt.event.MouseEvent) {
                     val b = when (e.button) {
                        1 -> PRIMARY
                        2 -> MIDDLE
                        3 -> SECONDARY
                        4 -> FORWARD
                        5 -> BACK
                        else -> NONE
                     }
                     val me = MouseEvent(
                        MOUSE_CLICKED, -1.0, -1.0,
                        e.xOnScreen.toDouble(), e.yOnScreen.toDouble(), b, e.clickCount,
                        e.isShiftDown, e.isControlDown, e.isAltDown, e.isMetaDown,
                        b==PRIMARY, false, b==SECONDARY, false, true, true, null
                     )

                     // show menu on right click
                     when (me.button) {
                        PRIMARY -> runFX { onClick(me) }
                        SECONDARY -> runFX {
                           cmOwner.show()
                           cmOwner.requestFocus()
                           cm.items += contextMenuItemsBuilder()
                           cm.show(cmOwner, me.screenX, me.screenY - 40)
                        }
                        FORWARD -> PlaylistManager.use { it.playNextItem() }
                        BACK -> PlaylistManager.use { it.playPreviousItem() }
                        else -> Unit
                     }
                  }
               })
            }
            add(trayIconTmp)
            trayIcon = trayIconTmp
         }
      }

      onEnd += Player.playingSong.onUpdate {
         if (showPlayingInTooltip.value)
            setTooltipText(it.getTitle()?.let { "${APP.name} - $it" })
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

      runAwt {
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
      runAwt { trayIcon?.toolTip = t }
   }

   /** Equivalent to: `showNotification(caption,text,NONE)`.  */
   fun showNotification(caption: String, text: String) {
      if (!running || !supported) return

      runAwt { trayIcon?.displayMessage(caption, text, TrayIcon.MessageType.NONE) }
   }

   /**
    * Shows an OS tray bubble message notification.
    *
    * @param caption - the caption displayed above the text, usually in bold
    * @param text - the text displayed for the particular message
    * @param type - an enum indicating the message type
    */
   fun showNotification(caption: String, text: String, type: TrayIcon.MessageType) {
      if (!running || !supported) return

      runAwt { trayIcon?.displayMessage(caption, text, type) }
   }

   /** Set tray icon. Null sets default icon. */
   fun setIcon(img: File?): Try<Nothing?, IOException> {
      if (!running || !supported) return Try.ok()

      trayIconImage = img ?: trayIconImageDefault
      return if (trayIcon!=null) {
         loadBufferedImage(trayIconImage)
            .ifOk {
               trayIcon?.image?.flush()
               trayIcon?.image = it
            }
            .map { null }
      } else {
         Try.ok()
      }
   }

   /** Set action on left mouse tray click. Null sets default behavior. */
   fun setOnTrayClick(action: ((MouseEvent) -> Unit)?) {
      onClick = action ?: onClickDefault
   }

   companion object: KLogging()
}