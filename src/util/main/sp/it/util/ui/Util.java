package sp.it.util.ui;

import java.util.List;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import sp.it.util.JavaLegacy;
import sp.it.util.access.V;
import sp.it.util.reactive.Subscription;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static sp.it.util.async.AsyncKt.runLater;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.sync1IfNonNull;
import static sp.it.util.reactive.UtilKt.syncC;

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

	static ScrollPane layScrollVText(Text t) {
		ScrollPane s = new ScrollPane(t);
		s.setOnScroll(Event::consume);
		s.setPannable(false);
		s.setFitToWidth(false);
		s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		s.setHbarPolicy(ScrollBarPolicy.NEVER);

		sync1IfNonNull(s.skinProperty(), consumer(skin -> {
			ScrollBar scrollBar = (ScrollBar) s.lookupAll("ScrollBar").stream()
				.filter(it -> it instanceof ScrollBar && ((ScrollBar) it).getOrientation()==Orientation.VERTICAL)
				.findFirst().orElse(null);
			var updateWrappingWidth = (Runnable) () -> {
				var sw = scrollBar==null || !scrollBar.isVisible() ? 0 : scrollBar.getWidth();
				t.setWrappingWidth(s.getWidth()-sw);
			};
			if (scrollBar!=null) syncC(scrollBar.visibleProperty(), v -> updateWrappingWidth.run());
			if (scrollBar!=null) syncC(scrollBar.widthProperty(), v -> updateWrappingWidth.run());
			syncC(s.widthProperty(), v -> updateWrappingWidth.run());
		}));

		return s;
	}

	static ScrollPane layScrollVTextCenter(Text t) {
		double reserve = 5;
		ScrollPane s = new ScrollPane(new StackPane(t));
		s.setOnScroll(Event::consume);
		s.setPannable(false);
		s.setFitToWidth(true);
		s.setFitToHeight(true);
		s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		s.setHbarPolicy(ScrollBarPolicy.NEVER);

		sync1IfNonNull(s.skinProperty(), consumer(skin -> {
			ScrollBar scrollBar = (ScrollBar) s.lookupAll("ScrollBar").stream()
				.filter(it -> it instanceof ScrollBar && ((ScrollBar) it).getOrientation()==Orientation.VERTICAL)
				.findFirst().orElse(null);
			var updateWrappingWidth = (Runnable) () -> {
				var sw = scrollBar==null || !scrollBar.isVisible() ? 0 : scrollBar.getWidth();
				t.setWrappingWidth(s.getWidth()-sw);
			};
			if (scrollBar!=null) syncC(scrollBar.visibleProperty(), v -> updateWrappingWidth.run());
			if (scrollBar!=null) syncC(scrollBar.widthProperty(), v -> updateWrappingWidth.run());
			syncC(s.widthProperty(), v -> updateWrappingWidth.run());
		}));

		return s;
	}

	static <E extends Event> void addEventHandler1Time(Window window, EventType<E> eType, Consumer<E> eHandler) {
		window.addEventHandler(eType, new EventHandler<>() {
			@Override
			public void handle(E event) {
				eHandler.accept(event);
				window.removeEventHandler(eType, this);
			}
		});
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
				else setText(getIndex() + 1 + ".");
			}
		});
		return c;
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

	/**
	 * Increases or decreases the scrolling speed (deltaX/Y, textDeltaX/Y of the {@link ScrollEvent#ANY})
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

/* ---------- FONT -------------------------------------------------------------------------------------------------- */

	static double computeFontWidth(Font font, String text) {
		return JavaLegacy.computeFontWidth(font , text);
	}

	static double computeFontHeight(Font font, String text) {
		return JavaLegacy.computeFontHeight(font, text);
	}

/* ---------- WINDOW ------------------------------------------------------------------------------------------------ */

	// TODO: fix scaling screwing up initial window position
	static Stage createFMNTStage(Screen screen) {
		return createFMNTStage(screen, true);
	}

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
	static Stage createFMNTStage(Screen screen, boolean show) {
		Stage owner = new Stage(UTILITY);  // UTILITY owner is the only way to get a 'top level' window with no taskbar.
		owner.setOpacity(0);
		owner.setWidth(10);
		owner.setHeight(10);
		owner.show();
		owner.setAlwaysOnTop(true);
		owner.setX(screen.getBounds().getMinX() + 1);    // owner and child should be on the same screen
		owner.setY(screen.getBounds().getMinY() + 1);

		Stage s = new Stage(UNDECORATED) {
			@Override
			public void hide() {
				//  Due to JavaFX bugs it is impossible to hide the owner after this is hidden, so we override hide()
				//  so the owner is hidden first. It automatically trues to hide this, after which we call super.hide()
				if (owner.isShowing()) {
					owner.hide();
				} else {
					super.hide();
				}
			}
		};
		s.initOwner(owner);
		s.setAlwaysOnTop(true);

		// Modality causes issue with focus not being returned to the correct window, unfortunately, this cant stay on
		// s.initModality(APPLICATION_MODAL); // eliminates focus stealing from apps and taskbar being visible

		if (show) s.show();

		s.setX(screen.getBounds().getMinX()); // screen does not necessarily start at [0,0]
		s.setY(screen.getBounds().getMinY());
		s.setWidth(screen.getBounds().getWidth());
		s.setHeight(screen.getBounds().getHeight());
		// on multi-monitor setup with varying screen dpi, javaFX may use wrong dpi, leading to wrong Stage size,
		// s.setWidth(screen.getVisualBounds().getWidth()/Screen.getPrimary().getOutputScaleX()*screen.getOutputScaleX());
		// s.setHeight(screen.getVisualBounds().getHeight()/Screen.getPrimary().getOutputScaleY()*screen.getOutputScaleY());

		// Fullscreen window is the got to choice, but fullscreen has its own share of problems.., like focus
		// stealing and the consequent disappearance of the window (nearly impossible to bring it back).
		// s.setFullScreen(true);
		// s.setFullScreenExitHint("");
		// s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

		return s;
	}

}