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
import sp.it.pl.main.APP
import sp.it.pl.main.Events
import sp.it.pl.main.Events.AppEvent.AppHidingEvent.AppHidden
import sp.it.pl.main.Events.AppEvent.AppHidingEvent.AppUnHidden
import sp.it.pl.main.Events.AppEvent.AppReopenedEvent
import sp.it.pl.main.Events.AppEvent.AppXGroundEvent.AppMovedToBackground
import sp.it.pl.main.Events.AppEvent.ScreenSleepEvent
import sp.it.pl.main.Events.AppEvent.SystemSleepEvent
import sp.it.pl.main.Events.AppEvent.UserSessionEvent
import sp.it.pl.main.showFloating
import sp.it.pl.main.textColon
import sp.it.pl.main.toUi
import sp.it.util.async.runFX
import sp.it.util.functional.toUnit
import sp.it.util.functional.traverse
import sp.it.util.system.EnvironmentContext
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox

object CoreEnv: Core {
   override fun init() {
      EnvironmentContext.defaultChooseFileDir = APP.location
      EnvironmentContext.onFileRecycled = { APP.actionStream(Events.FileEvent.Delete(it)) }
      EnvironmentContext.onNonExistentFileBrowse = { f ->
         val existingParent = f.traverse { it.parentFile }.filter { it.exists() }.firstOrNull()
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
   }
}