/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import GUI.ContextManager;
import GUI.objects.PopOver.PopOver;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import utilities.functional.functor.UnProcedure;

/**
 * Generic item picker pop-up.
 * <p>
 * The window displays elements as text within a single scrollable pop-up window
 * like a grid.
 * Elements are converted to their text representation according to provided
 * mapper. Element should override toString() method if no mapper is provided. 
 * <p>
 * Elements will be sorted lexicographically.
 * <p>
 * @author Plutonium_
 */
public class Picker<E> {
    private static final String STYLE_CLASS = "item-picker";
    private static final String BUTTON_STYLE_CLASS = "item-picker-button";
    private static final int EL_W = 85;     // element width
    private static final int EL_H = 20;     // element height
    private static final int EGAP = 5;      // element gap
    private static final int ROWSIZE = 5;   // element gap
    
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane(grid);
    private final PopOver popup = new PopOver(scroll);
    
    private UnProcedure<E> onSelect = item -> {};
    private ToStringMapper<E> converter = item -> item.toString();
    private ItemAccumulator<E> accumulator = () -> Stream.empty();
    
    public Picker() {        
        grid.setHgap(EGAP);
        grid.setVgap(EGAP);
        scroll.setPadding(new Insets(EGAP));
        
        scroll.setPannable(true);
        scroll.setPrefViewportWidth(460);
        scroll.setMaxHeight(450);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        popup.setDetachable(false);
        popup.setArrowSize(0);
        popup.setArrowIndent(0);
        popup.setCornerRadius(0);
        popup.setAutoHide(true);
        popup.setAutoFix(false);
        popup.getStyleClass().setAll(STYLE_CLASS); // doesnt work
        
        // support layout mode transition
        popup.setOnShown(e-> {
            if(ContextManager.transitForMenu)
                GUI.GUI.setLayoutMode(true);
        });
        popup.setOnHiding(e-> {
            if(ContextManager.transitForMenu)
                GUI.GUI.setLayoutMode(false);
        });
    }
    
    private void buildContent() {
        List<E> items = accumulator.accumulate()
                .sorted((o1,o2) -> converter.convert(o1).compareToIgnoreCase(converter.convert(o2)))
                .collect(Collectors.toList());
        
        grid.getChildren().clear();
        // populate
        for (int i=0; i<items.size(); i++) {
            E item = items.get(i);
            String text = converter.convert(item);
            Label l = new Label(text);
            BorderPane b = new BorderPane();
                   b.setCenter(l);
                   b.setPrefSize(EL_W, EL_H);
                   b.setOnMouseClicked( e -> {
                       onSelect.accept(item);
                       popup.hide();
                   });
                   b.getStyleClass().setAll(BUTTON_STYLE_CLASS);
            grid.add(b, i%ROWSIZE, i/ROWSIZE);
        }
        // set height based on row count
        scroll.setPrefHeight(Math.ceil(items.size()/(double)ROWSIZE)*(EL_H+EGAP));
    }

    public void show(Node n, PopOver.NodeCentricPos pos) {
        buildContent();
        popup.show(n, pos);
    }
    
    public boolean isShowing() {
        return popup.isShowing();
    }
    
    /***
     * Sets the method by which the items are converted to string.
     * Default implementation uses item's toString() method.
     * @param converter 
     */
    public void setConverter(ToStringMapper<E> converter) {
        this.converter = converter;
    }
    
    /***
     * Sets the procedure executed when item is selected. Default implementation
     * does nothing.
     * @param onSelect procedure. 
     */
    public void setOnSelect(UnProcedure<E> onSelect) {
        this.onSelect = onSelect;
    }
    
    /***
     * Gathers the source items as a stream. Default implementation returns empty
     * stream.
     * @param acc accumulator. 
     */
    public void setAccumulator(ItemAccumulator<E> acc) {
        this.accumulator = acc;
    }
}
    
    
