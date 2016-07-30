/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.beans.property.DoubleProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import de.jensd.fx.glyphs.GlyphIcons;
import gui.Gui;
import gui.objects.Text;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.spinner.Spinner;
import gui.objects.table.FilteredTable;
import gui.objects.table.ImprovedTable.PojoV;
import util.access.V;
import util.access.fieldvalue.FileField;
import util.access.fieldvalue.ObjectField;
import util.action.Action;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.future.Fut;
import util.collections.map.ClassListMap;
import util.collections.map.ClassMap;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.functional.Functors.Ƒ1;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static gui.objects.icon.Icon.createInfoIcon;
import static gui.objects.table.FieldedTable.defaultCell;
import static gui.pane.ActionPane.GroupApply.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.*;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static main.App.APP;
import static util.async.Async.FX;
import static util.async.Async.sleeping;
import static util.async.future.Fut.fut;
import static util.async.future.Fut.futAfter;
import static util.dev.Util.no;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;
import static util.type.Util.build;
import static util.type.Util.getEnumConstants;

/**
 * Action chooser pane. Displays icons representing certain actions.
 *
 * @author Martin Polakovic
 */
@IsConfigurable("Action Chooser")
public class ActionPane extends OverlayPane implements Configurable<Object> {

    static final ClassMap<Class<?>> fieldMap = new ClassMap<>();
    static {
        fieldMap.put(PlaylistItem.class, PlaylistItem.Field.class);
        fieldMap.put(Metadata.class, Metadata.Field.class);
        fieldMap.put(MetadataGroup.class, MetadataGroup.Field.class);
        fieldMap.put(File.class, FileField.class);
    }

    private static final String ROOT_STYLECLASS = "action-pane";
    private static final String ICON_STYLECLASS = "action-pane-action-icon";
    private static final String COD_TITLE = "Close when action ends";
    private static final String COD_INFO = "Closes the chooser when action finishes running.";


    @IsConfig(name = COD_TITLE, info = COD_INFO)
    public final V<Boolean> closeOnDone = new V<>(false);

