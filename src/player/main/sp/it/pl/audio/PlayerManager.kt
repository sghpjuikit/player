package sp.it.pl.audio

import javafx.collections.FXCollections.observableSet
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.media.AudioSpectrumListener
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.audio.playback.GeneralPlayer
import sp.it.pl.audio.playback.PlayTimeHandler
import sp.it.pl.audio.playback.RealTimeProperty
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.sequence.PlayingSequence
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.read
import sp.it.pl.audio.tagging.readTask
import sp.it.pl.audio.tagging.setOnDone
import sp.it.pl.audio.tagging.write
import sp.it.pl.audio.tagging.writeRating
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.util.access.Values
import sp.it.util.action.IsAction
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.executor.EventReducer.toLast
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.async.runNew
import sp.it.util.collections.mapset.MapSet
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.MultiConfigurableBase
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cvn
import sp.it.util.conf.cvro
import sp.it.util.conf.only
import sp.it.util.conf.relativeTo
import sp.it.util.dev.Idempotent
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.Functors.Ƒ.f
import sp.it.util.math.min
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.Subscription
import sp.it.util.system.browse
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.uuid
import sp.it.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.net.URI
import java.util.ArrayList
import java.util.function.BiConsumer
import java.util.function.Consumer

class PlayerManager: MultiConfigurableBase("Playback") {

   @JvmField val playing = InOutput<Metadata>(uuid("876dcdc9-48de-47cd-ab1d-811eb5e95158"), "Playing").appWide()
   @JvmField val playingSong = CurrentItem()
   @JvmField val state = PlayerState.deserialize()
   @JvmField val realTime = RealTimeProperty(state.playback.duration, state.playback.currentTime)
   private val player = GeneralPlayer(this)

   @IsConfig(name = "Remember playback state", info = "Continue last remembered playback when application starts.")
   var continuePlaybackOnStart by c(true)

   @IsConfig(name = "Pause playback on start", info = "Continue last remembered playback paused on application start.")
   var continuePlaybackPaused by c(false)

   @IsConfig(name = "Seek time unit", info = "Time to jump by when seeking forward/backward.")
   var seekUnitT by c(4.seconds)

   @IsConfig(name = "Seek fraction", info = "Relative time in fraction of song's length to seek forward/backward by.")
   var seekUnitP by c(0.05).between(0.0, 1.0)

   @IsConfig(name = "Player", info = "Exact player implementation currently in use.", editable = EditMode.NONE)
   val playerInfo by cvro("<none>") { player.pInfo }

   @IsConfig(name = "Vlc player location", info = "Location of the Vlc player that is or wil be used for playback",  editable = EditMode.APP)
   val playerVlcLocation by cvn<String>(null)

   val playerVlcLocationsRelativeTo = APP.location

   @IsConfig(
      name = "Vlc player locations",
      info = "Custom locations to look for the Vlc player, besides default installation locations and app-relative '/vlc' location." +
         "\n\nRequires application restart to take effect."
   )
   val playerVlcLocations by cList<File>().only(DIRECTORY).relativeTo(playerVlcLocationsRelativeTo)

   @IsConfig(name = "Last browse location")
   var browse by c<File>(APP.location.user).only(DIRECTORY)

   @IsConfig(name = "Last playlist export location")
   var lastSavePlaylistLocation by c<File>(APP.location.user).only(DIRECTORY)

   @IsConfig(
      name = "No song modification",
      info = "Disallow all song modifications by this application." +
         "\n\nWhen true, app will be unable to change any song metadata"
   )
   var readOnly by c(true)

   var startTime: Duration? = null
   var postActivating = false // this prevents onTime handlers to reset after playback activation the suspension-activation should undergo as if it never happen
   var postActivating1st = true // this negates the above when app starts and playback is activated 1st time
   var isSuspended = true
   var isSuspendedBecauseStartedPaused = false

   /**
    * Set of actions that execute when song starts playing. Seeking to song start doesn't activate this event.
    *
    * It is not safe to assume that application's information on currently played
    * song will be updated before this event. Therefore using cached information
    * can result in misbehavior due to outdated information.
    */
   val onPlaybackStart = Handler0()

