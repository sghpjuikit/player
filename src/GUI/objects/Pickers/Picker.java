
package GUI.objects.Pickers;

import static java.lang.Math.*;
import static java.util.Arrays.asList;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import util.functional.Util;
import static util.functional.Util.forEachIndexed;
import util.functional.functor.FunctionC;
import util.parsing.ToStringConverter;

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
    public static final List<String> CELL_STYLE_CLASS = asList("block","item-picker-element");
    /** Default on select action. Does nothing. */
    public static final Consumer DEF_onSelect = item -> {};
    /** Default on cancel action. Does nothing. */
    public static final Runnable DEF_onCancel = () -> {};
    /** Default Text factory. Uses item's toString() method. */
    public static final ToStringConverter DEF_textCoverter = Object::toString;
    /** Default Item supplier. Returns empty stream. */
    public static final Supplier<Stream> DEF_itemSupply = Stream::empty;
    
    private final AnchorPane tiles = new AnchorPane();
    public final ScrollPane root = new ScrollPane(tiles);
    
    /**
     * Procedure executed when item is selected passing the item as parameter.
     * Default implementation does nothing. Must not be null;
     */
    public Consumer<E> onSelect = DEF_onSelect;
    /**
     * Procedure executed when no item is selected. Invoked when user cancels
     * the picking by right click.
     * Default implementation does nothing. Must not be null;
     * <p>
     * For example one might want to close this picker when no item is selected.
     */
    public Runnable onCancel = DEF_onCancel;
    /**
     * Text factory.
     * Creates string representation of the item.
     * Default implementation uses item's toString() method.
     * Must not be null.
     */
    public ToStringConverter<E> textCoverter = DEF_textCoverter;
    /**
     * Item supplier. Fetches the items as a stream. 
     * Default implementation returns empty stream. Must not be null;
     */
    public Supplier<Stream<E>> itemSupply = (Supplier)DEF_itemSupply;
    /**
     * Cell factory.
     * Creates graphic representation of the item.
     * Also might define minimum and maximum item size.
     * Must not be null;
     */
    public FunctionC<E,Node> cellFactory = item -> {
        String text = textCoverter.toS(item);
        Label l = new Label(text);
        StackPane b = new StackPane(l);
        b.getStyleClass().setAll(CELL_STYLE_CLASS);
        b.setMinSize(80, 25);
        return b;
    };
    
    public Picker() {
        // auto-layout
        root.widthProperty().addListener((o,ov,nv) -> layout(nv.doubleValue(), root.getHeight()));
        root.heightProperty().addListener((o,ov,nv) -> layout(root.getWidth(),nv.doubleValue()));
        
        root.setPannable(false);  // forbid mouse panning
        root.setHbarPolicy(NEVER);
        root.setPrefSize(-1,-1);  // leave resizable
        root.setFitToWidth(true); // make content resize with scroll pane        
        // consume problematic events and prevent from propagating
        // disables unwanted behavior of the popup
        root.addEventFilter(MOUSE_PRESSED, Event::consume);
        root.addEventFilter(MOUSE_DRAGGED, Event::consume);
        root.setOnMouseClicked(e->{
            if(e.getButton()==SECONDARY) {
                onCancel.run();
//                e.consume(); // causes problems for layouter, off for now
            }
        });
        root.getStyleClass().add(STYLE_CLASS);
    }
    
    public void buildContent() {
        tiles.getChildren().clear(); 
        // get items
        itemSupply.get()
            // & sort
            .sorted(Util.cmpareNoCase(textCoverter::toS))
            // & create cells
            .forEach( item -> {
                Node cell = cellFactory.apply(item);
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
        return root;
    }
    
    private void layout(double width, double height) {
        int gap = 5;
        int elements = tiles.getChildren().size();
//        if(elements==0) return;
        
        final int columns = width>height ? (int) ceil(sqrt(elements))
                                         : (int) floor(sqrt(elements));
        int rows = (int) ceil(elements/(double)columns);
        
        double sumgapy = (rows-1) * gap;
        final double cell_height = (height-sumgapy)/rows-1/(double)rows;
        
        double W = rows*(cell_height+gap)-gap>height ? width-15 : width; // take care of scrollbar
        double sumgapx = (columns-1) * gap;  // n elements have n-1 gaps
        final double cell_width = (W-sumgapx)/columns;

        forEachIndexed(tiles.getChildren(), (i,n) -> {
            double x = i%columns * (cell_width+gap);
            double y = i/columns * (cell_height+gap);
            n.setLayoutX(x);
            n.setLayoutY(y);
            ((Region)n).setPrefWidth(cell_width);
            ((Region)n).setPrefHeight(cell_height);
        });
    }
}