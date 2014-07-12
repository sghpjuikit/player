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
    public static final String STYLECLASS = "text-shape";
   
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
    
    /**
     * Sets wrappingWidth to achieve roughly rectangular size. There is a slight
     * horizontal bias (width > height).
     * <p>
     * Use this feature when this Text object is expected to resize on its own
     * or/and influences the size of its parent. For example popups displaying
     * text.
     * <p>
     * Invoke after text has been set.
     * <p>
     * The value depends on type and size of the font and might not produce
     * optimal results. Try and see.
     */
    public void setWrappingWidthNaturally() {
        if(getText()!=null && !getText().isEmpty()) 
            setWrappingWidth(90+getText().length()/4);
    }
}
