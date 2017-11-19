package gui.objects.contextmenu;

import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import util.access.ref.SingleR;

public class TableContextMenuR<M> extends SingleR<ImprovedContextMenu<M>,M> {

	public TableContextMenuR() {
		super(ImprovedContextMenu::new, ImprovedContextMenu::setValueAndItems);
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