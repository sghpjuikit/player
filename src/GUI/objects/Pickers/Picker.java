/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import utilities.Parser.ToStringConverter;
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
 * Elements will be represented graphically depending on the cell factory.
 * <p>
 * @author Plutonium_
 */
public class Picker<E> {
    
    /** Style class for cell. */
    public static final String CELL_STYLE_CLASS = "item-picker-button";
    private static final int EGAP = 5;      // element gap
    
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane(grid);
    
    private UnProcedure<E> onSelect = item -> {};
    private ToStringConverter<E> converter = item -> item.toString();
    private ItemAccumulator<E> accumulator = () -> Stream.empty();
    private CellFactory<E> cellFactory = item -> {
        String text = getConverter().toS(item);
        Label l = new Label(text);
        BorderPane b = new BorderPane();
                   b.setCenter(l);
                   b.setPrefSize(85,20);
                   b.getStyleClass().setAll(CELL_STYLE_CLASS);
                   Tooltip.install(b, new Tooltip(text));
        return b;
    };
    
    public Picker() {        
        grid.setHgap(EGAP);
        grid.setVgap(EGAP);
        scroll.setPadding(new Insets(EGAP));
        
        scroll.setPannable(false);
        scroll.setMaxHeight(450);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                
        // consume problematic events and prevent from propagating
        // disables unwanted behavior of the popup
        scroll.addEventFilter(MOUSE_PRESSED, e->e.consume());
        scroll.addEventFilter(MOUSE_DRAGGED, e->e.consume());
    }
    
    private void buildContent() {
        List<E> items = getAccumulator().accumulate()
                .sorted((o1,o2) -> getConverter().toS(o1).
                        compareToIgnoreCase(getConverter().toS(o2)))
                .collect(Collectors.toList());
        
        grid.getChildren().clear();
        double el_w = 0;
        double el_h = 0;
        int row_size = calculateRowSize(items.size());
        
        // populate
        for (int i=0; i<items.size(); i++) {
            E item = items.get(i);
            Region cell = getCellFactory().createCell(item);
                   cell.setOnMouseClicked( e -> {
                       getOnSelect().accept(item);
                       e.consume();
                   });
            
            grid.add(cell, i%row_size, i/row_size);
            el_w = cell.getPrefWidth();
            el_h = cell.getPrefHeight();
        }
        
        // calculate size
        double height = Math.ceil(items.size()/(double)row_size)*(el_h+EGAP)-EGAP;
        double width = row_size*(el_w+EGAP)-EGAP;
        
        if(scroll.getHeight() < height) // FIX THIS
            scroll.setPrefWidth(width+15);
    }
    
    /***
     * Sets the method by which the items are converted to string.
     * Default implementation uses item's toString() method.
     * @param converter coverter. Must not be null;
     */
    public void setConverter(ToStringConverter<E> converter) {
        Objects.requireNonNull(converter);
        this.converter = converter;
    }
    
    /**
     * Returns converter. Never null.
     * @see #setConverter(utilities.Parser.ToStringConverter)
     */
    public ToStringConverter<E> getConverter() {
        return converter;
    }
    
    /***
     * Sets the procedure executed when item is selected. Default implementation
     * does nothing.
     * @param onSelect procedure. Must not be null;
     */
    public void setOnSelect(UnProcedure<E> onSelect) {
        Objects.requireNonNull(onSelect);
        this.onSelect = onSelect;
    }
    
    /**
     * Returns onSelect handler. Never null.
     * @see #setOnSelect(utilities.functional.functor.UnProcedure) 
     */
    public UnProcedure<E> getOnSelect() {
        return onSelect;
    }
    
    /***
     * Sets item accumulator.
     * Gathers the source items as a stream. Default implementation returns empty
     * stream.
     * @param acc accumulator. Must not be null;
     */
    public void setAccumulator(ItemAccumulator<E> acc) {
        Objects.requireNonNull(acc);
        this.accumulator = acc;
    }
    
    /**
     * Returns item accumulator. Never null.
     * @see #setAccumulator(GUI.objects.Pickers.ItemAccumulator) 
     */
    public ItemAccumulator<E> getAccumulator() {
        return accumulator;
    }
    
    /***
     * Sets cell factory.
     * Creates graphic representation of the item.
     * @param cf cell factory. Must not be null;
     */
    public void setCellFactory(CellFactory<E> cf) {
        Objects.requireNonNull(cf);
        this.cellFactory = cf;
    }
    
    /**
     * Returns cell factory. Never null.
     * @see #setAccumulator(GUI.objects.Pickers.ItemAccumulator) 
     */
    public CellFactory<E> getCellFactory() {
        return cellFactory;
    }
    
    
    protected int calculateRowSize(int items_amount) {
        return 4;
    }
    
    public void setMaxWidth(double max_width) {
        
    }
    
    public Node getNode() {
        buildContent();
        return scroll;
    }
}
    
    
