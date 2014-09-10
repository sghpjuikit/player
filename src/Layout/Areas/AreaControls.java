/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Areas;

import GUI.GUI;
import GUI.objects.Pickers.WidgetPicker;
import GUI.objects.PopOver.ContextPopOver;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import GUI.objects.SimpleConfigurator;
import GUI.objects.Text;
import Layout.BiContainer;
import Layout.Component;
import Layout.Container;
import Layout.Containers.Splitter;
import Layout.Widgets.Features.Feature;
import Layout.Widgets.Widget;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.COGS;
import static de.jensd.fx.fontawesome.AwesomeIcon.EXTERNAL_LINK_SQUARE;
import static de.jensd.fx.fontawesome.AwesomeIcon.INFO;
import static de.jensd.fx.fontawesome.AwesomeIcon.LINK;
import static de.jensd.fx.fontawesome.AwesomeIcon.LOCK;
import static de.jensd.fx.fontawesome.AwesomeIcon.REFRESH;
import static de.jensd.fx.fontawesome.AwesomeIcon.TH_LARGE;
import static de.jensd.fx.fontawesome.AwesomeIcon.TIMES_CIRCLE;
import static de.jensd.fx.fontawesome.AwesomeIcon.UNLINK;
import static de.jensd.fx.fontawesome.AwesomeIcon.UNLOCK;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import javafx.util.Duration;
import utilities.SingleInstance;
import utilities.Util;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
public final class AreaControls {
    
    // we only need one instance for all areas
    private static final SingleInstance<PopOver<Text>,AreaControls> helpP = new SingleInstance<>(
        () -> PopOver.createHelpPopOver(""),
        (p,ac) -> {
            Component c = ac.area.getActiveComponent();
            String info = "";
            if (c!=null && c instanceof Widget) {
                Widget w = Widget.class.cast(c);
                String f = (w.getController() instanceof Feature) ? Feature.class.cast(w.getController()).getFeatureName() : "-";
                info += "\n\nWidget: " + w.name();
                if(!w.author().isEmpty()) info += "\n\n" + w.author();
                if(!w.programmer().isEmpty()) info += "" + w.programmer();
                if(!w.contributor().isEmpty()) info += "" + w.contributor();
                info += "\n\nFeature API: " + f;
                if(!w.version().isEmpty()) info += "n" + w.version();
                if(!w.notes().isEmpty()) info += "" + w.notes();
                if(!w.howto().isEmpty()) info += "" + w.howto();
                if(w.group()!=Widget.Group.UNKNOWN) info += "" + w.group();
            }
            
            String text = "Available actions:\n"
                + "    Close : Closes the widget\n"
                + "    Detach : Opens the widget in new window\n"
                + "    Change : Opens widget chooser to pick new widget\n"
                + "    Settings : Opens settings for the widget if available\n"
                + "    Refresh : Refreshes the widget\n"
                + "    Lock : Forbids entering layout mode on mouse hover\n"
                + "    Press ALT : Toggles layout mode\n"
                + "    Drag & Drop header : Drags widget to other area\n"
                + "\n"
                + "Available actions in layout mode:\n"
                + "    Drag & Drop : Drags widget to other area\n"
                + "    Sroll : Changes widget area size\n"
                + "    Middle click : Set widget area size to max\n"
                + info;
            p.getContentNode().setText(text);
            // for some reason we need to set this every time, which
            // should not be the case, investigate
//            p.getContentNode().setWrappingWidth(400);
            // we need to handle hiding this AreaControls when popup
            // closes and 
            // we are outside of the area (not implemented yet)
            p.addEventHandler(WINDOW_HIDDEN, we -> {
                if(ac.isShowingWeak) ac.hide();
            });
        });
    
    @FXML public AnchorPane root = new AnchorPane();
    @FXML public Region deactivator;
    @FXML public Region deactivator2;
    @FXML public BorderPane header;
    @FXML public Label title;
    public Label propB;
    @FXML TilePane header_buttons;
    Label absB;
    
    // animations // dont initialize here or make final
    private final FadeTransition contrAnim;
    private final FadeTransition contAnim;
    private final TranslateTransition blurAnim;
    
