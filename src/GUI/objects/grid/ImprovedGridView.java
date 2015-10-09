/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.grid;

import javafx.scene.control.Skin;

import org.controlsfx.control.GridView;

/**
 *
 * @author Plutonium_
 */
public class ImprovedGridView<T> extends GridView<T>{

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ImprovedGridViewSkin<>(this);
    }

}
