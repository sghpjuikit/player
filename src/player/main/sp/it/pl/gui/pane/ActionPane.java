package sp.it.pl.gui.pane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import kotlin.Unit;
import javafx.scene.text.Text;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.table.FilteredTable;
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV;
import sp.it.pl.main.AppExtensionsKt;
import sp.it.pl.main.AppSettings.ui.view.actionViewer;
import sp.it.util.access.V;
import sp.it.util.animation.Anim;
import sp.it.util.async.future.Fut;
import sp.it.util.collections.map.ClassListMap;
import sp.it.util.dev.DebugKt;
import sp.it.util.dev.SwitchException;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Try;
import sp.it.util.type.ClassName;
import sp.it.util.type.InstanceDescription;
import sp.it.util.type.InstanceName;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RESIZE_BOTTOM_RIGHT;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.joining;
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
import static kotlin.streams.jdk8.StreamsKt.asStream;
import static sp.it.pl.gui.pane.ActionPaneHelperKt.futureUnwrapOrThrow;
import static sp.it.pl.gui.pane.ActionPaneHelperKt.getUnwrappedType;
import static sp.it.pl.gui.pane.GroupApply.FOR_ALL;
import static sp.it.pl.gui.pane.GroupApply.FOR_EACH;
import static sp.it.pl.gui.pane.GroupApply.NONE;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppExtensionsKt.getNameUi;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.main.AppProgressKt.withProgress;
import static sp.it.util.animation.Anim.anim;
import static sp.it.util.animation.Anim.animPar;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.NEW;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.future.Fut.fut;
import static sp.it.util.collections.UtilKt.collectionUnwrap;
import static sp.it.util.collections.UtilKt.collectionWrap;
import static sp.it.util.collections.UtilKt.getElementType;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listRO;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.ui.Util.layHeaderTopBottom;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.Util.layScrollVTextCenter;
import static sp.it.util.ui.Util.layStack;
import static sp.it.util.ui.Util.layVertically;
import static sp.it.util.ui.UtilKt.setScaleXY;

/** Action chooser pane. Displays icons representing certain actions. */
public class ActionPane extends OverlayPane<Object> {

	private static final String ROOT_STYLECLASS = "action-pane";
	private static final String ICON_STYLECLASS = "action-pane-action-icon";

	public final V<Boolean> closeOnDone = new V<>(true);
	public final ClassName className;
	public final InstanceName instanceName;
	public final InstanceDescription instanceDescription;
	private boolean showIcons = true;
	private Supplier<? extends Node> insteadIcons = null;