    Area area;
    
    public AreaControls(Area area) {
        this.area = area;

        // load graphics
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("AreaControls.fxml"));
                       loader.setRoot(root);
                       loader.setController(this);
                       loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        
        // avoid clashing of title and control buttons for small root size
        header_buttons.maxWidthProperty()
                .bind(root.widthProperty().subtract(title.widthProperty())
                .divide(2).subtract(15));
        header_buttons.setMinWidth(15);

        // build header buttons
        Label infoB = Util.createIcon(INFO,12,"Help",null);
              infoB.setOnMouseClicked( e ->  {
                  helpP.get(this).show(infoB);
                  Action.Action.actionStream.push("Widget info");
              });
        Label closeB = Util.createIcon(TIMES_CIRCLE,12,"Close widget", e -> {
                  close();
                  Action.Action.actionStream.push("Close widget");
              });
        Label detachB = Util.createIcon(EXTERNAL_LINK_SQUARE,12,"Detach widget to own window", e -> detach());
        Label changeB = Util.createIcon(TH_LARGE,12,"Change widget",e -> choose());
        propB = Util.createIcon(COGS,12,"Settings",e -> settings());
        Label lockB = Util.createIcon(area.isLocked() ? UNLOCK : LOCK,12,
                area.isLocked() ? "Unlock widget layout" : "Lock widget layout",null);
              lockB.setOnMouseClicked(e -> {
                   toggleLocked();
                   AwesomeDude.setIcon(lockB,area.isLocked() ? UNLOCK : LOCK,"12");
                   lockB.getTooltip().setText(area.isLocked() ? "Unlock widget layout" : "Lock widget layout");
               });
        Label refreshB = Util.createIcon(REFRESH,12,"Refresh widget",e -> refreshWidget());
        absB = Util.createIcon(LINK,12,"Toggle absolute size",e -> {
            toggleAbsSize();
            updateAbsB();
        });
        
        // build header
        header_buttons.getChildren().addAll(closeB,detachB,changeB,propB,refreshB,lockB,absB,infoB);
        header_buttons.getChildren().forEach(c->{
            ((Label)c).setMaxSize(13, 13);
            ((Label)c).setPrefSize(13, 13);
            ((Label)c).setMinSize(13, 13);
        });
        
