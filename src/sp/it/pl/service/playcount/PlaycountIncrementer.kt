package sp.it.pl.service.playcount

import javafx.util.Duration.seconds
import sp.it.pl.audio.Player
import sp.it.pl.audio.playback.PlayTimeHandler
import sp.it.pl.audio.playback.PlayTimeHandler.Companion.at
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataReader
import sp.it.pl.audio.tagging.MetadataWriter
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.Widgets
import sp.it.pl.service.ServiceBase
import sp.it.pl.service.notif.Notifier
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.NEVER
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_END
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_PERCENT
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_START
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_AND_PERCENT
import sp.it.pl.service.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_OR_PERCENT
import sp.it.pl.service.tray.TrayService
import sp.it.pl.util.access.v
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.functional.Util.max
import sp.it.pl.util.functional.Util.min
import sp.it.pl.util.math.Portion
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.validation.Constraint
import java.awt.TrayIcon.MessageType.INFO
import java.util.ArrayList

/** Playcount incrementing service. */
@IsConfigurable(value = "Playback.Playcount.Incrementing")
class PlaycountIncrementer: ServiceBase("Playcount Incrementer", false) {

    @IsConfig(name = "Incrementing strategy", info = "Playcount strategy for incrementing playback.")
    val whenStrategy = v(ON_PERCENT) { apply() }
    @Constraint.MinMax(min = 0.0, max = 1.0)
    @IsConfig(name = "Increment at percent", info = "Percent at which playcount is incremented.")
    val whenPercent = v(0.4) { apply() }
    @IsConfig(name = "Increment at time", info = "Time at which playcount is incremented.")
    val whenTime = v(seconds(5.0)) { apply() }
    @IsConfig(name = "Show notification", info = "Shows notification when playcount is incremented.")
    val showNotification = v(false)
    @IsConfig(name = "Show tray bubble", info = "Shows tray bubble notification when playcount is incremented.")
    val showBubble = v(false)
    @IsConfig(name = "Delay writing", info = "Delays writing playcount to tag for more seamless "
            +"playback experience. In addition, reduces multiple consecutive increments in a row "
            +"to a single operation. The writing happens when different song starts playing "
            +"(but the data in the application may update visually even later).")
    val delay = v(true)

    private val queue = ArrayList<Metadata>()
    private val incrementer = Runnable { increment() }
    private var incHandler: PlayTimeHandler? = null
    private var running = false
    private val onStop = Disposer()

    override fun start() {
        apply()
        onStop += Player.playingItem.onChange { ov, _ -> incrementQueued(ov) }
        running = true
    }

    override fun isRunning() = running

    override fun stop() {
        running = false
        apply()
        onStop()
        queue.distinctBy { it.uri }.forEach { incrementQueued(it) }
    }

    override fun isSupported() = true

    /**
     * Increments playcount of currently playing song. According to settings now or schedules it for
     * later. Also throws notifications if set.
     */
    @IsAction(name = "Increment playcount", desc = "Rises the number of times the song has been played by one and updates the song tag.")
    fun increment() {
        val m = Player.playingItem.get()
        if (!m.isEmpty() && m.isFileBased()) {
            if (delay.value) {
                queue += m
                if (showNotification.value)
                    APP.services.use<Notifier> { it.showTextNotification("Song playcount incrementing scheduled", "Playcount") }
                if (showBubble.value)
                    APP.services.use<TrayService> { it.showNotification(Widgets.TAGGER, "Playcount incremented scheduled", INFO) }
            } else {
                val pc = 1+m.getPlaycountOr0()
                MetadataWriter.use(m, { it.setPlaycount(pc) }) { ok ->
                    if (ok!!) {
                        if (showNotification.value)
                            APP.services.use<Notifier> { it.showTextNotification("Song playcount incremented to: $pc", "Playcount") }
                        if (showBubble.value)
                            APP.services.use<TrayService> { it.showNotification(Widgets.TAGGER, "Playcount incremented to: $pc", INFO) }
                    }
                }
            }
        }
    }

    private fun apply() {
        removeOld()
        if (!running) return

        when (whenStrategy.value) {
            ON_PERCENT -> {
                incHandler = at(Portion(whenPercent.value), incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_TIME -> {
                incHandler = at(whenTime.value, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_TIME_AND_PERCENT -> {
                incHandler = at({ total -> max(whenTime.value, total.multiply(whenPercent.value)) }, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_TIME_OR_PERCENT -> {
                incHandler = at({ total -> min(whenTime.value, total.multiply(whenPercent.value)) }, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_START -> Player.onPlaybackStart += incrementer
            ON_END -> Player.onPlaybackEnd += incrementer
            NEVER -> {}
        }
    }

    private fun removeOld() {
        Player.onPlaybackAt -= incHandler
        Player.onPlaybackEnd -= incrementer
        Player.onPlaybackStart -= incrementer
    }

    private fun incrementQueued(m: Metadata) {
        val queuedTimes = queue.count { it.same(m) }
        if (queuedTimes>0) {
            queue.removeIf { it.same(m) }
            val p = queuedTimes+m.getPlaycountOr0()
            Player.IO_THREAD.execute {
                MetadataWriter.useNoRefresh(m) { it.setPlaycount(p) }
                Player.refreshItemWith(MetadataReader.readMetadata(m), true)
            }
        }
    }

    /** Strategy for incrementing playcount. */
    enum class PlaycountIncStrategy {
        /** Increment when song starts playing. */
        ON_START,
        /** Increment when song stops playing naturally. */
        ON_END,
        /** Increment when song is playing for specified time. */
        ON_TIME,
        /** Increment when song is playing for portion of its time. */
        ON_PERCENT,
        /** Increment when song is playing for specified time or portion of its time. */
        ON_TIME_OR_PERCENT,
        /** Increment when song is playing for specified time and portion of its time. */
        ON_TIME_AND_PERCENT,
        /** Never increment. */
        NEVER
    }

}