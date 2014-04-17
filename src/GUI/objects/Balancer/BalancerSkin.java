/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Balancer;

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author uranium
 */
public class BalancerSkin  extends BehaviorSkinBase<Balancer, BalancerBehavior> {
    
    // the container for the traditional rating control. If updateOnHover and
    // partialClipping are disabled, this will show a combination of strong
    // and non-strong graphics, depending on the current rating value
    private ImageView backgroundContainer;
    
    // the container for the strong graphics which may be partially clipped.
    // Note that this only exists if updateOnHover or partialClipping is enabled.
    private ImageView foregroundContainer;
    private Rectangle forgroundClipRect;
    private double balance;
    
    /***************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/
    
    public BalancerSkin(Balancer control) {
        super(control, new BalancerBehavior(control));
        
//        this.updateOnHover = control.isUpdateOnHover();
        

        
        registerChangeListener(control.balanceProperty(), "BALANCE");
        registerChangeListener(control.maxProperty(), "MAX");
        registerChangeListener(control.minProperty(), "MIN");
//        registerChangeListener(control.orientationProperty(), "ORIENTATION");
//        registerChangeListener(control.updateOnHoverProperty(), "UPDATE_ON_HOVER");
        
        // remember rating and return to old after mouse hover ends
//        getSkinnable().addEventHandler(MouseEvent.MOUSE_ENTERED, (Event t) -> {
//            if (updateOnHover)
//                old_rating = getSkinnable().getRating();
//        });
//        getSkinnable().addEventHandler(MouseEvent.MOUSE_EXITED, (Event t) -> {
//            if (updateOnHover)
//                updateRating(old_rating);
//        });
        
        // create graphics
        backgroundContainer = new ImageView();
        backgroundContainer.setPreserveRatio(false);
        backgroundContainer.fitHeightProperty().bind(getSkinnable().prefHeightProperty());
        backgroundContainer.fitWidthProperty().bind(getSkinnable().prefWidthProperty());
        backgroundContainer.getStyleClass().add("bgr");
        getChildren().setAll(backgroundContainer);
        
        foregroundContainer = new ImageView();
        foregroundContainer.setPreserveRatio(false);
        foregroundContainer.fitHeightProperty().bind(getSkinnable().prefHeightProperty());
        foregroundContainer.fitWidthProperty().bind(getSkinnable().prefWidthProperty());
        foregroundContainer.getStyleClass().add("foregr");
        foregroundContainer.setMouseTransparent(true);
        getChildren().add(foregroundContainer);

        forgroundClipRect = new Rectangle();
        foregroundContainer.setClip(forgroundClipRect);
        
        getSkinnable().addEventHandler(MouseEvent.MOUSE_DRAGGED, (MouseEvent e) -> {
            if (getSkinnable().contains(e.getX(), e.getY())) {
                double bal = (e.getX()/getSkinnable().getWidth()-0.5)*2;
                getSkinnable().setBalance(bal);
            }
        });
        getSkinnable().addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
            if (getSkinnable().contains(e.getX(), e.getY())) {
                double bal = (e.getX()/getSkinnable().getWidth()-0.5)*2;
                getSkinnable().setBalance(bal);
            }
        });
        
        // init
        updateBalance(getSkinnable().getBalance());
    }
    
    /***************************************************************************
     * 
     * Implementation
     * 
     **************************************************************************/
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if (p == "BALANCE")
            updateBalance(getSkinnable().getBalance());
        else if (p == "MAX")
            updateClip();
        else if (p == "MIN")
            updateClip();
//        } else if (p == "ORIENTATION") {
//            recreateButtons();
//        } else if (p == "PARTIAL_RATING") {
//            this.partialRating = getSkinnable().isPartialRating();
//            recreateButtons();
//        } else if (p == "UPDATE_ON_HOVER") {
//            this.updateOnHover = getSkinnable().isUpdateOnHover();
//            recreateButtons();
//        }
    }
    
    private void updateBalance(double newBalance) {        
        balance = newBalance;
        getSkinnable().setBalance(balance);
        updateClip();
    }
    
    private void updateClip() {
        final Balancer control = getSkinnable();
        final double w = control.getPrefWidth();// - (snappedLeftInset() + snappedRightInset());
        final double h = control.getPrefHeight();// - (snappedTopInset() + snappedBottomInset());
        
        double start = balance<0 ? 0 : balance*w/2;
        double end =  balance>0 ? w : w/2+(balance+1)*w/2;
        
        forgroundClipRect.relocate(start,0);
        forgroundClipRect.setWidth(end-start);
        forgroundClipRect.setHeight(h);
    }
}
