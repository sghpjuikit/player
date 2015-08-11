/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import gui.objects.icon.Icon;
import main.App;
import util.animation.Anim;
import util.collections.map.ClassListMap;

import static java.util.stream.Collectors.toList;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.*;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static util.Util.setAnchors;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class ActionPane extends StackPane {
    
    public static String ROOT_STYLECLASS = "action-pane";
    public static String CONTENT_STYLECLASS = "action-pane-content";
    public static String ICON_STYLECLASS = "action-pane-icon";
    public static final ActionPane PANE = new ActionPane();
    public static final ClassListMap<ActionData> ACTIONS = new ClassListMap<>(null);
    
    public static void register(Class c, ActionData action) {
        ACTIONS.accumulate(c, action);
    }
    
    public static void register(Class c, ActionData... action) {
        ACTIONS.accumulate(c, listRO(action));
    }
    
    
    public ActionPane() {
        setVisible(false);
        
        setEffect(blurfront);
        getStyleClass().add(ROOT_STYLECLASS);
        setOnMouseClicked(e -> {
            if(e.getButton()==SECONDARY)
                hide();
        });
        
        
        StackPane layout = new StackPane(dataInfo,icons,description);
                  layout.setMaxSize(600,400);
                  layout.getStyleClass().add(CONTENT_STYLECLASS);
        getChildren().addAll(layout);
        StackPane.setAlignment(dataInfo, TOP_CENTER);
        StackPane.setAlignment(icons, CENTER);
        StackPane.setAlignment(description, BOTTOM_CENTER);
        
        
        description.setTextAlignment(TextAlignment.CENTER);
        description.wrappingWidthProperty().bind(min(350, layout.widthProperty()));
        icons.setAlignment(CENTER);
    }
    
    
/************************************ DATA ************************************/
    
    private Object o;
    private Class o_type;
    private List<ActionData> o_actions;
    
    public <T> void show(Class<T> type, T value, ActionData<T>... actions) {
        o = value;
        o_type = type;
        o_actions = list(actions);
        build();
    }
    
    public <T> void show(Class<T> type, Supplier<T> value, ActionData<Supplier<T>>... actions) {
        o = value;
        o_type = type;
        o_actions = list(actions);
        build();
    }
    
/********************************** GRAPHICS **********************************/

    private final Label dataInfo = new Label();
    private final Text description = new Text();
    private final HBox icons = new HBox(15);
    
    
    public void hide() {
        animation.playCloseDo(this::animEnd);
    }
    
/*********************************** HELPER ***********************************/

    private void build() {
        o_actions.addAll(ACTIONS.getElementsOfSuperV(o_type));
        // set content
        String iname = o instanceof Supplier ? "n/a" : App.instanceName.get(o);
        String di = "Data: " + iname + "\nType: " + App.className.get(o_type);
        dataInfo.setText(o_type.equals(Void.class) ? "" : di);
        description.setText("");
        icons.getChildren().setAll(o_actions.stream().sorted(by(ad -> ad.name)).map(a -> {
            String d = a.name + "\n\n" + a.description;
            Icon i = new Icon().icon(a.icon)
                               .styleclass(ICON_STYLECLASS)
                               .onClick(() -> a.action.accept(o));
                 i.addEventHandler(MOUSE_ENTERED, e -> description.setText(d));
                 i.addEventHandler(MOUSE_EXITED, e -> description.setText(""));
            return i;
        }).collect(toList()));
        
        animStart();
    }
    
    BoxBlur blurback = new BoxBlur(0,0,3);
    BoxBlur blurfront = new BoxBlur(0,0,3);
    AnchorPane bgr;
    // depth of field transition
    Anim animation = new Anim(millis(350),this::animDo).intpl(x->x*x);
    
    private void animStart() {
        // attach to scenegraph
        AnchorPane root = Window.getActive().root;
        if(!root.getChildren().contains(this)) {
            root.getChildren().add(this);
            setAnchors(this,0);
            toFront();
        }
        // show
        setVisible(true);
        bgr = Window.getActive().content;
        Window.getActive().front.setEffect(blurback);
        Window.getActive().back.setEffect(blurback);
        animation.playOpenDo(null);
    }
    
    private void animDo(double x) {
        bgr.setOpacity(1-x*0.5);
        setOpacity(x);
        // unfocus bgr
        blurback.setHeight(15*x*x);
        blurback.setWidth(15*x*x);
        bgr.setScaleX(1-0.02*x);
        bgr.setScaleY(1-0.02*x);
        // focus this
        blurfront.setHeight(28*(1-x*x));
        blurfront.setWidth(28*(1-x*x));
        // zoom in effect - make it appear this pane comes from the front
        setScaleX(1+2*(1-x));
        setScaleY(1+2*(1-x));
    }
    
    private void animEnd() {
        setVisible(false);
        Window.getActive().content.setEffect(null);
        Window.getActive().header.setEffect(null);
        bgr=null;
    }
    
    
    
    public static class ActionData<T> {
        public final String name;
        public final String description;
        public final GlyphIcons icon;
        public final Consumer<T> action; 

        public ActionData(String name, String description, GlyphIcons icon, Consumer<T> action) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.action = action;
        }
    }
    
}
