package sp.it.pl.layout

/** Defines an ui object with alternate state and ability to switch to it. */
interface AltState {

   /** Transition to alternative state. */
   fun show()

   /** Transition to normal state. */
   fun hide()

   /** Invokes [show] if true else [hide]. */
   fun setShow(v: Boolean): Unit = if (v) show() else hide()

}