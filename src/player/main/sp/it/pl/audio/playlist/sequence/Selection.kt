package sp.it.pl.audio.playlist.sequence

/** Selection logic for selection of next and previous item in a list. */
interface Selection<E> {

   /**
    * Selects an item from list.
    *
    * @param size size of the list.
    * @param index index of the currently selected item.
    * @param item currently selected item.
    * @param list list the selection operates on.
    */
   fun select(size: Int, index: Int, item: E, list: List<E>): E

}