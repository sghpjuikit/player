package sp.it.pl.plugin.impl

import mu.KLogging
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.toAbsoluteURIOrNull
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.read
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.main.APP
import sp.it.pl.main.withAppProgress
import sp.it.util.access.v
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.async.runNew
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.setTo
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.div
import sp.it.util.file.readTextTry
import sp.it.util.file.writeLnToFileTry
import sp.it.util.file.writeSafely
import sp.it.util.functional.net
import sp.it.util.functional.orAlsoTry
import sp.it.util.functional.orNull
import sp.it.util.units.uuid
import java.net.URI
import java.util.Collections.synchronizedMap
import java.util.Comparator
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class SongDb {

   private var running = false
   private lateinit var moods: Set<String>

   /** All library songs. Use output for reading/observing. Using input does not change db and has little use. */
   val songs = InOutput<List<Metadata>>(uuid("396d2407-7040-401e-8f85-56bc71288818"), "Song library").appWide()

   /** All library songs by [Song.id]. This is in memory db and should be used as read-only. */
   @ThreadSafe val songsById = MapSet(synchronizedMap(HashMap<String, Metadata>(2000)), { it.id })

   /** Map of unique values per field gathered from [songsById] */
   @ThreadSafe val itemUniqueValuesByField = ConcurrentHashMap<Metadata.Field<*>, Set<String>>()
   val songListFile = APP.location.user.library/"MetadataIdsDB.txt"

   /**
    * Comparator defining the sorting for songs in operations that wish to
    * provide consistent sorting across the application.
    *
    * The comparator should reflect library table sort order.
    */
   var libraryComparator = v<Comparator<in Metadata>>(Comparator { a, b -> a.compareTo(b) })

   fun init() {
      if (running) return
      running = true

      moods = APP.location.resources.moods_txt.readTextTry().orNull().orEmpty().lineSequence().toSet()
      runIO { updateInMemoryDbFromPersisted() }.withAppProgress("Loading song database")
   }

   fun stop() {
      running = true
   }

   fun exists(song: Song) = exists(song.uri)

   fun exists(uri: URI) = songsById.containsKey(uri.toString())

   /** @return song from library with the URI of the specified song or null if not found */
   fun getSong(song: Song) = getSong(song.uri)

   /** @return item from library with the specified URI or null if not found */
   fun getSong(uri: URI): Metadata? = songsById[uri.toString()]

   @Blocks
   private fun getAllSongs(): MetadatasDB = CoreSerializer.readSingleStorage<MetadatasDB>()
      .orAlsoTry {
         val songs = songListFile.useLines { it.toList() }
         val songsById = songs
            .mapNotNull { id -> id.toAbsoluteURIOrNull()?.net { id to it } }
            .associate { (id, uri) ->
               id to SimpleSong(uri).net {
                  it.read().takeUnless { it.isEmpty() } ?: it.toMeta()
               }
            }
         MetadatasDB(songsById)
      }
      .orNull() ?: MetadatasDB()

   fun addSongs(songs: Collection<Metadata>) {
      if (songs.isEmpty()) return

      CoreSerializer.useAtomically {
         val ms = MetadatasDB(songsById.backingMap())
         songs.forEach { ms[it.id] = it }
         songListFile.writeSafely { ms.keys.asSequence().writeLnToFileTry(it) }.orThrow
         writeSingleStorage(ms)
         updateInMemoryDbFromPersisted()
      }
   }

   fun removeSongs(songs: Collection<Song>) {
      if (songs.isEmpty()) return

      CoreSerializer.useAtomically {
         val ms = MetadatasDB(songsById.backingMap())
         songs.forEach { ms.remove(it.id) }
         songListFile.writeSafely { ms.keys.asSequence().writeLnToFileTry(it) }.orThrow
         writeSingleStorage(ms)
         updateInMemoryDbFromPersisted()
      }

   }

   fun removeAllSongs() {
      CoreSerializer.useAtomically {
         songListFile.writeSafely { sequenceOf<String>().writeLnToFileTry(it) }.orThrow
         writeSingleStorage(MetadatasDB())
         updateInMemoryDbFromPersisted()
      }
   }

   private fun setInMemoryDB(l: List<Metadata>) {
      songsById setTo l
      updateSongValues()

      runFX {
         songs.i.value = l
      }
   }

   private fun updateSongValues() {
      itemUniqueValuesByField.clear()
      Metadata.Field.all.asSequence()
         .filter { it.isAutoCompletable() }
         .forEach { f ->
            itemUniqueValuesByField[f] = songsById.asSequence()
               .map { it.getFieldS(f, "") }
               .filter { it.isNotBlank() }
               .toSet()
         }
      itemUniqueValuesByField[Metadata.Field.MOOD] = moods
   }

   @ThreadSafe
   fun updateInMemoryDbFromPersisted() = setInMemoryDB(getAllSongs().values.toList())

   @ThreadSafe
   fun refreshSongsFromFile(songs: List<Song>) {
      runNew {
         val metadatas = songs.asSequence().map { it.read() }.filter { !it.isEmpty() }.toList()
         APP.audio.refreshSongsWith(metadatas)
      }.withAppProgress("Refreshing library from disk")
   }

   @ThreadSafe
   fun removeInvalidSongs(): Fut<Unit> {
      return runNew {
         Song.removeMissingFromLibTask().run()
      }
   }

   companion object: KLogging()
}