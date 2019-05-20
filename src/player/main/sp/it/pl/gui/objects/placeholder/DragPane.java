package sp.it.pl.gui.objects.placeholder;

import de.jensd.fx.glyphs.GlyphIcons;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import kotlin.jvm.functions.Function1;
import sp.it.util.access.ref.SingleR;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_EXITED_TARGET;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.UtilKt.runnable;

/**
 * Placeholder node as a visual aid for mouse drag operations. Shown when drag enters drag accepting {@link Node} and
 * hidden when it exists.
 * <p/>
 * This pane shows icon and description of the action that will take place when drag is dropped and accepted and
 * highlights the drag accepting area.
 * <p/>
 * The drag areas (the nodes which install this pane) do not have to be mutually exclusive, i.e., the nodes can cover
 * each other, e.g. drag accepting node can be a child of already drag accepting pane. The highlighted area then
 * activates for the topmost drag accepting node
 * <p/>
 * The node should be drag accepting - have a drag over handler/filter. The condition under which the node accepts the
 * drag (e.g. only text) should be expressed as a {@link Predicate} and used when installing this pane. Otherwise it
 * will be shown for drag of any content and confuse user.
 * <p/>
 * See {@link #install(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, String, kotlin.jvm.functions.Function1)}
 */
public class DragPane extends Placeholder {

	private static final String ACTIVE = "DRAG_PANE";
	private static final String INSTALLED = "DRAG_PANE_INSTALLED";
	private static final String STYLECLASS = "drag-pane";
	private static final String STYLECLASS_ICON = "drag-pane-icon";
	private static final GlyphIcons DEFAULT_ICON = CLIPBOARD;
	public static final SingleR<DragPane,Data> PANE = new SingleR<>(DragPane::new,
			(p, data) -> {
				p.icon.icon(data.icon==null ? DEFAULT_ICON : data.icon);
				p.info.setText(data.info.get());
			}
	);

	public static void install(Node r, GlyphIcons icon, String info, Function1<? super DragEvent, Boolean> cond) {
		DragPane.install(r, icon, () -> info, cond);
	}

	public static void install(Node r, GlyphIcons icon, Supplier<? extends String> info, Function1<? super DragEvent, Boolean> cond) {
		install(r, icon, info, cond, e -> false, null);
	}

