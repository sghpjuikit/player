/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import gui.objects.icon.Icon;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import static javafx.beans.binding.Bindings.min;
import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.TOP_CENTER;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import static javafx.util.Duration.millis;
import util.ClassName;
import static util.Util.setAnchors;
import util.animation.Anim;
import static util.functional.Util.list;

/**
 *
 * @author Plutonium_
 */
public class ActionPane extends StackPane {
    
    public static String ROOT_STYLECLASS = "action-pane";
    public static String CONTENT_STYLECLASS = "action-pane-content";
    public static String ICON_STYLECLASS = "action-pane-icon";
    public static final ActionPane PANE = new ActionPane();
    
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
        animation.playCloseAnd(this::animEnd);
    }
    
/*********************************** HELPER ***********************************/

    private void build() {
        // set content
        dataInfo.setText("Data: " + ClassName.get(o_type) + "\n" + Objects.toString(o));
        description.setText("");
        icons.getChildren().setAll(o_actions.stream().map(a -> {
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
        animation.playOpenAnd(null);
    }
    
    private void animDo(double x) {
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
