
package GUI.objects.Pickers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Callback;
import utilities.Parser.ToStringConverter;
import utilities.Util;
import utilities.functional.functor.UnProcedure;

/**
 * Generic item picker.
 * <p>
 * Node displaying elements as grid.
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
    public static final String STYLE_CLASS = "item-picker";
    public static final List<String> CELL_STYLE_CLASS = Arrays.asList("block","item-picker-element");
    
    private static final int EGAP = 5;      // element gap
    
    private final TilePane tiles = new TilePane();
    private final ScrollPane scroll = new ScrollPane(tiles);
    
    private UnProcedure<E> onSelect = item -> {};
    private ToStringConverter<E> converter = item -> item.toString();
    private Supplier<Stream<E>> accumulator = () -> Stream.empty();
    private Callback<E,Node> cellFactory = item -> {
        String text = getConverter().toS(item);
        Label l = new Label(text);
        StackPane b = new StackPane(l);
        b.getStyleClass().setAll(CELL_STYLE_CLASS);
        b.setSnapToPixel(true);
        return b;
    };
    
    public Picker() {
        // set spacing
        tiles.setHgap(EGAP);
        tiles.setVgap(EGAP);
        tiles.setSnapToPixel(true);
        // set autosizing for tiles to always fill the grid entirely
        tiles.widthProperty().addListener((o,ov,nv) -> {
            int columns = (int) Math.floor(nv.doubleValue()/100);
            // set maximum number of columns to 7
                columns = Math.min(columns, 7);
            // for n elements there is n-1 gaps so we need to add 1 gap width
            // note: cast to int to avoid non integer values & sum of cells
            // surpassing that of the pane, causing incorrect columnt count
            int cell_width = (int) (nv.doubleValue()+EGAP)/columns;
            // above cell width includes 1 gap width per element so substract it
            tiles.setPrefTileWidth(cell_width-EGAP);
        });
//        scroll.setPadding(new Insets(0,0,EGAP,0));
        
        scroll.setPannable(false);  // forbid mouse panning
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefSize(-1,-1);  // leave resizable
        scroll.setFitToWidth(true); // make content resize with scroll pane        
        // consume problematic events and prevent from propagating
        // disables unwanted behavior of the popup
        scroll.addEventFilter(MOUSE_PRESSED, e->e.consume());
        scroll.addEventFilter(MOUSE_DRAGGED, e->e.consume());
        scroll.getStyleClass().add(STYLE_CLASS);
    }
    
    private void buildContent() {
        tiles.getChildren().clear(); 
        // get items
        getAccumulator().get()
            // & sort
            .sorted(Util.cmpareNoCase(getConverter()::toS))
            // & create cells
            .forEach( item -> {
                Node cell = getCellFactory().call(item);
                     cell.setOnMouseClicked( e -> {
                         getOnSelect().accept(item);
                         e.consume();
                     });
                tiles.getChildren().add(cell);
            });
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
     * Sets item supplier.
     * Gathers the source items as a stream. Default implementation returns empty
     * stream.
     * @param acc supplier. Must not be null;
     */
    public void setAccumulator(Supplier<Stream<E>> acc) {
        Objects.requireNonNull(acc);
        this.accumulator = acc;
    }
    
    /**
     * Returns item supplier. Never null.
     */
    public Supplier<Stream<E>> getAccumulator() {
        return accumulator;
    }
    
    /***
     * Sets cell factory.
     * Creates graphic representation of the item.
     * @param cf cell factory. Must not be null;
     */
    public void setCellFactory(Callback<E,Node> cf) {
        Objects.requireNonNull(cf);
        this.cellFactory = cf;
    }
    
    /**
     * Returns cell factory. Never null.
     */
    public Callback<E,Node> getCellFactory() {
        return cellFactory;
    }
    
    public Node getNode() {
        buildContent();
        return scroll;
    }
}
    
    
