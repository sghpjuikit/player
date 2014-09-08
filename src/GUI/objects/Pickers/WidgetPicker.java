/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import Layout.Widgets.WidgetFactory;
import Layout.Widgets.WidgetManager;

/**
 *
 * @author Plutonium_
 */
public class WidgetPicker extends Picker<WidgetFactory>{

    public WidgetPicker() {
        super();
        
        setAccumulator(WidgetManager::getFactories);
        setConverter(wf -> wf.name());
    }
    
}