   /**
    * Set of actions that will execute when song playback seek completes.
    */
   val onSeekDone = Handler0()

   /**
    * Set of actions that will execute when song playback ends.
    *
    * It is safe to use in-app cache of currently played song inside
    * the behavior parameter.
    */
   val onPlaybackEnd = Handler0()

   /**
    * Set of time-specific actions that individually execute when song playback reaches point of handler's interest.
    */
   val onPlaybackAt: MutableList<PlayTimeHandler> = ArrayList()

   /**
    * Playing song spectrum distributor.
    * Register listeners by adding it into collection. Adding listener multiple times has no
    * effect.
    */
   val spectrumListeners = observableSet<AudioSpectrumListener>()

   fun initialize() {
      playingSong.onUpdate { _, n -> playing.i.value = n }

      // use jaudiotagger for total time value (fixes incorrect values coming from player classes)
      playingSong.onChange { _, n -> state.playback.duration.value = n.getLength() }

      // maintain PLAYED_FIRST_TIME & PLAYED_LAST_TIME metadata
      // note: for performance reasons we update AFTER song stops playing, not WHEN it starts
      // as with playcount incrementing, it could disrupt playback, although now we are losing
      // updates on application closing!
      playingSong.onChange { o, _ ->
         if (!o.isEmpty()) // TODO: add config to avoid this operation if not in library
            o.write {
               it.setPlayedFirstNowIfEmpty()
               it.setPlayedLastNow()
            }
      }

      player.realTime.initialize()
      onPlaybackAt += PlayTimeHandler.at({ total -> total }, f { onPlaybackAt.forEach { it.restart(playingSong.value.getLength()) } }) // TODO: fix possible StackOverflowError
      onPlaybackEnd += {
         when (state.playback.loopMode.value) {
            PlayingSequence.LoopMode.OFF -> stop()
            PlayingSequence.LoopMode.PLAYLIST -> PlaylistManager.playNextItem()
            PlayingSequence.LoopMode.SONG -> seek(Duration.ZERO)
            else -> Unit
         }
      }
   }

   fun dispose() {
      player.dispose()
   }

   /** Initialize state from last session  */
   fun loadLastState() {
      logger.info { "Restoring last playback state..." }
      if (!continuePlaybackOnStart) {
         logger.info("    aborted: continuePlaybackOnStart==false")
         return
      }

      if (PlaylistManager.use<PlaylistSong>( { it.getPlaying() }, null)==null) {
         logger.info("    aborted: no playback was active")
         return
      }

      if (continuePlaybackPaused)
         state.playback.status.value = PAUSED

      activate()
   }

   @Idempotent
   fun suspend() {
      if (isSuspended) return
      logger.info("Suspending playback")

      isSuspended = true
      player.disposePlayback()
   }

   @Idempotent
   fun activate() {
      if (!isSuspended) return
      logger.info("Activating playback")

      postActivating = true
      val s = state.playback.status.value
      if (s==PAUSED || s==PLAYING)
         startTime = state.playback.currentTime.value

      when (s) {
         PAUSED -> {
            logger.info("playback state is paused, so playback will initialize on resume()/seek()/play()")
            isSuspendedBecauseStartedPaused = true
            playingSong.songChanged(PlaylistManager.use<PlaylistSong>( { it.getPlaying() }, null)!!)
         }
         PLAYING -> {
            player.play(PlaylistManager.use<PlaylistSong>( { it.playing }, null))
            // suspension_flag = false; // set inside player.play();
            runFX(200.millis) { isSuspended = false } // just in case som condition prevents resetting flag
         }
         else -> isSuspended = false
      }
   }

