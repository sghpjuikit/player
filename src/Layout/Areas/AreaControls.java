/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Areas;

import GUI.ContextManager;
import GUI.GUI;
import GUI.objects.Pickers.WidgetPicker;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import Layout.Widgets.Widget;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
public final class AreaControls {
    
    @FXML public AnchorPane root = new AnchorPane();
    @FXML public Region deactivator;
    @FXML public BorderPane header;
    @FXML public Label title;
    @FXML public Button propB;
    @FXML public Button menuB;
    
    // animations // dont initialize here or make final
    private FadeTransition contrAnim;
    private FadeTransition contAnim;
    private TranslateTransition blurAnim;
    
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
        
        // build animations
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), root);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), area.getContent());
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), area.getContent());
        BoxBlur blur = new BoxBlur(0, 0, 1);
        blur.widthProperty().bind(area.getContent().translateZProperty());
        blur.heightProperty().bind(area.getContent().translateZProperty());
        area.getContent().setEffect(blur);
        
        // show/hide behavior
        area.activator.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            if (!area.controlsOn && !area.isUnderLock())
                show();
        });
        root.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (!area.controlsOn)
                hide();
        });
        
        area.root.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (!area.controlsOn)
                hide();
        });
        
    }

    @FXML
    void refreshWidget() {
        area.refresh();
    }

    @FXML
    void toggleLocked() {
        area.toggleLocked();
        area.activator.setMouseTransparent(area.isUnderLock());
    }

    @FXML
    void settings() {
        if(area.getActiveComponents().isEmpty()) return;
        Widget w = (Widget) area.getActiveComponents().get(0);
        ContextManager.openFloatingSettingsWindow(w);
    }

    @FXML
    void choose() {
        WidgetPicker p = new WidgetPicker();
                     p.setConverter(widget_factory->widget_factory.name);
                     p.setOnSelect(factory->area.add(factory.create()));
                     p.show(propB,NodeCentricPos.Center);
    }
    @FXML
    void detach() {
        area.detach();
    }
    @FXML
    void close() {
        area.close();
    }
    
    public void show() {
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
        deactivator.setMouseTransparent(false);  // dont remove
    }
    
    public void hide() {
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
        deactivator.setMouseTransparent(true);  // dont remove
    }
    
}
