/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import action.Action;
import Layout.BiContainer;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.Layout;
import Layout.Widgets.WidgetManager;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import gui.LayoutAggregators.SwitchPane;
import gui.objects.icon.Icon;
import gui.objects.PopOver.PopOver;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import main.Guide.Hint;
import org.reactfx.Subscription;
import static util.async.Async.run;

/**
 *
 * @author Plutonium_
 */
public final class Guide {
    
    private final List<Hint> hints = new ArrayList();
    private int at = -1;
    private final Text text = new Text();
    private final PopOver<VBox> p = new PopOver(new VBox(15,text));
    private Subscription action_monitoring;
    Icon prevB, nextB;
    Label infoL;
    
    public Guide() {
        
        p.setAutoHide(false);
        p.setHideOnClick(false);
        p.setHideOnEscape(true);
        p.getSkinn().setContentPadding(new Insets(8));
        p.setArrowSize(0);
        p.detached.set(true);
        p.setOnHidden(e -> run(20,() -> App.actionStream.push("Guide closing")));
        
        text.setWrappingWidth(300);
        text.prefWidth(300);
        
        nextB = new Icon(ARROW_RIGHT,11,"Next",this::goToNext);
        infoL = new Label();
        prevB = new Icon(ARROW_LEFT,11,"Previus",this::goToPrevious);
        p.getHeaderIcons().addAll(prevB,infoL,nextB);
        
        addGuide("Intro", "Hi, this is automatic guide for this application. It will show you around. " +
                "\n\nBut first some music, right?", new Icon(MUSIC, 33, null, e->{
                    // find spot
                    SwitchPane la = (SwitchPane)Window.getFocused().getLayoutAggregator();
                    Container con = la.getActive().getAllContainers().filter(c->c.getEmptySpot()!=null).findAny()
                                    .orElse(la.getLayouts().values().stream().filter(c->c.getEmptySpot()!=null).findAny()
                                    .orElse(null));
                    if(con==null) {
                        Layout l = new Layout();
                        la.addTabToRight(l);
                        con = l;
                    }
                    // prepare container
                    BiContainer bc = new BiContainerPure(VERTICAL);
                    con.addChild(con.getEmptySpot(), bc);
                    // load widgets
                    ((Container)bc.getChildren().get(1)).addChild(1,WidgetManager.getFactory("Playlist").create());
                    ((Container)bc.getChildren().get(2)).addChild(1,WidgetManager.getFactory("PlayerControls").create());
                    // go to layout
                    la.alignTab(con);
                    // go to next guide
                    App.actionStream.push("Intro");
                }));
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
        if (at<hints.size()) {
            Hint h = hints.get(at);
            // the condition has 2 reasons
            // - avoids unneded show() call
            // - avoids relocating thepopupas a result of alignment with different popup size
            // - the popup size depends on the text
            if (!p.isShowing()) p.show(PopOver.ScreenCentricPos.App_Center);
            // progress
            infoL.setText((at+1) + "/" + hints.size());
            // title + text
            p.title.set(h.action.isEmpty() ? "Guide" : "Guide - " + h.action);
            text.setText(h.text);
            // graphics
            p.getContentNode().getChildren().retainAll(text);
            if(h.graphics!=null) p.getContentNode().getChildren().add(h.graphics);
        } else {
            stop();
        }
    }
    
    private void handleAction(String action) {
        if (hints.get(at).action.equals(action)) goToNext();
    }
    
    
    public void start() {
        action_monitoring = App.actionStream.subscribe(this::handleAction);
        goToStart();
    }
    
    public void stop() {
        if (p.isShowing()) p.hideImmediatelly();
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
        if(hints.isEmpty()) return;
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
        hints.add(new Hint(action, text));
    }
    public void addGuide(String action, String text, Node graphics) {
        hints.add(new Hint(action, text, graphics));
    }
    
    public static class Hint {
        public final String text;
        public final String action;
        public final Node graphics;

        public Hint(String action, String text) {
            this.action = action;
            this.text = text;
            this.graphics = null;
        }

        public Hint(String action, String text, Node graphics) {
            this.action = action;
            this.text = text;
            this.graphics = graphics;
        }
    }
}
