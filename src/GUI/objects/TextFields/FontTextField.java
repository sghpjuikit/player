/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.TextFields;

import java.util.List;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import main.App;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.dialog.Dialogs;
import utilities.Parser.Parser;
import utilities.functional.functor.UnProcedure;

/**
 *
 * @author Plutonium_
 */
public class FontTextField extends CustomTextField {
    private static final List<String> STYLE_CLASS = new TextField().getStyleClass();
    
    Font curr;
    
    public FontTextField() {
        // set the button to the right
        setRight(new DialogButton("..."));
        getRight().setOnMouseClicked( e -> {
            Font tmp = Dialogs.create().owner(App.getInstance().getScene().getWindow()).showFontSelector(curr);
            setFontText(tmp);
        });
        
        setEditable(false);
        
        //set same cass style as TextField
        getStyleClass().setAll(STYLE_CLASS);
    }
    
    public void setFontText(Font font) {
        if (font != null) {
           curr = font;
           setText(Parser.toS(curr));
           if(onFontChange!=null) onFontChange.accept(font);
        }
    }
    
    public Font getFontText() {
        return curr;
    }
    
    public void setPromptFontText(Font font) {
        if (font != null) {
           setPromptText(Parser.toS(font));
        }
    }
    
    UnProcedure<Font> onFontChange;
    
    public void setOnFontChange(UnProcedure<Font> _onFontChange) {
        onFontChange=_onFontChange;
    }
}