        // build animations
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), root);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), area.getContent());
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), area.getContent());
        BoxBlur blur = new BoxBlur(0, 0, 1);
        blur.widthProperty().bind(area.getContent().translateZProperty());
        blur.heightProperty().bind(area.getContent().translateZProperty());
        area.getContent().setEffect(blur);
        
        // weak mode and strong mode - strong mode is show/hide called from external code
        // - weak mode is show/hide by mouse enter/exit events in the corner (activator/deactivator)
        // - the weak behavior must not work in strong mode
        
        // show when area is hovered and in weak mode
        area.activator.addEventFilter(MOUSE_ENTERED, e -> {
            // avoid when locked and in strong mode
            if (!area.isUnderLock() && !isShowingStrong && 
                    // avoid pointless operation
                    !isShowingWeak)
                showWeak();
        });
        // hide when no longer hovered and in weak mode
        EventHandler<MouseEvent> closer = e -> {
            // avoid when locked and in strong mode
            if(!area.isUnderLock() && !isShowingStrong && 
                // avoid pointless operation
                isShowingWeak && 
                    // but not when helpPoOver is visible
                    // mouse entering the popup qualifies as root.mouseExited which we need
                    // to avoid
                    // now we need to handle hiding when popup closes
                    (helpP.isNull() || !helpP.get().isShowing())
                        // hide when none of the deactivators are hovered
                        // need to translate the coordinates to scene-relative
                        && !deactivator.localToScene(deactivator.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())
                            && !deactivator2.localToScene(deactivator2.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY()))
                hideWeak();
            
        };
        deactivator.setOnMouseExited(closer);
        deactivator2.setOnMouseExited(closer);
        
        // hide on mouse exit from area
        // same thing as above - need to take care of popup
        // theoretically not needed but sometimes mouse exited deactivator does
        // not get captured in fast movement. This additional handler fixes
        // the issue
        area.root.addEventFilter(MOUSE_EXITED, e -> {
            if(isShowingWeak && !isShowingStrong && (helpP.isNull() || !helpP.get().isShowing()))
                hide();
        });
        
        // enlarge deactivator that resizes with control buttons to give more
        // room for mouse movement
        deactivator.setScaleX(1.2);
        deactivator.setScaleY(1.2);
    }

    void refreshWidget() {
        area.refresh();
    }

    void toggleLocked() {
        area.toggleLocked();
        area.activator.setMouseTransparent(area.isUnderLock());
    }

    void settings() {
        if(area.getActiveComponents().isEmpty()) return;
        Widget w = (Widget) area.getActiveComponents().get(0);
        
        SimpleConfigurator sc = new SimpleConfigurator(w);
        PopOver p = new PopOver(sc);
                p.setTitle(w.getName() + " Settings");
                p.setAutoFix(false);
                p.setAutoHide(true);
                p.show(propB);  
    }

    // this method is basiclly the main source of new components
    void choose() {
        WidgetPicker w = new WidgetPicker();
        ContextPopOver pp = new ContextPopOver(w.getNode());
        w.setOnSelect(factory -> {
            // area has full responsibility over adding the newly created component
            // to the layout graph, but note that it is role of a container so
            // each area implementation should delegate to container
            // NOTE: this should be actually all implemented here like this:
            // get first empty index for new component -> container.add(...)
            area.add(factory.create());
            pp.hide();
        });
        pp.show(propB,NodeCentricPos.Center);
    }
    
    void detach() {
        area.detach();
    }
    
    void close() {
        area.container.close();
    }
    
    private void toggleAbsSize() {
        Container c = area.container.getParent();
        if(c!=null && c instanceof BiContainer) {
            Splitter s = BiContainer.class.cast(c).getGraphics();
            s.toggleAbsoluteSizeFor(area.container.indexInParent());
        }
    }
    private void updateAbsB() {
        Container c = area.container.getParent();
        if(c!=null && c instanceof BiContainer) {
            boolean l = c.properties.getI("abs_size")==area.container.indexInParent();
            AwesomeDude.setIcon(absB, l ? UNLINK : LINK, "12");
            if(!header_buttons.getChildren().contains(absB))
                header_buttons.getChildren().add(6,absB);
        } else {
            header_buttons.getChildren().remove(absB);
        }
    }
    
    private void showWeak() {
        //set state
        isShowingWeak = true;
        // stop animations if active
        contrAnim.stop();
        contAnim.stop();
        blurAnim.stop();
        // set new values
        contrAnim.setToValue(1);
        contAnim.setToValue(1-GUI.opacity_LM);
        blurAnim.setToZ(GUI.blur_LM);
        // play
        contrAnim.play();
        if(GUI.opacity_layoutMode) contAnim.play();
        if(GUI.blur_layoutMode) blurAnim.play();
        // handle graphics
        area.disableContent();
        root.setMouseTransparent(false);
        updateAbsB();
    }
    
    private void hideWeak() {
        isShowingWeak = false;
        contrAnim.stop();
        contAnim.stop();
        blurAnim.stop();
        contrAnim.setToValue(0);
        contAnim.setToValue(1);
        blurAnim.setToZ(0);
        contrAnim.play();
        if(GUI.opacity_layoutMode)  contAnim.play();
        if(GUI.blur_layoutMode) blurAnim.play();
        area.enableContent();
        root.setMouseTransparent(true);
        // hide help popup if open
        if(!helpP.isNull() && helpP.get().isShowing()) helpP.get().hide();        
    }
    
    public void show() {
        //set state
        isShowingStrong = true;
        showWeak();
    }
    
    public void hide() {
        //set state
        isShowingStrong = false;
        hideWeak();
    }
    
    private boolean isShowingStrong = false;
    private boolean isShowingWeak = false;
    public boolean isShowing() {
        return isShowingStrong;
    }
    public boolean isShowingWeak() {
        return isShowingWeak;
    }
    
}
