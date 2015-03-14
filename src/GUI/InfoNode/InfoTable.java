/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.InfoNode;

import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.function.BiFunction;
import javafx.beans.InvalidationListener;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;
import static org.atteo.evo.inflector.English.plural;
import util.dev.TODO;
import static util.dev.TODO.Purpose.BUG;
import static util.dev.TODO.Severity.LOW;
import static util.Util.copySelectedItems;

/**
 * Provides information about table items and table item selection.
 * 
 * @param <E> type of table element
 * 
 * @author Plutonium_
 */
public final class InfoTable<E> implements InfoNode<TableView<E>> {
    
    /** Default text factory. Provides texts like: <pre>
     * 'All: 1 item'
     * 'Selected: 89 items'
     * </pre>
     * Custom implementation change or expand the text with additional 
     * information depending on type of table elements. */
    public static final BiFunction<Boolean,List<?>,String> DEFAULT_TEXT_FACTORY = (all, list) -> {
        String prefix1 = all ? "All: " : "Selected: ";
        int s = list.size();
        return prefix1 + s + " " + plural("item", s);
    };
    
    

    /** The graphical text element */
    public Labeled node;
    
    /** Provides text to the node. The first parameters specifies whether selection
     * is empty, the other is the list of table items if selection is empty or 
     * selected items if nonempty. */
    public BiFunction<Boolean,List<E>,String> textFactory = (BiFunction)DEFAULT_TEXT_FACTORY;

    private InvalidationListener itemL;
    private InvalidationListener selL;
    private final TableView<E> t;
    
    
    @TODO(purpose = BUG, severity = LOW, note = "see the commented part"
            + "modification erros - INVSTIGATE if te issue still appears (some sort of IndexOutOfBounds in Table SortedList)")
    /** Sets the node and listeners to update the text automatically by monitoring
      * the table items and selection. */
    public InfoTable(Labeled node, TableView<E> t) {
        this.node = node;
        this.t = t;
        bind(t);
    }
    
    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean v) {
        node.setVisible(v);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(TableView<E> t) {
        // unbind old
        if(this.t!=null) unbind();
        
        List<E> items = t.getItems();
        
        // the below causes bug & not sure if EMPTY_LIST does not produce incorrect
        // behavior, but looks fine so far the problem seems to be when the
        // whole table list changes and both of the
        // listeners are called, just a guess
        //           updateText(items, copySelectedItems(t))
        itemL = o -> updateText(t.getItems(), EMPTY_LIST);
        selL = o -> updateText(items, copySelectedItems(t));
        
        t.getItems().addListener(itemL);
        t.getSelectionModel().getSelectedItems().addListener(selL);
        updateText(items, EMPTY_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public void unbind() {
        if(itemL != null) t.getItems().removeListener(itemL);
        if(selL != null)t.getSelectionModel().getSelectedItems().removeListener(selL);
    }
    
    /** Updates the text of the node using the text factory.
     * @param all all items of the table
     * @param selected  selected items of the table */
    public void updateText(List<E> all, List<E> selected) {
        boolean isAll = selected.isEmpty();
        List<E> l = isAll ? all : selected;
        node.setText(textFactory.apply(isAll,l));
        
        // if bugs appear avoid using original list by copying it
        node.setText(textFactory.apply(isAll,l));
    }

}
