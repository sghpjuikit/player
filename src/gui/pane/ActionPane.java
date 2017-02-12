package gui.pane;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.Gui;
import gui.objects.Text;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.spinner.Spinner;
import gui.objects.table.FilteredTable;
import gui.objects.table.ImprovedTable.PojoV;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.animation.Interpolator;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import main.App;
import util.SwitchException;
import util.access.V;
import util.action.Action;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.Async;
import util.async.future.Fut;
import util.collections.map.ClassListMap;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.functional.Functors.Ƒ1;
import util.functional.Try;
import util.type.ClassName;
import util.type.InstanceInfo;
import util.type.InstanceName;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.*;
import static gui.objects.icon.Icon.createInfoIcon;
import static gui.objects.table.FieldedTable.defaultCell;
import static gui.pane.ActionPane.GroupApply.*;
import static java.util.stream.Collectors.joining;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.*;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static util.async.Async.*;
import static util.async.future.Fut.fut;
import static util.async.future.Fut.futAfter;
import static util.dev.Util.throwIfNotFxThread;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;
import static util.type.Util.build;

/**
 * Action chooser pane. Displays icons representing certain actions.
 *
 * @author Martin Polakovic
 */
@IsConfigurable("Gui.Action Chooser")
public class ActionPane extends OverlayPane<Object> implements Configurable<Object> {

	private static final String ROOT_STYLECLASS = "action-pane";
	private static final String ICON_STYLECLASS = "action-pane-action-icon";
	private static final String COD_TITLE = "Close when action ends";
	private static final String COD_INFO = "Closes the chooser when action finishes running.";

	@IsConfig(name = COD_TITLE, info = COD_INFO)
	public final V<Boolean> closeOnDone = new V<>(false);
	private final ClassName className;
	private final InstanceName instanceName;
	private final InstanceInfo instanceInfo;

	private boolean showIcons = true;
	private Node insteadIcons = null;

	public ActionPane(ClassName className, InstanceName instanceName, InstanceInfo instanceInfo) {
		this.className = className;
		this.instanceName = instanceName;
		this.instanceInfo = instanceInfo;

		getStyleClass().add(ROOT_STYLECLASS);

		// icons and descriptions
		ScrollPane descriptionFullPane = layScrollVTextCenter(descFull);
		StackPane infoPane = layStack(dataInfo,TOP_LEFT);
		VBox descPane = layVertically(8, BOTTOM_CENTER, descTitle,descriptionFullPane);
		HBox iconPaneSimple = layHorizontally(15,CENTER);
		icons = iconPaneSimple.getChildren();

		// content for icons and descriptions
		StackPane iconBox = layStack(iconPaneComplex,CENTER, iconPaneSimple,CENTER);
		StackPane iconPane = layStack(infoPane, TOP_LEFT, iconBox,CENTER, descPane,BOTTOM_CENTER);
		// Minimal and maximal height of the 3 layout components. The heights should add
		// up to full length (including the spacing of course). Sounds familiar? No, could not use
		// VBox or StackPane as we need the icons to be always in the center.
		// Basically we want the individual components to resize individually, but still respect
		// each other's presence (so to not cover each other).
		// We do not want any component to be very small (hence the min height) but the text should not
		// be too spacey - the icons are important - hence the max size. The icon's max size is simply
		// totalHeight - height_of_others - 2*spacing.
		infoPane.setMinHeight(100);
		infoPane.maxHeightProperty().bind(min(iconPane.heightProperty().multiply(0.3), 400));
		descPane.setMinHeight(100);
		descPane.maxHeightProperty().bind(min(iconPane.heightProperty().multiply(0.3), 400));
		iconBox.maxHeightProperty().bind(iconPane.heightProperty().multiply(0.4).subtract(2*25));

		// content
		HBox content = layHorizontally(0, CENTER, tablePane,iconPane); // table is an optional left complement to iconPane
			 content.setPadding(new Insets(0,50,0,50)); // top & bottom padding set differently, below
		tableContentGap = content.spacingProperty();
		// iconPane and table complement each other horizontally, though iconPane is more
		// important and should be wider & closer to center
		iconPane.minWidthProperty().bind(content.widthProperty().multiply(0.6));

		Icon resizeB = new Icon(RESIZE_BOTTOM_RIGHT, 20);
			 resizeB.setCursor(Cursor.SE_RESIZE);
		Pane controlsMirror = new Pane();
			 controlsMirror.prefHeightProperty().bind(controls.heightProperty()); // see below
		setContent(
			layStack(
				layHeaderTopBottom(20, CENTER_RIGHT,
					controls, // tiny header
					content, // the above and below also serve as top/bottom padding
					controlsMirror // fills bottom so the content resizes vertically to center
				), Pos.CENTER
//				resizeB, Pos.BOTTOM_RIGHT
			)
		);
		getContent().setMinSize(300,200);

		// put some padding of the content from edge
		// note: The content is user-resizable now, sp we do not use bind, but set maxSize on show
		// getContent().maxWidthProperty().bind(widthProperty().multiply(CONTENT_SIZE_SCALE));
		// getContent().maxHeightProperty().bind(heightProperty().multiply(CONTENT_SIZE_SCALE);

		descPane.setMouseTransparent(true); // just in case
		infoPane.setMouseTransparent(true); // same here
		descTitle.setTextAlignment(TextAlignment.CENTER);
		descFull.setTextAlignment(TextAlignment.JUSTIFY);
		descriptionFullPane.maxWidthProperty().bind(min(400, iconPane.widthProperty()));

		makeResizableByUser();
	}

/* ---------- PRE-CONFIGURED ACTIONS --------------------------------------------------------------------------------- */