   inner class CurrentItem {
      /**
       * Returns the playing song and all its information.
       *
       *
       * Note: It is always safe to call this method, even during playing song
       * change events.
       */
      var value = Metadata.EMPTY
         private set
      private var valNext = Metadata.EMPTY
      private val valNextLoader = fxTimer(400.millis, 1) { preloadNext() }
      private val changes = ArrayList<(Metadata, Metadata) -> Unit>()
      private val updates = ArrayList<(Metadata, Metadata) -> Unit>()

      private fun setValue(change: Boolean, new_metadata: Metadata) {
         failIfNotFxThread()

         val ov = value
         value = new_metadata

         // There is a small problem
         // During tagging it is possible the playback needs to be suspended and activated
         // This unfortunately cascades and fires this method, but suspending/activating
         // should be transparent to playback song change/update events (not when app starts,
         // only when tagging)
         //
         // This can lead to dangerous situations (rarely) for example when tagging suspends
         // playback and calls this method and there is a listener to this which calls tagging
         // this will cause infinite loop!
         //
         // for now we will use flag as dirty solution
         if (isSuspended) return

         if (change) changes.forEach { h -> h(ov, new_metadata) }
         updates.forEach { h -> h(ov, new_metadata) }
      }


      /**
       * Add behavior to playing song changed event.
       *
       *
       * The event is fired every time playing song changes. This includes
       * replaying the same song.
       *
       *
       * Use in cases requiring constantly updated information about the playing
       * song.
       *
       *
       * Note: It is safe to call [.getValue] method when this even fires.
       * It has already been updated.
       */
      fun onChange(bc: (Metadata, Metadata) -> Unit): Subscription {
         changes.add(bc)
         return Subscription { changes.remove(bc) }
      }

      /**
       * Add behavior to playing item updated event.
       *
       *
       * The event is fired every time playing item changes or even if some of its
       * metadata is changed such artist or rating. More eager version of change
       * event.
       *
       *
       * Use in cases requiring not only change updates, but also constantly
       * (real time) updated information about the playing item, such as when
       * displaying this information somewhere - for example artist of the
       * played item.
       *
       *
       * Do not use when only the identity (defined by its URI) of the played
       * item is required. For example lastFM scrobbling service would not want
       * to update played item status when the metadata of the item change as it
       * is not a change in played item - it is still the same item.
       *
       *
       * Note: It is safe to call [.getValue] method when this even fires.
       * It has already been updated.
       */
      fun onUpdate(bc: (Metadata, Metadata) -> Unit): Subscription {
         updates.add(bc)
         return Subscription { updates.remove(bc) }
      }

      fun onUpdateAndNow(bc: (Metadata) -> Unit): Subscription {
         bc.invoke(value)
         return onUpdate { _, n -> bc.invoke(n) }
      }

      fun update() {
         load(false, value)
      }

      fun update(m: Metadata) {
         setValue(false, m)
      }

      /** Execute when song starts playing.  */
      fun songChanged(song: Song?) {
         if (song==null) {
            setValue(true, Metadata.EMPTY)
            logger.info("Current song changed to none.")
            logger.info("Current song metadata set to empty.")
         } else if (value.same(song)) {
            setValue(true, value)
            logger.info("Current song changed to the same song.")
            logger.info("Current song metadata reused.")
         } else if (valNext.same(song)) {
            setValue(true, valNext)
            logger.info("Current song changed to song in order.")
            logger.info("Current song metadata copied from cache of next song metadata.")
            // else load
         } else {
            logger.info("Current song changed to song not in order.")
            logger.info("Current song metadata will be loaded...")
            load(true, song)
         } // if pre-loaded, set
         // if same song, still fire change

         // wait 400ms, preload metadata for next song
         valNextLoader.start()
      }

      // load metadata, type indicates UPDATE vs CHANGE
      private fun load(changeType: Boolean, song: Song) {
         runIO {
            song.read()
         } ui {
            setValue(changeType, if (it.isEmpty()) song.toMeta() else it)
         }
      }

      private fun preloadNext() {
         logger.info("Pre-loading metadata for next song to play.")

         val next = PlaylistManager.use( { it.getNextPlaying() }, null)
         if (next!=null) {
            runIO {
               next.read()
            } ui {
               valNext = it
            }
         }
      }
   }

