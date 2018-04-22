package sp.it.pl.audio

import sp.it.pl.main.AppUtil
import sp.it.pl.main.MultiConfigurableBase
import sp.it.pl.main.between
import sp.it.pl.main.c
import sp.it.pl.main.cv
import sp.it.pl.main.only
import sp.it.pl.util.access.V
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY

class PlayerConfiguration {
    companion object: MultiConfigurableBase("Playback") {

        @IsConfig(name = "Remember playback state", info = "Continue last remembered playback when application starts.")
        var continuePlaybackOnStart by c(true)

        @IsConfig(name = "Pause playback on start", info = "Continue last remembered playback paused on application start.")
        var continuePlaybackPaused by c(false)

        @IsConfig(name = "Seek time unit", info = "Fixed time unit to jump, when seeking forward/backward.")
        var seekUnitT by c(millis(4000))

        @IsConfig(name = "Seek fraction", info = "Relative time in fraction of song's length to seek forward/backward by.")
        var seekUnitP by c(0.05).between(0.0, 1.0)

        @IsConfig(name = "Player", info = "Exact player implementation currently in use.", editable = EditMode.APP)
        val playerInfo by cv("<none>") { V(it).apply { syncFrom(Player.player.pInfo) } } // TODO: make readonly

        @IsConfig(name = "Default browse location", info = "Opens this location for file dialogs.")
        var browse by c(AppUtil.APP.DIR_APP).only(DIRECTORY)
    }
}