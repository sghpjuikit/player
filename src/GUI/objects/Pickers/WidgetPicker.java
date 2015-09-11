/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.Pickers;

import javafx.scene.control.Tooltip;

import Layout.Widgets.WidgetFactory;
import Layout.Widgets.WidgetManager;

/** Widget factory picker. */
public class WidgetPicker extends Picker<WidgetFactory<?>>{

    public WidgetPicker() {
        super();
        itemSupply = WidgetManager::getFactories;
        textCoverter = WidgetFactory::name;
        cellFactory = cellFactory.andApply((w,cell) -> {
            Tooltip t = new Tooltip(w.toStr());
                    t.setMaxWidth(300);
            Tooltip.install(cell, new Tooltip(w.toStr()));
        });
    }

}