	/**
	 * Installs drag highlighting for specified node and drag defined by specified predicate,
	 * displaying specified icon and action description.
	 * <p/>
	 *
	 * @param node drag accepting node. The node should be the accepting object for the drag event.
	 * @param icon icon symbolizing the action that will take place when drag is dropped
	 * @param info description of the action that will take place when drag is dropped. The text is supplied when the
	 * drag enters the node. Normally, just pass in {@code () -> "text" }, but you can derive the text from a state of
	 * the node or however you wish, e.g. when the action can be different under some circumstances.
	 * @param cond predicate filtering the drag events. The highlighting will show if the drag event tests true.
	 * <p/>
	 * Must be consistent with the node's DRAG_OVER event handler which accepts the drag in order for highlighting to
	 * make sense! Check out {@link sp.it.pl.main.AppDragKt#installDrag(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, String, kotlin.jvm.functions.Function1, kotlin.jvm.functions.Function1)}, which
	 * guarantees consistency.
	 * <p/>
	 * Normally, one simple queries the dragboard of the event for type of content. Predicate returning always true will
	 * cause the drag highlighting to show regardless of the content of the drag - even if the node does not allow the
	 * content to be dropped.
	 * <p/>
	 * It is recommended to build a predicate and use it for drag over handler as well, see {@link
	 * sp.it.util.ui.drag.DragUtilKt#handlerAccepting(kotlin.jvm.functions.Function1)}. This will guarantee
	 * consistency in drag highlighting and drag accepting behavior.
	 * @param except Optionally, it is possible to forbid drag highlighting even if the condition tests true. This is
	 * useful for when node that shouldn't accept given event doesn't wish for any of its parents accept (and highlight
	 * if installed) the drag. For example node may use this parameter to refuse drag&drop from itself.
	 * <pre>
	 * This can simply be thought of as this (pseudo-code):
	 * Condition: event -> event.hasImage   // accept drag containing image file
	 * Except: event -> hasPngImage   // but do not accept png
	 * </pre>
	 * Exception condition should be a subset of condition - if condition returns false, the except should as well. It
	 * should only constrict the condition
	 * <p/>
	 * Using more strict condition (logically equivalent with using except condition) will not have the same effect,
	 * because then any parent node which can accept the event will be able to do so and also show drag highlight if it
	 * is installed. Normally that is the desired behavior here, but there are cases when it is not.
	 * <p/>
	 * Generally, this is useful to hint that the node which would normally accept the event, can not. If the condition
	 * is not met, parents may accept the event instead. But if it is and the except condition returns true, then area
	 * covering the node will refuse the drag whether parents like it or not.
	 * @param area Optionally, the highlighting can have specified size and position. Normally it mirrors the size and
	 * position of the node. This function is called repeatedly (on DRAG_OVER event, which behaves like MOUSE_MOVE ) and
	 * may be used to calculate size and position of the highlight. The result can be a portion of the node's area and
	 * even react on mouse drag moving across the node.
	 */
	public static void install(Node node, GlyphIcons icon, Supplier<? extends String> info, Function1<? super DragEvent, Boolean> cond, Function1<? super DragEvent, Boolean> except, Function1<? super DragEvent, ? extends Bounds> area) {
		Data d = new Data(info, icon, cond);
		node.getProperties().put(INSTALLED, d);
		node.addEventHandler(DragEvent.DRAG_OVER, e -> {
			if (!node.getProperties().containsKey(ACTIVE)) { // guarantees cond executes only once
				if (d.cond.invoke(e)) {
					PANE.get().hide();

					if (except==null || !except.invoke(e)) { // null is equivalent to e -> false
						node.getProperties().put(ACTIVE, ACTIVE);
						Pane p = node instanceof Pane ? (Pane) node : node.getParent()==null ? null : (Pane) node.getParent();
						Pane dp = PANE.getM(d);
						if (p!=null && !p.getChildren().contains(dp)) {
							p.getChildren().add(dp);
							Bounds b = area==null ? node.getLayoutBounds() : area.invoke(e);
							double w = b.getWidth();
							double h = b.getHeight();
							dp.setMaxSize(w, h);
							dp.setPrefSize(w, h);
							dp.setMinSize(w, h);
							dp.resizeRelocate(b.getMinX(), b.getMinY(), w, h);
							dp.toFront();
							dp.setVisible(true);
						}
					}
					e.consume();
				}
			}

			if (area!=null && node.getProperties().containsKey(ACTIVE)) {
				DragPane dp = PANE.getM(d);
				Bounds b = area.invoke(e);
				double w = b.getWidth();
				double h = b.getHeight();
				dp.setMaxSize(w, h);
				dp.setPrefSize(w, h);
				dp.setMinSize(w, h);
				dp.resizeRelocate(b.getMinX(), b.getMinY(), w, h);
				dp.toFront();
				dp.setVisible(true);
			}
		});
		node.addEventHandler(DRAG_EXITED_TARGET, e ->
			node.getProperties().remove(ACTIVE)
		);
		node.addEventHandler(DRAG_EXITED, e -> {
			PANE.get().hide();
			node.getProperties().remove(ACTIVE);
		});
		// Fixes an issue when the above handlers do not cause pane to hide
		node.hoverProperty().addListener((o, ov, nv) -> {
			if (!nv) {
				PANE.get().hide();
				node.getProperties().remove(ACTIVE);
			}
		});
	}

	public static class Data {
		private final Supplier<? extends String> info;
		private final GlyphIcons icon;
		private final Function1<? super DragEvent, Boolean> cond;

		public Data(Supplier<? extends String> info, GlyphIcons icon) {
			this.info = info;
			this.icon = icon;
			this.cond = IS;
		}

		public Data(Supplier<? extends String> info, GlyphIcons icon, Function1<? super DragEvent, Boolean> cond) {
			this.info = info;
			this.icon = icon;
			this.cond = cond;
		}
	}

	private DragPane() {
		super(DEFAULT_ICON, "", runnable(() -> {}));
		icon.styleclass(STYLECLASS_ICON);
		getStyleClass().add(STYLECLASS);
		setMouseTransparent(true);   // must not interfere with events
		setManaged(false);           // must not interfere with layout
	}
}