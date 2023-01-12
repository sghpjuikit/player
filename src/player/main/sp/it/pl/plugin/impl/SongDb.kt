package sp.it.pl.plugin.impl

import java.lang.String.CASE_INSENSITIVE_ORDER
import java.net.URI
import java.util.Collections.synchronizedMap
import java.util.concurrent.ConcurrentHashMap
import mu.KLogging
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.toAbsoluteURIOrNull
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.read
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.controller.io.Output
import sp.it.pl.layout.controller.io.appWide
import sp.it.pl.main.APP
import sp.it.pl.main.withAppProgress
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.async.runVT
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.setTo
import org.jetbrains.annotations.Blocking
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.div
import sp.it.util.file.readTextTry
import sp.it.util.file.writeLnToFileTry
import sp.it.util.file.writeSafely
import sp.it.util.functional.net
import sp.it.util.functional.orAlsoTry
import sp.it.util.functional.orNull
import sp.it.util.type.type
import sp.it.util.units.uuid

@Suppress("unused")
class SongDb {

   private var running = false
   private lateinit var moods: LinkedHashSet<String>

   /** All library songs. Use output for reading/observing. Setting its [Output.value] does not change db and has little use outside [SongDb]. */
   val songs = Output<List<Metadata>>(uuid("396d2407-7040-401e-8f85-56bc71288818"), "Song library", type(), listOf()).appWide()

   /** All library songs by [Song.id]. This is in memory db and should be used as read-only. */
   @ThreadSafe val songsById = MapSet(synchronizedMap(HashMap<String, Metadata>(2000)), { it.id })

   /** Map of unique values per field gathered from [songsById], sorted by [CASE_INSENSITIVE_ORDER] ASC. */
   @ThreadSafe val itemUniqueValuesByField = ConcurrentHashMap<Metadata.Field<*>, LinkedHashSet<String>>()
   val songListFile = APP.location.user.library/"MetadataIdsDB.txt"

   fun init() {
      if (running) return
      running = true

      moods = APP.location.resources.moods_yml.readTextTry().orNull().orEmpty().lineSequence().filterNot { it.startsWith("#") || it.isBlank() }.sorted().toCollection(LinkedHashSet())
      runVT { updateInMemoryDbFromPersisted() }.withAppProgress("Loading song database")
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

   @Blocking
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
         songs.value = l
      }
   }

   private fun updateSongValues() {
      itemUniqueValuesByField.clear()
      Metadata.Field.all.asSequence()
         .filter { it.isAutoCompletable() }
         .forEach { f ->
            itemUniqueValuesByField[f] = songsById.asSequence()
               .flatMap { f.autocompleteGetOf(it) }
               .sortedWith(CASE_INSENSITIVE_ORDER)
               .toCollection(LinkedHashSet())
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