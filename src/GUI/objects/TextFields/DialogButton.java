/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.TextFields;

import javafx.scene.control.Button;

/**
 * 
 * Button for calling dialogs, mainly used inside other controls for example
 * custom TextAreas.
 * Provides standard across application. The button is completely normal except
 * for having its own styleclass "dialog-button", that is free to be used by 
 * other buttons.
 *
 * @author Plutonium_
 */
public class DialogButton extends Button {
    
    private static final String STYLE_CLASS = "dialog-button";
    
    public DialogButton() {
        this("");
    }
    public DialogButton(String text) {
        super(text);
        getStyleClass().add(STYLE_CLASS);
    }
}
