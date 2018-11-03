package sp.it.pl.gui.infonode

/** Node bindable to an element and displaying information about it. */
interface InfoNode<B> {

    /**
     * Starts monitoring the bindable element.
     *
     * @implSpec this method should first call [.unbind] to remove any previous monitoring.
     */
    fun bind(bindable: B)

    /** Stops monitoring the bindable element. */
    fun unbind()

    /** Sets visibility for the graphics. */
    fun setVisible(v: Boolean)

    /** Binds and sets visible true. */
    @JvmDefault
    fun showNbind(b: B) {
        bind(b)
        setVisible(true)
    }

    /** Unbinds and sets visible false. */
    @JvmDefault
    fun hideNunbind() {
        unbind()
        setVisible(false)
    }

}