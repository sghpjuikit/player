/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.InfoNode;

import java.util.List;
import java.util.function.BiFunction;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;
import static org.atteo.evo.inflector.English.plural;
import org.reactfx.Subscription;

/**
 * Provides information about table items and table item selection.
 * 
 * @param <E> type of table element
 * 
 * @author Plutonium_
 */
public final class InfoTable<E> implements InfoNode<TableView<E>> {
    
    /** 
     * Default text factory. Provides texts like: <pre>
     * 'All: 1 item'
     * 'Selected: 89 items'
     * </pre>
     * Custom implementation change or expand the text with additional 
     * information depending on type of table elements. 
     */
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

    private Subscription s;
    
    /** Sets the node and listeners to update the text automatically by monitoring
      * the table items and selection. */
    public InfoTable(Labeled node, TableView<E> t) {
        this(node);
        bind(t);
    }
    public InfoTable(Labeled node) {
        this.node = node;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean v) {
        node.setVisible(v);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(TableView<E> t) {
        unbind();
        
        ObservableList<E> al = t.getItems();
        ObservableList<E> sl = t.getSelectionModel().getSelectedItems();
        InvalidationListener l = o -> updateText(al, sl);
        
        al.addListener(l);
        sl.addListener(l);
        s = () -> t.getItems().removeListener(l);
        s = s.and(() -> t.getSelectionModel().getSelectedItems().removeListener(l));
        
        updateText(al,sl);
    }

    /** {@inheritDoc} */
    @Override
    public void unbind() {
        if(s != null) s.unsubscribe();
        s = null;
    }
    
    /** Updates the text of the node using the text factory.
     * @param all all items of the table
     * @param selected  selected items of the table */
    public void updateText(List<E> all, List<E> selected) {
        // bug fix, without this line, which does exactly nothing,
        // mgs list contains nulls sometimes (no idea why)
        util.functional.Util.toS(selected,m->m==null ? "null" : "v");
        
        boolean isAll = selected.isEmpty();
        List<E> l = isAll ? all : selected;
        node.setText(textFactory.apply(isAll,l));
    }

}
