/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.Traits;

import javafx.scene.Node;

/**
 *
 * @author uranium
 */
public interface NodeTrait {
    
    /**
     * Returns the node itself as an owner of this trait. This method
     * exist solely for extending traits to gain access to the node
     * @return the node itself.
     */
    public Node getNode();
}
