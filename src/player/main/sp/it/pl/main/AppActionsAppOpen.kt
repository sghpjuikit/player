package sp.it.pl.main

import javafx.stage.FileChooser.ExtensionFilter
import sp.it.pl.ui.pane.action
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.launch
import sp.it.util.file.FileType
import sp.it.util.functional.orNull
import sp.it.util.system.chooseFile
import sp.it.util.system.chooseFiles

/** Denotes actions for 'App.Open...' */
object AppOpen

/** Denotes actions for [AppOpen] */
object AppActionsAppOpen {

   val init = action<App>("Open...", "Set of actions to open things", IconMD.OPEN_IN_APP) { AppOpen }

   val file = action<AppOpen>("Select file", "Open file chooser to select files", IconUN(0x1f4c4)) {
      chooseFiles("Select file...", null, window).orNull() ?: Unit
   }

   val directory = action<AppOpen>("Select directory", "Open file chooser to select directory", IconUN(0x1f4c1)) {
      chooseFile("Select directory...", FileType.DIRECTORY, null, window).orNull() ?: Unit
   }

   val widget = action<AppOpen>("Open widget", "Open file chooser to open an exported widget", IconMA.WIDGETS) {
      chooseFile("Open widget...", FileType.FILE, APP.location.user.layouts, window, ExtensionFilter("Component", "*.fxwl")).ifOk { f ->
         launch(FX) { APP.windowManager.launchComponent(f) }
      }
      Unit
   }

   val skin = action<AppOpen>("Open skin", "Open file chooser to find a skin", IconMA.BRUSH) {
      chooseFile("Open skin...", FileType.FILE, APP.location.skins, window, ExtensionFilter("Skin", "*.css")).ifOk {
         APP.ui.setSkin(it)
      }
      Unit
   }

   val audio = action<AppOpen>("Open audio files", "Open file chooser to find a audio files", IconFA.FILE_AUDIO_ALT) {
      chooseFiles("Open audio...", APP.locationHome, window, audioExtensionFilter())
   }

   val video = action<AppOpen>("Open video files", "Open file chooser to find a video files", IconFA.FILE_VIDEO_ALT) {
      chooseFiles("Open video...", APP.locationHome, window, audioExtensionFilter())
   }

   val audioOrVideo = action<AppOpen>("Open audio or video files", "Open file chooser to find a audio files", IconFA.FILE_SOUND_ALT) {
      chooseFiles("Open audio or video...", APP.locationHome, window, audioOrVideoExtensionFilter())
   }

}