/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import Layout.Widgets.WidgetFactory;
import Layout.Widgets.WidgetManager;

/** Widget factory picker. */
public class WidgetPicker extends Picker<WidgetFactory>{

    public WidgetPicker() {
        super();
        itemSupply = WidgetManager::getFactories;
        textCoverter = WidgetFactory::name;
    }
    
}
