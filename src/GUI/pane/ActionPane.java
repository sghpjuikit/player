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

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataGroup;
import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import action.Action;
import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.Text;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.spinner.Spinner;
import gui.objects.table.FilteredTable;
import gui.objects.table.ImprovedTable.PojoV;
import util.access.FieldValue.FileField;
import util.access.FieldValue.ObjectField;
import util.access.V;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.future.Fut;
import util.collections.map.ClassListMap;
import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static gui.objects.icon.Icon.createInfoIcon;
import static gui.objects.table.FieldedTable.defaultCell;
import static gui.pane.ActionPane.GroupApply.FOR_ALL;
import static gui.pane.ActionPane.GroupApply.FOR_EACH;
import static gui.pane.ActionPane.GroupApply.NONE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.*;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static main.App.APP;
import static util.Util.GR;
import static util.Util.getEnumConstants;
import static util.async.Async.FX;
import static util.async.future.Fut.fut;
import static util.async.future.Fut.futAfter;
import static util.dev.Util.no;
import static util.functional.Util.*;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layStack;
import static util.graphics.Util.layVertically;
import static util.graphics.Util.setScaleXY;
import static util.reactive.Util.maintain;

/**
 * Action chooser pane. Displays icons representing certain actions.
 *
 * @author Plutonium_
 */
@IsConfigurable("Action Chooser")
public class ActionPane extends OverlayPane implements Configurable {

    static final ClassMap<Class<?>> fieldmap = new ClassMap<>();
    static {
        fieldmap.put(PlaylistItem.class, PlaylistItem.Field.class);
        fieldmap.put(Metadata.class, Metadata.Field.class);
        fieldmap.put(MetadataGroup.class, MetadataGroup.Field.class);
        fieldmap.put(File.class, FileField.class);
    }

    private static final String ROOT_STYLECLASS = "action-pane";
    private static final String ICON_STYLECLASS = "action-pane-action-icon";
    private static final String COD_TITLE = "Close when action ends";
    private static final String COD_INFO = "Closes the chooser when action finishes running.";

    @IsConfig(name = COD_TITLE, info = COD_INFO)
    public final V<Boolean> closeOnDone = new V<>(false);

    public ActionPane() {
        getStyleClass().add(ROOT_STYLECLASS);

        HBox iconBox = layHorizontally(15,CENTER);
        icons = iconBox.getChildren();

        // layout
        StackPane layout = layStack(
            tablePane,CENTER_LEFT,
            desc, BOTTOM_CENTER,
            layVertically(10, TOP_CENTER, controls,dataInfo), TOP_CENTER,
            iconBox, CENTER
        );
        layout.setMaxSize(GR*450,450);
        tablePane.setMaxSize(300,450);
        setContent(layHorizontally(10, CENTER, tablePane,layout));
        HBox.setHgrow(layout, javafx.scene.layout.Priority.ALWAYS);
        ((Pane)layout.getParent()).setMaxSize(900,450);

        iconBox.setMaxHeight(layout.getMaxHeight()-2*30); // top/bottom padding for clickable controls
        desc.setMouseTransparent(true); // just in case, if it is under icons all's well
        dataInfo.setMouseTransparent(true); // same here
        desctitl.setTextAlignment(TextAlignment.CENTER);
        descfull.setTextAlignment(TextAlignment.JUSTIFY);
        descfull.wrappingWidthProperty().bind(min(350, layout.widthProperty()));
    }

/***************************** PRECONFIGURED ACTIONS ******************************/

    public final ClassListMap<ActionData<?,?,?>> actions = new ClassListMap<>(null);

    public final <T> void register(Class<T> c, ActionData<T,?,?> action) {
        actions.accumulate(c, action);
    }

    @SafeVarargs
    public final <T> void register(Class<T> c, ActionData<T,?,?>... action) {
        actions.accumulate(c, listRO(action));
    }

/************************************ CONTROLS ************************************/

