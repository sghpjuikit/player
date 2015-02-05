
package GUI.objects.Pickers;

import static java.lang.Integer.min;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Callback;
import util.Parser.ToStringConverter;
import util.functional.FunctUtil;

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
    
    
    
    private final TilePane tiles = new TilePane();
    private final ScrollPane scroll = new ScrollPane(tiles);
    
    /**
     * Procedure executed when item is selected passing the item as parameter.
     * Default implementation does nothing. Must not be null;
     */
    public Consumer<E> onSelect = item -> {};
    /**
     * Procedure executed when no item is selected. Invoked when user cancels
     * the picking by right click.
     * Default implementation does nothing. Must not be null;
     * <p>
     * For example one might want to close this picker when no item is selected.
     */
    public Runnable onCancel = () -> {};
    /**
     * Text factory.
     * Creates string representation of the item.
     * Default implementation uses item's toString() method.
     * Must not be null.
     */
    public ToStringConverter<E> textCoverter = Object::toString;
    /**
     * Item supplier. Fetches the items as a stream. 
     * Default implementation returns empty stream. Must not be null;
     */
    public Supplier<Stream<E>> itemSupply = Stream::empty;
    /**
     * Cell factory.
     * Creates graphic representation of the item.
     * @param cf cell factory. Must not be null;
     */
    public Callback<E,Node> cellFactory = item -> {
        String text = textCoverter.toS(item);
        Label l = new Label(text);
        StackPane b = new StackPane(l);
        b.getStyleClass().setAll(CELL_STYLE_CLASS);
        return b;
    };
    
    public Picker() {
        int gap = 5;
        
        // set spacing
        tiles.setHgap(gap);
        tiles.setVgap(gap);
        // set autosizing for tiles to always fill the grid entirely
        tiles.widthProperty().addListener((o,ov,nv) -> {
            int columns = (int) floor(nv.doubleValue()/100);
            // set maximum number of columns to 7
                columns = min(columns, 6);
            // for n elements there is n-1 gaps so we add 1 gap and divide by
            // columns to get cell width + gap, which we substract
            // note: cast to int to avoid non integer values & sum of cells
            // surpassing that of the pane, causing incorrect columnt count
            double cell_width = ((int) (nv.doubleValue()+gap)/columns) - gap;
            tiles.setPrefTileWidth(cell_width);
            
            int rows = (int) ceil(tiles.getChildren().size()/columns);
            double cell_height = ((int) ((tiles.getHeight()+gap)/rows)) - gap;
            tiles.setPrefTileHeight(cell_height);
            tiles.setPrefTileHeight(50);
        });
        
        scroll.setPannable(false);  // forbid mouse panning
        scroll.setHbarPolicy(NEVER);
        scroll.setPrefSize(-1,-1);  // leave resizable
        scroll.setFitToWidth(true); // make content resize with scroll pane        
        // consume problematic events and prevent from propagating
        // disables unwanted behavior of the popup
        scroll.addEventFilter(MOUSE_PRESSED, Event::consume);
        scroll.addEventFilter(MOUSE_DRAGGED, Event::consume);
        scroll.setOnMouseClicked(e->{
            if(e.getButton()==SECONDARY) {
                onCancel.run();
                e.consume();
            }
        });
        scroll.getStyleClass().add(STYLE_CLASS);
    }
    
    private void buildContent() {
        tiles.getChildren().clear(); 
        // get items
        itemSupply.get()
            // & sort
            .sorted(FunctUtil.cmpareNoCase(textCoverter::toS))
            // & create cells
            .forEach( item -> {
                Node cell = cellFactory.call(item);
                     cell.setOnMouseClicked( e -> {
                         if(e.getButton()==PRIMARY) {
                            onSelect.accept(item);
                            e.consume();
                         }
                     });
                tiles.getChildren().add(cell);
            });
    }
    
    public Node getNode() {
        buildContent();
        return scroll;
    }
}