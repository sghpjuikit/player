package sp.it.pl.ui.objects.window.pane;

import javafx.css.PseudoClass;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import sp.it.pl.ui.objects.window.Resize;
import sp.it.util.math.P;
import static sp.it.pl.ui.objects.window.Resize.N;
import static sp.it.pl.ui.objects.window.Resize.NE;
import static sp.it.pl.ui.objects.window.Resize.NONE;
import static sp.it.pl.ui.objects.window.Resize.NW;
import static sp.it.pl.ui.objects.window.Resize.S;
import static sp.it.pl.ui.objects.window.Resize.SE;
import static sp.it.pl.ui.objects.window.Resize.SW;
import static sp.it.pl.ui.objects.window.Resize.W;
import static sp.it.pl.ui.objects.window.pane.PaneWindowControlsUtilKt.buildWindowLayout;
import static sp.it.pl.ui.objects.window.pane.PaneWindowControlsUtilKt.lookupId;
import static sp.it.pl.main.AppBuildersKt.resizeIcon;
import static sp.it.util.collections.UtilKt.setToOne;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.MouseDragKt.initMouseDrag;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.pseudoclass;

public class PaneWindowControls extends WindowPane {

	/** Pseudoclass active when this window is resized. Applied on root as '.window'. */
	public static final PseudoClass pcResized = pseudoclass("resized");
	/** Pseudoclass active when this window is moved. Applied on root as '.window'. */
	public static final PseudoClass pcMoved = pseudoclass("moved");
	/** Pseudoclass active when this window is focused. Applied on root as '.window'. */
	public static final PseudoClass pcFocused = pseudoclass("focused");

	private final Pane subroot = buildWindowLayout(consumer(this::borderDragStart), consumer(this::borderDragged), consumer(this::borderDragEnd));
	public final AnchorPane borders = lookupId(subroot, "borders", AnchorPane.class);
	public final AnchorPane content = lookupId(subroot, "content", AnchorPane.class);
	public final StackPane header = lookupId(subroot, "headerContainer", StackPane.class);
	public final Label titleL = lookupId(header, "titleL", Label.class);
	public final VBox contentRoot = lookupId(subroot, "contentRoot", VBox.class);
	public final HBox leftHeaderBox = lookupId(header, "leftHeaderBox", HBox.class);
	public final HBox rightHeaderBox = lookupId(header, "rightHeaderBox", HBox.class);
	private boolean headerVisible = true;
	private boolean headerAllowed = true;

	public PaneWindowControls(AnchorPane owner) {
		super(owner);

		root.getChildren().add(subroot);

		// maintain custom pseudoclasses for .window styleclass
		resizing.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcResized, nv!=NONE));
		moving.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcMoved, nv));
		focused.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFocused, nv));

		// disable resizing behavior completely when not resizable
		syncC(resizable, it ->
			borders.getChildren().stream()
				.filter(c -> !(c instanceof Pane))
				.forEach(b -> b.setMouseTransparent(!it))
		);

		var resizeB = resizeIcon();
		initMouseDrag(
			resizeB,
			new P(),
			consumer(drag -> drag.setData(new P(w.getValue(), h.getValue()))),
			consumer(drag -> {
				if (resizable.get()) {
					w.setValue(drag.getData().getX() + drag.getDiff().getX());
					w.setValue(drag.getData().getY() + drag.getDiff().getY());
				}
			})
         );
		resizeB.setOnMousePressed(this::button_onDragStart);
		resizeB.setOnMouseDragged(this::borderDragged);
		resizeB.setOnMouseReleased(this::borderDragEnd);
		borders.getChildren().add(resizeB);
		setAnchors(resizeB, null, 0.0, 0.0, null);
	}

	public void setContent(Node n) {
		setToOne(content.getChildren(), n);
		setAnchors(n, 0d);
	}

	/**
	 * Sets visibility of the window header, including its buttons for control
	 * of the window (close, etc).
	 */
	public void setHeaderVisible(boolean val) {
		// prevent pointless operation
		if (!headerAllowed) return;
		headerVisible = val;
		showHeader(val);
	}

	public boolean isHeaderVisible() {
		return headerVisible;
	}

	private void showHeader(boolean v) {
		if (v && !contentRoot.getChildren().contains(header)) contentRoot.getChildren().add(0, header);
		if (!v && contentRoot.getChildren().contains(header)) contentRoot.getChildren().remove(header);
	}

	/**
	 * Set false to permanently hide header.
	 */
	public void setHeaderAllowed(boolean val) {
		headerAllowed = val;
		setHeaderVisible(val);
	}

	/**
	 * Set title for this window shown in the header.
	 */
	public void setTitle(String text) {
		titleL.setText(text);
	}

	/**
	 * Set title alignment.
	 */
	public void setTitlePosition(Pos align) {
		BorderPane.setAlignment(titleL, align);
	}

	private void button_onDragStart(MouseEvent e) {
		if (resizable.get()) {
			_resizing.set(SE);
			e.consume();
		}
	}

	private void borderDragStart(MouseEvent e) {
		if (resizable.get()) {
			Point2D b = root.getParent().screenToLocal(new Point2D(e.getScreenX(), e.getScreenY()));
			double X = b.getX() - x.get();
			double Y = b.getY() - y.get();
			double WW = w.get();
			double WH = h.get();
			double L = 20; // corner threshold

			Resize r = NONE;
			if ((X>WW - L) && (Y>WH - L)) r = SE;
			else if ((X<L) && (Y>WH - L)) r = SW;
			else if ((X<L) && (Y<L)) r = NW;
			else if ((X>WW - L) && (Y<L)) r = NE;
			else if ((X>WW - L)) r = Resize.E;
			else if ((Y>WH - L)) r = S;
			else if ((X<L)) r = W;
			else if ((Y<L)) r = N;

			if (r!=NONE) {
				_resizing.set(r);
				e.consume();
			}
		}
	}

	private void borderDragged(MouseEvent e) {
		if (_resizing.get()!=NONE) {
			Point2D b = root.getParent().screenToLocal(new Point2D(e.getScreenX(), e.getScreenY()));
			double X = x.get();
			double Y = y.get();
			Resize r = _resizing.get();
			if (r==SE) {
				w.set(b.getX() - X);
				h.set(b.getY() - Y);
			} else if (r==S) {
				h.set(b.getY() - Y);
			} else if (r==Resize.E) {
				w.set(b.getX() - X);
			} else if (r==SW) {
				w.set(X + w.get() - b.getX());
				h.set(b.getY() - Y);
				x.set(b.getX());
			} else if (r==W) {
				w.set(X + w.get() - b.getX());
				x.set(b.getX());
			} else if (r==NW) {
				w.set(X + w.get() - b.getX());
				h.set(Y + h.get() - b.getY());
				x.set(b.getX());
				y.set(b.getY());
			} else if (r==N) {
				h.set(Y + h.get() - b.getY());
				y.set(b.getY());
			} else if (r==NE) {
				w.set(b.getX() - X);
				h.set(Y + h.get() - b.getY());
				y.set(b.getY());
			}
			e.consume();
		}
	}

	private void borderDragEnd(MouseEvent e) {
		if (_resizing.get()!=NONE) {
			_resizing.set(NONE);
			startX = x.get() - e.getSceneX();
			startY = y.get() - e.getSceneY();
			e.consume();
		}
	}

}