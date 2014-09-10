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
    
    /**
     * Returns feature name.
     * <p>
     * Note that should an implementing class implement multiple Features, it
     * needs to override this method, but in a way that retains names of all of
     * the implementations.
     * <pre>
     *   public String getFeatureName() {
     *       return Feature1.super.getFeatureName() + ", " + Feature2.super.getFeatureName();
     *   }
     * </pre>
     * 
     * @return human readable name of the feature.
     */
    String getFeatureName();
}
