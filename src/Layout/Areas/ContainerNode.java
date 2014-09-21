/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Areas;

import Layout.AltState;
import util.Closable;

/**
 *
 * @author Plutonium_
 */
public interface ContainerNode extends SceneGraphNode, AltState, Closable {
    
    @Override 
    public default void close() {}
}
