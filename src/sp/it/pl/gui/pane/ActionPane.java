package sp.it.pl.gui.pane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.animation.Interpolator;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.gui.objects.Text;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.table.FilteredTable;
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.access.V;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.animation.interpolator.ElasticInterpolator;
import sp.it.pl.util.async.future.Fut;
import sp.it.pl.util.collections.map.ClassListMap;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.MultiConfigurable;
import sp.it.pl.util.functional.Functors.Ƒ1;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.type.ClassName;
import sp.it.pl.util.type.InstanceInfo;
import sp.it.pl.util.type.InstanceName;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RESIZE_BOTTOM_RIGHT;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static sp.it.pl.gui.pane.GroupApply.FOR_ALL;
import static sp.it.pl.gui.pane.GroupApply.FOR_EACH;
import static sp.it.pl.gui.pane.GroupApply.NONE;
import static sp.it.pl.gui.pane.ActionPaneHelperKt.collectionUnwrap;
import static sp.it.pl.gui.pane.ActionPaneHelperKt.collectionWrap;
import static sp.it.pl.gui.pane.ActionPaneHelperKt.futureUnwrapOrThrow;
import static sp.it.pl.gui.pane.ActionPaneHelperKt.getUnwrappedType;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppBuildersKt.createInfoIcon;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.animation.Anim.animPar;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.dev.Util.throwIfNotFxThread;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.listRO;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.functional.UtilKt.getElementType;
import static sp.it.pl.util.graphics.Util.layHeaderTopBottom;
import static sp.it.pl.util.graphics.Util.layHorizontally;
import static sp.it.pl.util.graphics.Util.layScrollVTextCenter;
import static sp.it.pl.util.graphics.Util.layStack;
import static sp.it.pl.util.graphics.Util.layVertically;
import static sp.it.pl.util.graphics.UtilKt.setScaleXY;

/** Action chooser pane. Displays icons representing certain actions. */
public class ActionPane extends OverlayPane<Object> implements MultiConfigurable {

	private static final String ROOT_STYLECLASS = "action-pane";
	private static final String ICON_STYLECLASS = "action-pane-action-icon";
	private static final String COD_TITLE = "Close when action ends";
	private static final String COD_INFO = "Closes the chooser when action finishes running.";

	private final String configurableDiscriminant;

	@IsConfig(name = COD_TITLE, info = COD_INFO)
	public final V<Boolean> closeOnDone = new V<>(true);

	private final ClassName className;
	private final InstanceName instanceName;
	private final InstanceInfo instanceInfo;

	private boolean showIcons = true;
	private Node insteadIcons = null;

	public ActionPane(String configurableDiscriminant, ClassName className, InstanceName instanceName, InstanceInfo instanceInfo) {
		this.configurableDiscriminant = configurableDiscriminant;
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

	@NotNull
	@Override
	public String getConfigurableDiscriminant() {
		return configurableDiscriminant;
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
									.icons(CLOSE_CIRCLE_OUTLINE, CHECKBOX_BLANK_CIRCLE_OUTLINE)
									.tooltip(COD_TITLE+"\n\n"+COD_INFO);
	private final ProgressIndicator dataProgress = appProgressIndicator();
	public final ProgressIndicator actionProgress = appProgressIndicator();
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
	public final void show(Object data) {
		throwIfNotFxThread();

		data = collectionUnwrap(data);
		Class c = data==null ? Void.class : data.getClass();
		show(c, data);
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
	public final <T> void show(Class<T> type, Fut<T> value, boolean exclusive, SlowAction<T>... actions) {
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
	private void doneHide(ActionData<?,?> action) {
		if (action.isComplex) {
			ComplexActionData complexAction = action.complexData.invoke(this);
			showIcons = false;
			insteadIcons = (Node) complexAction.gui.invoke((Supplier) () -> action.prepInput(getData()));
			show(complexAction.input.invoke(action.prepInput(getData())));
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
			data = ((Fut) data)
					.useBy(FX, this::setData)
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
			Class itemType = getElementType(items);
			if (APP.classFields.get(itemType) != null) {	// TODO: add support for any item by using generic ToString objectField and column
				FilteredTable<Object> t = new FilteredTable<>(itemType, null);
				t.getSelectionModel().setSelectionMode(MULTIPLE);
				t.setColumnFactory(f -> {
					TableColumn<?,Object> c = new TableColumn<>(f.toString());
					c.setCellValueFactory(cf -> cf.getValue()== null ? null : new PojoV(f.getOf(cf.getValue())));
					c.setCellFactory(col -> (TableCell) t.buildDefaultCell(f));
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

	// TODO: remove
	@SuppressWarnings("deprecation")
	private String computeDataInfo(Object data, boolean computed) {
		Class<?> type = data==null ? Void.class : data.getClass();
		Object d = computed ? data instanceof Fut ? ((Fut)data).getDoneOrNull() : data : null;

		String dName = !computed ? "n/a" : instanceName.get(d);
		String dKind = !computed ? "n/a" : className.get(type);
		String dInfo = !computed ? "" : instanceInfo.get(d).entrySet().stream()
											.map(e -> e.getKey() + ": " + e.getValue())
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
				return (boolean) a.condition.invoke(collectionWrap(d));
			}
			if (a.groupApply==FOR_EACH) {
				Stream<Object> ds = d instanceof Collection ? ((Collection)d).stream() : stream(d);
				return ds.noneMatch(it -> (boolean) a.condition.invoke(it));
			}
			if (a.groupApply==NONE) {
				Object o = collectionUnwrap(d);
				return o instanceof Collection || !((boolean) a.condition.invoke(o));
			}
			throw new SwitchException(a.groupApply);
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
				Icon i = new Icon()
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
			.collect(collectingAndThen(toList(), icons::setAll));

		// Animate - pop icons in parallel, but with increasing delay
		// We do not want the total animation length be dependent on number of icons (by using
		// absolute icon delay), rather we calculate the delay so total length remains the same.
		Duration total = seconds(1);
		double delayAbs = total.divide(icons.size()).toMillis(); // use for consistent total length
		double delayRel = 200; // use for consistent frequency
		double delay = delayAbs;
		Interpolator intpl = new ElasticInterpolator();
		animPar(icons, (i, icon) -> new Anim(at -> setScaleXY(icon, at*at)).dur(millis(500)).intpl(intpl).delay(millis(350+i*delay)))
			.play();
	}

	private void runAction(ActionData<?,?> action, Object data) {
		if (!action.isLong) {
			action.accept(data);
			doneHide(action);
		} else {
			fut(data)
				.useBy(FX, it -> actionProgress.setProgress(-1))
				// run action and obtain output
				.useBy(action)
				// 1) the actions may invoke some action on FX thread, so we give it some by waiting a bit
				// 2) very short actions 'pretend' to run for a while
				.thenWait(millis(150))
				.useBy(FX, it -> actionProgress.setProgress(1))
				.useBy(FX, it -> doneHide(action));
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

	public <I> ConvertingConsumer<? super I> converting(Ƒ1<? super I,Try<?,?>> converter) {
		return d -> {
			converter.apply(d).ifOk(result -> runFX(() -> ActionPane.this.show(result)));
			return Unit.INSTANCE;
		};
	}

}