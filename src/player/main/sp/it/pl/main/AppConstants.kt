package sp.it.pl.main

object Widgets {
   const val APP_LAUNCHER = "AppLauncher"
   const val CONVERTER = "Converter"
   const val INSPECTOR = "Tree inspector"
   const val SONG_GROUP_TABLE = "Song Group Table"
   const val SONG_TABLE = "Song Table"
   const val SONG_TAGGER = "Song Tagger"
   const val LOGGER = "Logger"
   const val PLAYBACK = "Playback"
   const val PLAYLIST = "Playlist"
}

object Actions {
   const val LAYOUT_MODE = "Layout overlay in/out"
}

object Settings {

   object Plugin {
      const val name = "Plugins"
   }

   object Search {
      const val name = "Search"
   }

   object Ui {
      const val name = "Ui"

      object View {
         const val name = "${Ui.name}.View"

         object Action {
            const val name = "${View.name}.Action Viewer"
         }

         object Shortcut {
            const val name = "${View.name}.Shortcut Viewer"
         }
      }

      object Image {
         const val name = "${Ui.name}.Images"
      }

      object Table {
         const val name = "${Ui.name}.Table"
      }

      object Window {
         const val name = "${Ui.name}.Window"
      }

      object Dock {
         const val name = "${Ui.name}.Dock"
      }
   }

}

object Ui {
   const val STYLE_FONT_STYLE_NORMAL = "text-style-normal"
   const val STYLE_FONT_STYLE_ITALIC = "text-style-italic"
   const val STYLE_FONT_WEIGHT_NORMAL = "text-weight-normal"
   const val STYLE_FONT_WEIGHT_BOLD = "text-weight-bold"
}