    public ActionPane() {
        getStyleClass().add(ROOT_STYLECLASS);

        // icons and descriptions
        ScrollPane descriptionFullPane = layScrollVTextCenter(descFull);
        StackPane infoPane = layStack(dataInfo,TOP_LEFT);
        VBox descPane = layVertically(8, BOTTOM_CENTER, descTitle,descriptionFullPane);
        HBox iconBox = layHorizontally(15,CENTER);
        icons = iconBox.getChildren();

        // content for icons and descriptions
        StackPane iconPane = layStack(infoPane, TOP_LEFT, iconBox,CENTER, descPane,BOTTOM_CENTER);
        // Minimal and maximal height of the 3 layout components. The heights should add
        // up to full length (including the spacing of course). Sounds familiar? No, could not use
        // VBox or stackpane as we need the icons to be always in the center.
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

        Pane controlsMirror = new Pane();
             controlsMirror.prefHeightProperty().bind(controls.heightProperty()); // see below
        setContent(
            layHeaderTopBottom(20, CENTER_RIGHT,
                controls, // tiny header
                content, // the above and below also serve as top/bottom padding
                controlsMirror // fills bottom so the content resizes vertically to center
            )
        );
        getContent().setMinSize(300,200);
        // guarantee some padding of the content from edge
        getContent().maxWidthProperty().bind(widthProperty().multiply(0.65));
        getContent().maxHeightProperty().bind(heightProperty().multiply(0.65));

        descPane.setMouseTransparent(true); // just in case
        infoPane.setMouseTransparent(true); // same here
        descTitle.setTextAlignment(TextAlignment.CENTER);
        descFull.setTextAlignment(TextAlignment.JUSTIFY);
        descriptionFullPane.maxWidthProperty().bind(min(400, iconPane.widthProperty()));
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
    private final ProgressIndicator actionProgress = build(new Spinner(1), s -> maintain(s.progressProperty(), p -> p.doubleValue()<1, s.visibleProperty()));
    private final HBox controls = layHorizontally(5,CENTER_RIGHT, actionProgress, dataProgress,hideI,helpI);

/* ---------- DATA -------------------------------------------------------------------------------------------------- */

    private boolean use_registered_actions = true;
    private Object data;
    private List<ActionData> actionsIcons;
    private final List<ActionData> actionsData = new ArrayList<>();

    @Override
    public void show() {
        setData(data);

        // Bug fix. We need to initialize the layout before it is visible or it may visually
        // jump around as it does on its own.
        // Cause: unknown, probably the many bindings we use...
        getContent().layout();
        getContent().requestLayout();
        getContent().autosize();

        super.show();
    }

    @SuppressWarnings("unchecked")
    public final void show(Object value) {
        value = collectionUnwrap(value);
        Class c = value==null ? Void.class : value.getClass();
        show(c, value);
    }

    public final <T> void show(Class<T> type, T value) {
        no(value==null && (type!=Void.class || type!=void.class));
        show(type, value, false);
    }

    public final <T> void show(Class<T> type, T value, boolean exclusive, ActionData<?,?>... actions) {
        data = value;
        actionsIcons = list(actions);
        use_registered_actions = !exclusive;
        show();
    }

    @SafeVarargs
    public final <T> void show(Class<T> type, Fut<T> value, boolean exclusive, SlowAction<T,?>... actions) {
        data = value;
        actionsIcons = list(actions);
        use_registered_actions = !exclusive;
        show();
    }

    private void doneHide() {
        if (closeOnDone.get()) hide();
    }

/* ---------- GRAPHICS ---------------------------------------------------------------------------------------------- */

    private final Label dataInfo = new Label();
    private final Label descTitle = new Label();
    private final Text descFull = new Text();
    private final ObservableList<Node> icons;
    private final DoubleProperty tableContentGap;
    private StackPane tablePane = new StackPane();
    private FilteredTable<?,?> table;

/* ---------- HELPER ------------------------------------------------------------------------------------------------ */

    // retrieve set data
    private Object getData() {
        return data instanceof Collection ? list(table.getItems()) : data;
    }

    // set data to retrieve
    private void setData(Object d) {
        // clear content
        setActionInfo(null);
        icons.clear();

        // set content
        data = collectionUnwrap(d);
        boolean isDataReady = !(data instanceof Fut && !((Fut)data).isDone());
        if (isDataReady) {
            data = futureUnwrap(data);
            setDataInfo(data, true);
            showIcons(data);
        } else {
            setDataInfo(null, false);
            // obtain data & invoke again
            Fut<Object> f = ((Fut)data)
                    .use(this::setData,FX)
                    .showProgress(dataProgress);
            f.run();
            data = f;
        }
    }

    private void setActionInfo(ActionData<?,?> a) {
        descTitle.setText(a==null ? "" : a.name);
        descFull.setText(a==null ? "" : a.description);
    }

    private void setDataInfo(Object data, boolean computed) {
        dataInfo.setText(getDataInfo(data, computed));
        tablePane.getChildren().clear();
        double gap = 0;
        if (data instanceof Collection && !((Collection)data).isEmpty()) {
            Collection<?> collection = (Collection) data;
            // TODO: improve collection element type recognition
            Class<?> coltype = collection.stream().findFirst().map(Object::getClass).orElse(Void.class);
            if (fieldMap.containsKey(coltype)) {
                FilteredTable<Object,?> t = new FilteredTable<>((ObjectField)getEnumConstants(fieldMap.get(coltype))[0]);
                t.setFixedCellSize(Gui.font.getValue().getSize() + 5);
                t.getSelectionModel().setSelectionMode(MULTIPLE);
                t.setColumnFactory(f -> {
                    TableColumn<?,?> c = new TableColumn<>(f.toString());
                    c.setCellValueFactory(cf -> cf.getValue()== null ? null : new PojoV(f.getOf(cf.getValue())));
                    c.setCellFactory(col -> (TableCell)defaultCell(f));
                    c.setResizable(true);
                    return (TableColumn)c;
                });
                t.setColumnState(t.getDefaultColumnInfo());
                tablePane.getChildren().setAll(t.getRoot());
                gap = 70;
                table = t;
                t.setItemsRaw(collection);
                t.getSelectedItems().addListener((Change<?> c) -> showIcons(t.getSelectedOrAllItemsCopy()));
            }
        }
        tableContentGap.set(gap);
    }

    private String getDataInfo(Object data, boolean computed) {
        Class<?> type = data==null ? Void.class : data.getClass();
        if (Void.class.equals(type)) return "";

        Object d = computed ? data instanceof Fut ? ((Fut)data).getDone() : data : null;
        String dName = computed ? APP.instanceName.get(d) : "n/a";
        String dKind = computed ? APP.className.get(type) : "n/a";
        String dInfo = stream(APP.instanceInfo.get(d)).mapKeyValue((key,val) -> key + ": " + val).sorted().collect(joining("\n"));
        if (!dInfo.isEmpty()) dInfo = "\n" + dInfo;

        return "Data: " + dName + "\nType: " + dKind + dInfo ;
    }

    @SuppressWarnings("unchecked")
    private void showIcons(Object d) {
        Class<?> dt = d==null ? Void.class : d instanceof Collection ? ((Collection)d).stream().findFirst().orElse(null).getClass() : d.getClass();
        // get suitable actions
        actionsData.clear();
        actionsData.addAll(actionsIcons);
        if (use_registered_actions) actionsData.addAll(actions.getElementsOfSuperV(dt));
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

        icons.setAll(actionsData.stream().sorted(by(a -> a.name)).map(action -> {
            Icon i = new Icon<>()
                  .icon(action.icon)
                  .styleclass(ICON_STYLECLASS)
                  .onClick(e -> {
                      if (!action.isLong) {
                          action.apply(d);
                          doneHide();
                      } else {
                          futAfter(fut(d))
                            .then(() -> actionProgress.setProgress(-1),FX)
                            .use(action) // run action and obtain output
                            // 1) the actions may invoke some action on FX thread, so we give it some
                            // by waiting a bit
                            // 2) very short actions 'pretend' to run for a while
                            .then(sleeping(millis(100)))
                            .then(() -> actionProgress.setProgress(1),FX)
                            .then(this::doneHide,FX);
                      }
                   });
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
        }).collect(toList()));

        // Animate - pop icons in parallel, but with increasing delay
        // We do not want the total animation length be dependent on number of icons (by using
        // absolute icon delay), rather we calculate the delay so total length remains the same.
        Duration total = seconds(1);
        double delay_abs = total.divide(icons.size()).toMillis(); // use for consistent total length
        double delay_rel = 200; // use for consistent frequency
        Anim.par(icons, (i,icon) -> new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(350+i*delay_abs))
            .play();
    }



    private static Collection<?> collectionWrap(Object o) {
        return o instanceof Collection ? (Collection)o : listRO(o);
    }

    private static Object collectionUnwrap(Object o) {
        if (o instanceof Collection) {
            Collection<?> c = (Collection)o;
            if (c.isEmpty()) return null;
            if (c.size()==1) return c.stream().findAny().get();
        }
        return o;
    }

    private static Object futureUnwrap(Object o) {
        return o instanceof Fut ? ((Fut)o).getDone() : o;
    }


    /** Action. */
    public static abstract class ActionData<C,T> implements Ƒ1<Object,Object>{
        public final String name;
        public final String description;
        public final GlyphIcons icon;
        public final Predicate<? super T> condition;
        public final GroupApply groupApply;
        private final Ƒ1<T,?> action;
        public final boolean isLong;

        private ActionData(String name, String description, GlyphIcons icon, GroupApply group, Predicate<? super T> constriction, boolean isLong, Ƒ1<T,?> action) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.condition = constriction;
            this.groupApply = group;
            this.isLong = isLong;
            this.action = action;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object apply(Object data) {
            boolean isCollection = data instanceof Collection;
            if (groupApply==FOR_ALL) {
                return action.apply(isCollection ? (T) data : (T) collectionWrap(data));
            } else
            if (groupApply==FOR_EACH) {
                if (isCollection) {
                    ((Collection<T>) data).forEach(action::apply);
                    return null;
                } else {
                    return action.apply((T)data);
                }
            } else
            if (groupApply==NONE) {
                if (isCollection) throw new RuntimeException("Action can not use collection");
                return action.apply((T)data);
            } else {
                throw new util.SwitchException(groupApply);
            }
        }
    }

    /** Action that executes synchronously - simply consumes the input. */
    private static class FastActionBase<C,T> extends ActionData<C,T> {

        private FastActionBase(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Consumer<T> act) {
            super(name, description, icon, groupApply, constriction, false, in -> { act.accept(in); return in; });
        }

    }
    /** FastAction that consumes simple input - its type is the same as type of the action. */
    public static class FastAction<T> extends FastActionBase<T,T> {

        private FastAction(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Consumer<T> act) {
            super(name, description, icon, groupApply, constriction, act);
        }

        public FastAction(String name, String description, GlyphIcons icon, Consumer<T> act) {
            this(name, description, icon, NONE, IS, act);
        }

        public FastAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction, Consumer<T> act) {
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

        public FastColAction(String name, String description, GlyphIcons icon, Consumer<Collection<T>> act) {
            super(name, description, icon, FOR_ALL, ISNT, act);
        }

        public FastColAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction,  Consumer<Collection<T>> act) {
            super(name, description, icon, FOR_ALL, c -> c.stream().noneMatch(constriction), act);
        }

    }

