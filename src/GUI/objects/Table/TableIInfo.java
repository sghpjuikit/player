/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import javafx.beans.Observable;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;
import static org.atteo.evo.inflector.English.plural;
import util.functional.functor.BiCallback;

/**
 *
 * @param <E> TableView element type
 * 
 * @author Plutonium_
 */
public final class TableIInfo<E> {
    
    public static final BiCallback<Boolean,List<?>,String> DEFAULT_TEXT_FACTORY = (all, list) -> {
        String prefix1 = all ? "All: " : "Selected: ";
        int s = list.size();
        return prefix1 + s + " " + plural("item", s);
    };
    
    public Labeled node;
    public BiCallback<Boolean,List<E>,String> textFactory = (BiCallback)DEFAULT_TEXT_FACTORY;
    
    public TableIInfo(Labeled node) {
        this.node = node;
    }
    
    public TableIInfo(Labeled n, TableView<E> t) {
        this(n);
        List<E> items = t.getItems();
        t.getItems().addListener((Observable o)-> updateText(items, t.getSelectionModel().getSelectedItems()));
        t.getSelectionModel().getSelectedItems().addListener((Observable o)-> updateText(items, t.getSelectionModel().getSelectedItems()));
        updateText(items, EMPTY_LIST);
    }
    
    public final void updateText(List<E> all, List<E> selected) {
        boolean isAll = selected.isEmpty();
        List<E> l = isAll ? all : selected;
        node.setText(textFactory.call(isAll,l));
    }
}