   /**
    * Starts player of song.
    *
    *
    * It is safe to assume that application will have updated currently played
    * song after this method is invoked. The same is not guaranteed for cached
    * metadata of this song.
    *
    *
    * Immediately after method is invoked, real time and current time are 0 and
    * all current song related information are updated and can be assumed to be
    * correctly initialized.
    *
    *
    * Invocation of this method fires playbackStart event.
    *
    * @param song to play
    */
   fun play(song: PlaylistSong) {
      player.play(song)
   }

   /** Resumes player, if file is being played. Otherwise does nothing.  */
   @IsAction(name = "Resume", desc = "Resumes playback, if file is being played.", global = true)
   fun resume() {
      player.resume()
   }

   /** Pauses player, if already paused, does nothing.  */
   @IsAction(name = "Pause", desc = "Pauses playback, if file is being played.", global = true)
   fun pause() {
      player.pause()
   }

   /** Pauses/resumes player, if file is being played. Otherwise does nothing.  */
   @IsAction(name = "Pause/resume", desc = "Pauses/resumes playback, if file is being played.", keys = "ALT+S", global = true)
   fun pause_resume() {
      player.pauseResume()
   }

   /** Stops player.  */
   @IsAction(name = "Stop", desc = "Stops playback.", keys = "ALT+F", global = true)
   fun stop() {
      player.stop()
   }

   /** Seeks player to position specified by duration parameter.  */
   fun seek(duration: Duration) {
      player.seek(duration)
   }

   /** Seeks player to position specified by percent value 0-1.  */
   fun seek(at: Double) {
      if (at<0 || at>1) throw IllegalArgumentException("Seek value must be 0-1")
      seek(state.playback.duration.get().multiply(at))
      if (state.playback.status.get()==PAUSED) player.pauseResume()
   }

   /** Seek forward by specified duration  */
   @IsAction(name = "Seek to beginning", desc = "Seek playback to beginning.", keys = "ALT+R", global = true)
   fun seekZero() {
      seek(0.0)
   }

   fun seekForward(type: Seek) {
      if (type==Seek.ABSOLUTE)
         seekForwardAbsolute()
      else
         seekForwardRelative()
   }

   /** Seek forward by small duration unit.  */
   @IsAction(name = "Seek forward", desc = "Seek playback forward by small duration unit.", keys = "ALT+D", repeat = true, global = true)
   fun seekForwardAbsolute() {
      seek(state.playback.currentTime.get().add(seekUnitT))
   }

   /** Seek forward by small fraction unit.  */
   @IsAction(name = "Seek forward (%)", desc = "Seek playback forward by fraction.", keys = "SHIFT+ALT+D", repeat = true, global = true)
   fun seekForwardRelative() {
      val d = state.playback.currentTime.get().toMillis()/state.playback.duration.get().toMillis() + seekUnitP
      seek(d min 1.0)
   }

   fun seekBackward(type: Seek) {
      if (type==Seek.ABSOLUTE)
         seekBackwardAbsolute()
      else
         seekBackwardRelative()
   }

   /** Seek backward by small duration unit.  */
   @IsAction(name = "Seek backward", desc = "Seek playback backward by small duration unit.", keys = "ALT+A", repeat = true, global = true)
   fun seekBackwardAbsolute() {
      seek(state.playback.currentTime.get().subtract(seekUnitT))
   }

   /** Seek backward by small fraction unit.  */
   @IsAction(name = "Seek backward (%)", desc = "Seek playback backward by fraction.", keys = "SHIFT+ALT+A", repeat = true, global = true)
   fun seekBackwardRelative() {
      val d = state.playback.currentTime.get().toMillis()/state.playback.duration.get().toMillis() - seekUnitP
      seek(d min 0.0)
   }

   /** Seek forward by specified duration  */
   @IsAction(name = "Seek to end", desc = "Seek playback to end.", global = true)
   fun seekEnd() {
      seek(1.0)
   }

   /** Increment volume by elementary unit.  */
   @IsAction(name = "Volume up", desc = "Increment volume by elementary unit.", keys = "CTRL+SHIFT+2", repeat = true, global = true)
   fun volumeInc() {
      state.playback.volume.incByStep()
   }

