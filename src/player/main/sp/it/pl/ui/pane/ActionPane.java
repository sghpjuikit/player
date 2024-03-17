package sp.it.pl.ui.pane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import kotlin.Unit;
import kotlin.reflect.KClass;
import sp.it.pl.main.AppBuildersKt;
import sp.it.pl.main.AppSettings.ui.overlay.actionViewer;
import sp.it.pl.ui.objects.icon.CheckIcon;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.ui.objects.table.FilteredTable;
import sp.it.pl.ui.pane.ActionData.UiResult;
import sp.it.util.access.V;
import sp.it.util.async.future.Fut;
import sp.it.util.collections.map.KClassListMap;
import sp.it.util.dev.DebugKt;
import sp.it.util.file.json.JsValue;
import sp.it.util.text.Jwt;
import sp.it.util.type.ClassName;
import sp.it.util.type.InstanceDescription;
import sp.it.util.type.InstanceName;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RESIZE_BOTTOM_RIGHT;
import static java.lang.Math.sqrt;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.scene.layout.Priority.SOMETIMES;
import static javafx.util.Duration.millis;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static sp.it.pl.main.AppBuildersKt.animShowNodes;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppBuildersKt.tableViewForClassJava;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.main.AppProgressKt.withProgress;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.autoResizeColumns;
import static sp.it.pl.ui.pane.ActionPaneWrappingKt.futureUnwrapOrThrow;
import static sp.it.pl.ui.pane.ActionPaneWrappingKt.getUnwrappedType;
import static sp.it.pl.ui.pane.ActionPaneWrappingKt.nounwrapUnWrap;
import static sp.it.util.animation.Anim.anim;
import static sp.it.util.async.AsyncKt.CURR;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.collections.UtilKt.collectionUnwrap;
import static sp.it.util.collections.UtilKt.getElementClass;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.file.json.JsonKt.toPrettyS;
import static sp.it.util.functional.TryKt.getOr;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listRO;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.sync1IfInScene;
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
		var iconPaneSimple = new FlowPane(Orientation.HORIZONTAL, 20, 20);
			iconPaneSimple.setAlignment(Pos.CENTER);
			iconPaneSimple.setRowValignment(VPos.CENTER);
			iconPaneSimple.setRowValignment(VPos.CENTER);
			iconPaneSimple.setId("iconPaneSimple");
		icons = iconPaneSimple.getChildren();

		// content for icons and descriptions
		var iconBox = layStack(iconPaneComplex,CENTER, iconPaneSimple,CENTER);
			iconBox.setId("iconBox");
		var iconPane = layVertically(10.0, Pos.CENTER, infoPane, iconBox, descPane);
			iconPane.setId("iconPane");
			VBox.setVgrow(iconBox, ALWAYS);

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
		var content = layHorizontally(0, CENTER, dataTablePane,iconPane);
			content.setPadding(new Insets(0,50,0,50)); // top & bottom padding set differently, below
            content.setMinSize(300, infoPane.getMinHeight() + contentSpacing + iconBox.getMinHeight() + contentSpacing + descPane.getMinHeight());
		dataTableContentGap = content.spacingProperty();
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

		getContent().setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE); // TODO: apply on content change?, fix null

		// put some padding of the content from edge
		// note: The content is user-resizable now, sp we do not use bind, but set maxSize on show
		// getContent().maxWidthProperty().bind(widthProperty().multiply(CONTENT_SIZE_SCALE));
		// getContent().maxHeightProperty().bind(heightProperty().multiply(CONTENT_SIZE_SCALE);

		descTitle.setTextAlignment(TextAlignment.CENTER);
		descFull.setTextAlignment(TextAlignment.JUSTIFY);
		descriptionFullPane.maxWidthProperty().bind(min(600, iconPane.widthProperty()));

		makeResizableByUser();
	}

	/* ---------- PRE-CONFIGURED ACTIONS --------------------------------------------------------------------------------- */

	public final KClassListMap<ActionData<?,?>> actions = new KClassListMap<>(it -> { throw new AssertionError(""); });

	public final <T> void register(KClass<T> c, ActionData<T,?> action) {
		actions.accumulate(c, action);
	}

	@SafeVarargs
	public final <T> void register(KClass<T> c, ActionData<T,?>... action) {
		actions.accumulate(c, listRO(action));
	}

