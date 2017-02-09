package gui.objects.contextmenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

/**
 * @author Martin Polakovic
 */
public class TableContextMenuR<E, TABLE extends TableView<E>> extends TableContextMenuMR<E,TABLE> {

	public TableContextMenuR(Supplier<ImprovedContextMenu<List<E>>> builder) {
		super(builder);
	}

	public TableContextMenuR(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>,TABLE> mutator) {
		super(builder, mutator);
	}

	/**
	 * @see #show(javafx.scene.control.TableView, javafx.scene.input.MouseEvent)
	 */
	public void show(TABLE table, MouseEvent e) {
		show(table, table, e);
	}

	/**
	 * @see #show(javafx.scene.control.TableView, javafx.scene.input.ContextMenuEvent)
	 */
	public void show(TABLE table, ContextMenuEvent e) {
		show(table, table, e);
	}

}