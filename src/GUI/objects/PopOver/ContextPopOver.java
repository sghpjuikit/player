/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.PopOver;

import GUI.ContextManager;
import javafx.scene.Node;

/**
 *
 * @author Plutonium_
 */
public class ContextPopOver<N extends Node> extends PopOver<N> {
    
    public ContextPopOver() {
        super();
        
        setDetachable(false);
        setArrowSize(0);
        setArrowIndent(0);
        setCornerRadius(0);
        setAutoHide(true);
        setAutoFix(true);
        
        // support layout mode transition
        setOnShown(e-> {
            if(ContextManager.transitForMenu)
                GUI.GUI.setLayoutMode(true);
        });
        setOnHiding(e-> {
            if(ContextManager.transitForMenu)
                GUI.GUI.setLayoutMode(false);
        });
    }
    
    public ContextPopOver(N content) {
        this();
        setContentNode(content);
    }
}