/* ---------- CONTROLS ---------------------------------------------------------------------------------------------- */

	private final Icon helpI = infoIcon(
		"Action chooser\n\nChoose an action. It may use some input data. Data not immediately ready will display progress indicator."
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
	private List<ActionData<?,?>> actionsIcons;
	private final List<ActionData<?,?>> actionsData = new ArrayList<>();
	private static final double CONTENT_SIZE_SCALE = 0.65;

	protected void show() {
		failIfNotFxThread();

		setContentEmpty();
		super.show();
		resizeContentToDefault();

		dataInfo.setOpacity(0.0);
		dataTablePane.setOpacity(0.0);
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
		var c = data==null ? Void.class : data.getClass();
		show((Class<Object>) c, data);
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

	@SuppressWarnings("unused")
	public final <T> void show(Class<T> type, Fut<T> value, boolean exclusive, ActionData<?,?>... actions) {
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

	private <DATA, NEW_DATA> void doneHide(ActionData<?, DATA> action, Object result) {
		if (result instanceof UiResult uir) {
			showIcons = false;
			insteadIcons = () -> uir.getUi();
			show(result);
		} else if (!action.isResultUnit(result)) {
			show(result);
		} else if (!action.preventClosing) {
			doneHide();
		}
	}

/* ---------- GRAPHICS ---------------------------------------------------------------------------------------------- */

	private final Text dataInfo = new Text();
	private final Label descTitle = new Label();
	private final Text descFull = new Text();
	private final ObservableList<Node> icons;
	private FilteredTable<?> dataTable;
	private TextArea dataTextArea;
	private final DoubleProperty dataTableContentGap;
	private final StackPane dataTablePane = new StackPane();
	private final StackPane iconPaneComplex = new StackPane();
	{
		iconPaneComplex.setId("iconPaneComplex");
		dataTablePane.setId("tablePane");
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
			if (dataTable!=null) {
				return dataTable.getItems()==null || dataTable.getItems().isEmpty() ? d : dataTable.getSelectedOrAllItemsCopy();
			} else {
				return d;
			}
		} else
			return d;
	}

	public void setData(Object d) {
		failIfNotFxThread();

		// clear content
		setActionInfo(null);
		icons.clear();

		// set content
		data = collectionUnwrap(d);
		boolean isDataReady = !(data instanceof Fut<?> dataFut && !dataFut.isDone());
		if (isDataReady) {
			data = nounwrapUnWrap(collectionUnwrap(futureUnwrapOrThrow(data)));
			setDataInfo(data, true);
			showIcons(data);
		} else {
			setDataInfo(null, false);
			data = withProgress(((Fut<?>) data).useBy(FX, this::setData), dataProgress); // obtain data & invoke again
		}
	}

	private void setActionInfo(ActionData<?,?> a) {
		descTitle.setText(a==null ? "" : a.name);
		descFull.setText(a==null ? "" : a.description);
	}

	@SuppressWarnings({"unchecked", "AccessStaticViaInstance"})
	private void setDataInfo(Object data, boolean computed) {
		dataInfo.setText(computeDataInfo(data, computed));
		dataTablePane.getChildren().clear();
		var gap = 0.0;
		var priority = NEVER;

		var dataAsS = switch (data) {
			case null -> null;
			case UiResult dataUi -> null;
			case String dataS when dataS.length()>40 -> dataS;
			case Throwable t -> DebugKt.getStacktraceAsString(t);
			case JsValue dataJs -> toPrettyS(dataJs, "  ", "\n");
			case Jwt dataJwt -> APP.getConverter().ui.toS(dataJwt);
			case Object	dataO when getKotlinClass(dataO.getClass()).isData() -> APP.getConverter().ui.toS(data);
			default -> null;
		};

		if (dataAsS!=null) {
			dataTextArea = new TextArea();
			dataTextArea.setEditable(false);
			dataTextArea.setText(dataAsS);
			dataTablePane.getChildren().setAll(dataTextArea);
			gap = 70.0;
			priority = SOMETIMES;
		}

		var dataAsC = (Collection<?>) null;
		if (data instanceof UiResult) {
			dataTable = null;
		} else if (data instanceof Collection<?> items && !items.isEmpty()) {
			var itemType = (KClass<Object>) getKotlinClass(getElementClass(items));
			dataTable = tableViewForClassJava(itemType, consumer(t -> {
				t.getSelectedItems().addListener((Change<?> c) -> {
					if (insteadIcons==null) {
						dataInfo.setText(computeDataInfo(collectionUnwrap(t.getSelectedOrAllItemsCopy()), true));
					}
				});
				t.setItemsRaw(items, consumer(unit -> autoResizeColumns(t)));
			}));
			dataTablePane.getChildren().setAll(dataTable.getRoot());
			gap = 70.0;
			priority = SOMETIMES;
		} else {
			dataTable = null;
		}

		HBox.setHgrow(dataTablePane, priority);
		dataTableContentGap.set(gap);
	}

	private void setContentEmpty() {
		dataInfo.setText("");
		dataTablePane.getChildren().clear();
		if (dataTextArea!=null) dataTextArea.setText(null);
		dataTableContentGap.setValue(0.0);
		descFull.setText("");
		descTitle.setText("");
		icons.clear();
		hideCustomActionUi();
	}

	private void resizeContentToDefault() {
		sync1IfInScene(getContent(), runnable(() -> {
			Bounds b = getLayoutBounds();
			getContent().setPrefSize(b.getWidth() * CONTENT_SIZE_SCALE, b.getHeight() * CONTENT_SIZE_SCALE);
		}));
	}

	private String computeDataInfo(Object data, boolean computed) {
		if (data instanceof UiResult uir) return uir.getInfo();
		if (computed) return getOr(AppBuildersKt.computeDataInfo(data).getDone().toTry(), "Failed to obtain data information.");
	    else return "Data: n/a\nType: n/a\n";
	}

	private void showIcons(Object d) {
		var dataType = getUnwrappedType(d);
		// get suitable actions
		actionsData.clear();
		actionsData.addAll(actionsIcons);
		if (use_registered_actions) actions.getElementsOfSuper(dataType).iterator().forEachRemaining(actionsData::add);
		actionsData.removeIf(a -> !a.invokeIsDoable(d));

		if (!showIcons || data instanceof UiResult) {
			dataInfo.setOpacity(1.0);
			dataTablePane.setOpacity(1.0);
			descFull.setOpacity(1.0);
			descTitle.setOpacity(1.0);
			iconPaneComplex.setOpacity(1.0);
			showCustomActionUi((data instanceof UiResult x) ? x.getUi() : insteadIcons.get());
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
				  .action(e -> runAction(action, getData()));

				 // Description is shown when mouse hovers
				 i.addEventHandler(MOUSE_ENTERED, e -> setActionInfo(action));
				 i.addEventHandler(MOUSE_EXITED, e -> setActionInfo(null));

				 // Long descriptions require scrollbar, but because mouse hovers on icon, scrolling
				 // is not possible. Hence, we detect scrolling above mouse and pass it to the
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
		animShowNodes(icons, (i, node, at) -> {
			iconNodes.get(i).setOpacity(at);
			setScaleXY(iconGlyphs.get(i), sqrt(at));
			return Unit.INSTANCE;
		}).play();
		anim(millis(200), consumer(it -> {
				dataInfo.setOpacity(it);
				dataTablePane.setOpacity(it);
				descFull.setOpacity(it);
				descTitle.setOpacity(it);
				iconPaneComplex.setOpacity(it);
			}))
			.delay(millis(100))
			.intpl(x -> x*x)
			.play();
	}

	private void runAction(ActionData<?,?> action, Object data) {
		var context = new ActContext(this);
		action.invokeFut(context, data)
			.thenFlatten(CURR)
			.onDone(FX, consumer(rt ->
				rt.toTryRaw()
					.ifOk(consumer(r -> doneHide(action, r)))
					.ifError(consumer(e -> show(e)))
			));
	}

	private void hideCustomActionUi() {
		iconPaneComplex.getParent().getChildrenUnmodifiable().forEach(n -> n.setVisible(true));
		iconPaneComplex.getChildren().clear();
	}

	private void showCustomActionUi(Node node) {
		iconPaneComplex.getParent().getChildrenUnmodifiable().forEach(n -> n.setVisible(false));
		iconPaneComplex.setVisible(true);
		iconPaneComplex.getChildren().setAll(node);
	}

}