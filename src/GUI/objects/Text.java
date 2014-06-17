/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects;

/**
 * {@link javafx.scene.text.Text} with a default styleclass. There is no other
 *  difference. Use when css-specific default values are expected.
 * @author Plutonium_
 */
public class Text extends javafx.scene.text.Text {
    public static final String STYLECLASS = "text";
   
    public Text() {
        super();
        getStyleClass().add(STYLECLASS);
    }
    public Text(String text) {
        super(text);
        getStyleClass().add(STYLECLASS);
    }
    public Text(double x, double y, String text) {
        super(x,y,text);
        getStyleClass().add(STYLECLASS);
    }
}
