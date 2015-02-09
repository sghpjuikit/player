
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
import javafx.util.Callback;
import util.Parser.ToStringConverter;
import util.functional.FunctUtil;
import static util.functional.FunctUtil.forEachIndexed;

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
    
    private final AnchorPane tiles = new AnchorPane();
    public final ScrollPane root = new ScrollPane(tiles);
    
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
     * Also might define minimum and maximum item size.
     * Must not be null;
     */
    public Callback<E,Region> cellFactory = item -> {
        String text = textCoverter.toS(item);
        Label l = new Label(text);
        StackPane b = new StackPane(l);
        b.getStyleClass().setAll(CELL_STYLE_CLASS);
        b.setMinSize(50, 20);
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
//        int gap = 5;
//        final int columns = clip(1,(int) floor(width/100), 5);
//        int rows = (int) ceil(tiles.getChildren().size()/(double)columns);
//        double W = rows*(50+gap)>height ? width-15 : width; // take care of scrollbar
//        
//        double sumgap = (columns-1) * gap;  // n elements have n-1 gaps
//        final double cell_width = (W-sumgap)/columns;
//        final double cell_height = (height-sumgap)/rows;
//
//        forEachIndexed(tiles.getChildren(), (i,n) -> {
//            double x = i%columns * (cell_width+gap);
//            double y = i/columns * (cell_height+gap);
//            n.setLayoutX(x);
//            n.setLayoutY(y);
//            ((Region)n).setPrefWidth(cell_width);
//            ((Region)n).setPrefHeight(cell_height);
//        });
    }
}