package sp.it.pl.ui.nodeinfo

/** Node bindable to an element and displaying information about it. */
interface NodeInfo<B> {

   /**
    * Starts monitoring the bindable element.
    *
    * This method should first call [unbind] to remove any previous monitoring.
    */
   fun bind(bindable: B)

   /** Stops monitoring the bindable element. */
   fun unbind()

   /** Sets visibility for the graphics. */
   fun setVisible(v: Boolean)

   /** Binds and sets visible true. */
   fun showAndBind(b: B) {
      bind(b)
      setVisible(true)
   }

   /** Unbinds and sets visible false. */
   fun hideAndUnbind() {
      unbind()
      setVisible(false)
   }

}