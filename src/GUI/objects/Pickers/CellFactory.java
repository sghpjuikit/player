/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import javafx.scene.layout.Region;

/**
 *
 * @author Plutonium_
 */
public interface CellFactory<E> {
    public Region createCell(E item);
}