   /** Decrement volume by elementary unit.  */
   @IsAction(name = "Volume down", desc = "Decrement volume by elementary unit.", keys = "CTRL+SHIFT+1", repeat = true, global = true)
   fun volumeDec() {
      state.playback.volume.decByStep()
   }

   fun getLoopMode(): PlayingSequence.LoopMode {
      return state.playback.loopMode.get()
   }

   @IsAction(name = "Toggle looping", desc = "Switch between playlist looping mode.", keys = "ALT+L")
   fun toggleLoopMode() {
      setLoopMode(Values.next(getLoopMode()))
   }

   fun toggleLoopMode(e: MouseEvent) {
      when (e.button) {
         MouseButton.PRIMARY -> setLoopMode(Values.next(getLoopMode()))
         MouseButton.SECONDARY -> setLoopMode(Values.previous(getLoopMode()))
         else -> Unit
      }
   }

   fun setLoopMode(mode: PlayingSequence.LoopMode) {
      state.playback.loopMode.set(mode)
      PlaylistManager.playingItemSelector.setSelector(mode.selector())
   }

   /** Switches between on/off state for mute property.  */
   @IsAction(name = "Toggle mute", desc = "Switch mute on/off.", keys = "ALT+M")
   fun toggleMute() {
      state.playback.mute.set(!state.playback.mute.get())
   }

   /**
    * Rates playing song specified by percentage rating.
    *
    * @param rating <0,1> representing percentage of the rating, 0 being minimum and 1 maximum possible rating for
    * current song.
    */
   fun ratePlaying(rating: Double) {
      if (PlaylistManager.active==null) return
      playingSong.value.writeRating(rating)
   }

   /** Rate playing song 0/5.  */
   @IsAction(name = "Rate playing 0/5", desc = "Rate currently playing song 0/5.", keys = "ALT+BACK_QUOTE", global = true)
   fun rate0() = ratePlaying(0.0)

   /** Rate playing song 1/5.  */
   @IsAction(name = "Rate playing 1/5", desc = "Rate currently playing song 1/5.", keys = "ALT+1", global = true)
   fun rate1() = ratePlaying(0.2)

   /** Rate playing song 2/5.  */
   @IsAction(name = "Rate playing 2/5", desc = "Rate currently playing song 2/5.", keys = "ALT+2", global = true)
   fun rate2() = ratePlaying(0.4)

   /** Rate playing song 3/5.  */
   @IsAction(name = "Rate playing 3/5", desc = "Rate currently playing song 3/5.", keys = "ALT+3", global = true)
   fun rate3() = ratePlaying(0.6)

   /** Rate playing song 4/5.  */
   @IsAction(name = "Rate playing 4/5", desc = "Rate currently playing song 4/5.", keys = "ALT+4", global = true)
   fun rate4() = ratePlaying(0.8)

   /** Rate playing song 5/5.  */
   @IsAction(name = "Rate playing 5/5", desc = "Rate currently playing song 5/5.", keys = "ALT+5", global = true)
   fun rate5() = ratePlaying(1.0)

   /** Explore current song directory - opens file browser for its location.  */
   @IsAction(name = "Explore current song directory", desc = "Explore current song directory.", keys = "ALT+V", global = true)
   fun openPlayedLocation() {
      if (PlaylistManager.active==null) return
      val i = PlaylistManager.use<PlaylistSong>( { it.getPlaying() }, null)
      i?.uri?.browse()
   }

   enum class Seek {
      ABSOLUTE, RELATIVE
   }

   companion object: KLogging()

