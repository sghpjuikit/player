/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.ContextMenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import util.SingleInstance;

/**
 *
 * @author Plutonium_
 */
public class TreeContextMenuInstance<E> extends SingleInstance<ImprovedContextMenu<List<E>>,TreeView<E>> {
    
    public TreeContextMenuInstance(Supplier<ImprovedContextMenu<List<E>>> builder) {
        super(builder);
    }
    public TreeContextMenuInstance(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>, TreeView<E>> mutator) {
        super(builder, mutator);
    }
    
    /**
     * Equivalent to: {@code get(table).show(table, e)} . But called only if the
     * table is not empty.
     * 
     * @param table
     * @param e 
     */
    public void show(TreeView<E> tree, MouseEvent e) {
        if(!tree.getSelectionModel().isEmpty())
            get(tree).show(tree, e);
    }
    
}