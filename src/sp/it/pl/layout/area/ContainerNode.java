/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sp.it.pl.layout.area;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import sp.it.pl.layout.AltState;

public interface ContainerNode extends AltState {

    Pane getRoot();

    default Parent getParent() {
        return getRoot()==null ? null : getRoot().getParent();
    }

    default void close() {}

}