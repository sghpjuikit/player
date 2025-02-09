package sp.it.pl.main

import java.io.File
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import sp.it.pl.layout.WidgetManager.FactoryRef
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Sort
import sp.it.util.action.Action
import sp.it.util.dev.failIf
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.MimeTypes
import sp.it.util.text.keys
import sp.it.util.text.nameUi

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
   val ICON_BROWSER = FactoryRef("Icon Browser", "IconPickerContent")
   val INSPECTOR = FactoryRef(INSPECTOR_NAME, "Inspector")
   val SONG_GROUP_TABLE = FactoryRef(SONG_GROUP_TABLE_NAME, "LibraryView")
   val SONG_TABLE = FactoryRef(SONG_TABLE_NAME, "Library")
   val SONG_TAGGER = FactoryRef(SONG_TAGGER_NAME, "Tagger")
   val LOGGER = FactoryRef(LOGGER_NAME, "Logger")
   val PLAYBACK = FactoryRef(PLAYBACK_NAME, "PlayerControls")
   val PLAYLIST = FactoryRef(PLAYLIST_NAME, "PlaylistView")
   val TESTER = FactoryRef(TESTER_NAME, "Tester")
}

/** Widget's intended functionality. */
object WidgetTags {
   const val APP = "App"
   const val LIBRARY = "Library"
   const val IOT = "IoT"
   const val VISUALISATION = "Visualisation"
   const val DEVELOPMENT = "Development"
   const val IMAGE = "Image"
   const val AUDIO = "Audio"
   const val VIDEO = "Video"
   const val UTILITY = "Utility"
   const val GAME = "Game"
}

object Actions {
   const val LAYOUT_MODE = "Layout overlay in/out"
   const val APP_SEARCH = "Search"
}

object Ui {
   const val FPS = 60.0 // frames per second (locked)

   const val STYLE_FONT_STYLE_NORMAL = "text-style-normal"
   const val STYLE_FONT_STYLE_ITALIC = "text-style-italic"
   const val STYLE_FONT_WEIGHT_NORMAL = "text-weight-normal"
   const val STYLE_FONT_WEIGHT_BOLD = "text-weight-bold"

   @JvmField val ICON_CLOSE = IconUN(0x271b)
   @JvmField val ICON_CONF = IconFA.GEARS
}

object Css {
   const val DESCRIPTION = "description"
   object Pseudoclasses {
      const val played = "played"
   }
}

object FileExtensions {
   const val fxwl = "fxwl"
   const val command = "spit-command"
   const val m3u = "m3u"
   const val m3u8 = "m3u8"
}

object FileMimes {
   val fxwl = MimeType("application/spit-component", FileExtensions.fxwl)
   val command = MimeType("application/spit-command", FileExtensions.command)
   val m3u = MimeTypes.ofExtension(FileExtensions.m3u).apply {
      failIf(this==MimeType.unknown)
      failIf(!hasExtension(FileExtensions.m3u))
      failIf(!hasExtension(FileExtensions.m3u8))
   }
}

object Events {

   data class ActionEvent(val action: Action) {
      override fun toString() = "ActionEvent(${action.name})"
   }

   sealed interface FileEvent {
      data class Create(val file: File): FileEvent
      data class Delete(val file: File): FileEvent
   }

