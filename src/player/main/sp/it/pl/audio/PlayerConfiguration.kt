package sp.it.pl.audio

import sp.it.pl.main.APP
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.MultiConfigurableBase
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cvn
import sp.it.util.conf.cvro
import sp.it.util.conf.only
import sp.it.util.units.seconds
import sp.it.util.validation.Constraint.FileActor.DIRECTORY

class PlayerConfiguration {
   companion object: MultiConfigurableBase("Playback") {

      @IsConfig(name = "Remember playback state", info = "Continue last remembered playback when application starts.")
      var continuePlaybackOnStart by c(true)

      @IsConfig(name = "Pause playback on start", info = "Continue last remembered playback paused on application start.")
      var continuePlaybackPaused by c(false)

      @IsConfig(name = "Seek time unit", info = "Time to jump by when seeking forward/backward.")
      var seekUnitT by c(4.seconds)

      @IsConfig(name = "Seek fraction", info = "Relative time in fraction of song's length to seek forward/backward by.")
      var seekUnitP by c(0.05).between(0.0, 1.0)

      @IsConfig(name = "Player", info = "Exact player implementation currently in use.", editable = EditMode.NONE)
      val playerInfo by cvro("<none>") { Player.player.pInfo }

      @IsConfig(name = "Vlc player location", editable = EditMode.APP)
      val playerVlcLocation by cvn<String>(null)

      @IsConfig(name = "Last browse location")
      var browse by c(APP.DIR_USERDATA).only(DIRECTORY)

      @IsConfig(name = "Last playlist export location")
      var lastSavePlaylistLocation by c(APP.DIR_USERDATA).only(DIRECTORY)

      @IsConfig(
         name = "No song modification",
         info = "Disallow all song modifications by this application." +
            "\n\nWhen true, app will be unable to change any song metadata" +
            "\n\nAfter setting this to false, it is recommended to run `Update library` action"
      )
      var readOnly by c(true)

   }
}