/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.Dialogs;

import GUI.Window;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.Callback;
import main.App;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
public class ContentDialog<C extends Node> {
    @FXML private AnchorPane root = new AnchorPane();
    @FXML private BorderPane content;
    @FXML private BorderPane messagePane;
    @FXML private Label message;
    @FXML private VBox bottomPane;
    
    Window w;
    Callback<C, Boolean> onOk;
    
    public ContentDialog() {
        // load content graphics
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ItemHolderDialog.fxml"));
                       loader.setController(this);
                       loader.setRoot(root);
                       loader.load();
            bottomPane.getChildren().remove(messagePane);
        } catch(IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        
        // create window
        w = Window.create();
        w.setIsPopup(true);
        w.getStage().initOwner(App.getWindowOwner().getStage());
        w.getStage().initModality(Modality.APPLICATION_MODAL);
        w.setContent(root);
        w.show();
        w.setLocationCenter();
        
    }
    
    public void show() {
        w.show();
        w.setLocationCenter();
    }

    @FXML
    public void close() {
        w.close();
    }
    
    @FXML
    private void cancel() {
        close();
    }
    
    @FXML
    private void ok() {
        if(onOk!=null) {
            try {
                boolean success = onOk.call((C)content.getCenter());
                if(success) close();
            } catch(Exception e) {
                setMessagee(e.getMessage());
            }
            
        }
    }

/******************************************************************************/
    
    /**
     * Sets code that will execute when OK button is pressed.
     * <p>
     * The callback returns true on successful execution and false otherwise, for
     * example on incorrect user input. An error {@link #setMessagee() message}
     * can be displayed.
     * <p>
     * If any exception occurs it will be caught (even runtime) and its message
     * displayed. This causes messages to be displayed automatically and removes
     * the need for developer to do it manually here. It does produce a dangerous
     * reliance on external code, but it still can be avoided by displaying the
     * messages manually and preventing the exception being caught.
     * <p>
     * It is recommended to refer to javadoc for code used within callback and see if
     * the code throws exceptions in incorrect use.
     * @param onOk 
     */
    public void setOnOk(Callback<C, Boolean> onOk) {
        this.onOk = onOk;
    }
    
    public void setContent(Control c) {
        content.setCenter(c);
    }
    
    public void setTitle(String title) {
        w.setTitle(title);
    }
    
    /**
     * Displays message. It can be a result of error or a simple notofication.
     * <p>
     * In order to automatically hide displayed message if it is no longer valid
     * this method must be called with empty string as parameter when that happens.
     * Because the exact circumstances can not be forseen, the hiding should be
     * done manually.
     * <pre>
     * For example:
     *     field.textProperty().addListener(text -> dialog.setMessagee(""));
     * 
     * The above example hides the message that would normally be an error such
     * as empty text.
     * </pre>
     * @param message text of the message. Empty text hides message.
     */
    public void setMessagee(String message) {
        this.message.setText(message);
        if(message.isEmpty()) {
            bottomPane.getChildren().remove(messagePane);
            root.requestLayout();
            root.autosize();
        } else {
            if(!bottomPane.getChildren().contains(messagePane)) {
                bottomPane.getChildren().add(messagePane);
//                VBox.setMargin(messagePane, new Insets(15, 0, 0, 0));
                root.requestLayout();
                root.autosize();
        }}
    }
}
