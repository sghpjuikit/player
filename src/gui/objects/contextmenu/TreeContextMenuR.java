/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.contextmenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;

import util.SingleR;

/**
 *
 * @author Martin Polakovic
 */
public class TreeContextMenuR<E> extends SingleR<ImprovedContextMenu<List<E>>,TreeView<E>> {
    
    public TreeContextMenuR(Supplier<ImprovedContextMenu<List<E>>> builder) {
        super(builder);
    }
    public TreeContextMenuR(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>, TreeView<E>> mutator) {
        super(builder, mutator);
    }
    
    /**
     * Equivalent to: {@code getM(table).show(table, e)} . But called only if the
     * table is not empty.
     * 
     * @param table
     * @param e 
     */
    public void show(TreeView<E> tree, MouseEvent e) {
        if(!tree.getSelectionModel().isEmpty())
            getM(tree).show(tree, e);
    }
    
}