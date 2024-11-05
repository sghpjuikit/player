package sp.it.pl.core

import java.awt.desktop.AppReopenedEvent as AppReopenedEventFX
import java.awt.desktop.ScreenSleepEvent as ScreenSleepEventFX
import java.awt.desktop.SystemSleepEvent as SystemSleepEventFX
import java.awt.desktop.UserSessionEvent as UserSessionEventFX
import java.awt.Desktop
import java.awt.desktop.AppForegroundEvent
import java.awt.desktop.AppForegroundListener
import java.awt.desktop.AppHiddenEvent
import java.awt.desktop.AppHiddenListener
import java.awt.desktop.AppReopenedListener
import java.awt.desktop.ScreenSleepListener
import java.awt.desktop.SystemSleepListener
import java.awt.desktop.UserSessionListener
import javafx.stage.Stage
import mu.KLogging
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.main.APP
import sp.it.pl.main.App
import sp.it.pl.main.COM
import sp.it.pl.main.CacheHICON
import sp.it.pl.main.Events
import sp.it.pl.main.Events.AppEvent.AppHidingEvent.AppHidden
import sp.it.pl.main.Events.AppEvent.AppHidingEvent.AppUnHidden
import sp.it.pl.main.Events.AppEvent.AppReopenedEvent
import sp.it.pl.main.Events.AppEvent.AppXGroundEvent.AppMovedToBackground
import sp.it.pl.main.Events.AppEvent.ScreenSleepEvent
import sp.it.pl.main.Events.AppEvent.SystemSleepEvent
import sp.it.pl.main.Events.AppEvent.UserSessionEvent
import sp.it.pl.main.THUMBBUTTON
import sp.it.pl.main.WindowsTaskbarInternal
import sp.it.pl.main.WndProcCallbackOverride
import sp.it.pl.main.showFloating
import sp.it.pl.main.textColon
import sp.it.pl.main.toContiguousMemoryArray
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.window.stage.lookupHwnd
import sp.it.util.async.NEW
import sp.it.util.async.runFX
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.file.traverseParents
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.system.EnvironmentContext
import sp.it.util.system.Os
import sp.it.util.text.lengthInChars
import sp.it.util.type.volatile
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.seconds

object CoreEnv: Core, KLogging() {

   override fun init() {
      initEnvironmentContext()
      initAppEventListeners()
      initWindowsTaskbarButtons()
   }

   override fun dispose() {
      disposeWindowsTaskbarButtons()
   }

   private fun initEnvironmentContext() {
      EnvironmentContext.defaultChooseFileDir = APP.location
      EnvironmentContext.onFileRecycled = { APP.actionStream(Events.FileEvent.Delete(it)) }
      EnvironmentContext.onNonExistentFileBrowse = { f ->
         val existingParent = f.traverseParents().filter { it.exists() }.firstOrNull()
         runFX {
            showFloating("Browsing not possible") {
               vBox {
                  lay += stackPane {
                     lay += label("File or directory does not exist.") {
                        styleClass += "h4p-bottom"
                     }
                  }
                  lay += textColon("File", f.toUi())
                  lay += textColon("First parent that exists", existingParent)
               }
            }
         }
      }
   }

