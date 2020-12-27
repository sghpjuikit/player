package sp.it.pl.main

import java.io.File
import sp.it.pl.layout.widget.WidgetManager.FactoryRef

object Widgets {
   const val CONVERTER_NAME = "Converter"
   const val ICON_BROWSER_NAME = "Icon Browser"
   const val INSPECTOR_NAME = "Tree inspector"
   const val SONG_GROUP_TABLE_NAME = "Song Group Table"
   const val SONG_TABLE_NAME = "Song Table"
   const val SONG_TAGGER_NAME = "Song Tagger"
   const val LOGGER_NAME = "Logger"
   const val PLAYBACK_NAME = "Playback"
   const val PLAYLIST_NAME = "Playlist"
   const val TESTER_NAME = "Tester"

   val CONVERTER = FactoryRef(CONVERTER_NAME, "Converter")
   val ICON_BROWSER = FactoryRef(ICON_BROWSER_NAME, "IconBrowser")
   val INSPECTOR = FactoryRef(INSPECTOR_NAME, "Inspector")
   val SONG_GROUP_TABLE = FactoryRef(SONG_GROUP_TABLE_NAME, "LibraryView")
   val SONG_TABLE = FactoryRef(SONG_TABLE_NAME, "Library")
   val SONG_TAGGER = FactoryRef(SONG_TAGGER_NAME, "Tagger")
   val LOGGER = FactoryRef(LOGGER_NAME, "Logger")
   val PLAYBACK = FactoryRef(PLAYBACK_NAME, "PlayerControls")
   val PLAYLIST = FactoryRef(PLAYLIST_NAME, "PlaylistView")
   val TESTER = FactoryRef(TESTER_NAME, "Tester")
}

object Actions {
   const val LAYOUT_MODE = "Layout overlay in/out"
   const val APP_SEARCH = "Search"
}

object Ui {
   const val STYLE_FONT_STYLE_NORMAL = "text-style-normal"
   const val STYLE_FONT_STYLE_ITALIC = "text-style-italic"
   const val STYLE_FONT_WEIGHT_NORMAL = "text-weight-normal"
   const val STYLE_FONT_WEIGHT_BOLD = "text-weight-bold"
}

object Css {
   const val DESCRIPTION = "description"
}

object Events {
   sealed class FileEvent {
      data class Create(val file: File): FileEvent()
      data class Delete(val file: File): FileEvent()
   }
   sealed class AppEvent {
      sealed class AppXGroundEvent: AppEvent() {
         /** [java.awt.desktop.AppForegroundListener.appRaisedToForeground]. Raised on FX thread. */
         object AppMovedToForeground: AppXGroundEvent()
         /** [java.awt.desktop.AppForegroundListener.appMovedToBackground]. Raised on FX thread. */
         object AppMovedToBackground: AppXGroundEvent()
      }
      sealed class AppHidingEvent: AppEvent() {
         /** [java.awt.desktop.AppHiddenListener.appHidden]. Raised on FX thread. */
         object AppHidden: AppHidingEvent()
         /** [java.awt.desktop.AppHiddenListener.appUnhidden]. Raised on FX thread. */
         object AppUnHidden: AppHidingEvent()
      }
      sealed class SystemSleepEvent: AppEvent() {
         /** [java.awt.desktop.SystemSleepListener.systemAboutToSleep]. Raised on FX thread. */
         object Start: SystemSleepEvent()
         /** [java.awt.desktop.SystemSleepListener.systemAwoke]. Raised on FX thread. */
         object Stop: SystemSleepEvent()
      }
      sealed class ScreenSleepEvent: AppEvent() {
         /** [java.awt.desktop.ScreenSleepListener.screenAboutToSleep]. Raised on FX thread. */
         object Start: ScreenSleepEvent()
         /** [java.awt.desktop.ScreenSleepListener.screenAwoke]. Raised on FX thread. */
         object Stop: ScreenSleepEvent()
      }
      sealed class UserSessionEvent: AppEvent() {
         /** [java.awt.desktop.UserSessionListener.userSessionActivated]. Raised on FX thread. */
         object Start: UserSessionEvent()
         /** [java.awt.desktop.UserSessionListener.userSessionDeactivated]. Raised on FX thread. */
         object Stop: UserSessionEvent()
      }
      /** [java.awt.desktop.AppReopenedListener.appReopened]. Raised on FX thread. */
      object AppReopenedEvent: AppEvent()
   }
}