	public ActionPane(ClassName className, InstanceName instanceName, InstanceDescription instanceDescription) {
		this.className = className;
		this.instanceName = instanceName;
		this.instanceDescription = instanceDescription;

		getStyleClass().add(ROOT_STYLECLASS);

		// icons and descriptions
		var descriptionFullPane = layScrollVTextCenter(descFull);
		    descriptionFullPane.setId("descriptionFullPane");
		var infoPane = layStack(layScrollVTextCenter(dataInfo),TOP_LEFT);
			infoPane.setId("infoPane");
		var descPane = layVertically(8, BOTTOM_CENTER, descTitle,descriptionFullPane);
			descPane.setId("descPane");
		var iconPaneSimple = layHorizontally(15,CENTER);
			iconPaneSimple.setId("iconPaneSimple");
		icons = iconPaneSimple.getChildren();

		// content for icons and descriptions
		var iconBox = layStack(iconPaneComplex,CENTER, iconPaneSimple,CENTER);
			iconBox.setId("iconBox");
		var iconPane = layStack(infoPane, TOP_LEFT, iconBox,CENTER, descPane,BOTTOM_CENTER);
			iconPane.setId("iconPane");

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
		iconBox.setMinHeight(100);
		// iconBox.maxHeightProperty().bind(iconPane.heightProperty().multiply(0.4).subtract(2*25));
		descPane.setMinHeight(100);
		descPane.maxHeightProperty().bind(min(iconPane.heightProperty().multiply(0.3), 400));
		descPane.setMouseTransparent(true);
		iconPaneComplex.getChildren().addListener((Change<?> e) -> {
			var isSimple = iconPaneComplex.getChildren().isEmpty();
			descPane.setVisible(isSimple);
			iconBox.maxHeightProperty().unbind();
			iconBox.maxHeightProperty().bind(
				isSimple
					? iconPane.heightProperty().multiply(0.4).subtract(2*25.0)
					: iconPane.heightProperty().multiply(0.7).subtract(25.0)
			);
			StackPane.setAlignment(iconBox, isSimple ? CENTER : BOTTOM_CENTER);
		});

		// content
		var contentSpacing = 20.0;
		var content = layHorizontally(0, CENTER, tablePane,iconPane); // table is an optional left complement to iconPane
			content.setPadding(new Insets(0,50,0,50)); // top & bottom padding set differently, below
            content.setMinSize(300, infoPane.getMinHeight() + contentSpacing + iconBox.getMinHeight() + contentSpacing + descPane.getMinHeight());
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
		getContent().setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		// put some padding of the content from edge
		// note: The content is user-resizable now, sp we do not use bind, but set maxSize on show
		// getContent().maxWidthProperty().bind(widthProperty().multiply(CONTENT_SIZE_SCALE));
		// getContent().maxHeightProperty().bind(heightProperty().multiply(CONTENT_SIZE_SCALE);

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

	private final Icon helpI = infoIcon(
		"Action chooser"
	  + "\n"
	  + "\nChoose an action. It may use some input data. Data not immediately ready will "
	  + "display progress indicator."
	);
	private final Icon hideI = new CheckIcon(closeOnDone)
									.icons(CLOSE_CIRCLE_OUTLINE, CHECKBOX_BLANK_CIRCLE_OUTLINE)
									.tooltip(actionViewer.closeWhenActionEnds.cname +"\n\n"+ actionViewer.closeWhenActionEnds.cinfo);
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
		failIfNotFxThread();

		setContentEmpty();
		super.show();
		resizeContentToDefault();

		dataInfo.setOpacity(0.0);
		tablePane.setOpacity(0.0);
		descFull.setOpacity(0.0);
		descTitle.setOpacity(0.0);
		iconPaneComplex.setOpacity(0.0);
		runFX(millis(400.0), () -> setData(data));
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void show(Object data) {
		failIfNotFxThread();

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
	private <DATA, NEW_DATA> void doneHide(ActionData<?, DATA> action) {
		if (action.isComplex) {
			ComplexActionData<DATA, NEW_DATA> complexAction = (ComplexActionData) action.complexData.invoke(this);
			showIcons = false;
			insteadIcons = () -> (Node) complexAction.gui.invoke((NEW_DATA) action.prepInput(getData()));
			var newData = complexAction.input.invoke(action.prepInputExact(getData()));
			show(newData);
		}

		if (!action.preventClosing)
			doneHide();
	}

/* ---------- GRAPHICS ---------------------------------------------------------------------------------------------- */

	private final Text dataInfo = new Text();
	private final Label descTitle = new Label();
	private final Text descFull = new Text();
	private final ObservableList<Node> icons;
	private final DoubleProperty tableContentGap;
	private final StackPane tablePane = new StackPane();
	private FilteredTable<?> table;
	private final StackPane iconPaneComplex = new StackPane();
	{
		iconPaneComplex.setId("iconPaneComplex");
		tablePane.setId("tablePane");
	}

/* ---------- HELPER ------------------------------------------------------------------------------------------------ */

	/**
	 * This is an advanced API. Use with caution.
	 * <p/>
	 * Returns input data or its subset if user selection is active.
	 *
	 * @return user selection of the data available
	 */
	public Object getData() {
		failIfNotFxThread();

		Object d = futureUnwrapOrThrow(data);
		if (d instanceof Collection) {
			if (table!=null) {
				return table.getSelectedOrAllItemsCopy();
			} else {
				return d;
			}
		} else
			return d;
	}

	@SuppressWarnings("unchecked")
	private void setData(Object d) {
		failIfNotFxThread();

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
			data = withProgress(
				((Fut) data).useBy(FX, this::setData),
				dataProgress
			);
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
		double gap = 0.0;
		if (data instanceof Collection && !((Collection)data).isEmpty()) {
			Collection<Object> items = (Collection) data;
			Class itemType = getElementType(items);
			if (APP.getClassFields().get(itemType) != null) {	// TODO: add support for any item by using generic ToString objectField and column
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
				gap = 70.0;
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

	private void setContentEmpty() {
		dataInfo.setText("");
		tablePane.getChildren().clear();
		tableContentGap.setValue(0.0);
		descFull.setText("");
		descTitle.setText("");
		icons.clear();
		hideCustomActionUi();
	}

	private void resizeContentToDefault() {
		sp.it.util.reactive.UtilKt.sync1IfInScene(getContent(), runnable(() -> {
			Bounds b = getLayoutBounds();
			getContent().setPrefSize(b.getWidth() * CONTENT_SIZE_SCALE, b.getHeight() * CONTENT_SIZE_SCALE);
		}));
	}

	// TODO: remove
	@SuppressWarnings("deprecation")
	private String computeDataInfo(Object data, boolean computed) {
		Class<?> type = data==null ? Void.class : data.getClass();
		Object d = computed ? data instanceof Fut ? ((Fut)data).getDoneOrNull() : data : null;

		String dName = !computed ? "n/a" : instanceName.get(d);
		String dKind = !computed ? "n/a" : getNameUi(type) + (APP.getDeveloperMode().getValue() ? " (" + type + ")" : "");
		String dInfo = !computed ? "" : asStream(instanceDescription.get(d)).map(e -> e.getName() + ": " + e.getValue()).sorted().collect(joining("\n"));
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
			dataInfo.setOpacity(1.0);
			tablePane.setOpacity(1.0);
			descFull.setOpacity(1.0);
			descTitle.setOpacity(1.0);
			iconPaneComplex.setOpacity(1.0);
			showCustomActionUi();
			insteadIcons = null;
			showIcons = true;
			return;
		} else {
			hideCustomActionUi();
		}

		var iconGlyphs = new ArrayList<Icon>();
		var iconNodes = new ArrayList<Node>();
		stream(actionsData).sorted(by(a -> a.name)).forEach(action -> {
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
			iconGlyphs.add(i);
			iconNodes.add(i.withText(action.name));
		});
		icons.setAll(iconNodes);

		// animate icons
		Duration total = seconds(0.4);
		double delayAbs = total.divide(icons.size()).toMillis(); // use for consistent total length
		double delayRel = 200; // use for consistent frequency
		double delay = delayAbs;
		animPar(icons, (i, icon) ->
				new Anim(at -> {
					iconNodes.get(i).setOpacity(at);
					setScaleXY(iconGlyphs.get(i), sqrt(at));
				})
				.dur(millis(500)).delay(millis(150+i*delay))
			)
			.play();
		anim(millis(200), consumer(it -> {
				dataInfo.setOpacity(it);
				tablePane.setOpacity(it);
				descFull.setOpacity(it);
				descTitle.setOpacity(it);
				iconPaneComplex.setOpacity(it);
			}))
			.delay(millis(100))
			.intpl(x -> x*x)
			.play();
	}

	private void runAction(ActionData<?,?> action, Object data) {
		if (!action.isLong) {
			try {
				action.invoke(data);
				doneHide(action);
			} catch (Throwable e) {
				DebugKt.logger(ActionPane.class).error("Running action={} failed", action.name, e);
			}
		} else {
			fut(data)
				.useBy(FX, it -> actionProgress.setProgress(-1))
				// run action and obtain output
				.useBy(NEW, action::invoke)
				// 1) the actions may invoke some action on FX thread, so we give it some by waiting a bit
				// 2) very short actions 'pretend'r a while
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
		iconPaneComplex.getChildren().setAll(insteadIcons.get());
	}

	public <I> ConvertingConsumer<? super I> converting(F1<? super I,Try<?,?>> converter) {
		return d -> {
			converter.apply(d).ifOkUse(result -> runFX(() -> ActionPane.this.show(result)));
			return Unit.INSTANCE;
		};
	}

}