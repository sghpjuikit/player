/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Areas;

import GUI.GUI;
import GUI.objects.Pickers.WidgetPicker;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import GUI.objects.SimpleConfigurator;
import Layout.Widgets.Widget;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.COGS;
import static de.jensd.fx.fontawesome.AwesomeIcon.EXTERNAL_LINK;
import static de.jensd.fx.fontawesome.AwesomeIcon.INFO;
import static de.jensd.fx.fontawesome.AwesomeIcon.LIST;
import static de.jensd.fx.fontawesome.AwesomeIcon.LOCK;
import static de.jensd.fx.fontawesome.AwesomeIcon.REFRESH;
import static de.jensd.fx.fontawesome.AwesomeIcon.TIMES;
import static de.jensd.fx.fontawesome.AwesomeIcon.UNLOCK;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import javafx.util.Duration;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
public final class AreaControls {
    
    private static PopOver helpPopOver; // we only need one instance for all areas
    
    @FXML public AnchorPane root = new AnchorPane();
    @FXML public Region deactivator;
    @FXML public BorderPane header;
    @FXML public Label title;
    public  Button propB;
    @FXML HBox header_buttons;
    
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
        
        // build header buttons
        Button helpB = AwesomeDude.createIconButton(INFO,"","12","12",CENTER);
               helpB.setTooltip(new Tooltip("Help"));
               helpB.setOnMouseClicked( e -> {
                   if(helpPopOver==null) {
                       String text = "Available buttons:\n"
                                   + "    Close : closes the widget\n"
                                   + "    Detach : opens the widget in new window\n"
                                   + "    Change : opens widget chooser to pick new widget\n"
                                   + "    Settings : opens settings for the widget if available\n"
                                   + "    Refresh : refreshes the widget\n"
                                   + "    Lock : forbids entering layout mode on mouse hover\n"
                                   + "           in the top right corner";
                       helpPopOver = PopOver.createHelpPopOver(text);
                       // we need to handle hiding this AreaControls when popup
                       // closes and 
                       // we are outside of the area (not implemented yet)
                       helpPopOver.addEventHandler(WINDOW_HIDDEN, we -> {
                           if(isShowingStrong) hide();
                       });
                   }
                   helpPopOver.show(helpB);
                   e.consume();
               });
        Button closeB = AwesomeDude.createIconButton(TIMES,"","12","12",CENTER);
               closeB.setTooltip(new Tooltip("Close widget"));
               closeB.setOnMouseClicked( e -> {
                   close();
                   e.consume();
               });
        Button detachB = AwesomeDude.createIconButton(EXTERNAL_LINK,"","12","12",CENTER);
               detachB.setTooltip(new Tooltip("Detach widget to own window"));
               detachB.setOnMouseClicked( e -> {
                   detach();
                   e.consume();
               });
        Button changeB = AwesomeDude.createIconButton(LIST,"","12","12",CENTER);
               changeB.setTooltip(new Tooltip("Change widget"));
               changeB.setOnMouseClicked( e -> {
                   choose();
                   e.consume();
               });
               propB = AwesomeDude.createIconButton(COGS,"","12","12",CENTER);
               propB.setTooltip(new Tooltip("Settings"));
               propB.setOnMouseClicked( e -> {
                   settings();
                   e.consume();
               });
        Button lockB = AwesomeDude.createIconButton(area.isLocked() ? UNLOCK : LOCK,"","12","12",CENTER);
               lockB.setTooltip(new Tooltip(area.isLocked() ? "Unlock widget layout" : "Lock widget layout"));
               lockB.setOnMouseClicked( e -> {
                   toggleLocked();
                   AwesomeDude.setIcon(lockB,area.isLocked() ? UNLOCK : LOCK,"12");
                   lockB.getTooltip().setText(area.isLocked() ? "Unlock widget layout" : "Lock widget layout");
                   e.consume();
               });
        Button refreshB = AwesomeDude.createIconButton(REFRESH,"","12","12",CENTER);
               refreshB.setTooltip(new Tooltip("Refresh widget"));
               refreshB.setOnMouseClicked( e -> {
                   refreshWidget();
                   e.consume();
               });
               
        // build header
        header_buttons.getChildren().addAll(helpB,lockB,refreshB,propB,changeB,detachB,closeB);
        // set common header button properties all at once...
        header_buttons.getChildren().stream().map(c->(Button)c).forEach( c -> {
            c.getStyleClass().add("header-button");
            c.setPrefSize(15, 15);
            c.setMinSize(15, 15);
            c.setMaxSize(15, 15);
        });
        
        // build animations
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), root);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), area.getContent());
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), area.getContent());
        BoxBlur blur = new BoxBlur(0, 0, 1);
        blur.widthProperty().bind(area.getContent().translateZProperty());
        blur.heightProperty().bind(area.getContent().translateZProperty());
        area.getContent().setEffect(blur);
        
        // weak mode and strong mode - strong mode is show/hide from outside
        // - weak mode is show/hide by mouse enter/exit events in the corner
        // - the weak behavior must not work in strong mode
        
        // show when area is hovered and in weak mode
        area.activator.addEventFilter(MOUSE_ENTERED, e -> {
            // avoid when locked and in strong mode
            if (!area.isUnderLock() && !isShowingStrong && 
                    // avoiid pointless operation
                    !isShowingWeak)
                showWeak();
        });
        // hide when no longer hovered and in weak mode
        root.addEventFilter(MOUSE_EXITED, e -> {
            // avoid when locked and in strong mode
            if(!area.isUnderLock() && !isShowingStrong && 
                // avoid pointless operation
                isShowingWeak && 
                    // but not when helpPoOver is visible
                    // mouse entering the popup qualifies as root.mouseExited which we need
                    // to avoid
                    // now we need to handle hiding when popup closes
                    (helpPopOver==null || !helpPopOver.isShowing()))
                hideWeak();
        });        
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
        
        SimpleConfigurator c = new SimpleConfigurator(w,()->w.getController().refresh());
        PopOver p = new PopOver(c);
                p.setTitle(w.getName() + " Settings");
                p.setAutoFix(false);
                p.setAutoHide(true);
                p.show(propB);  
    }

    void choose() {
        WidgetPicker p = new WidgetPicker();
                     p.setConverter(widget_factory->widget_factory.name);
                     p.setOnSelect(factory->area.add(factory.create()));
                     p.show(propB,NodeCentricPos.Center);
    }
    
    void detach() {
        area.detach();
    }
    
    void close() {
        area.close();
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
        // make activator accessible when showing
        deactivator.setMouseTransparent(false);
        deactivator.setScaleX(1.5);
        deactivator.setScaleY(1.5);        
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
        // make activator inaccessible when not showing so it doesnt block 
        // controls below it
        deactivator.setMouseTransparent(true);
        deactivator.setScaleX(1);
        deactivator.setScaleY(1);
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
    
}
