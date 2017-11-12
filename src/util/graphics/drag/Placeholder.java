package util.graphics.drag;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.icon.Icon;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.reactfx.Subscription;
import static util.dev.Util.noØ;
import static util.graphics.Util.layHeaderBottom;
import static util.graphics.UtilKt.removeFromParent;
import static util.reactive.Util.maintain;

/**
 * Placeholder pane. Can invoke action and display its icon and name.<br/>
 * Useful mostly instead of content when there is none, e.g., "click to add items". Or to highlight on hover/mouse
 * over to signal possible interaction.
 */
public class Placeholder extends StackPane {

	private static final String STYLECLASS = "placeholder-pane";
	private static final String STYLECLASS_ICON = "placeholder-pane-icon";

	public final Icon icon = new Icon().styleclass(STYLECLASS_ICON);
	public final Label desc = new Label();
	private Subscription s;
//	private Pane parent;

	/**
	 * @param actionIcon non null icon
	 * @param actionName non null action name
	 * @param action non null action
	 */
	public Placeholder(GlyphIcons actionIcon, String actionName, Runnable action) {
		icon.icon(noØ(actionIcon));
		desc.setText(noØ(actionName));
		icon.onClick(noØ(action));
		getStyleClass().add(STYLECLASS);
		setOnMouseClicked(e -> { action.run(); e.consume(); });
//		setOnKeyPressed(e -> { if (ENTER) action.run(); e.consume(); });   // TODO: implement sth along these lines
		getChildren().add(layHeaderBottom(8, Pos.CENTER, icon, desc));
		setVisible(false);
	}

	/**
	 * Shows this placeholder for given node or does nothing if node null.
	 * Use {@link #visibleProperty()} to observe visibility.
	 *
	 * @param n nullable node
	 */
	public void showFor(Node n) {
		Pane p = n instanceof Pane ? (Pane) n : n==null || n.getParent()==null ? null : (Pane) n.getParent();
		if (p!=null && !p.getChildren().contains(this)) {
//            parent = p;
//            parent.getChildren().forEach(c -> c.setOpacity(0.2));
			setVisible(true);
			p.getChildren().add(this);
			s = maintain(n.layoutBoundsProperty(), b -> {
				double w = b.getWidth();
				double h = b.getHeight();
				setMaxSize(w, h);
				setPrefSize(w, h);
				setMinSize(w, h);
				resizeRelocate(b.getMinX(), b.getMinY(), w, h);
			});
			toFront();
		}
	}

	/**
	 * Use {@link #visibleProperty()} to observe visibility.
	 */
	public void hide() {
//        if (parent!=null) parent.getChildren().forEach(c -> c.setOpacity(1));
		if (s!=null) s.unsubscribe();
		removeFromParent(this);
		setVisible(false);
	}

	/**
	 * Use {@link #visibleProperty()} to observe visibility.
	 */
	public void show(Node n, boolean visible) {
		if (visible) showFor(n);
		else hide();
	}
}