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
                "explain functionalities for their respective sections and how to use them." +
                "\n\nThese popups can be shown by clicking on the help buttons labelled by 'i'. " +
                "Info popup is volatile and closes next time you click somewhere, including the popup itself." +
                "\n\nGo to header of the application window and click on the help button.");
        addGuide("New widget", "The aplication functions as a group of widgets on top of the " +
                "application core. Widget is a standalone block with some functionality." +
                "\n\nClick anywhere within empty space in the layout and choose 'Place widget' to add new widget. " +
                "All available widgets will display for you to choose from, sorted lexicographically. There is a " +
                "widget called 'Empty' which contains no content. Select it.");
        addGuide("Widget info", "To read more about the widget and its functionality, you can open widget's info " +
                "popup. It contains information about the widget, its intention, functions and more. The popup " +
                "opens after click on info button located in the widget header. The widget header is displayed automatically " +
                "when mouse cursor enters right top corner of the widget." +
                "\n\nMove the mouse cursor over the top right corner of the widget, " +
                "wait for the widget header to appear and click on the info button");
        addGuide("Close widget", "Widgets can be opened and closed." +
                "\n\nTry closing the widget by moving the mouse cursor over the top right corner of the widget, " +
                "wait for the widget header to appear and click on the 'x' button");
        addGuide("Divide layout", "In order to create layout that best suits your needs, you need to create " +
                "more containers for storing the widgets, by dividing the layout - horizontally or vertically." +
                "\nThe orientation determines how the layout gets split by the divider and can be changed later. " +
                "The divider can be dragged by mouse to change the sizes of the sub containers." +
                "\n\nClick anywhere within empty space and choose one of the 'Split' choices.");
        addGuide("Layout mode", "When widget header is visible, the widget is in layout mode. Layout mode is used " +
                "for advanced manipulation with the widget. In order to quickly make changes to the layout, layout " +
                "mode can be activated by shortcut." +
                "\n\nPress '" + Action.getAction("Manage Layout").getKeys() + "' to enter/leave layout mode");
        addGuide("Layout mode", "For layout mode, there is also fast-shortcut reacting on key press and release." +
                "\n\nPress '" + Action.Shortcut_ALTERNATE + "' to temporarily enter layout mode. (If the shortcut " +
                "is empty (disabled) go to next hint manually).");
        addGuide("Layout lock", "Because automatic layout mode for widgets and containers can be intrusive, " +
                "the layout can be locked. Locked layout will enter full layout mode with shortcuts, but not " +
                "individual widgets. You may want to lock the layout after configuring it to your needs." +
                "\n\nClick on the lock button in the window header or press '" + Action.getAction("Toggle layout lock.").getKeys() + 
                "' to temporarily enter layout mode.");
        addGuide("Widget layout lock", "Note that individual widgets and containers can be locked as well to " +
                "achieve semi-locked layout effect. You may lock individual widgets or containers if layout mode " +
                "gets in the way of using them. This could be the case for widgets that display tables or contain " +
                "other controls in the corner activation area." +
                "\n\nClick on the lock button in the widget's header.");
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
