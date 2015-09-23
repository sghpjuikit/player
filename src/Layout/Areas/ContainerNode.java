/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Areas;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import Layout.AltState;

/**
 *
 * @author Plutonium_
 */
public interface ContainerNode extends AltState {

    Pane getRoot();

    public default Parent getParent() {
        return getRoot()==null ? null : getRoot().getParent();
    }

    public default void close() {}

}