   sealed interface AppEvent {
      sealed interface AppXGroundEvent: AppEvent {
         /** [java.awt.desktop.AppForegroundListener.appRaisedToForeground]. Raised on FX thread. */
         data object AppMovedToForeground: AppXGroundEvent
         /** [java.awt.desktop.AppForegroundListener.appMovedToBackground]. Raised on FX thread. */
         data object AppMovedToBackground: AppXGroundEvent
      }
      sealed interface AppHidingEvent: AppEvent {
         /** [java.awt.desktop.AppHiddenListener.appHidden]. Raised on FX thread. */
         data object AppHidden: AppHidingEvent
         /** [java.awt.desktop.AppHiddenListener.appUnhidden]. Raised on FX thread. */
         data object AppUnHidden: AppHidingEvent
      }
      sealed interface SystemSleepEvent: AppEvent {
         /** Happens just before [java.awt.desktop.SystemSleepListener.systemAboutToSleep] if sleep is invoked from this application. Allows reacting to sleep before it happens. Raised on FX thread. */
         data object SystemSleepPreEvent: SystemSleepEvent
         /** [java.awt.desktop.SystemSleepListener.systemAboutToSleep]. May be raised after wake-up, see [SystemSleepPreEvent]. Raised on FX thread. */
         data object SystemSleepStartEvent: SystemSleepEvent
         /** [java.awt.desktop.SystemSleepListener.systemAwoke]. Raised on FX thread. */
         data object SystemSleepStopEvent: SystemSleepEvent
      }
      sealed interface ScreenSleepEvent: AppEvent {
         /** [java.awt.desktop.ScreenSleepListener.screenAboutToSleep]. Raised on FX thread. */
         data object ScreenSleepStartEvent: ScreenSleepEvent
         /** [java.awt.desktop.ScreenSleepListener.screenAwoke]. Raised on FX thread. */
         data object ScreenSleepStopEvent: ScreenSleepEvent
      }
      sealed interface UserSessionEvent: AppEvent {
         /** [java.awt.desktop.UserSessionListener.userSessionActivated]. Raised on FX thread. */
         data object UserSessionStartEvent: UserSessionEvent
         /** [java.awt.desktop.UserSessionListener.userSessionDeactivated]. Raised on FX thread. */
         data object UserSessionStopEvent: UserSessionEvent
      }
      /** [java.awt.desktop.AppReopenedListener.appReopened]. Raised on FX thread. */
      data object AppReopenedEvent: AppEvent
   }

}

object HelpEntries {
    val Table = listOf(
       Entry("Table", "Filter", keys("CTRL+F")),
       Entry("Table", "Filter (cancel)", ESCAPE.nameUi),
       Entry("Table", "Filter (clear)", ESCAPE.nameUi),
       Entry("Table", "Search", "Type text"),
       Entry("Table", "Search (cancel)", ESCAPE.nameUi),
       Entry("Table", "Selection (cancel)", ESCAPE.nameUi),
       Entry("Table", "Scroll vertically", keys("Scroll")),
       Entry("Table", "Scroll horizontally", keys("Scroll+SHIFT")),
       Entry("Table columns", "Show column context menu", SECONDARY.toUi()),
       Entry("Table columns", "Swap columns", "Column drag"),
       Entry("Table columns", "Sort ${Sort.entries.joinToString(" | ") { it.toUi() }}", PRIMARY.nameUi),
       Entry("Table columns", "Sorts by multiple columns", keys("SHIFT+${PRIMARY.nameUi})")),
       Entry("Table row", "Selects item", PRIMARY.nameUi),
       Entry("Table row", "Show context menu", SECONDARY.nameUi),
       Entry("Table row", "Add items after row", "Drag & drop items"),
       Entry("Table footer", "Opens additional action menus", "Menu bar"),
    )
    val Grid = listOf(
      Entry("Grid", "Filter", keys("CTRL+F")),
      Entry("Grid", "Filter (cancel)", ESCAPE.nameUi),
      Entry("Grid", "Filter (clear)", ESCAPE.nameUi),
      Entry("Grid", "Search", "Type text"),
      Entry("Grid", "Search (cancel)", ESCAPE.nameUi),
      Entry("Grid", "Selection (cancel)", ESCAPE.nameUi),
      Entry("Grid", "Scroll vertically", keys("Scroll")),
      Entry("Grid", "Toggle cell size", keys("Scroll+CTRL")),
      Entry("Grid", "Scroll cell/view type", keys("Scroll+CTRL+SHIFT")),
      Entry("Grid", "Scroll cell size ratio", keys("Scroll+CTRL+ALT")),
      Entry("Grid cell", "Selects icon", "Hover or ${PRIMARY.nameUi}"),
      Entry("Grid cell", "Show context menu", SECONDARY.nameUi),
      Entry("Grid cell", "Copy item", keys("CTRL+C")),
      Entry("Grid footer", "Opens additional action menus", "Menu bar"),
    )
}