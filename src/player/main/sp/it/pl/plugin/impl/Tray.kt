package sp.it.pl.plugin.impl

import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.io.File
import javafx.scene.control.ContextMenu
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.MIDDLE
import javafx.scene.input.MouseButton.NONE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.stage.Screen
import javafx.stage.Stage
import mu.KLogging
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.main.APP
import sp.it.pl.main.App
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.util.async.AWT
import sp.it.util.async.future.Fut
import sp.it.util.async.runAwt
import sp.it.util.async.runFX
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.attach
import sp.it.util.reactive.syncFalse
import sp.it.util.reactive.syncWhileTrue
import sp.it.util.type.volatile
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.createImageBlack
import sp.it.util.ui.image.loadBufferedImage
import sp.it.util.ui.menuItem
import sp.it.util.ui.menuSeparator

class Tray: PluginBase() {

   val tooltipVisible by cv(true).def(name = "Show tooltip", info = "Enables tooltip displayed when mouse hovers tray icon.")
   val tooltipShowPlaying by cv(true).def(name = "Show playing in tooltip", info = "Shows playing song title in tray tooltip.")
   private var tooltipText = APP.name
   private val tooltipPlayingSongUpdater = Subscribed {
      tooltipShowPlaying syncWhileTrue {
         APP.audio.playingSong.updated attach { song ->
            setTooltipText(song.getTitle()?.let { "${APP.name} - $it" })
         }
      }
   }
   private var tray: Fut<SystemTray>? = null
   private val trayIconImageDefault: File = APP.location.resources.icons.icon48_png
   private var trayIconImage: File by volatile(trayIconImageDefault)
   private var trayIcon: TrayIcon? by volatile(null)
   private val onClickDefault: (MouseEvent) -> Unit = { APP.ui.toggleMinimize() }
   private var onClick by volatile(onClickDefault)
   private var contextMenu: ContextMenu? = null
   private var contextMenuOwner: Stage? = null

   override fun start() {
      val cm = ValueContextMenu<App>().apply {
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

      tray = runAwt {
         SystemTray.getSystemTray().apply {
            val image = loadBufferedImage(trayIconImage)
               .ifError { logger.warn { "Failed to load tray icon=$trayIconImage" } }
               .orNull()
               ?.scaledToTray(this)
               ?: createImageBlack(ImageSize(1, 1))
            val trayIconTmp = TrayIcon(image).apply {
               toolTip = tooltipText
               isImageAutoSize = true
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
                           cm.setItemsFor(APP)
                           cm.items += menuSeparator()
                           cm.items += menuItem("Disable tray") { APP.plugins.getRaw<Tray>()?.stop() }
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

      tooltipPlayingSongUpdater.subscribe()
   }

   override fun stop() {
      tooltipPlayingSongUpdater.unsubscribe()
      contextMenu = null
      contextMenuOwner?.close()
      contextMenuOwner = null

      tray?.then(AWT) { it.remove(trayIcon) }?.cancel()
      tray = null
   }

   /**
    * Sets the tooltip string for this tray icon. The tooltip is displayed
    * automatically when the mouse hovers over the icon.
    *
    * Null or empty text shows application name.
    */
   fun setTooltipText(text: String?) {
      tooltipText = text?.takeIf { it.isNotBlank() } ?: APP.name
      val t = text.takeIf { tooltipVisible.value }
      runAwt { trayIcon?.toolTip = t }
   }

   /**
    * Shows an OS tray bubble message notification.
    *
    * @param caption - the caption displayed above the text, usually in bold
    * @param text - the text displayed for the particular message
    * @param type - an enum indicating the message type
    */
   fun showNotification(caption: String, text: String, type: TrayIcon.MessageType = TrayIcon.MessageType.NONE) {
      runAwt { trayIcon?.displayMessage(caption, text, type) }
   }

   /** Set tray icon. Null sets default icon. */
   fun setIcon(img: File?) {
      trayIconImage = img ?: trayIconImageDefault

      tray?.then(AWT) { tray ->
         tray.trayIcons.first().ifNotNull { trayIcon ->
            loadBufferedImage(trayIconImage)
               .ifError { logger.warn { "Failed to load tray icon=$trayIconImage" } }
               .ifOk {
                  trayIcon.image?.flush()
                  trayIcon.image = it.scaledToTray(tray)
               }
         }
      }
   }

   /** Set action on left mouse tray click. Null sets default behavior. */
   fun setOnTrayClick(action: ((MouseEvent) -> Unit)?) {
      onClick = action ?: onClickDefault
   }

   companion object: KLogging(), PluginInfo {
      override val name = "Tray"
      override val description = "Provides OS tray facilities, such as tray icon, tray tooltip, tray click action or bubble notification"
      override val isSupported get() = SystemTray.isSupported()
      override val isSingleton = true
      override val isEnabledByDefault = true

      private fun SystemTray.trayIconSizeRaw() = (trayIconSize.width * (Screen.getScreens().maxOfOrNull { it.outputScaleX } ?: 1.0)).toInt()
      private fun Image.scaledToTray(tray: SystemTray) = getScaledInstance(tray.trayIconSizeRaw(), -1, Image.SCALE_SMOOTH)
   }
}