/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.grid;

import org.controlsfx.control.GridView;

/**
 *
 * @author Plutonium_
 */
public class ImprovedGridView<T> extends GridView<T>{

    @Override
    protected ImprovedGridViewSkin<T> createDefaultSkin() {
        return new ImprovedGridViewSkin<>(this);
    }

    public ImprovedGridViewSkin<T> getSkinn() {
        return (ImprovedGridViewSkin<T>)getSkin();
    }

}
