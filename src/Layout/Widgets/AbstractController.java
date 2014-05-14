/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets;

/**
 *
 * @author Plutonium_
 */
public interface AbstractController<W extends Object> {
    
    /**
     * Create relationship between this controller and specified widget. Usually
     * but not necessarily, the relationship is permanent.
     * @param w 
     */
    public void setWidget(W w);
    
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
    public W getWidget();
}
