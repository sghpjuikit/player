/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

/**
 * Marker interface denoting a feature - a functionality of some object.
 * <p>
 * It is expected that the application contains many widgets and widget
 * types, some of which share some behavior. This can be discovered and
 * exploited by using common behavior interface.
 * <p>
 * This interface should not be ever used directly, only its extending interfaces.
 * This interface can be used to limit possible generic methods to Feature by 
 * using bounded wildcards for example: <? extends Feature>
 * 
 * @author Plutonium_
 */
public interface Feature {
    
}
