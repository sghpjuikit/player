package sp.it.pl.plugin.playcount

import javafx.util.Duration.seconds
import sp.it.pl.audio.Player
import sp.it.pl.audio.playback.PlayTimeHandler
import sp.it.pl.audio.playback.PlayTimeHandler.Companion.at
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.write
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.notif.Notifier
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.MANUAL
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_END
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_PERCENT
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_START
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_AND_PERCENT
import sp.it.pl.plugin.playcount.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_OR_PERCENT
import sp.it.util.action.IsAction
import sp.it.util.conf.IsConfig
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.readOnlyUnless
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.map
import sp.it.util.units.times
import java.util.ArrayList

/** Playcount incrementing service. */
class PlaycountIncrementer: PluginBase("Playcount Incrementer", false) {

    @IsConfig(name = "Incrementing strategy", info = "Playcount strategy for incrementing playback.")
    val whenStrategy by cv(ON_TIME) attach { applyStrategy() }
    @IsConfig(name = "Increment at percent", info = "Percent at which playcount is incremented.")
    val whenPercent by cv(0.4).between(0.0, 1.0).readOnlyUnless(whenStrategy.map { it.needsPercent() }) attach { applyStrategy() }
    @IsConfig(name = "Increment at time", info = "Time at which playcount is incremented.")
    val whenTime by cv(seconds(5.0)).readOnlyUnless(whenStrategy.map { it.needsTime() }) attach { applyStrategy() }
    @IsConfig(name = "Show notification (schedule)", info = "Shows notification when playcount incrementing is scheduled.")
    val showNotificationSchedule by cv(false)
    @IsConfig(name = "Show notification (update)", info = "Shows notification when playcount is incremented.")
    val showNotificationUpdate by cv(false)
    @IsConfig(name = "Delay writing", info = "Delays editing tag until different song starts playing." +
        "\n\n* May improve playback experience." +
        "\n\n* Reduces consecutive updates to a single update."
    )
    val delay by cv(true)

    private val queue = ArrayList<Metadata>()
    private val incrementer = { increment() }
    private var incHandler: PlayTimeHandler? = null
    private var running = false
    private val onStop = Disposer()

    override fun start() {
        applyStrategy()
        onStop += Player.playingSong.onChange { ov, _ -> if (!ov.isEmpty()) incrementQueued(ov) }
        running = true
    }

    override fun isRunning() = running

    override fun stop() {
        running = false
        applyStrategy()
        onStop()
        queue.distinctBy { it.uri }.forEach { incrementQueued(it) }
    }

    override fun isSupported() = true

    /** Manually increments playcount of currently playing song. According to [delay] now or schedules it for later. */
    @IsAction(name = "Increment playcount", desc = "Manually increments number of times the song has been played by one.")
    fun increment() {
        val m = Player.playingSong.value
        if (!m.isEmpty() && m.isFileBased()) {
            if (delay.value) {
                queue += m
                if (showNotificationSchedule.value)
                    APP.plugins.use<Notifier> { it.showTextNotification("Song playcount incrementing scheduled", "Playcount") }
            } else {
                val pc = 1 + m.getPlaycountOr0()
                m.write({ it.setPlaycount(pc) }) {
                    if (it.isOk && showNotificationUpdate.value)
                        APP.plugins.use<Notifier> { it.showTextNotification("Song playcount incremented by 1 to: $pc", "Playcount") }
                }
            }
        }
    }

    private fun applyStrategy() {
        removeOld()
        if (!running) return

        when (whenStrategy.value) {
            ON_PERCENT -> {
                incHandler = at({ total -> total*whenPercent.value }, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_TIME -> {
                incHandler = at({ total -> whenTime.value min total*0.8 }, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_TIME_AND_PERCENT -> {
                incHandler = at({ total -> whenTime.value max total*whenPercent.value }, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_TIME_OR_PERCENT -> {
                incHandler = at({ total -> whenTime.value min total*whenPercent.value }, incrementer)
                Player.onPlaybackAt.add(incHandler)
            }
            ON_START -> Player.onPlaybackStart += incrementer
            ON_END -> Player.onPlaybackEnd += incrementer
            MANUAL -> Unit
        }
    }

    private fun removeOld() {
        Player.onPlaybackAt -= incHandler
        Player.onPlaybackEnd -= incrementer
        Player.onPlaybackStart -= incrementer
    }

    private fun incrementQueued(m: Metadata) {
        val by = queue.count { it.same(m) }
        if (by>0) {
            queue.removeIf { it.same(m) }
            val pc = by + m.getPlaycountOr0()
            m.write({ it.setPlaycount(pc) }) {
                if (it.isOk && showNotificationUpdate.value)
                    APP.plugins.use<Notifier> { it.showTextNotification("Song playcount incremented by: $by to: $pc", "Playcount") }
            }
        }
    }

    /** Strategy for auto-incrementing playcount. */
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
        MANUAL;

        fun needsTime() = this==ON_TIME || this==ON_TIME_OR_PERCENT || this==ON_TIME_AND_PERCENT

        fun needsPercent() = this==ON_PERCENT || this==ON_TIME_OR_PERCENT || this==ON_TIME_AND_PERCENT
    }

}