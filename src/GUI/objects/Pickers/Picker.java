/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import GUI.ContextManager;
import GUI.objects.PopOver.PopOver;
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
import main.App;
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
    private static final String STYLE_CLASS = "item-picker";
    
    private static final int EGAP = 5;      // element gap
    
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane(grid);
    private final PopOver popup = new PopOver(scroll);
    
    private UnProcedure<E> onSelect = item -> {};
    private ToStringMapper<E> converter = item -> item.toString();
    private ItemAccumulator<E> accumulator = () -> Stream.empty();
    private CellFactory<E> cellFactory = (item, text) -> {
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
        
        popup.setDetachable(false);
        popup.setArrowSize(0);
        popup.setArrowIndent(0);
        popup.setCornerRadius(0);
        popup.setAutoHide(true);
        popup.setAutoFix(true);
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
        
        // consume problematic events and prevent from propagating
        // disables unwanted behavior of the popup
        scroll.addEventFilter(MOUSE_PRESSED, e->e.consume());
        scroll.addEventFilter(MOUSE_DRAGGED, e->e.consume());
    }
    
    private void buildContent(Node n) {
        List<E> items = accumulator.accumulate()
                .sorted((o1,o2) -> converter.convert(o1).compareToIgnoreCase(converter.convert(o2)))
                .collect(Collectors.toList());
        
        grid.getChildren().clear();
        double el_w = 0;
        double el_h = 0;
        int row_size = calculateRowSize(items.size());
        
        // populate
        for (int i=0; i<items.size(); i++) {
            E item = items.get(i);
            String text = converter.convert(item);
            Region cell = cellFactory.createCell(item, text);
                   cell.setOnMouseClicked( e -> {
                       onSelect.accept(item);
                       popup.hide();
                       e.consume();
                   });
            
            grid.add(cell, i%row_size, i/row_size);
            el_w = cell.getPrefWidth();
            el_h = cell.getPrefHeight();
        }
        
        // calculate size
        double height = Math.ceil(items.size()/(double)row_size)*(el_h+EGAP)+2*EGAP;
        double width = row_size*(el_w+EGAP);System.out.println("w "+width);
        // calculate size fixes
        double h_fix = n.getScene().getWindow().equals(App.getWindowOwner().getStage()) ? 15 : 0;// no idea why this needs to be here, but it DOES
        double w_fix = scroll.getMaxHeight() < height ? 15 : 0;// needs vertical scrollbar
        // set size
        scroll.setPrefSize(width+w_fix,height+h_fix);
    }

    public void show(Node n, PopOver.NodeCentricPos pos) {
        buildContent(n);
        popup.show(n, pos);
    }
    
    public boolean isShowing() {
        return popup.isShowing();
    }
    
    /***
     * Sets the method by which the items are converted to string.
     * Default implementation uses item's toString() method.
     * @param converter coverter. Must not be null;
     */
    public void setConverter(ToStringMapper<E> converter) {
        Objects.requireNonNull(converter);
        this.converter = converter;
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
    
    /***
     * Sets cell factory.
     * Creates graphic representation of the item.
     * @param cf cell factory. Must not be null;
     */
    public void setCellFactory(CellFactory<E> cf) {
        Objects.requireNonNull(cf);
        this.cellFactory = cf;
    }
    
    
    protected int calculateRowSize(int items_amount) {
        return 4;
    }
    
    public void setMaxWidth(double max_width) {
        
    }
}
    
    
