package sp.it.pl.audio.playlist.sequence

/** Selector of next or previous item in the list. */
class PlaylistSongSelector<E>(private val prev: Selection<E>, private val next: Selection<E>) {

   @Suppress("UNUSED_PARAMETER")
   fun same(size: Int, index: Int, current_item: E, list: List<E>): E = current_item

   fun next(size: Int, index: Int, current_item: E, list: List<E>): E = next.select(size, index, current_item, list)

   fun previous(size: Int, index: Int, current_item: E, list: List<E>): E = prev.select(size, index, current_item, list)

}