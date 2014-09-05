/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import Action.Action;
import GUI.objects.PopOver.PopOver;
import GUI.objects.Text;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.reactfx.Subscription;
import utilities.FxTimer;

/**
 *
 * @author Plutonium_
 */
public final class Guide {
    
    private final List<String> actions = new ArrayList();
    private final List<String> texts = new ArrayList();
    private int at = -1;
    private final PopOver<Text> popup = new PopOver(new Text());
    private Subscription action_monitoring;
    Label infoL;
    
    public Guide() {
        popup.setAutoHide(false);
        popup.setHideOnClick(false);
        popup.setHideOnEscape(true);
        popup.getSkinn().setContentPadding(new Insets(8));
        popup.setArrowSize(0);
        popup.setDetached(true);
        popup.getContentNode().setWrappingWidth(250);
        popup.getContentNode().prefWidth(250);
        popup.setOnHidden(e -> FxTimer.run(Duration.millis(20), () -> Action.actionStream.push("Guide closing")));
        
        Label nextB = AwesomeDude.createIconLabel(AwesomeIcon.ARROW_RIGHT,"11");                     
        nextB.setTooltip(new Tooltip("Cancel edit"));
        nextB.setOnMouseClicked( e -> {
            goToNext();
            e.consume();
        });
        infoL = new Label();
        Label prevB = AwesomeDude.createIconLabel(AwesomeIcon.ARROW_LEFT,"11");                     
        prevB.setTooltip(new Tooltip("Cancel edit"));
        prevB.setOnMouseClicked( e -> {
            goToPrevious();
            e.consume();
        });
        popup.getHeaderIcons().addAll(prevB,infoL,nextB);
        
        addGuide(" ", "Hi, this is automatic guide for this application. It will show you around. " +
                "Completing a hint will display next one. You can navigate manually too." +
                "\n\nShow next hint by clicking on the right arrow button in the header of this popup.");
        addGuide("Guide closing", "Guide can be closed simply by closing the popup.\n\n" +
                "Try closing the guide by clicking the 'x' close button in the upper right corner of this popup.");
        addGuide("Guide closing", "Some popups can be also closed by pressing the ESC key. " +
                "\n\nTry pressing the ESC key on your keyboard.");
        addGuide("Guide resumed", "When you close the guide, it can always be resumed later." +
                "\n\nClose this guide and go to header of the application window and click on the button labelled by graduation cap.");
        addGuide("Layout info popup", "The application contains lots of informative popups, that " +
                "explain functionalities for their respective sections and how to use them. " +
                "These popups can be shown by clicking on the help buttons labelled by 'i'." +
                "\n\nGo to header of the application window and click on the help button.");
        addGuide("New widget", "The aplication functions as a group of widgets on top of the " +
                "application core. Widget is a standalone block with some functionality." +
                "\n\nClick anywhere within empty space in the window and choose 'Place widget' to add new widget. " +
                "All available widgets will display for you to choose from.");
        addGuide("Close widget", "Widgets can be opened and closed." +
                "\n\nFirst try closing the widget by moving the mouse cursor over the top right corner of the widget" +
                " and click on the 'x' button");
        addGuide("Divide layout", "In order to create layout that best suits your needs, you need to create " +
                "more containers for storing the widgets, by dividing the layout - horizontally or vertically." +
                "The orientation can be changed later." +
                "\n\nClick anywhere within empty space and choose one of the 'Split' choices.");
    }
            
    private void proceed() {
        if (at<0) at=0;
        if (at<actions.size()) {
            // the condition has 2 reasons
            // - avoids unneded show() call
            // - avoids relocating thepopupas a result of alignment with different popup size
            // - the popup size depends on the text
            if (!popup.isShowing()) popup.show(PopOver.ScreenCentricPos.AppCenter);
            infoL.setText((at+1) + "/" + actions.size());
            popup.setTitle(actions.get(at).isEmpty() ? "Guide" : "Guide - " + actions.get(at));
            popup.getContentNode().setText(texts.get(at));
        } else {
            stop();
        }
    }
    
    private void handleAction(String action) {
        if (action.equals(actions.get(at))) goToNext();
    }
    
    
    public void start() {
        action_monitoring = Action.actionStream.subscribe(this::handleAction);
        goToStart();
    }
    
    public void stop() {
        if (popup.isShowing()) popup.hideImmediatelly();
        if (action_monitoring!=null) {
            action_monitoring.unsubscribe();
            action_monitoring = null;
        }
    }
    
    public void resume() {
        if (action_monitoring==null) start();
        else proceed();
    }
    
    public void goToStart() {
        if(actions.isEmpty()) return;
        at = 0;
        proceed();
    }
    public void goToPrevious() {
        at--;
        proceed();
    }
    public void goToNext() {
        at++;
        proceed();
    }
    
    public void addGuide(String action, String text) {
        actions.add(action);
        texts.add(text);
    }
}
