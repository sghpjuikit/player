package sp.it.pl.plugin.impl

import javafx.util.Duration.seconds
import sp.it.pl.audio.playback.PlayTimeHandler
import sp.it.pl.audio.playback.PlayTimeHandler.Companion.at
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.write
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.plugin.impl.PlaycountIncrementer.Events.PlaycountInc
import sp.it.pl.plugin.impl.PlaycountIncrementer.Events.PlaycountIncScheduled
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.MANUAL
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.ON_END
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.ON_PERCENT
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.ON_START
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_AND_PERCENT
import sp.it.pl.plugin.impl.PlaycountIncrementer.PlaycountIncStrategy.ON_TIME_OR_PERCENT
import sp.it.util.action.IsAction
import sp.it.util.async.runFX
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyUnless
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.map
import sp.it.util.type.type
import sp.it.util.units.millis
import sp.it.util.units.times
import java.util.ArrayList

/** Playcount incrementing service. */
class PlaycountIncrementer: PluginBase() {

   val onStop = Disposer()
   val whenStrategy by cv(ON_TIME)
      .def(name = "Incrementing strategy", info = "Playcount strategy for incrementing playback.") attach { initStrategy() }
   val whenPercent by cv(0.4).between(0.0, 1.0).readOnlyUnless(whenStrategy.map { it.needsPercent() })
      .def(name = "Increment at percent", info = "Percent at which playcount is incremented.") attach { initStrategy() }
   val whenTime by cv(seconds(5.0)).readOnlyUnless(whenStrategy.map { it.needsTime() })
      .def(name = "Increment at time", info = "Time at which playcount is incremented.") attach { initStrategy() }
   val showNotificationSchedule by cv(false) {
         NotifySource<PlaycountIncScheduled>(type(), "On song playcount incrementing scheduled") {
            showTextNotification("Playcount", "Song\n${it.song.titleOrFilename}\nplaycount incrementing scheduled")
         }.toSubscribed(onStop).toV(it)
      }
      .def(name = "Show notification (schedule)", info = "Shows notification when playcount incrementing is scheduled.")
   val showNotificationUpdate by cv(false) {
         NotifySource<PlaycountInc>(type(), "On song playcount incremented") {
            showTextNotification("Playcount", "Song\n${it.song.titleOrFilename}\nplaycount incremented by ${it.by} to: ${it.to}")
         }.toSubscribed(onStop).toV(it)
      }
      .def(name = "Show notification (update)", info = "Shows notification when playcount is incremented.")
   val delay by cv(true)
      .def(name = "Delay writing", info = "Delays editing tag until different song starts playing." +
         "\n\n* May improve playback experience." +
         "\n\n* Reduces consecutive updates to a single update."
      )

   private val queue = ArrayList<Metadata>()
   private val incrementer = { increment() }
   private var incHandler: PlayTimeHandler = at({ it }, {})
   private val plyingSongIncrementer = Subscribed {
      APP.audio.playingSong.onChange { ov, _ ->
         if (!ov.isEmpty()) runFX(200.millis) {
            if (it.isSubscribed)
               incrementQueued(ov)
         }
      }
   }

   override fun start() {
      plyingSongIncrementer.subscribe()
   }

   override fun stop() {
      onStop()
      plyingSongIncrementer.unsubscribe()
      disposeStrategy()
      queue.distinctBy { it.uri }.forEach(::incrementQueued)
   }

   /** Manually increments playcount of currently playing song. According to [delay] now or schedules it for later. */
   @IsAction(name = "Increment playcount", info = "Manually increments number of times the song has been played by one.")
   fun increment() = increment(APP.audio.playingSong.value)

   /** Manually increments playcount of the specified song. According to [delay] now or schedules it for later. */
   fun increment(song: Metadata) {
      if (!song.isEmpty() && song.isFileBased()) {
         if (delay.value) {
            queue += song
            APP.actionStream(PlaycountIncScheduled(song))
         } else {
            val by = 1
            val to = song.getPlaycountOr0() + by
            song.write({ it.setPlaycount(to) }) {
               if (it.isOk) APP.actionStream(PlaycountInc(song, by, to))
            }
         }
      }
   }

   private fun initStrategy() {
      disposeStrategy()

      when (whenStrategy.value) {
         ON_PERCENT -> {
            incHandler = at({ total -> total*whenPercent.value }, incrementer)
            APP.audio.onPlaybackAt += incHandler
         }
         ON_TIME -> {
            incHandler = at({ total -> whenTime.value min total*0.8 }, incrementer)
            APP.audio.onPlaybackAt += incHandler
         }
         ON_TIME_AND_PERCENT -> {
            incHandler = at({ total -> whenTime.value max total*whenPercent.value }, incrementer)
            APP.audio.onPlaybackAt += incHandler
         }
         ON_TIME_OR_PERCENT -> {
            incHandler = at({ total -> whenTime.value min total*whenPercent.value }, incrementer)
            APP.audio.onPlaybackAt += incHandler
         }
         ON_START -> APP.audio.onPlaybackStart += incrementer
         ON_END -> APP.audio.onPlaybackEnd += incrementer
         MANUAL -> Unit
      }
   }

   private fun disposeStrategy() {
      APP.audio.onPlaybackAt -= incHandler
      APP.audio.onPlaybackEnd -= incrementer
      APP.audio.onPlaybackStart -= incrementer
   }

   private fun incrementQueued(song: Metadata) {
      val by = queue.count { it.same(song) }
      if (by>0) {
         queue.removeIf { it.same(song) }
         val to = song.getPlaycountOr0() + by
         song.write({ it.setPlaycount(to) }) {
            if (it.isOk) APP.actionStream(PlaycountInc(song, by, to))
         }
      }
   }

   object Events {
      data class PlaycountIncScheduled(val song: Metadata)
      data class PlaycountInc(val song: Metadata, val by: Int, val to: Int)
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

   companion object: PluginInfo {
      override val name = "Playcount Incrementer"
      override val description = "Provides configurable automatic incrementing of song playcount"
      override val isSupported = true
      override val isSingleton = false
      override val isEnabledByDefault = false

      val Metadata.titleOrFilename: String get() = getTitle() ?: getFilename()
   }
}