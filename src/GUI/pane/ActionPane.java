/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import java.util.List;
import java.util.function.Consumer;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import action.Action;
import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.Text;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.spinner.Spinner;
import main.App;
import util.access.Ѵ;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.future.Fut;
import util.collections.map.ClassListMap;
import util.functional.Functors.Ƒ1;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static gui.objects.icon.Icon.createInfoIcon;
import static gui.pane.OverlayPane.CONTENT_STYLECLASS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.*;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static util.Util.GR;
import static util.async.Async.FX;
import static util.async.future.Fut.fut;
import static util.async.future.Fut.futAfter;
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
public class ActionPane extends OverlayPane {

    private static final String ROOT_STYLECLASS = "action-pane";
    private static final String ICON_STYLECLASS = "action-pane-action-icon";
    private static final String COD_INFO_SHRT = "Close when action ends";
    private static final String COD_INFO_LONG = "Closes the chooser when action is picked and "
                                              + "finishes running.";

    @IsConfig(name = COD_INFO_SHRT, info = COD_INFO_LONG)
    public static final Ѵ<Boolean> closeOnDone = new Ѵ(false);
    public static final ClassListMap<FastAction> ACTIONS = new ClassListMap<>(null);

    public static void register(Class c, FastAction action) {
        ACTIONS.accumulate(c, action);
    }

    public static void register(Class c, FastAction... action) {
        ACTIONS.accumulate(c, listRO(action));
    }


    public ActionPane() {
        getStyleClass().add(ROOT_STYLECLASS);

        // layout
        StackPane layout = layStack(
            desc, BOTTOM_CENTER,
            layVertically(10, TOP_CENTER, controls,dataInfo), TOP_CENTER,
            icons, CENTER
        );
        layout.setMaxSize(GR*450,450);
        layout.getStyleClass().add(CONTENT_STYLECLASS);
        getChildren().addAll(layout);

        icons.setMaxHeight(layout.getMaxHeight()-2*30); // top/bottom padding for clickable controls
        desc.setMouseTransparent(true); // just in case, if it is under icons all's well
        dataInfo.setMouseTransparent(true); // same here
        desctitl.setTextAlignment(TextAlignment.CENTER);
        descfull.setTextAlignment(TextAlignment.JUSTIFY);
        descfull.wrappingWidthProperty().bind(min(350, layout.widthProperty()));
    }

/************************************ CONTROLS ************************************/

    private final Icon helpI = createInfoIcon(
        "Action chooser"
      + "\n"
      + "\nChoose an action. It may use some input data. Data not immediatelly ready will "
      + "display progress indicator."
    );
    private final Icon hideI = new CheckIcon(closeOnDone)
                                    .tooltip(COD_INFO_SHRT+"\n\n"+COD_INFO_LONG)
                                    .icons(CHECKBOX_BLANK_CIRCLE_OUTLINE, CLOSE_CIRCLE_OUTLINE);
    private final ProgressIndicator dataObtainingProgress = new Spinner(1){{
        maintain(progressProperty(),p -> p.doubleValue()<1, visibleProperty());
    }};
    private final ProgressIndicator actionProgress = new Spinner(1){{
        maintain(progressProperty(),p -> p.doubleValue()<1, visibleProperty());
    }};
    private final HBox controls = layHorizontally(5,CENTER_RIGHT, actionProgress,dataObtainingProgress,hideI,helpI);

/************************************ DATA ************************************/

    private Object data;
    private Class dtype;
    private List<ActionData> dactions;

    public <T> void show(Class<T> type, T value, ActionData<?,?>... actions) {
        data = value;
        dtype = type;
        dactions = list(actions);
        build();
        show();
    }

    public <T> void show(Class<T> type, Fut<T> value, SlowAction<T,?>... actions) {
        data = value;
        dtype = type;
        dactions = list(actions);
        build();
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
    private final HBox icons = layHorizontally(15,CENTER);

/*********************************** HELPER ***********************************/

    private void build() {
        dactions.addAll(ACTIONS.getElementsOfSuperV(dtype));
        // set content
        boolean dataready = !(data instanceof Fut && !((Fut)data).isDone());
        setDataInfo(data, dataready);
        setActionInfo(null);

        // if we've got a Future over here, start computation immediately (obtain data)
        // update data info when done
        if(!dataready) {
            Fut<Object> f = ((Fut)data)
                    .use(dat -> setDataInfo(dat, true),FX)
                    .showProgress(dataObtainingProgress);
            f.run();
            data = f;
        }

        icons.getChildren().setAll(dactions.stream().sorted(by(a -> a.name)).map(a -> {
            Icon i = new Icon()
                  .icon(a.icon)
                  .styleclass(ICON_STYLECLASS)
                  .onClick(() -> {
                      if (a instanceof FastAction) {
                          ((Consumer)a.action).accept(data);
                          doneHide();
                      } else {
                          Fut<?> datafut = data instanceof Fut ? (Fut)data : fut(data);
                          futAfter(datafut)
                                .then(() -> actionProgress.setProgress(-1),FX)
                                .then((Ƒ1)a.action)
                                .then(() -> actionProgress.setProgress(1),FX)
                                .then(this::doneHide,FX);
                      }
                   });
                 i.addEventHandler(MOUSE_ENTERED, e -> setActionInfo(a));
                 i.addEventHandler(MOUSE_EXITED, e -> setActionInfo(null));
            return i;
        }).collect(toList()));
        Anim.par(icons.getChildren(), (i,icon) -> new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(350+i*200))
            .play();
    }

    private void setActionInfo(ActionData a) {
        desctitl.setText(a==null ? "" : a.name);
        descfull.setText(a==null ? "" : a.description);
    }

    private void setDataInfo(Object data, boolean computed) {
        dataInfo.setText(getDataInfo(data, computed));
    }

    private String getDataInfo(Object data, boolean computed) {
        if(Void.class.equals(dtype)) return "";

        Object d = computed ? data instanceof Fut ? ((Fut)data).getDone() : data : null;
        String dname = computed ? App.instanceName.get(d) : "n/a";
        String dkind = App.className.get(dtype);
        String dinfo = App.instanceInfo.get(d).entrySet().stream()
                       .map(e -> e.getKey() + ": " + e.getValue()).sorted().collect(joining("\n"));
        if(!dinfo.isEmpty()) dinfo = "\n" + dinfo;

        return "Data: " + dname
             + "\nType: " + dkind
             + dinfo ;
    }


    public static abstract class ActionData<T,F> {
        public final String name;
        public final String description;
        public final GlyphIcons icon;
        public final F action;

        private ActionData(String name, String description, GlyphIcons icon, F action) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.action = action;
        }
    }
    public static class FastAction<T> extends ActionData<T,Consumer<T>> {
        public FastAction(String name, String description, GlyphIcons icon, Consumer<T> act) {
            super(name, description, icon, act);
        }
        public FastAction(GlyphIcons icon, Action action) {
            super(action.getName(),
                  action.getInfo() + (action.hasKeysAssigned() ? "\n\nShortcut keys: " + action.getKeys() : ""),
                  icon, ignored -> action.run());
        }
    }
    public static class SlowAction<T,R> extends ActionData<T,Ƒ1<Fut<T>,Fut<R>>> {
        public SlowAction(String name, String description, GlyphIcons icon, Ƒ1<Fut<T>,Fut<R>> act) {
            super(name, description, icon, act);
        }
    }

}