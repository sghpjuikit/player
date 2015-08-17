/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.ContextMenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

import util.SingleⱤ;

/**
 *
 * @author Plutonium_
 */
public class TableContextMenuMⱤ<E,M> extends SingleⱤ<ImprovedContextMenu<List<E>>,M> {
    
    public TableContextMenuMⱤ(Supplier<ImprovedContextMenu<List<E>>> builder) {
        super(builder);
    }
    public TableContextMenuMⱤ(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>, M> mutator) {
        super(builder, mutator);
    }
    
    /**
     * Equivalent to: {@code getM(mutator).show(table, e)} . But called only if the
     * table selection is not empty.
     * 
     * @param table
     * @param e 
     */
    public void show(M mutator, TableView<E> table, MouseEvent e) {
        if(!table.getSelectionModel().isEmpty())
            getM(mutator).show(table, e);
    }
    public void show(M mutator, TableView<E> table, ContextMenuEvent e) {
        if(!table.getSelectionModel().isEmpty())
            getM(mutator).show(table, e);
    }
    
}
