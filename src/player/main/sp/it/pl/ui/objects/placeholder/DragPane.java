package sp.it.pl.ui.objects.placeholder;

import de.jensd.fx.glyphs.GlyphIcons;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import kotlin.jvm.functions.Function1;
import sp.it.util.access.ref.SingleR;
import sp.it.util.math.P;
import sp.it.util.reactive.Disposer;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static javafx.scene.input.DragEvent.DRAG_DONE;
import static javafx.scene.input.DragEvent.DRAG_DROPPED;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.EventsKt.onEventDown;
import static sp.it.util.reactive.UnsubscribableKt.on;
import static sp.it.util.reactive.UtilKt.attach;

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
 * The node should be accepting drag - have a drag over handler/filter. The condition under which the node accepts the
 * drag (e.g. only text) should be expressed as a {@link Predicate} and used when installing this pane. Otherwise, it
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
			p.icon.icon(data.icon==null, DEFAULT_ICON, data.icon);
			p.info.setText(data.info.get());
			p.whileActiveOfSibling = data.whileActive;
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
	 * even react on mouse drag moving across the node. The resulting bounds will be shifted by the parent's (left, top) padding.
	 */
	public static void install(Node node, GlyphIcons icon, Supplier<? extends String> info, Function1<? super DragEvent, Boolean> cond, Function1<? super DragEvent, Boolean> except, Function1<? super DragEvent, ? extends Bounds> area) {
		var d = new Data(info, icon, cond);
		node.getProperties().put(INSTALLED, d);
		node.addEventHandler(DRAG_OVER, e -> {
			if (!node.getProperties().containsKey(ACTIVE)) { // guarantees cond executes only once
				if (d.cond.invoke(e)) {
					PANE.ifSet(it -> it.hide());
					PANE.ifSet(it -> it.whileActiveOfSibling.invoke());

					if (except==null || !except.invoke(e)) { // null is equivalent to e -> false
						installUninstall(node, d.whileActive);
						node.getProperties().put(ACTIVE, ACTIVE);
						var p = node.getParent()==null ? null : (Pane) node.getParent();
						var dp = PANE.getM(d);
						if (p!=null && !p.getChildren().contains(dp)) {
							dp.animateShow(node);
							p.getChildren().add(dp);
							var b = area==null ? node.getBoundsInParent() : area.invoke(e);
							var padding = area==null ? new P(0.0, 0.0) : new P(p.snappedLeftInset(), p.snappedTopInset());
							var w = b.getWidth();
							var h = b.getHeight();
							dp.setMaxSize(w, h);
							dp.setPrefSize(w, h);
							dp.setMinSize(w, h);
							dp.resizeRelocate(padding.getX() + b.getMinX(), padding.getY() + b.getMinY(), w, h);
							dp.toFront();
							dp.setVisible(true);
						}
					}
					e.consume();
				}
			}

			if (area!=null && node.getProperties().containsKey(ACTIVE)) {
				var dp = PANE.getM(d);
				var p = (Pane) dp.getParent();
				var padding = area==null ? new P(0.0, 0.0) : new P(p.snappedLeftInset(), p.snappedTopInset());
				var b = area.invoke(e);
				var w = b.getWidth();
				var h = b.getHeight();
				dp.setMaxSize(w, h);
				dp.setPrefSize(w, h);
				dp.setMinSize(w, h);
				dp.resizeRelocate(padding.getX() + b.getMinX(), padding.getY() + b.getMinY(), w, h);
				dp.toFront();
				dp.setVisible(true);
			}
		});
	}

	private static void installUninstall(Node node, Disposer whileActive) {
		whileActive.plusAssign(runnable(() -> {
			if (node.getProperties().containsKey(ACTIVE)) { // guarantees cond executes only once
				node.getProperties().remove(ACTIVE);
				PANE.get().hide();
				PANE.get().animateHide();
			}
		}));
		on(onEventDown(node, DRAG_DONE, consumer(e -> whileActive.invoke())), whileActive);
		on(onEventDown(node, DRAG_DROPPED, consumer(e -> whileActive.invoke())), whileActive);
		on(onEventDown(node, DRAG_EXITED, consumer(e -> whileActive.invoke())), whileActive);
		on(attach(node.hoverProperty(), consumer(nv -> { if (!nv) whileActive.invoke(); })), whileActive); // drag handlers can miss event
	}

	public static class Data {
		private final Supplier<? extends String> info;
		private final GlyphIcons icon;
		private final Function1<? super DragEvent, Boolean> cond;
		private final Disposer whileActive = new Disposer();

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

	private Disposer whileActiveOfSibling = new Disposer();

	private DragPane() {
		super(DEFAULT_ICON, "", runnable(() -> {}));
		icon.styleclass(STYLECLASS_ICON);
		getStyleClass().add(STYLECLASS);
		setMouseTransparent(true);   // must not interfere with events
		setManaged(false);           // must not interfere with layout
	}
}