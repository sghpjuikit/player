package gui.objects.contextmenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import util.SingleR;

/**
 * @author Martin Polakovic
 */
public class TableContextMenuMR<E, M> extends SingleR<ImprovedContextMenu<List<E>>,M> {

	public TableContextMenuMR(Supplier<ImprovedContextMenu<List<E>>> builder) {
		super(builder);
	}

	public TableContextMenuMR(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>,M> mutator) {
		super(builder, mutator);
	}

	/**
	 * Equivalent to: {@code getM(mutator).show(table, e)} . But called only if the table selection is not empty.
	 */
	public void show(M mutator, TableView<E> table, MouseEvent e) {
		if (!table.getSelectionModel().isEmpty())
			getM(mutator).show(table, e);
	}

	public void show(M mutator, TableView<E> table, ContextMenuEvent e) {
		if (!table.getSelectionModel().isEmpty())
			getM(mutator).show(table, e);
	}

}
