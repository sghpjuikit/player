/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.LayoutAggregators;

import Layout.Layout;
import java.util.Map;
import javafx.scene.Parent;

/**
 *
 * @author Plutonium_
 */
public interface LayoutAggregator {
    
    /**
     * Get all layouts of this aggregator.
     * @return 
     */
    Map<Integer,Layout> getLayouts();
    
    /**
     * Root of this layout aggregator. Use to place the layouts to the scene 
     * graph. Every layout of this aggregator is at some level a child of this
     * root.
     * @return 
     */
    Parent getRoot();
    
    /**
     * Aggregator is empty if it does not contain any Layouts. {@link #getLayouts()}
     * always returns empty list in such case.
     * @return whether this aggregator is empty
     */
    public default boolean isEmpty() {
        return getLayouts().isEmpty();
    }
    
    /**
     * Get maximum number of layouts that can be aggregated in this aggregator.
     * @return Nonnegative number or Long.MAX_VALUE if infinite.
     */
    public long getCapacity();
    
    /**
     * Get active layout or null if none.
     */
    public Layout getActive();
}
