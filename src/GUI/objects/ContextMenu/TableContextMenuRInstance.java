/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.ContextMenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import util.SingleInstance;

/**
 *
 * @author Plutonium_
 */
public class TableContextMenuRInstance<E,R> extends SingleInstance<ImprovedContextMenu<List<E>>,R> {
    
    public TableContextMenuRInstance(Supplier<ImprovedContextMenu<List<E>>> builder) {
        super(builder);
    }
    public TableContextMenuRInstance(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>, R> mutator) {
        super(builder, mutator);
    }
    
    /**
     * Equivalent to: {@code get(mutator).show(table, e)} . But called only if the
     * table selection is not empty.
     * 
     * @param table
     * @param e 
     */
    public void show(R mutator, TableView<E> table, MouseEvent e) {
        if(!table.getSelectionModel().isEmpty())
            get(mutator).show(table, e);
    }
    public void show(R mutator, TableView<E> table, ContextMenuEvent e) {
        if(!table.getSelectionModel().isEmpty())
            get(mutator).show(table, e);
    }
    
}