   /**
    * Adds songs refreshed event handler. When application updates song metadata it will fire this
    * event. For example widget displaying song information may update that information, if the
    * event contains the song.
    *
    *
    * Say, there is a widget with an input, displaying its value and sending it to its output, for
    * others to listen. And both are of [Song] type and updating the contents may be heavy
    * operation we want to avoid unless necessary.
    * Always update the output. It will update all inputs (of other widgets) bound to it. However,
    * there may be widgets whose inputs are not bound, but set manually. In order to not update the
    * input multiple times (by us and because it is bound) it should be checked whether input is
    * bound and then handled accordingly.
    *
    *
    * But because the input can be bound to multiple outputs, we must check whether the input is
    * bound to the particular output it is displaying value of (and would be auto-updated from).
    * This is potentially complex or impossible (with unfortunate
    * [Object.equals] implementation).
    * It is possible to use an [EventReducer] which will
    * reduce multiple events into one, in such case always updating input is recommended.
    */
   fun onSongRefresh(handler: (MapSet<URI, Metadata>) -> Unit): Subscription {
      refreshHandlers.add(handler)
      return Subscription { refreshHandlers.remove(handler) }
   }

   /** Singleton variant of [.refreshSongs].  */
   fun refreshSong(i: Song) {
      refreshSongs(listOf(i))
   }

   /**
    * Read metadata from tag of all songs and invoke [.refreshSongsWith].
    *
    *
    * Safe to call from any thread.
    *
    *
    * Use when metadata of the songs changed.
    */
   fun refreshSongs(`is`: Collection<Song>) {
      if (`is`.isEmpty()) return

      val task = Song.readTask(`is`)
      AppProgress.start(task)
      task.setOnDone(BiConsumer { ok, m -> if (ok) refreshSongsWith(m) })
      runNew(task)
   }

   /** Singleton variant of [.refreshSongsWith].  */
   fun refreshItemWith(m: Metadata) {
      refreshSongsWith(listOf(m))
   }

   /** Singleton variant of [.refreshSongsWith].  */
   fun refreshItemWith(m: Metadata, allowDelay: Boolean) {
      refreshSongsWith(listOf(m), allowDelay)
   }

   /** Simple version of [.refreshSongsWith] with false argument.  */
   fun refreshSongsWith(ms: List<Metadata>) {
      refreshSongsWith(ms, false)
   }

   /**
    * Updates application (playlist, library, etc.) with latest metadata. Refreshes the given
    * data for the whole application.
    *
    *
    * Safe to call from any thread.
    *
    * @param ms metadata to refresh
    * @param allowDelay flag for using delayed refresh to reduce refresh successions to single refresh. Normally false
    * is used.
    *
    *
    * Use false to refresh immediately and true to queue the refresh for future execution (will wait few seconds for
    * next refresh request and if it comes, will wait again and so on until none will come, which is when all queued
    * refreshes execute all at once).
    */
   fun refreshSongsWith(ms: List<Metadata>, allowDelay: Boolean) {
      if (allowDelay)
         runFX { red.push(ms) }
      else
         refreshSongsWithNow(ms)
   }

   // processes delayed refreshes - queues them up and invokes refresh after some time
   // use only on fx thread
   private val red = toLast<List<Metadata>>(3000.0, { o, n ->
      n + o // TODO: fix performance using mutable list
   }, { refreshSongsWithNow(it) })

   private val refreshHandlers = ArrayList<(MapSet<URI, Metadata>) -> Unit>()

   // runs refresh on bgr thread, thread safe
   private fun refreshSongsWithNow(ms: List<Metadata>) {
      if (ms.isEmpty()) return

      // always on br thread
      runIO {
         val msInDb = ms.filter { APP.db.exists(it) }
         if (msInDb.isEmpty()) return@runIO

         // metadata map hashed with resource identity : O(n^2) -> O(n)
         val mm = MapSet<URI, Metadata>({ obj: Metadata -> obj.uri }, msInDb)

         // update library
         APP.db.addSongs(mm)

         runFX {
            // update all playlist songs referring to this updated metadata
            PlaylistManager.playlists.forEach { playlist ->
               playlist.forEach { song ->
                  mm.ifHasK(song.uri, Consumer { playlist.updateItem(song) }) // TODO: fix O(n^2) to O(1_
               }
            }

            // refresh playing song data
            mm.ifHasE(playingSong.value, Consumer { playingSong.update(it) })

            if (playing.i.value!=null) mm.ifHasE(playing.i.value!!, Consumer { playing.i.value = it })   // TODO: fix nullpointer

            // refresh rest
            refreshHandlers.forEach { it(mm) }
         }
      }
   }

}