	public final ClassListMap<ActionData<?,?>> actions = new ClassListMap<>(null);

	public final <T> void register(Class<T> c, ActionData<T,?> action) {
		actions.accumulate(c, action);
	}

	@SafeVarargs
	public final <T> void register(Class<T> c, ActionData<T,?>... action) {
		actions.accumulate(c, listRO(action));
	}

/* ---------- CONTROLS ---------------------------------------------------------------------------------------------- */

	private final Icon helpI = createInfoIcon(
		"Action chooser"
	  + "\n"
	  + "\nChoose an action. It may use some input data. Data not immediately ready will "
	  + "display progress indicator."
	);
	private final Icon hideI = new CheckIcon(closeOnDone)
									.tooltip(COD_TITLE+"\n\n"+COD_INFO)
									.icons(CLOSE_CIRCLE_OUTLINE, CHECKBOX_BLANK_CIRCLE_OUTLINE);
	private final ProgressIndicator dataProgress = build(new Spinner(1), s -> maintain(s.progressProperty(), p -> p.doubleValue()<1, s.visibleProperty()));
	public final ProgressIndicator actionProgress = build(new Spinner(1), s -> maintain(s.progressProperty(), p -> p.doubleValue()<1, s.visibleProperty()));
	private final HBox controls = layHorizontally(5,CENTER_RIGHT, actionProgress, dataProgress,hideI,helpI);

/* ---------- DATA -------------------------------------------------------------------------------------------------- */

	private boolean use_registered_actions = true;
	private Object data;
	private List<ActionData> actionsIcons;
	private final List<ActionData> actionsData = new ArrayList<>();
	private static final double CONTENT_SIZE_SCALE = 0.65;