    /** Action that executes asynchronously - receives a future, processes the data and returns it. */
    private static class SlowActionBase<C,T,R> extends ActionData<C,T> {

        public SlowActionBase(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Ƒ1<T,R> act) {
            super(name, description, icon, groupApply, constriction, true, in -> { act.accept(in); return in; });
        }

    }
    /** SlowAction that processes simple input - its type is the same as type of the action. */
    public static class SlowAction<T,R> extends SlowActionBase<T,T,R> {

        public SlowAction(String name, String description, GlyphIcons icon, Consumer<T> act) {
            super(name, description, icon, NONE, IS, in -> { act.accept(in); return null; });
        }

        public SlowAction(String name, String description, GlyphIcons icon, GroupApply groupApply, Consumer<T> act) {
            super(name, description, icon, groupApply, IS, in -> { act.accept(in); return null; });
        }

    }
    /** SlowAction that processes collection input - its input type is collection of its type. */
    public static class SlowColAction<T> extends SlowActionBase<T,Collection<T>,Void> {

        public SlowColAction(String name, String description, GlyphIcons icon, Consumer<Collection<T>> act) {
            super(name, description, icon, FOR_ALL, ISNT, in -> { act.accept(in); return null; });
        }

        public SlowColAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction, Consumer<Collection<T>> act) {
            super(name, description, icon, FOR_ALL, c -> c.stream().noneMatch(constriction), in -> { act.accept(in); return null; });
        }

    }

    public  enum GroupApply {
        FOR_EACH,
        FOR_ALL,
        NONE
    }
}