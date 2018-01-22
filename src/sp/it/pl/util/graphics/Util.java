package sp.it.pl.util.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.reactfx.EventSource;
import org.reactfx.Subscription;
import sp.it.pl.util.access.V;
import sp.it.pl.util.dev.Dependency;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static java.time.Duration.ofMillis;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.Modality.APPLICATION_MODAL;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.dev.Util.noØ;

/**
 * Graphic utility methods.
 */
@SuppressWarnings("unused")
public interface Util {

/* ---------- LAYOUT ------------------------------------------------------------------------------------------------ */

	/** Constructs ordinary {@link javafx.scene.layout.HBox}. Convenience constructor for more fluent style. */
	static HBox layHorizontally(double gap, Pos align, Node... nodes) {
		HBox l = new HBox(gap, nodes);
		l.setAlignment(align);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.HBox}. Convenience constructor for more fluent style. */
	static HBox layHorizontally(double gap, Pos align, List<? extends Node> nodes) {
		HBox l = new HBox(gap);
		l.setAlignment(align);
		l.getChildren().addAll(nodes);
		return l;
	}

	/**
	 * Horizontal layout where content takes as much horizontal space as possible. Header on the left
	 * shrinks to its content.
	 * Sets {@link HBox#setHgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
	 * to {@link javafx.scene.layout.Priority#ALWAYS}.
	 * <p/>
	 * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
	 */
	static HBox layHeaderLeft(double gap, Pos align, Node header, Node content) {
		HBox l = layHorizontally(gap, align, header, content);
		HBox.setHgrow(content, ALWAYS);
		return l;
	}