	protected void show() {
		throwIfNotFxThread();

		setData(data);

		// Bug fix. We need to initialize the layout before it is visible or it may visually
		// jump around as it does on its own.
		// Cause: unknown, probably the many bindings we use...
		// TODO: fix this
		getContent().layout();
		getContent().requestLayout();
		getContent().autosize();

		super.show();

		// Reset content size to default (overrides previous user-defined size)
		// Note, sometimes we need to delay this action, hence the runLater
		runLater(() -> {
			if (getParent()!=null) { // TODO: running this in a loop may be necessary
				Bounds b = getParent().getLayoutBounds();
				getContent().setPrefSize(b.getWidth() * CONTENT_SIZE_SCALE, b.getHeight() * CONTENT_SIZE_SCALE);
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void show(Object value) {
		throwIfNotFxThread();

		value = collectionUnwrap(value);
		Class c = value==null ? Void.class : value.getClass();
		show(c, value);
	}

	public final <T> void show(Class<T> type, T value) {
		show(type, value, false);
	}

	@SuppressWarnings("unused")
	public final <T> void show(Class<T> type, T value, boolean exclusive, ActionData<?,?>... actions) {
		data = value;
		actionsIcons = list(actions);
		use_registered_actions = !exclusive;
		show();
	}

	@SafeVarargs
	@SuppressWarnings("unused")
	public final <T> void show(Class<T> type, Fut<T> value, boolean exclusive, SlowAction<T,?>... actions) {
		data = value;
		actionsIcons = list(actions);
		use_registered_actions = !exclusive;
		show();
	}

	private void doneHide() {
		if (closeOnDone.get()) {
			showIcons = true;
			insteadIcons = null;
			hide();
		}
	}

	@SuppressWarnings("unchecked")
	private void doneHide(ActionData action) {
		if (action.isComplex) {
			ComplexActionData complexAction = action.complexData;
			showIcons = false;
			insteadIcons = (Node) complexAction.gui.get();
			show(complexAction.input.apply(action.prepInput(getData())));
		}

		if (!action.preventClosing)
			doneHide();
	}

/* ---------- GRAPHICS ---------------------------------------------------------------------------------------------- */

	private final Label dataInfo = new Label();
	private final Label descTitle = new Label();
	private final Text descFull = new Text();
	private final ObservableList<Node> icons;
	private final DoubleProperty tableContentGap;
	private final StackPane tablePane = new StackPane();
	private FilteredTable<?> table;
	private final StackPane iconPaneComplex = new StackPane();

/* ---------- HELPER ------------------------------------------------------------------------------------------------ */

	/**
	 * This is an advanced API. Use with caution.
	 * <p/>
	 * Returns input data or its subset if user selection is active.
	 *
	 * @return user selection of the data available
	 */
	public Object getData() {
		throwIfNotFxThread();

		Object d = futureUnwrapOrThrow(data);
		if (d instanceof Collection) {
			if (table!=null) {
				if (table.getSelectionModel().isEmpty()) return table.getItems();
				else return table.getSelectedItemsCopy();
			} else {
				return d;
			}
		} else
			return d;
	}

	@SuppressWarnings("unchecked")
	private void setData(Object d) {
		throwIfNotFxThread();

		// clear content
		setActionInfo(null);
		icons.clear();

		// set content
		data = collectionUnwrap(d);
		boolean isDataReady = !(data instanceof Fut && !((Fut)data).isDone());
		if (isDataReady) {
			data = collectionUnwrap(futureUnwrapOrThrow(data));
			setDataInfo(data, true);
			showIcons(data);
		} else {
			setDataInfo(null, false);
			// obtain data & invoke again
			data = ((Fut)data)
					.use(this::setData,FX)
					.showProgress(dataProgress);
		}
	}

	private void setActionInfo(ActionData<?,?> a) {
		descTitle.setText(a==null ? "" : a.name);
		descFull.setText(a==null ? "" : a.description);
	}

	@SuppressWarnings("unchecked")
	private void setDataInfo(Object data, boolean computed) {
		dataInfo.setText(computeDataInfo(data, computed));
		tablePane.getChildren().clear();
		double gap = 0;
		if (data instanceof Collection && !((Collection)data).isEmpty()) {
			Collection<Object> items = (Collection) data;
			Class itemType = getCollectionType(items);
			if (App.APP.classFields.get(itemType) != null) {	// TODO: add support for any item by using generic ToString objectField and column
				FilteredTable<Object> t = new FilteredTable<>(itemType, null);
				t.setFixedCellSize(Gui.font.getValue().getSize() + 5);
				t.getSelectionModel().setSelectionMode(MULTIPLE);
				t.setColumnFactory(f -> {
					TableColumn<?,?> c = new TableColumn<>(f.toString());
					c.setCellValueFactory(cf -> cf.getValue()== null ? null : new PojoV(f.getOf(cf.getValue())));
					c.setCellFactory(col -> defaultCell(f));
					c.setResizable(true);
					return (TableColumn)c;
				});
				t.setColumnState(t.getDefaultColumnInfo());
				tablePane.getChildren().setAll(t.getRoot());
				gap = 70;
				table = t;
				t.setItemsRaw(items);
				t.getSelectedItems().addListener((Change<?> c) -> {
					if (insteadIcons==null) {
						dataInfo.setText(computeDataInfo(collectionUnwrap(t.getSelectedOrAllItemsCopy()), true));
					}
				});
			}
		}
		tableContentGap.set(gap);
	}

	private String computeDataInfo(Object data, boolean computed) {
		Class<?> type = data==null ? Void.class : data.getClass();
		Object d = computed ? data instanceof Fut ? ((Fut)data).getDone() : data : null;

		String dName = !computed ? "n/a" : instanceName.get(d);
		String dKind = !computed ? "n/a" : className.get(type);
		String dInfo = !computed ? "" : stream(instanceInfo.get(d))
											.mapKeyValue((key,val) -> key + ": " + val)
											.sorted().collect(joining("\n"));
		return "Data: " + dName + "\n" +
			   "Type: " + dKind +
			   (dInfo.isEmpty() ? "" : "\n" + dInfo);
	}

	@SuppressWarnings("unchecked")
	private void showIcons(Object d) {
		Class<?> dataType = getUnwrappedType(d);
		// get suitable actions
		actionsData.clear();
		actionsData.addAll(actionsIcons);
		if (use_registered_actions) actionsData.addAll(actions.getElementsOfSuper(dataType));
		actionsData.removeIf(a -> {
			if (a.groupApply==FOR_ALL) {
				return a.condition.test(collectionWrap(d));
			}
			if (a.groupApply==FOR_EACH) {
				List ds = list(d instanceof Collection ? (Collection)d : listRO(d));
				return ds.stream().noneMatch(a.condition);
			}
			if (a.groupApply==NONE) {
				Object o = collectionUnwrap(d);
				return o instanceof Collection || !a.condition.test(o);
			}
			throw new RuntimeException("Illegal switch case");
		});

		if (!showIcons) {
			showCustomActionUi();
			insteadIcons = null;
			showIcons = true;
			return;
		} else {
			hideCustomActionUi();
		}

		stream(actionsData)
			.sorted(by(a -> a.name))
			.map(action -> {
				Icon i = new Icon<>()
					  .icon(action.icon)
					  .styleclass(ICON_STYLECLASS)
					  .onClick(e -> runAction(action, getData()));

					 // Description is shown when mouse hovers
					 i.addEventHandler(MOUSE_ENTERED, e -> setActionInfo(action));
					 i.addEventHandler(MOUSE_EXITED, e -> setActionInfo(null));

					 // Long descriptions require scrollbar, but because mouse hovers on icon, scrolling
					 // is not possible. Hence we detect scrolling above mouse and pass it to the
					 // scrollbar. A bit unintuitive, but works like a charm and description remains
					 // fully readable.
					 i.addEventHandler(ScrollEvent.ANY, e -> {
						 descFull.getParent().getParent().fireEvent(e);
						 e.consume();
					 });
				return i.withText(action.name);
			})
			.toListAndThen(icons::setAll);

		// Animate - pop icons in parallel, but with increasing delay
		// We do not want the total animation length be dependent on number of icons (by using
		// absolute icon delay), rather we calculate the delay so total length remains the same.
		Duration total = seconds(1);
		double delayAbs = total.divide(icons.size()).toMillis(); // use for consistent total length
		double delayRel = 200; // use for consistent frequency
		double delay = delayAbs;
		Interpolator intpl = new ElasticInterpolator();
		Anim.par(icons, (i,icon) -> new Anim(at -> setScaleXY(icon,at*at)).dur(500).intpl(intpl).delay(350+i*delay))
			.play();
	}

	private void runAction(ActionData<?,?> action, Object data) {
		if (!action.isLong) {
			action.accept(data);
			doneHide(action);
		} else {
			futAfter(fut(data))
				.then(() -> actionProgress.setProgress(-1),FX)
				.use(action) // run action and obtain output
				// 1) the actions may invoke some action on FX thread, so we give it some
				// by waiting a bit
				// 2) very short actions 'pretend' to run for a while
				.then(sleeping(millis(150)))
				.then(() -> actionProgress.setProgress(1),FX)
				.then(() -> doneHide(action),FX);
		}
	}

	private void hideCustomActionUi() {
		iconPaneComplex.getParent().getChildrenUnmodifiable().forEach(n -> n.setVisible(true));
		iconPaneComplex.getChildren().clear();
	}
	private void showCustomActionUi() {
		iconPaneComplex.getParent().getChildrenUnmodifiable().forEach(n -> n.setVisible(false));
		iconPaneComplex.setVisible(true);
		iconPaneComplex.getChildren().setAll(insteadIcons);
	}

	private static Class<?> getUnwrappedType(Object d) {
		return d==null
				? Void.class
				: d instanceof Collection
					? getCollectionType((Collection)d)
					: d.getClass();
	}

	private static Class<?> getCollectionType(Collection<?> c) {
		// TODO: improve collection element type recognition
		return stream(c).nonNull().findFirst().map(o -> (Class)o.getClass()).orElse(Void.class);
	}

	public static Collection<?> collectionWrap(Object o) {
		return o instanceof Collection ? (Collection)o : listRO(o);
	}

	private static Object collectionUnwrap(Object o) {
		if (o instanceof Collection) {
			Collection<?> c = (Collection)o;
			if (c.isEmpty()) return null;
			if (c.size()==1) return c.stream().findAny().orElse(null);
		}
		return o;
	}

	private static Object futureUnwrap(Object o) {
		return o instanceof Fut && ((Fut)o).isDone() ? ((Fut)o).getDone() : o;
	}

	private static Object futureUnwrapOrThrow(Object o) {
		if (o instanceof Fut && !((Fut)o).isDone()) throw new IllegalStateException("Future not done yet");
		return o instanceof Fut ? ((Fut)o).getDone() : o;
	}

	public <I> ConvertingConsumer<? super I> converting(Ƒ1<? super I,Try<?,?>> converter) {
		return d -> converter.apply(d).ifOk(result -> Async.runFX(() -> ActionPane.this.show(result)));
	}

	private interface ConvertingConsumer<T> extends Consumer<T> {}
	public static class ComplexActionData<R,T> {
		public final Supplier<Node> gui;
		public final Ƒ1<? super R, ?> input;

		public ComplexActionData(Supplier<Node> gui, Ƒ1<? super R, ?> input) {
			this.gui = gui;
			this.input = input;
		}
	}
	/** Action. */
	public static abstract class ActionData<C,T> implements Consumer<Object> {
		public final String name;
		public final String description;
		public final GlyphIcons icon;
		public final Predicate<? super T> condition;
		public final GroupApply groupApply;
		public final boolean isLong;
		private final Consumer<? super T> action;

		private boolean isComplex = false;
		private boolean preventClosing = false;
		private ComplexActionData<?,?> complexData = null;

		private ActionData(String name, String description, GlyphIcons icon, GroupApply group, Predicate<? super T> constriction, boolean isLong, Consumer<? super T> action) {
			this.name = name;
			this.description = description;
			this.icon = icon;
			this.condition = constriction;
			this.groupApply = group;
			this.isLong = isLong;
			this.action = action;
			if (action instanceof ConvertingConsumer) preventClosing = true;
		}

		public ActionData<C,T> preventClosing(ComplexActionData<T,?> action) {
			isComplex = true;
			complexData = action;
			preventClosing = true;
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void accept(Object data) {
			boolean isCollection = data instanceof Collection;
			if (groupApply==FOR_ALL) {
				action.accept((T) collectionWrap(data));
			} else
			if (groupApply==FOR_EACH) {
				if (isCollection) {
					((Collection<T>) data).forEach(action::accept);
				} else {
					action.accept((T)data);
				}
			} else
			if (groupApply==NONE) {
				if (isCollection) throw new RuntimeException("Action can not use collection");
				action.accept((T)data);
			} else {
				throw new SwitchException(groupApply);
			}
		}

		@SuppressWarnings("unchecked")
		public T prepInput(Object data) {
			boolean isCollection = data instanceof Collection;
			if (groupApply==FOR_ALL) {
				return (T) collectionWrap(data);
			} else
			if (groupApply==FOR_EACH) {
				throw new AssertionError("not a good idea...");
			} else
			if (groupApply==NONE) {
				if (isCollection) throw new RuntimeException("Action can not use collection");
				return (T) data;
			} else {
				throw new SwitchException(groupApply);
			}
		}
	}

	/** Action that executes synchronously - simply consumes the input. */
	private static class FastActionBase<C,T> extends ActionData<C,T> {

		private FastActionBase(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Consumer<? super T> act) {
			super(name, description, icon, groupApply, constriction, false, act);
		}

	}
	/** FastAction that consumes simple input - its type is the same as type of the action. */
	public static class FastAction<T> extends FastActionBase<T,T> {

		private FastAction(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Consumer<? super T> act) {
			super(name, description, icon, groupApply, constriction, act);
		}

		public FastAction(String name, String description, GlyphIcons icon, Consumer<? super T> act) {
			this(name, description, icon, NONE, IS, act);
		}

		public FastAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction, Consumer<? super T> act) {
			this(name, description, icon, NONE, constriction, act);
		}

		public FastAction(GlyphIcons icon, Action action) {
			this(action.getName(),
				  action.getInfo() + (action.hasKeysAssigned() ? "\n\nShortcut keys: " + action.getKeys() : ""),
				  icon, NONE, IS, ignored -> action.run());
		}

	}
	/** FastAction that consumes collection input - its input type is collection of its type. */
	public static class FastColAction<T> extends FastActionBase<T,Collection<T>> {

		public FastColAction(String name, String description, GlyphIcons icon, Consumer<? super Collection<T>> act) {
			super(name, description, icon, FOR_ALL, ISNT, act);
		}

		public FastColAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction,  Consumer<? super Collection<T>> act) {
			super(name, description, icon, FOR_ALL, c -> c.stream().noneMatch(constriction), act);
		}

	}

	/** Action that executes asynchronously - receives a future, processes the data and returns it. */
	private static class SlowActionBase<C,T,R> extends ActionData<C,T> {

		public SlowActionBase(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Consumer<? super T> act) {
			super(name, description, icon, groupApply, constriction, true, act);
		}

	}
	/** SlowAction that processes simple input - its type is the same as type of the action. */
	public static class SlowAction<T,R> extends SlowActionBase<T,T,R> {

		public SlowAction(String name, String description, GlyphIcons icon, Consumer<? super T> act) {
			super(name, description, icon, NONE, IS, act);
		}

		public SlowAction(String name, String description, GlyphIcons icon, GroupApply groupApply, Consumer<? super T> act) {
			super(name, description, icon, groupApply, IS, act);
		}

	}
	/** SlowAction that processes collection input - its input type is collection of its type. */
	public static class SlowColAction<T> extends SlowActionBase<T,Collection<T>,Void> {

		public SlowColAction(String name, String description, GlyphIcons icon, Consumer<? super Collection<T>> act) {
			super(name, description, icon, FOR_ALL, ISNT, act);
		}

		public SlowColAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction, Consumer<? super Collection<T>> act) {
			super(name, description, icon, FOR_ALL, c -> c.stream().noneMatch(constriction), act);
		}

	}

	public enum GroupApply {
		FOR_EACH, FOR_ALL, NONE
	}

}