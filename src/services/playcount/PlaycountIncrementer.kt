package services.playcount

import audio.Player
import audio.playback.PLAYBACK
import audio.playback.PlayTimeHandler
import audio.playback.PlayTimeHandler.Companion.at
import audio.tagging.Metadata
import audio.tagging.MetadataReader
import audio.tagging.MetadataWriter
import javafx.util.Duration.seconds
import main.App.APP
import services.ServiceBase
import services.notif.Notifier
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.NEVER
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_END
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_PERCENT
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_START
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_AND_PERCENT
import services.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_OR_PERCENT
import services.tray.TrayService
import util.SwitchException
import util.access.v
import util.action.IsAction
import util.conf.IsConfig
import util.conf.IsConfigurable
import util.functional.Util.max
import util.functional.Util.min
import util.math.Portion
import util.reactive.Disposer
import util.validation.Constraint
import java.awt.TrayIcon.MessageType.INFO
import java.util.*

/** Playcount incrementing service.  */
@IsConfigurable(value = "Playback.Playcount.Incrementing")
class PlaycountIncrementer : ServiceBase(false) {

    @IsConfig(name = "Incrementing strategy", info = "Playcount strategy for incrementing playback.")
    val whenStrategy = v(ON_PERCENT) { this.apply() }
    @Constraint.MinMax(min = 0.0, max = 1.0)
    @IsConfig(name = "Increment at percent", info = "Percent at which playcount is incremented.")
    val whenPercent = v(0.4) { this.apply() }
    @IsConfig(name = "Increment at time", info = "Time at which playcount is incremented.")
    val whenTime = v(seconds(5.0)) { this.apply() }
    @IsConfig(name = "Show notification", info = "Shows notification when playcount is incremented.")
    val showNotification = v(false)
    @IsConfig(name = "Show tray bubble", info = "Shows tray bubble notification when playcount is incremented.")
    val showBubble = v(false)
    @IsConfig(name = "Delay writing", info = "Delays writing playcount to tag for more seamless "
            + "playback experience. In addition, reduces multiple consecutive increments in a row "
            + "to a single operation. The writing happens when different song starts playing "
            + "(but the data in the application may update visually even later).")
    val delay = v(true)

    private val queue = ArrayList<Metadata>()
    private val incrementer = Runnable { this.increment() }
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
        queue.asSequence().distinct().forEach { incrementQueued(it) }
    }

    override fun isSupported() = true

    /**
     * Increments playcount of currently playing song. According to settings now or schedules it for
     * later. Also throws notifications if set.
     */
    @IsAction(name = "Increment playcount", desc = "Rises the number of times the song has been played by one and updates the song tag.")
    fun increment() {
        val m = Player.playingItem.get()
        if (!m.isEmpty && m.isFileBased) {
            if (delay.value) {
                queue += m
                if (showNotification.value)
                    APP.use(Notifier::class.java) { it.showTextNotification("Song playcount incrementing scheduled", "Playcount") }
                if (showBubble.value)
                    APP.use(TrayService::class.java) { it.showNotification("Tagger", "Playcount incremented scheduled", INFO) }
            } else {
                val pc = 1 + m.playcount
                MetadataWriter.use(m, { it.setPlaycount(pc) }) { ok ->
                    if (ok!!) {
                        if (showNotification.value)
                            APP.use(Notifier::class.java) { it.showTextNotification("Song playcount incremented to: $pc", "Playcount") }
                        if (showBubble.value)
                            APP.use(TrayService::class.java) { it.showNotification("Tagger", "Playcount incremented to: $pc", INFO) }
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
                PLAYBACK.onPlaybackAt.add(incHandler)
            }
            ON_TIME -> {
                incHandler = at(whenTime.value, incrementer)
                PLAYBACK.onPlaybackAt.add(incHandler)
            }
            ON_TIME_AND_PERCENT -> {
                incHandler = at({ total -> max(whenTime.value, total.multiply(whenPercent.value)) }, incrementer)
                PLAYBACK.onPlaybackAt.add(incHandler)
            }
            ON_TIME_OR_PERCENT -> {
                incHandler = at({ total -> min(whenTime.value, total.multiply(whenPercent.value)) }, incrementer)
                PLAYBACK.onPlaybackAt.add(incHandler)
            }
            ON_START -> PLAYBACK.onPlaybackStart += incrementer
            ON_END -> PLAYBACK.onPlaybackEnd += incrementer
            NEVER -> {}
            else -> throw SwitchException(whenStrategy.value)
        }
    }

    private fun removeOld() {
        PLAYBACK.onPlaybackAt -= incHandler
        PLAYBACK.onPlaybackEnd -= incrementer
        PLAYBACK.onPlaybackStart -= incrementer
    }

    private fun incrementQueued(m: Metadata) {
        val queuedTimes = queue.count { it.same(m) }
        if (queuedTimes > 0) {
            queue.removeIf { it.same(m) }
            val p = queuedTimes + m.playcount
            Player.IO_THREAD.execute {
                MetadataWriter.useNoRefresh(m) { it.setPlaycount(p) }
                Player.refreshItemWith(MetadataReader.readMetadata(m), true)
            }
        }
    }

    /** Strategy for incrementing playcount.  */
    enum class PlaycountIncStrategy {
        /** Increment when song starts playing.  */
        ON_START,
        /** Increment when song stops playing naturally.  */
        ON_END,
        /** Increment when song is playing for specified time.  */
        ON_TIME,
        /** Increment when song is playing for portion of its time.  */
        ON_PERCENT,
        /** Increment when song is playing for specified time or portion of its time.  */
        ON_TIME_OR_PERCENT,
        /** Increment when song is playing for specified time and portion of its time.  */
        ON_TIME_AND_PERCENT,
        /** Never increment.  */
        NEVER
    }
}