/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.function.BiFunction;
import javafx.beans.Observable;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;
import static org.atteo.evo.inflector.English.plural;
import util.TODO;
import static util.TODO.Purpose.BUG;
import static util.TODO.Severity.LOW;
import static util.Util.copySelectedItems;

/**
 * Provides information about table items and table item selection.
 * 
 * @param <E> type of table element
 * 
 * @author Plutonium_
 */
public final class TableInfo<E> {
    
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
    
    /** Sets the node. The text updating needs to be done manually. */
    public TableInfo(Labeled node) {
        this.node = node;
    }
    
    @TODO(purpose = BUG, severity = LOW, note = "see the commented part"
            + "modification erros - INVSTIGATE if te issue still appears (some sort of IndexOutOfBounds in Table SortedList)")
    /** Sets the node and listeners to update the text automatically by monitoring
      * the table items and selection. */
    public TableInfo(Labeled node, TableView<E> t) {
        this(node);
        List<E> items = t.getItems();
        // the below causes bug & not sure if EMPTY_LIST does not produce incorrect
        // behavior, but looks fine so far the problem seems to be when the
        // whole table list changes and both of the
        // listeners are called, just a guess
        // t.getItems().addListener((Observable o)-> updateText(items, copySelectedItems(t)));
        t.getItems().addListener((Observable o)-> updateText(items, EMPTY_LIST));
        t.getSelectionModel().getSelectedItems().addListener((Observable o)-> updateText(items, copySelectedItems(t)));
        updateText(items, EMPTY_LIST);
    }
    
    /** Updates the text of the node using the text factory.
     * @param all all items of the table
     * @param selected  selected items of the table */
    public final void updateText(List<E> all, List<E> selected) {
        boolean isAll = selected.isEmpty();
        List<E> l = isAll ? all : selected;
        node.setText(textFactory.apply(isAll,l));
        
        // if bugs appear avoid using original list by copying it
        node.setText(textFactory.apply(isAll,l));
    }
}