	/**
	 * Horizontal layout where content takes as much horizontal space as possible. Header on the right
	 * shrinks to its content.
	 * Sets {@link HBox#setHgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
	 * to {@link javafx.scene.layout.Priority#ALWAYS}.
	 * <p/>
	 * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
	 */
	static HBox layHeaderRight(double gap, Pos align, Node content, Node header) {
		HBox l = layHorizontally(gap, align, content, header);
		HBox.setHgrow(content, ALWAYS);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style. */
	static VBox layVertically(double gap, Pos align, Node... nodes) {
		VBox l = new VBox(gap, nodes);
		l.setAlignment(align);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style. */
	static VBox layVertically(double gap, Pos align, List<? extends Node> nodes) {
		VBox l = new VBox(gap);
		l.setAlignment(align);
		l.getChildren().addAll(nodes);
		return l;
	}

	/**
	 * Vertical layout where content takes as much vertical space as possible. Header at the top
	 * shrinks to its content.
	 * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
	 * to {@link javafx.scene.layout.Priority#ALWAYS}.
	 * <p/>
	 * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
	 */
	static VBox layHeaderTop(double gap, Pos align, Node header, Node content) {
		VBox l = layVertically(gap, align, header, content);
		VBox.setVgrow(content, ALWAYS);
		return l;
	}

	/**
	 * Vertical layout where content takes as much vertical space as possible. Header at the bottom
	 * shrinks to its content.
	 * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
	 * to {@link javafx.scene.layout.Priority#ALWAYS}.
	 * <p/>
	 * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
	 */
	static VBox layHeaderBottom(double gap, Pos align, Node content, Node header) {
		VBox l = layVertically(gap, align, content, header);
		VBox.setVgrow(content, ALWAYS);
		return l;
	}

	/**
	 * Vertical layout where content takes as much vertical space as possible. Headers at the top
	 * and bottom shrink to their content.
	 * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
	 * to {@link javafx.scene.layout.Priority#ALWAYS}.
	 * <p/>
	 * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
	 */
	static VBox layHeaderTopBottom(double gap, Pos align, Node headerTop, Node content, Node headerBottom) {
		VBox l = layVertically(gap, align, headerTop, content, headerBottom);
		VBox.setVgrow(content, ALWAYS);
		return l;
	}

	/**
	 * Constructs ordinary {@link javafx.scene.layout.StackPane} with children aligned to {@link Pos#CENTER}.
	 * Convenience constructor for more fluent style.
	 */
	static StackPane layStack(Node... ns) {
		return new StackPane(ns);
	}

	/** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
	static StackPane layStack(Node n, Pos a) {
		StackPane l = new StackPane(n);
		StackPane.setAlignment(n, a);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
	static StackPane layStack(Node n1, Pos a1, Node n2, Pos a2) {
		StackPane l = new StackPane(n1, n2);
		StackPane.setAlignment(n1, a1);
		StackPane.setAlignment(n2, a2);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
	static StackPane layStack(Node n1, Pos a1, Node n2, Pos a2, Node n3, Pos a3) {
		StackPane l = new StackPane(n1, n2, n3);
		StackPane.setAlignment(n1, a1);
		StackPane.setAlignment(n2, a2);
		StackPane.setAlignment(n3, a3);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
	static StackPane layStack(Node n1, Pos a1, Node n2, Pos a2, Node n3, Pos a3, Node n4, Pos a4) {
		StackPane l = new StackPane(n1, n2, n3, n4);
		StackPane.setAlignment(n1, a1);
		StackPane.setAlignment(n2, a2);
		StackPane.setAlignment(n3, a3);
		StackPane.setAlignment(n4, a4);
		return l;
	}

	/** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
	static AnchorPane layAnchor(Node n, Double a) {
		AnchorPane p = new AnchorPane();
		setAnchor(p, n, a);
		return p;
	}

	/** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
	static AnchorPane layAnchor(Node n, Double top, Double right, Double bottom, Double left) {
		AnchorPane p = new AnchorPane();
		setAnchor(p, n, top, right, bottom, left);
		return p;
	}

	/** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
	static AnchorPane layAnchor(Node n1, Double top1, Double right1, Double bottom1, Double left1,
								Node n2, Double top2, Double right2, Double bottom2, Double left2) {
		AnchorPane p = new AnchorPane();
		setAnchor(p, n1, top1, right1, bottom1, left1, n2, top2, right2, bottom2, left2);
		return p;
	}

	/** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
	static AnchorPane layAnchor(Node n1, Double top1, Double right1, Double bottom1, Double left1,
								Node n2, Double top2, Double right2, Double bottom2, Double left2,
								Node n3, Double top3, Double right3, Double bottom3, Double left3) {
		AnchorPane p = new AnchorPane();
		setAnchor(p, n1, top1, right1, bottom1, left1, n2, top2, right2, bottom2, left2, n3, top3, right3, bottom3, left3);
		return p;
	}

	/** Sets {@link AnchorPane} anchors to the same value. Null clears all anchors. */
	static void setAnchors(Node n, Double a) {
		if (a==null) {
			AnchorPane.clearConstraints(n);
		} else {
			AnchorPane.setTopAnchor(n, a);
			AnchorPane.setRightAnchor(n, a);
			AnchorPane.setBottomAnchor(n, a);
			AnchorPane.setLeftAnchor(n, a);
		}
	}

	/** Sets {@link AnchorPane} anchors. Null clears the respective anchor. */
	static void setAnchors(Node n, Double top, Double right, Double bottom, Double left) {
		AnchorPane.clearConstraints(n);
		if (top!=null) AnchorPane.setTopAnchor(n, top);
		if (right!=null) AnchorPane.setRightAnchor(n, right);
		if (bottom!=null) AnchorPane.setBottomAnchor(n, bottom);
		if (left!=null) AnchorPane.setLeftAnchor(n, left);
	}

	/** Sets {@link javafx.scene.layout.AnchorPane} anchors for node. Convenience method for more fluent style. */
	static void setAnchor(AnchorPane pane, Node n, Double a) {
		pane.getChildren().add(n);
		setAnchors(n, a);
	}

	/** Sets {@link javafx.scene.layout.AnchorPane} anchors for node. Convenience method for more fluent style. */
	static void setAnchor(AnchorPane pane, Node n, Double top, Double right, Double bottom, Double left) {
		pane.getChildren().add(n);
		setAnchors(n, top, right, bottom, left);
	}

	/** Sets {@link javafx.scene.layout.AnchorPane} anchors for nodes. Convenience method for more fluent style. */
	static void setAnchor(AnchorPane pane, Node n1, Double top1, Double right1, Double bottom1, Double left1,
						  Node n2, Double top2, Double right2, Double bottom2, Double left2) {
		setAnchor(pane, n1, top1, right1, bottom1, left1);
		setAnchor(pane, n2, top2, right2, bottom2, left2);
	}

	/** Sets {@link javafx.scene.layout.AnchorPane} anchors for nodes. Convenience method for more fluent style. */
	static void setAnchor(AnchorPane pane, Node n1, Double top1, Double right1, Double bottom1, Double left1,
						  Node n2, Double top2, Double right2, Double bottom2, Double left2,
						  Node n3, Double top3, Double right3, Double bottom3, Double left3) {
		setAnchor(pane, n1, top1, right1, bottom1, left1);
		setAnchor(pane, n2, top2, right2, bottom2, left2);
		setAnchor(pane, n3, top3, right3, bottom3, left3);
	}

	// TODO: make sure this works 100%
	/** Gives the text shape wrapping to its width and scrollable functionalities. */
	static ScrollPane layScrollVText(Text t) {
		// This is how it should be done, but there is a bug.
		// Unfortunately the pane resizes with the text so we cant bind
		// t.wrappingWidthProperty().bind(sa.widthProperty());
		// The only (to me) known solution is to make the text t not manageable, but that
		// causes the height calculation of the pane sa fail and consequently breaks the
		// scrolling behavior
		// I do not know what to do anymore, believe me I have tried...
//        Pane sa = new StackPane(t);
//        ScrollPane s = new ScrollPane(sa);
//                   s.setPannable(false);
//                   s.setFitToWidth(true);
//                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
//                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
//        t.wrappingWidthProperty().bind(sa.widthProperty());

		// Scrollbar width hardcoded!
		double reserve = 5;
		ScrollPane s = new ScrollPane(t);
		s.setOnScroll(Event::consume);
		s.setPannable(false);
		s.setFitToWidth(false);
		s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		s.setHbarPolicy(ScrollBarPolicy.NEVER);
		t.wrappingWidthProperty().bind(s.widthProperty().subtract(15 + reserve));
		return s;
	}

	static ScrollPane layScrollVTextCenter(Text t) {
		double reserve = 5;
		ScrollPane s = new ScrollPane(new StackPane(t));
		s.setOnScroll(Event::consume);
		s.setPannable(false);
		s.setFitToWidth(false);
		s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		s.setHbarPolicy(ScrollBarPolicy.NEVER);
		t.wrappingWidthProperty().bind(s.widthProperty().subtract(15 + reserve));
		return s;
	}

	static <E extends Event> void add1timeEventHandler(Stage eTarget, EventType<E> eType, Consumer<E> eHandler) {
		eTarget.addEventHandler(eType, new EventHandler<>() {
			@Override
			public void handle(E event) {
				eHandler.accept(event);
				eTarget.removeEventHandler(eType, this);
			}
		});
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	// TODO: make dpi aware
	static WritableImage makeSnapshot(Node n) {
		return n.snapshot(new SnapshotParameters(), null);
	}

	@Deprecated
	static Subscription hovered(Node n, Consumer<? super Boolean> handler) {
		EventSource<Boolean> events = new EventSource<>();
		EventHandler<MouseEvent> eTrue = e -> events.push(true);
		EventHandler<MouseEvent> eFalse = e -> events.push(false);
		ChangeListener<Boolean> eVal = (o, ov, nv) -> events.push(nv);

		n.hoverProperty().addListener(eVal);
		//        n.addEventFilter(MOUSE_MOVED, eTrue);
		//        n.addEventFilter(MOUSE_ENTERED_TARGET, eTrue);
		//        n.addEventFilter(MOUSE_EXITED_TARGET, eFalse);
		Subscription s = events.successionEnds(ofMillis(50)).subscribe(handler);

		return Subscription.multi(
				s,
				() -> n.removeEventFilter(MOUSE_MOVED, eTrue),
				() -> n.removeEventFilter(MOUSE_ENTERED_TARGET, eTrue),
				() -> n.removeEventFilter(MOUSE_EXITED_TARGET, eFalse),
				() -> n.hoverProperty().removeListener(eVal)
		);
	}

/* ---------- TABLE ------------------------------------------------------------------------------------------------- */

	/**
	 * Creates column that indexes rows from 1 and is right aligned. The column
	 * is of type Void - table data type agnostic.
	 *
	 * @param name name of the column. For example "#"
	 * @return the column
	 */
	static <T> TableColumn<T,Void> createIndexColumn(String name) {
		TableColumn<T,Void> c = new TableColumn<>(name);
		c.setSortable(false);
		c.setCellFactory(column -> new TableCell<>() {
			{
				setAlignment(CENTER_RIGHT);
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) setText(null);
				else setText(String.valueOf(getIndex() + 1) + ".");
			}
		});
		return c;
	}

	/**
	 * Creates default cell factory, which sets cell text to provided text when
	 * cells text equals "". This is to differentiate between empty cell and nonempty
	 * cell with 'empty' value.
	 * For example: '<empty cell>'
	 *
	 * @param empty_value empty cell string value
	 * @return cell factory
	 */
	static <T, O> Callback<TableColumn<T,O>,TableCell<T,O>> EMPTY_TEXT_DEFAULT_CELL_FACTORY(String empty_value) {
		return param -> new TableCell<>() {
			@Override
			protected void updateItem(O item, boolean empty) {
				if (item==getItem()) return;

				super.updateItem(item, empty);

				if (item==null) {
					super.setText(null);
					super.setGraphic(null);
				} else if ("".equals(item)) {
					super.setText(empty_value);
					super.setGraphic(null);
				} else if (item instanceof Node) {
					super.setText(null);
					super.setGraphic((Node) item);
				} else {
					super.setText(item.toString());
					super.setGraphic(null);
				}
			}
		};
	}

	/**
	 * Same as {@link #cellFactoryAligned(javafx.geometry.Pos, String)}, but
	 * the alignment is inferred from the type of element in the cell (not table
	 * or column, because we are aligning cell content) in the following way:
	 * <br>
	 * String content is aligned to CENTER_LEFT and the rest CENTER_RIGHT.
	 * <p/>
	 * The factory will need to be cast if it its generic types are declared.
	 *
	 * @param type for cell content.
	 */
	static <T, O> Ƒ1<TableColumn<T,O>,TableCell<T,O>> cellFactoryAligned(Class<O> type, String no_val_text) {
		Pos a = type.equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
		return cellFactoryAligned(a, no_val_text);
	}

	/**
	 * Returns {@link TableColumn#DEFAULT_CELL_FACTORY} (the default factory used
	 * when no factory is specified), aligning the cell content to specified value.
	 * <p/>
	 * The factory will need to be cast if it its generic types are declared.
	 *
	 * @param a cell alignment
	 * @return cell factory
	 */
	@SuppressWarnings("unchecked")
	static <T, O> Ƒ1<TableColumn<T,O>,TableCell<T,O>> cellFactoryAligned(Pos a, String no_val_text) {
		Ƒ1<TableColumn<T,O>,TableCell<T,O>> f = Util.<T,O>EMPTY_TEXT_DEFAULT_CELL_FACTORY(no_val_text)::call;
		return f.andApply(cell -> cell.setAlignment(a));
	}

	/**
	 * Convenience method to make it easier to select given rows of the
	 * TableView via its SelectionModel.
	 * This methods provides alternative to TableViewSelectionModel.selectIndices()
	 * that requires array parameter. This method makes the appropriate conversions
	 * and selects the items using List parameter
	 * <p/>
	 * After the method is invoked only the provided rows will be selected - it
	 * clears any previously selected rows.
	 */
	static void selectRows(List<Integer> selectedIndexes, TableViewSelectionModel<?> selectionModel) {
		selectionModel.clearSelection();
		int[] newSelected = new int[selectedIndexes.size()];
		for (int i = 0; i<selectedIndexes.size(); i++) {
			newSelected[i] = selectedIndexes.get(i);
		}
		if (newSelected.length!=0) {
			selectionModel.selectIndices(newSelected[0], newSelected);
		}
	}

/* ---------- EVENT ------------------------------------------------------------------------------------------------- */

	EventHandler<MouseEvent> consumeOnSecondaryButton = e -> {
		if (e.getButton()==MouseButton.SECONDARY) e.consume();
	};

	/**
	 * Increases or increases the scrolling speed (deltaX/Y, textDeltaX/Y of the {@link ScrollEvent#ANY})
	 * by a multiplication factor.
	 */
	static Subscription multiplyMouseScrollingSpeed(Node node, double factor) {
		V<Boolean> scrollFlag = new V<>(true);
		EventHandler<ScrollEvent> h = e -> {
			if (scrollFlag.get()) {
				Event ne = new ScrollEvent(
						e.getEventType(), e.getX(), e.getY(), e.getScreenX(), e.getScreenY(),
						e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(), e.isDirect(),
						e.isInertia(), e.getDeltaX()*factor, e.getDeltaY()*factor, e.getTextDeltaX()*factor, e.getTextDeltaY()*factor,
						e.getTextDeltaXUnits(), e.getTextDeltaX()*factor, e.getTextDeltaYUnits(), e.getTextDeltaY()*factor,
						e.getTouchCount(), e.getPickResult()
				);
				e.consume();
				scrollFlag.set(false);
				runLater(() -> {
					if (e.getTarget() instanceof Node) {
						((Node) e.getTarget()).fireEvent(ne);
					}
					scrollFlag.set(true);
				});
			}
		};
		node.addEventFilter(ScrollEvent.ANY, h);
		return () -> node.removeEventFilter(ScrollEvent.ANY, h);

	}

/* ---------- MENU -------------------------------------------------------------------------------------------------- */

	/**
	 * Creates menu item.
	 *
	 * @param text non null text of the menu item
	 * @param action non null action taking the action even as a parameter
	 * @return non null menu item
	 * @throws java.lang.RuntimeException if any param null
	 */
	static MenuItem menuItem(String text, EventHandler<ActionEvent> action) {
		noØ(action);
		MenuItem i = new MenuItem(text);
		i.setOnAction(action);
		return i;
	}

/* ---------- FONT -------------------------------------------------------------------------------------------------- */

	// internal com.sun.javafx.scene.control.skin.Utils class seems to be able to do this
	@Dependency("requires access to javafx.graphics/com.sun.javafx.tk")
	static double computeFontWidth(javafx.scene.text.Font font, String text) {
		// TODO: jigsaw
		// return com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(text, font); // !work since java 9 b114
		com.sun.javafx.tk.FontMetrics fm = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().getFontMetrics(font);
		return text==null || text.isEmpty() ? 0 : text.chars().mapToDouble(c -> fm.getCharWidth((char) c)).sum();
	}

	@Dependency("requires access to javafx.graphics/com.sun.javafx.tk")
	static double computeFontHeight(javafx.scene.text.Font font) {
		// TODO: jigsaw
		// requires -XaddExports:javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
		return com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().getFontMetrics(font).getLineHeight();
	}

/* ---------- WINDOW ------------------------------------------------------------------------------------------------ */

	/**
	 * Create fullscreen modal no taskbar stage on given screen. The stage will have its owner,
	 * style and modality initialized and be prepared to be shown.
	 * <p/>
	 * Use: just set your scene on it and call show().
	 * <p/>
	 * Easy way to get popup like behavior that:
	 * <ul>
	 * <li> is always fullscreen
	 * <li> is modal - does not lose focus and is always on top of other application windows
	 * <li> has no taskbar icon (for your information, javafx normally disallows this, but it is
	 * doable using owner stage with UTILITY style).
	 * </ul>
	 */
	// TODO: fix scaling screwing up initial window position
	static Stage createFMNTStage(Screen screen) {
		return createFMNTStage(screen, true);
	}

	static Stage createFMNTStage(Screen screen, boolean show) {
		// Using owner stage of UTILITY style is the only way to get a 'top level'
		// window with no taskbar.
		Stage owner = new Stage(UTILITY);
		owner.setOpacity(0); // make sure it will never be visible
		owner.setWidth(5); // stay small to leave small footprint, just in case
		owner.setHeight(5);
		owner.show();
		owner.setX(screen.getBounds().getMinX() + 1);    // owner and child should be on the same screen
		owner.setY(screen.getBounds().getMinY() + 1);

		Stage s = new Stage(UNDECORATED); // no OS header & buttons, we want full space
		s.initOwner(owner);
		s.initModality(APPLICATION_MODAL); // eliminates focus stealing form other apps
		s.setAlwaysOnTop(true); // maybe not needed, but just in case

		if (show) s.show();    // part of the workaround below
		s.setX(screen.getBounds().getMinX()); // screen does not necessarily start at [0,0]
		s.setY(screen.getBounds().getMinY());
		s.setWidth(screen.getBounds().getWidth());
		s.setHeight(screen.getBounds().getHeight());
		// on multi-monitor setup with varying screen dpi, javaFX may use wrong dpi, leading to wrong Stage size,
		// s.setWidth(screen.getVisualBounds().getWidth()/Screen.getPrimary().getOutputScaleX()*screen.getOutputScaleX());
		// s.setHeight(screen.getVisualBounds().getHeight()/Screen.getPrimary().getOutputScaleY()*screen.getOutputScaleY());

		// Going fullscreen actually breaks things.
		// We do not need fullscreen, we use UNDECORATED stage of maximum size. Fullscreen
		// was supposed to be more of a final nail to prevent possible focus stealing.
		//
		// In reality, using fullscreen actually causes this issue! - focus stealing
		// and the consequent disappearance of the window (nearly impossible to bring it
		// back). This is even when using modality on the window or even its owner stage.
		//
		// Fortunately things work as they should using things like we do.
		//
		// s.setFullScreen(true); // just in case
		// s.setFullScreenExitHint(""); // completely annoying, remove
		// // not always desired! and if we do not use fullscreen it wont work or we could just
		// // introduce inconsistent behavior. Let the dev implement his own hide if he needs.
		// s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

		// The owner must not escape garbage collection or remain visible forever
//        s.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> owner.hide());

		return s;
	}

/* ---------- SCREEN ------------------------------------------------------------------------------------------------ */

	/** Captures screenshot of the entire screen and runs custom action on fx thread. */
	static void screenCaptureAndDo(Screen screen, Consumer<Image> action) {
		Rectangle2D r = screen.getBounds();
		screenCaptureAndDo((int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(), (int) r.getHeight(), action);
	}

	/**
	 * Captures screenshot of the screen of given size and position and runs custom
	 * action on fx thread.
	 */
	static void screenCaptureAndDo(int x, int y, int w, int h, Consumer<Image> action) {
		screenCaptureRawAndDo(
				x, y, w, h,
				img -> {
					Image i = img==null ? null : SwingFXUtils.toFXImage(img, new WritableImage(img.getWidth(), img.getHeight()));
					runFX(() -> action.accept(i));
				}
		);
	}

	/** Captures screenshot of the entire screen and runs custom action on non fx thread. */
	static void screenCaptureRawAndDo(Screen screen, Consumer<BufferedImage> action) {
		Rectangle2D r = screen.getBounds();
		screenCaptureRawAndDo((int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(), (int) r.getHeight(), action);
	}

	/**
	 * Captures screenshot of the screen of given size and position and runs custom action on non fx thread.
	 * <p/>
	 * Based on: <a href="http://www.aljoscha-rittner.de/blog/archive/2011/03/09/javafxdev-screen-capture-tool-with-200-lines-and-500ms-startup-time/">javafx-dev-screen-capture-tool</a>
	 */
	static void screenCaptureRawAndDo(int x, int y, int w, int h, Consumer<BufferedImage> action) {
		EventQueue.invokeLater(() -> {
			Rectangle area = new Rectangle(x, y, w, h);
			try {
				Robot robot = new Robot();
				BufferedImage img = robot.createScreenCapture(area);
				action.accept(img);
			} catch (Exception e) {
				sp.it.pl.util.dev.Util.logger(Util.class).error("Failed to screenshot the screen {}", area, e);
				action.accept(null);
			}
		});
	}
}