    private final Icon helpI = createInfoIcon(
        "Action chooser"
      + "\n"
      + "\nChoose an action. It may use some input data. Data not immediatelly ready will "
      + "display progress indicator."
    );
    private final Icon hideI = new CheckIcon(closeOnDone)
                                    .tooltip(COD_TITLE+"\n\n"+COD_INFO)
                                    .icons(CLOSE_CIRCLE_OUTLINE, CHECKBOX_BLANK_CIRCLE_OUTLINE);
    private final ProgressIndicator dataObtainingProgress = new Spinner(1){{
        maintain(progressProperty(),p -> p.doubleValue()<1, visibleProperty());
    }};
    private final ProgressIndicator actionProgress = new Spinner(1){{
        maintain(progressProperty(),p -> p.doubleValue()<1, visibleProperty());
    }};
    private final HBox controls = layHorizontally(5,CENTER_RIGHT, actionProgress,dataObtainingProgress,hideI,helpI);

/************************************ DATA ************************************/

    private boolean use_registered_actions = true;
    private Object data;
    private List<ActionData> iactions;
    private final List<ActionData> dactions = new ArrayList<>();

    @Override
    public void show() {
        setData(data);
        super.show();
    }

    public final void show(Object value) {
        value = collectionUnwrap(value);
        Class c = value==null ? Void.class : value.getClass();
        show(c, value);
    }

    public final <T> void show(Class<T> type, T value) {
        no(value==null && (type!=Void.class || type!=void.class));
        show(type, value, false);
    }

    public final <T> void show(Class<T> type, T value, boolean exclusive, ActionData<?,?,?>... actions) {
        data = value;
        iactions = list(actions);
        use_registered_actions = !exclusive;
        show();
    }

    @SafeVarargs
    public final <T> void show(Class<T> type, Fut<T> value, boolean exclusive, SlowAction<T,?>... actions) {
        data = value;
        iactions = list(actions);
        use_registered_actions = !exclusive;
        show();
    }

    private void doneHide() {
        if(closeOnDone.get()) hide();
    }

/********************************** GRAPHICS **********************************/

