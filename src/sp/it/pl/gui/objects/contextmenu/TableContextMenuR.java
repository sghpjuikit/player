package sp.it.pl.gui.objects.contextmenu;

import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import sp.it.pl.util.access.ref.SingleR;

public class TableContextMenuR<M> extends SingleR<ValueContextMenu<M>,M> {

	public TableContextMenuR() {
		super(ValueContextMenu::new, ValueContextMenu::setValueAndItems);
	}

	public void show(M mutator, TableView<?> table, MouseEvent e) {
		if (!table.getSelectionModel().isEmpty())
			getM(mutator).show(table, e);
	}

	public void show(M mutator, TableView<?> table, ContextMenuEvent e) {
		if (!table.getSelectionModel().isEmpty())
			getM(mutator).show(table, e);
	}

}