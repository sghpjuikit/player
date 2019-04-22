package sp.it.pl.gui.objects.window.pane;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import sp.it.pl.gui.objects.window.Resize;
import sp.it.util.ui.fxml.ConventionFxmlLoader;
import static sp.it.pl.gui.objects.window.Resize.N;
import static sp.it.pl.gui.objects.window.Resize.NE;
import static sp.it.pl.gui.objects.window.Resize.NONE;
import static sp.it.pl.gui.objects.window.Resize.NW;
import static sp.it.pl.gui.objects.window.Resize.S;
import static sp.it.pl.gui.objects.window.Resize.SE;
import static sp.it.pl.gui.objects.window.Resize.SW;
import static sp.it.pl.gui.objects.window.Resize.W;
import static sp.it.pl.main.AppBuildersKt.resizeButton;
import static sp.it.util.reactive.UtilKt.maintain;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.initClip;
import static sp.it.util.ui.UtilKt.pseudoclass;

public class PaneWindowControls extends WindowPane {

	/** Pseudoclass active when this window is resized. Applied on root as '.window'. */
	public static final PseudoClass pcResized = pseudoclass("resized");
	/** Pseudoclass active when this window is moved. Applied on root as '.window'. */
	public static final PseudoClass pcMoved = pseudoclass("moved");
	/** Pseudoclass active when this window is focused. Applied on root as '.window'. */
	public static final PseudoClass pcFocused = pseudoclass("focused");

	@FXML public AnchorPane borders;
	@FXML public AnchorPane content;
	@FXML public HBox controls;

	public PaneWindowControls(AnchorPane owner) {
		super(owner);

		new ConventionFxmlLoader(root, this).loadNoEx();
		initClip(content);

		// maintain custom pseudoclasses for .window styleclass
		resizing.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcResized, nv!=NONE));
		moving.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcMoved, nv));
		focused.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFocused, nv));

		// disable resizing behavior completely when not resizable
		maintain(resizable, it ->
			borders.getChildren().stream()
				.filter(c -> !(c instanceof Pane))
				.forEach(b -> b.setMouseTransparent(!it))
		);

		var resizeB = resizeButton();
		resizeB.setOnMouseDragged(this::border_onDragged);
		resizeB.setOnMousePressed(this::border_onDragStart);
		resizeB.setOnMouseReleased(this::border_onDragEnd);
		borders.getChildren().add(resizeB);
		setAnchors(resizeB, null, 0.0, 0.0, null);
	}

	public void setContent(Node n) {
		content.getChildren().clear();
		content.getChildren().add(n);
		setAnchors(n, 0d);
	}

	/***************************    HEADER & BORDER    ****************************/
	@FXML public StackPane header;
	@FXML private ImageView iconI;
	@FXML private Label titleL;
	@FXML
	private HBox leftHeaderBox;
	private boolean headerVisible = true;
	private boolean headerAllowed = true;

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

	private void showHeader(boolean val) {
		header.setVisible(val);
		if (val) {
			AnchorPane.setTopAnchor(content, 25d);
			setBorderless(!val);
		} else {
			AnchorPane.setTopAnchor(content, 0d);
		}
	}

	/**
	 * Set false to permanently hide header.
	 */
	public void setHeaderAllowed(boolean val) {
		setHeaderVisible(val);
		headerAllowed = val;
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

	public boolean isBorderless() {
		return AnchorPane.getBottomAnchor(content)==0;
	}

	public void setBorderless(boolean v) {
		if (v) setAnchors(content, 0d);
		else setAnchors(content, 25d, 5d, 5d, 5d);
		borders.setVisible(!v);
	}

	@FXML
	private void border_onDragStart(MouseEvent e) {
		// start resize if allowed
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

	@FXML
	private void border_onDragEnd(MouseEvent e) {
		// end resizing if active
		if (_resizing.get()!=NONE) {
			_resizing.set(NONE);
			e.consume();
		}
	}

	@FXML
	private void border_onDragged(MouseEvent e) {
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

	@FXML
	private void consumeMouseEvent(MouseEvent e) {
		e.consume();
	}

}