    private final Label dataInfo = new Label();
    private final Label desctitl = new Label();
    private final Text descfull = new Text();
    private final VBox desc = layVertically(8, BOTTOM_CENTER, desctitl,descfull);
    private final ObservableList<Node> icons;
    private StackPane tablePane = new StackPane();
    private FilteredTable<?,?> table;

/*********************************** HELPER ***********************************/

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
        boolean dataready = !(data instanceof Fut && !((Fut)data).isDone());
        if(dataready) {
            data = futureUnwrap(data);
            setDataInfo(data, true);
            showIcons(data);
        } else {
            setDataInfo(null, false);
            // obtain data & invoke again
            Fut<Object> f = ((Fut)data)
                    .use(this::setData,FX)
                    .showProgress(dataObtainingProgress);
            f.run();
            data = f;
        }
    }

    private void setActionInfo(ActionData<?,?,?> a) {
        desctitl.setText(a==null ? "" : a.name);
        descfull.setText(a==null ? "" : a.description);
    }

    private void setDataInfo(Object data, boolean computed) {
        dataInfo.setText(getDataInfo(data, computed));
        tablePane.getChildren().clear();
        if(data instanceof Collection && !((Collection)data).isEmpty()) {
            Class coltype = ((Collection<?>)data).stream().findFirst().map(Object::getClass).orElse(Void.class);
            if(fieldmap.containsKey(coltype)) {
                FilteredTable<Object,?> t = new gui.objects.table.FilteredTable<>((ObjectField)getEnumConstants(fieldmap.get(coltype))[0]);
                t.setFixedCellSize(gui.GUI.font.getValue().getSize() + 5);
                t.getSelectionModel().setSelectionMode(MULTIPLE);
                t.setColumnFactory(f -> {
                    TableColumn<?,?> c = new TableColumn(f.toString());
                    c.setCellValueFactory(cf -> cf.getValue()== null ? null : new PojoV(f.getOf(cf.getValue())));
                    c.setCellFactory(col -> (TableCell)defaultCell(f));
                    c.setResizable(true);
                    return (TableColumn)c;
                });
                t.setColumnState(t.getDefaultColumnInfo());
                tablePane.getChildren().setAll(t.getRoot());
                table = t;
                t.setItemsRaw((Collection)data);
                t.getSelectedItems().addListener((Change<?> c) -> showIcons(t.getSelectedOrAllItemsCopy()));
            }
        }
    }

    private String getDataInfo(Object data, boolean computed) {
        Class<?> type = data==null ? Void.class : data.getClass();
        if(Void.class.equals(type)) return "";

        Object d = computed ? data instanceof Fut ? ((Fut)data).getDone() : data : null;
        String dname = computed ? APP.instanceName.get(d) : "n/a";
        String dkind = computed ? APP.className.get(type) : "n/a";
        String dinfo = APP.instanceInfo.get(d).entrySet().stream()
                          .map(e -> e.getKey() + ": " + e.getValue()).sorted().collect(joining("\n"));
        if(!dinfo.isEmpty()) dinfo = "\n" + dinfo;

        return "Data: " + dname + "\nType: " + dkind + dinfo ;
    }

    private void showIcons(Object d) {
        Class<?> dt = d==null ? Void.class : d instanceof Collection ? ((Collection)d).stream().findFirst().orElse(null).getClass() : d.getClass();
        // get suitable actions
        dactions.clear();
        dactions.addAll(iactions);
        if(use_registered_actions) dactions.addAll(actions.getElementsOfSuperV(dt));
        dactions.removeIf(a -> {
            if(a.groupApply==FOR_ALL) {
                return a.condition.test(collectionWrap(d));
            }
            if(a.groupApply==FOR_EACH) {
                List ds = list(d instanceof Collection ? (Collection)d : listRO(d));
                return ds.stream().noneMatch(a.condition);
            }
            if(a.groupApply==NONE) {
                Object o = collectionUnwrap(d);
                return o instanceof Collection ? true : !a.condition.test(o);
            }
            throw new RuntimeException("Illegal switch case");
        });

        icons.setAll(dactions.stream().sorted(by(a -> a.name)).map(a -> {
            Icon i = new Icon()
                  .icon(a.icon)
                  .styleclass(ICON_STYLECLASS)
                  .onClick(() -> {
                      if (a instanceof FastActionBase) {
                          ((FastActionBase)a).run(d,d instanceof Collection);
                          doneHide();
                      }
                      if (a instanceof SlowActionBase) {
                          Fut<?> datafut = fut(d);
                          futAfter(datafut)
                                .then(() -> System.out.println("fut begin"))
                                .then(() -> actionProgress.setProgress(-1),FX)
                                .then(() -> System.out.println("fut begin progress done"))
                                .thenChain((Ƒ1)a.action)
                                .then(() -> System.out.println("fut begin action done"))
                                .then(() -> actionProgress.setProgress(1),FX)
                                .then(() -> System.out.println("fut begin p done"))
                                .then(this::doneHide,FX);
                      }
                   });
                 i.addEventHandler(MOUSE_ENTERED, e -> setActionInfo(a));
                 i.addEventHandler(MOUSE_EXITED, e -> setActionInfo(null));
            return i.withText(a.name);
        }).collect(toList()));
        // animate
        Anim.par(icons, (i,icon) -> new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(350+i*200))
            .play();
    }



    private static Collection<?> collectionWrap(Object o) {
        if(o instanceof Collection) {
            return (Collection)o;
        } else {
            return listRO(o);
        }
    }
    private static Object collectionUnwrap(Object o) {
        if(o instanceof Collection) {
            Collection c = (Collection)o;
            if(c.isEmpty()) return null;
            if(c.size()==1) return c.stream().findAny().get();
        }
        return o;
    }

    private static Object futureUnwrap(Object o) {
        return o instanceof Fut ? ((Fut)o).getDone() : o;
    }


    /** Action. */
    public static abstract class ActionData<C,T,F> {
        public final String name;
        public final String description;
        public final GlyphIcons icon;
        public final Predicate<? super T> condition;
        public final GroupApply groupApply;
        public final F action;

        private ActionData(String name, String description, GlyphIcons icon, GroupApply group, Predicate<? super T> constriction, F action) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.condition = constriction;
            this.groupApply = group;
            this.action = action;
        }

        Object run(Object data, boolean isCollection){
            return data; // do nothing
        };

        Object run(Object data){
            if(groupApply!=NONE && !(data instanceof Collection)) data=listRO(data); // wrap into collection
            return run(data, data instanceof Collection);
        };

        public boolean isColl() {
            return groupApply==FOR_ALL;
        }

        public boolean canUse(Object o) {
            return o instanceof Collection ? groupApply!=NONE : groupApply!=FOR_ALL;
        }

        public Object maptToUsable(Object o) {
            if(groupApply==FOR_ALL) {
                return o instanceof Collection ? o : listRO(o);
            }
            if(groupApply==FOR_EACH) {
                return o;
            }
            if(groupApply==NONE) {
                if(o instanceof Collection) throw new RuntimeException("Action can not use collection");
                else return o;
            }
            throw new RuntimeException("Illegal switch case");
        }
    }

    /** Action that executes synchronously - simply consumes the input. */
    private static class FastActionBase<C,T> extends ActionData<C,T,Consumer<T>> {

        private FastActionBase(String name, String description, GlyphIcons icon, GroupApply groupApply, Predicate<? super T> constriction, Consumer<T> act) {
            super(name, description, icon, groupApply, constriction, act);
        }

        @Override
        public Object run(Object data, boolean isCollection) {
            if(isCollection && groupApply==FOR_EACH) {
                for(T t : (Collection<T>)data)
                    action.accept(t);
            } else {
                action.accept((T) (groupApply==NONE ? collectionUnwrap(data) : data));
            }
            return data;
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
    private static class SlowActionBase<C,T,R> extends ActionData<C,T,Ƒ1<Fut<T>,Fut<R>>> {

        public SlowActionBase(String name, String description, GlyphIcons icon, GroupApply groupally, Predicate<? super T> constriction, Ƒ1<Fut<T>,Fut<R>> act) {
            super(name, description, icon, groupally, constriction, act);
        }

    }
    /** SlowAction that processes simple input - its type is the same as type of the action. */
    public static class SlowAction<T,R> extends SlowActionBase<T,T,R> {

        public SlowAction(String name, String description, GlyphIcons icon, Ƒ1<Fut<T>,Fut<R>> act) {
            super(name, description, icon, NONE, IS, act);
        }

        public SlowAction(String name, String description, GlyphIcons icon, GroupApply groupally, Ƒ1<Fut<T>,Fut<R>> act) {
            super(name, description, icon, groupally, IS, act);
        }

    }
    /** SlowAction that processes collection input - its input type is collection of its type. */
    public static class SlowColAction<T,R> extends SlowActionBase<T,Collection<T>,R> {

        public SlowColAction(String name, String description, GlyphIcons icon, Ƒ1<Fut<Collection<T>>, Fut<R>> act) {
            super(name, description, icon, FOR_ALL, ISNT, act);
        }

        public SlowColAction(String name, String description, GlyphIcons icon, Predicate<? super T> constriction, Ƒ1<Fut<Collection<T>>,Fut<R>> act) {
            super(name, description, icon, FOR_ALL, c -> c.stream().noneMatch(constriction), act);
        }

    }

    public static enum GroupApply {
        FOR_EACH,
        FOR_ALL,
        NONE;
    }
}