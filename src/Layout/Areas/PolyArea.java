/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Areas;

import Layout.Component;
import Layout.PolyContainer;

/**
 *
 * @author Plutonium_
 */
public abstract class PolyArea extends Area<PolyContainer>{

    public PolyArea(PolyContainer _container, Integer i) {
        super(_container, i);
    }
    
    abstract public void removeComponent(Component c);
    abstract public void removeAllComponents();
}
