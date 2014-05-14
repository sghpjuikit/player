/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Containers;

import Layout.AltState;
import Layout.AltStateHandler;
import Layout.Container;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

/**
 * Graphical part of the container within layout.
 * The container - area is 1:1 non null relationship. Container makes up for the
 * abstract side, this class represents the graphical side.
 * <p>
 * The lifecycle of the graphics entirely depends on the lifecycle of the
 * container. Instances of this class can not live outside of container's
 * life cycle. Note that the opposite doesnt necessarily hold true.
 * <p>
 * @author uranium
 */
public abstract class Area<T extends Container> implements AltState {
    
    /**
     * Container this are is associated with. The relationship can not be changed.
     * Never null.
     */
    public final T container;
    /** The root Pane of this Area. Never null. */
    public final AnchorPane root = new AnchorPane();
    

    
    /**
     * 
     * @param _container must not be null
     */
    public Area(T _container) {
        // init final 1:1 container-area relationship
        Objects.requireNonNull(_container);
        container = _container;
        
        // init properties
        container.properties.initProperty(Double.class, "padding", 0d);
        
        // init behavior
        root.setOnScroll(e -> {
            if(layoutMode.isAlt()) {
                if(e.getDeltaY()<0) collapse();
                else if(e.getDeltaY()>0) expand();
            }
        });
        root.setOnMouseClicked(e->{
            if(layoutMode.isAlt())
                if(e.getButton()==MouseButton.MIDDLE)
                    setPadding(0);
        });
    }
    
    /**
     * Refresh active widget. Refreshes the wrapped widget by calling its
     * refresh() method from its controller. Some widgets might not support
     * this behavior.
     */
    abstract public void refreshWidget();
    
    public void close() {
        container.close();
    }
    
/******************************************************************************/
    
    /** Returns the content. */
    abstract public AnchorPane getContent();
    /** Returns the controls. */
    abstract public AnchorPane getControls();
    
/******************************************************************************/
    
    public final void expand() {
        changePadding(-1);
    }
    public final void collapse() {
        changePadding(+1);
    }
    public final void changePadding(double by) {
        Insets pad = root.getPadding();
        double to = pad.getTop()+by;
        if(to<0) to = 0;
        else if(to>root.getWidth()/2) to = root.getWidth()/2;
        else if(to>root.getHeight()/2) to = root.getHeight()/2;
        
        setPadding(to);
    }
    public final void setPadding(double to) {
        // update properties if changed
        if(root.getPadding().getTop()!=to)
            container.properties.set("padding", to);
        root.setPadding(new Insets(to));
    }
    
/******************************************************************************/
    
    final AltStateHandler layoutMode = new AltStateHandler(){
        @Override public void in() { 
            showControls();
        }
        @Override public void out() { 
            hideControls(); 
        }
    };
    
    @FXML
    @Override
    public void show() {
        layoutMode.in();
        layoutMode.setAlt(true);
    }
    @FXML
    @Override
    public void hide() {
        layoutMode.out();
        layoutMode.setAlt(false);
    }
    @FXML
    public void setLocked(boolean val) {
        layoutMode.setLocked(val);
    }
    @FXML
    public void toggleLocked() {
        if (layoutMode.isLocked())
            layoutMode.setLocked(false);
        else
            layoutMode.setLocked(true);
    }
    
    abstract protected void showControls();
    abstract protected void hideControls();
}
