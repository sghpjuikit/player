
package GUI.objects.Pickers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    
    private static final int EGAP = 5;      // element gap
    
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
        
        scroll.setOnMouseClicked(e->{
            if(e.getButton()==SECONDARY) {
                onCancel.run();
                e.consume();
            }
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
    
    
