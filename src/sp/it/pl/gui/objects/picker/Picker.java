package sp.it.pl.gui.objects.picker;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import sp.it.pl.gui.objects.Text;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.functional.Functors.Ƒ1;
import sp.it.pl.util.parsing.ConverterToString;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.scene.text.TextAlignment.JUSTIFY;
import static javafx.util.Duration.millis;
import static sp.it.pl.util.animation.Anim.seq;
import static sp.it.pl.util.functional.Util.byNC;
import static sp.it.pl.util.functional.Util.forEachWithI;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.graphics.Util.layScrollVText;
import static sp.it.pl.util.graphics.UtilKt.setScaleXY;


/**
 * Generic item picker.
 * <p/>
 * Node displaying elements as grid.
 * Elements are converted to their text representation according to provided
 * mapper. Element should override toString() method if no mapper is provided.
 * <p/>
 * Elements will be sorted lexicographically.
 * <p/>
 * Elements will be represented graphically depending on the cell factory.
 */
public class Picker<E> {

	/** Style class for cell. */
	public static final String STYLE_CLASS = "item-picker";
	public static final List<String> CELL_STYLE_CLASS = asList("block", "item-picker-element");
	/** Default on select action. Does nothing. */
	public static final Consumer<Object> DEF_onSelect = item -> {};
	/** Default on cancel action. Does nothing. */
	public static final Runnable DEF_onCancel = () -> {};
	/** Default text factory. Uses null safe version of object's toString() method. */
	public static final ConverterToString<Object> DEF_textConverter = Objects::toString;
	/** Default text factory. Returns empty string. */
	public static final ConverterToString<Object> DEF_infoConverter = item -> "";
	/** Default Item supplier. Returns empty stream. */
	public static final Supplier<? extends Stream<?>> DEF_itemSupply = Stream::empty;

	public boolean consumeCancelClick = false;

	private final CellPane tiles = new CellPane();
	public final ScrollPane root = new ScrollPane(tiles);

	/**
	 * Procedure executed when item is selected passing the item as parameter.
	 * Default implementation does nothing. Must not be null;
	 */
	public Consumer<? super E> onSelect = DEF_onSelect;
	/**
	 * Procedure executed when no item is selected. Invoked when user cancels
	 * the picking by right click.
	 * Default implementation does nothing. Must not be null;
	 * <p/>
	 * For example one might want to close this picker when no item is selected.
	 */
	public Runnable onCancel = DEF_onCancel;
	/**
	 * Text factory.
	 * Creates string representation of the item.
	 * Default implementation is {@link Picker#DEF_textConverter}
	 * Must not be null.
	 */
	public ConverterToString<? super E> textConverter = DEF_textConverter;
	/**
	 * Info text factory.
	 * Creates string representation of the item.
	 * Default implementation is {@link Picker#DEF_infoConverter}
	 * Must not be null.
	 */
	public ConverterToString<? super E> infoConverter = DEF_infoConverter;
	/**
	 * Item supplier. Fetches the items as a stream.
	 * Default implementation returns empty stream. Must not be null;
	 */
	public Supplier<? extends Stream<E>> itemSupply = (Supplier) DEF_itemSupply;
	/**
	 * Cell factory.
	 * Creates graphic representation of the item.
	 * Also might define minimum and maximum item size.
	 * Must not be null;
	 */
	public Ƒ1<E,Pane> cellFactory = item -> {
		String text = textConverter.toS(item);
		Label l = new Label(text);
		StackPane cell = new StackPane(l);
		cell.setMinSize(90, 30);
		cell.getStyleClass().setAll(CELL_STYLE_CLASS);

		// set up info pane
		String info = infoConverter.toS(item);
		if (!info.isEmpty()) {
			// info content
			Node content = cell.getChildren().get(0);
			Text ta = new Text(info);
			ta.setMouseTransparent(true);
			ta.setTextAlignment(JUSTIFY);
			ScrollPane sp = layScrollVText(ta);
			cell.getChildren().add(sp);
			cell.setPadding(new Insets(20));
			// animation
			Anim anim = new Anim(millis(300), x -> {
				sp.setOpacity(x);
				content.setOpacity(1 - x);
				setScaleXY(sp, 0.5 + 0.5*x);
			});
			anim.applier.accept(0d);
			cell.hoverProperty().addListener((o, ov, nv) -> anim.playFromDir(nv));
		}

		return cell;
	};

	public Picker() {
		root.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
		root.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);  // leave resizable
		root.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
		root.setPannable(false);  // forbid mouse panning (can cause unwanted horizontal scrolling)
		root.setFitToWidth(true); // make content horizontally resize with scroll pane
		root.setHbarPolicy(NEVER);  // thus no need for horizontal scrollbar
		root.setOnMouseClicked(e -> {
			// right click runs cancel, sometimes its important to consume the event, sometimes
			// it is important for it to pass through. so I left it configurable until I find a
			// better way
			if (e.getButton()==SECONDARY) {
				onCancel.run();
				if (consumeCancelClick) e.consume();
			}
		});
		root.getStyleClass().add(STYLE_CLASS);
	}

	public void buildContent() {
		tiles.getChildren().clear();
		// get items
		itemSupply.get()
			// & sort
			.sorted(byNC(textConverter::toS))
			// & create cells
			.forEach(item -> {
				Node cell = cellFactory.apply(item);
				cell.setOnMouseClicked(e -> {
					if (e.getButton()==PRIMARY) {
						onSelect.accept(item);
						e.consume();
					}
				});
				tiles.getChildren().add(cell);
			});

		// animate & show
		int s = getCells().size();
		Anim.par(getCells(), (i, n) -> seq(
			new Anim(n::setOpacity).dur(i*(750/s)).intpl(0),
			new Anim(n::setOpacity).dur(500).intpl(x -> sqrt(x))
		)).play();
	}

	public Region getNode() {
		buildContent();
		return root;
	}

	public List<Region> getCells() {
		return (List) list(tiles.getChildren());
	}

	private class CellPane extends Pane {

		@Override
		protected void layoutChildren() {
			double width = root.getWidth();
			double height = root.getHeight();

			int gap = 5;
			int elements = getChildren().size();
			double min_cell_w = max(1, getCells().get(0).getMinWidth());
			double min_cell_h = max(1, getCells().get(0).getMinHeight());
			// if (elements==0) return;

			int c = width>height ? (int) ceil(sqrt(elements)) : (int) floor(sqrt(elements));
			c = width<c*min_cell_w ? (int) floor(width/min_cell_w) : c;
			final int columns = max(1, c);

			int rows = (int) ceil(elements/(double) columns);

			double sumgapy = (rows - 1)*gap;
			final double cell_height = height<rows*min_cell_h ? min_cell_h : (height - sumgapy)/rows - 1/(double) rows;

			double W = rows*(cell_height + gap) - gap>height ? width - 15 : width; // take care of scrollbar
			double sumgapx = (columns - 1)*gap;  // n elements have n-1 gaps
			final double cell_width = (W - sumgapx)/columns;

			forEachWithI(getCells(), (i, n) -> {
				double x = i%columns*(cell_width + gap);
				double y = i/columns*(cell_height + gap);
				n.relocate(x, y);
				n.resize(cell_width, cell_height);
			});
		}

	}
}