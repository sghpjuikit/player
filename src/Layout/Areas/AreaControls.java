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
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.COGS;
import static de.jensd.fx.fontawesome.AwesomeIcon.COLUMNS;
import static de.jensd.fx.fontawesome.AwesomeIcon.EXTERNAL_LINK;
import static de.jensd.fx.fontawesome.AwesomeIcon.INFO;
import static de.jensd.fx.fontawesome.AwesomeIcon.LOCK;
import static de.jensd.fx.fontawesome.AwesomeIcon.REFRESH;
import static de.jensd.fx.fontawesome.AwesomeIcon.TH_LARGE;
import static de.jensd.fx.fontawesome.AwesomeIcon.TIMES;
import static de.jensd.fx.fontawesome.AwesomeIcon.UNLOCK;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import javafx.util.Duration;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
public final class AreaControls {
    
    private static PopOver<Text> helpPopOver; // we only need one instance for all areas
    
    @FXML public AnchorPane root = new AnchorPane();
    @FXML public Region deactivator;
    @FXML public Region deactivator2;
    @FXML public BorderPane header;
    @FXML public Label title;
    public Label propB;
    @FXML TilePane header_buttons;
    
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
        Label infoB = AwesomeDude.createIconLabel(INFO,"","12","12",ContentDisplay.RIGHT);
              infoB.setTooltip(new Tooltip("Help"));
              infoB.setOnMouseClicked( e -> {
                   // create popover lazily if not yet
                   if(helpPopOver==null) {
                       helpPopOver = PopOver.createHelpPopOver("");
                       // we need to handle hiding this AreaControls when popup
                       // closes and 
                       // we are outside of the area (not implemented yet)
                       helpPopOver.addEventHandler(WINDOW_HIDDEN, we -> {
                           if(isShowingWeak) hide();
                       });
                   }
                   // update text
                   Component c = area.getActiveComponent();
                   String info = "";
                   if (c!=null && c instanceof Widget) {
                       WidgetInfo i = ((Widget)c).getInfo();
                       info = "\n\nWidget: " + i.name();
                       if(!i.description().isEmpty()) info += "\n\n" + i.description();
                       if(!i.howto().isEmpty()) info += "\n\n" + i.howto();
                       if(!i.notes().isEmpty()) info += "\n\n" + i.notes();
                   }
                   String text = "Available actions:\n"
                                + "    Close : Closes the widget\n"
                                + "    Detach : Opens the widget in new window\n"
                                + "    Change : Opens widget chooser to pick new widget\n"
                                + "    Settings : Opens settings for the widget if available\n"
                                + "    Refresh : Refreshes the widget\n"
                                + "    Lock : Forbids entering layout mode on mouse hover\n"
                                + "    Press ALT : Toggles layout mode\n"
                                + "\n"
                                + "Available actions in layout mode:\n"
                                + "    Drag & Drop : Drags widget to other area\n"
                                + "    Sroll : Changes widget area size\n"
                                + "    Middle click : Set widget area size to max\n"
                                + info;
                   helpPopOver.getContentNode().setText(text);
                   // for some reason we need to set this every time, which
                   // should not be the case, investigate
                   helpPopOver.getContentNode().setWrappingWidth(400);
                   helpPopOver.show(infoB);
                   e.consume();
              });
        Label closeB = AwesomeDude.createIconLabel(TIMES,"","12","12",ContentDisplay.RIGHT);
               closeB.setTooltip(new Tooltip("Close widget"));
               closeB.setOnMouseClicked( e -> {
                   close();
                   e.consume();
               });
        Label detachB = AwesomeDude.createIconLabel(EXTERNAL_LINK,"","12","12",ContentDisplay.RIGHT);
               detachB.setTooltip(new Tooltip("Detach widget to own window"));
               detachB.setOnMouseClicked( e -> {
                   detach();
                   e.consume();
               });
        Label changeB = AwesomeDude.createIconLabel(TH_LARGE,"","12","12",CENTER);
               changeB.setTooltip(new Tooltip("Change widget"));
               changeB.setOnMouseClicked( e -> {
                   choose();
                   e.consume();
               });
               propB = AwesomeDude.createIconLabel(COGS,"","12","12",CENTER);
               propB.setTooltip(new Tooltip("Settings"));
               propB.setOnMouseClicked( e -> {
                   settings();
                   e.consume();
               });
        Label lockB = AwesomeDude.createIconLabel(area.isLocked() ? UNLOCK : LOCK,"","12","12",CENTER);
               lockB.setTooltip(new Tooltip(area.isLocked() ? "Unlock widget layout" : "Lock widget layout"));
               lockB.setOnMouseClicked( e -> {
                   toggleLocked();
                   AwesomeDude.setIcon(lockB,area.isLocked() ? UNLOCK : LOCK,"12");
                   lockB.getTooltip().setText(area.isLocked() ? "Unlock widget layout" : "Lock widget layout");
                   e.consume();
               });
        Label refreshB = AwesomeDude.createIconLabel(REFRESH,"","12","12",CENTER);
               refreshB.setTooltip(new Tooltip("Refresh widget"));
               refreshB.setOnMouseClicked( e -> {
                   refreshWidget();
                   e.consume();
               });
        Label absB = AwesomeDude.createIconLabel(COLUMNS,"","12","12",CENTER);
              absB.setTooltip(new Tooltip("Toggle absolute size"));
              absB.setOnMouseClicked( e -> {
                  toggleAbsSize();
                  e.consume();
              });
        
        // build header
        header_buttons.getChildren().addAll(closeB,detachB,changeB,propB,refreshB,lockB,absB,infoB);
        
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
        area.activator.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
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
                    (helpPopOver==null || !helpPopOver.isShowing())
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
            if(isShowingWeak && !isShowingStrong && (helpPopOver==null || !helpPopOver.isShowing()))
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
        
        SimpleConfigurator sc = new SimpleConfigurator(w,c->w.getController().refresh());
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
        if(c!=null && c instanceof BiContainer)
            BiContainer.class.cast(c).getGraphics().toggleAbsoluteSize();
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
        if(helpPopOver!=null && helpPopOver.isShowing()) helpPopOver.hide();        
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
