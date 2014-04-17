/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused;

import GUI.ContextManager;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

/**
 * @author uranium
 * 
 * Defines a GUI in-app window that can move, resize, relocate inside its display
 *  - screen. Generally the display is this object's parent.
 * 
 * @TODO
 * add resize support
 */
public class MovableWindow extends Movable {
    private AnchorPane THIS = new AnchorPane();  
    @FXML
    AnchorPane content;
    @FXML
    Label titleL;
    private boolean pinned = false;
    
    public MovableWindow () {
        FXMLLoader loader = new FXMLLoader(MovableWindow.class.getResource("MovableWindow.fxml"));
        loader.setRoot(THIS);
        loader.setController(this);
        try {
            THIS = (AnchorPane) loader.load();
            installMovingBehavior();
            THIS.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
                if (event.getCode() == KeyCode.ESCAPE)
                    close();
            });
        } catch (IOException ex) {
            Logger.getLogger(MovableWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Closes this object. Effectively removes this object from its display. If
     * isPinned() the window will not close.
     */
    public boolean closeUnpinned() {
        if (!isPinned()) close();
        return !isPinned();
    }
    /**
     * Closes this object. Effectively removes this object from its display.
     */
    public void close() {
        THIS.setVisible(false);
        getDisplay().getChildren().remove(THIS);
        ContextManager.windows.remove(this);
    }
    
    public boolean isPinned() {
        return pinned;
    }
    @FXML
    public void togglePinned() {
        pinned = !pinned;
    }
    
    @FXML
    @Override
    public void toggleMaximize() {
        super.toggleMaximize();
    }
    
    /**
     * Title of the window.
     * @param title 
     */
    public void setTitle(String title) {
        titleL.setText(title);
    }
    
    /**
     * Content of the window. Previous content will be removed. Window will
     * automatically resize to fit the content size.
     * @param c 
     */
    public void setContent(Node c) {
        content.getChildren().clear();
        content.getChildren().add(c);
        AnchorPane.setBottomAnchor(c, 0.0);
        AnchorPane.setLeftAnchor(c, 0.0);
        AnchorPane.setTopAnchor(c, 0.0);
        AnchorPane.setRightAnchor(c, 0.0);
        content.setEffect(null);
    }
    
    /**
     * Sets the display - screen for this .
     * @param parent 
     */
    public void setDisplay(AnchorPane parent) {
        parent.getChildren().add(THIS);
    }
    
    @Override
    public AnchorPane getDisplay() {
        return (AnchorPane) THIS.getParent();
    }
    
    @Override
    public AnchorPane getPane() {
        return THIS;
    }

}