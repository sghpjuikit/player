/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.picker;


import layout.widget.WidgetFactory;

import static main.App.APP;

/** Widget factory picker. */
public class WidgetPicker extends Picker<WidgetFactory<?>>{

    public WidgetPicker() {
        super();
        itemSupply = APP.widgetManager::getFactories;
        textCoverter = WidgetFactory::nameGui;
        infoCoverter = widget -> widget.toStr();
    }

}