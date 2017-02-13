package gui.objects.grid;

import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.SkinBase;
import javafx.scene.control.skin.VirtualFlow;

/**
 * Parent class to control skins whose contents are virtualized and scrollable.
 * This class handles the interaction with the VirtualFlow class, which is the
 * main class handling the virtualization of the contents of this container.
 */
public abstract class CustomVirtualContainerBase<C extends Control, I extends IndexedCell> extends SkinBase<C> {

	/** The virtualized container which handles the layout and scrolling of all the cells. */
	final VirtualFlow<I> flow;

	/**
	 *
	 * @param control
	 */
	public CustomVirtualContainerBase(final C control) {
		super(control);
		flow = createVirtualFlow();
	}

	abstract void updateRowCount();

	/**
	 * Enables skin subclasses to provide a custom VirtualFlow implementation,
	 * rather than have VirtualContainerBase instantiate the default instance.
	 */
	protected VirtualFlow<I> createVirtualFlow() {
		return new VirtualFlow<>();
	}

}