   private fun initAppEventListeners() =
      Desktop.getDesktop().addAppEventListener(
         object: AppForegroundListener, AppHiddenListener, AppReopenedListener, ScreenSleepListener, SystemSleepListener, UserSessionListener {
            override fun appRaisedToForeground(e: AppForegroundEvent) = runFX { APP.actionStream(AppMovedToBackground) }.toUnit()
            override fun appMovedToBackground(e: AppForegroundEvent) = runFX { APP.actionStream(AppMovedToBackground) }.toUnit()

            override fun appHidden(e: AppHiddenEvent) = runFX { APP.actionStream(AppHidden) }.toUnit()
            override fun appUnhidden(e: AppHiddenEvent) = runFX { APP.actionStream(AppUnHidden) }.toUnit()

            override fun appReopened(e: AppReopenedEventFX) = runFX { APP.actionStream(AppReopenedEvent) }.toUnit()

            override fun screenAboutToSleep(e: ScreenSleepEventFX) = runFX { APP.actionStream(ScreenSleepEvent.Start) }.toUnit()
            override fun screenAwoke(e: ScreenSleepEventFX) = runFX { APP.actionStream(ScreenSleepEvent.Stop) }.toUnit()

            override fun systemAboutToSleep(e: SystemSleepEventFX) = runFX { APP.actionStream(SystemSleepEvent.Start) }.toUnit()
            override fun systemAwoke(e: SystemSleepEventFX) = runFX { APP.actionStream(SystemSleepEvent.Stop) }.toUnit()

            override fun userSessionActivated(e: UserSessionEventFX) = runFX { APP.actionStream(UserSessionEvent.Start) }.toUnit()
            override fun userSessionDeactivated(e: UserSessionEventFX) = runFX { APP.actionStream(UserSessionEvent.Stop) }.toUnit()
         }
      )

   private var taskbarEventHandler: Any? by volatile(null)

   private fun initWindowsTaskbarButtons() {
      if (APP.rankAtStart==App.Rank.MASTER && Os.WINDOWS.isCurrent) {
         runFX(10.seconds) {
            // TODO: observe main window and install without delay
            APP.windowManager.getMain()?.stage?.ifNotNull { wMain ->
               NEW("taskbar-icon-installer") {
                  runTry {
                     val hwnd = runFX { wMain.lookupHwnd()!! }.blockAndGetOrThrow()
                     val buttons = listOf(
                        THUMBBUTTON(
                           THUMBBUTTON.THB_ICON or THUMBBUTTON.THB_TOOLTIP or THUMBBUTTON.THB_FLAGS,
                           1,
                           0,
                           CacheHICON create (APP.location.resources.icons/"taskbar-1-icon.ico").absolutePath,
                           CharArray(260).apply { System.arraycopy("Previous".toCharArray(), 0, this, 0, "Previous".lengthInChars) },
                           THUMBBUTTON.THBF_DISMISSONCLICK or THUMBBUTTON.THBF_ENABLED
                        ),
                        THUMBBUTTON(
                           THUMBBUTTON.THB_ICON or THUMBBUTTON.THB_TOOLTIP or THUMBBUTTON.THB_FLAGS,
                           2,
                           0,
                           CacheHICON create (APP.location.resources.icons/"taskbar-2-icon.ico").absolutePath,
                           CharArray(260).apply { System.arraycopy("Play/Pause".toCharArray(), 0, this, 0, "Play/Pause".lengthInChars) },
                           THUMBBUTTON.THBF_DISMISSONCLICK or THUMBBUTTON.THBF_ENABLED
                        ),
                        THUMBBUTTON(
                           THUMBBUTTON.THB_ICON or THUMBBUTTON.THB_TOOLTIP or THUMBBUTTON.THB_FLAGS,
                           3,
                           0,
                           CacheHICON create (APP.location.resources.icons/"taskbar-3-icon.ico").absolutePath,
                           CharArray(260).apply { System.arraycopy("Next".toCharArray(), 0, this, 0, "Next".lengthInChars) },
                           THUMBBUTTON.THBF_DISMISSONCLICK or THUMBBUTTON.THBF_ENABLED
                        )
                     )

                     taskbarEventHandler = WndProcCallbackOverride(hwnd) {
                        when (it) {
                           1 -> PlaylistManager.playPreviousItem()
                           2 -> APP.audio.pauseResume()
                           3 -> PlaylistManager.playNextItem()
                        }
                     }
                     WindowsTaskbarInternal.INSTANCE.ThumbBarAddButtons(hwnd, buttons.size, buttons.toContiguousMemoryArray())
                  }.ifError {
                     logger.error(it) { "Failed to install windows taskbar icons" }
                  }
               }
            }
         }
      }
   }

   private fun disposeWindowsTaskbarButtons() {
      taskbarEventHandler = null
      CacheHICON.disposeAll()
   }

}