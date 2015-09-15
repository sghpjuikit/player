/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.reactfx.Subscription;

import Layout.BiContainer;
import Layout.BiContainerPure;
import Layout.Container;
import Layout.Layout;
import Layout.SwitchPane;
import Layout.Widgets.WidgetManager;
import action.Action;
import gui.objects.PopOver.PopOver;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import gui.objects.icon.Icon;
import main.Guide.Hint;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_MARKED_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_BOX_OUTLINE;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER;
import static main.App.APP;
import static util.async.Async.run;
import static util.graphics.Util.layHorizontally;

/**
 *
 * @author Plutonium_
 */
public final class Guide {

    private final double ICON_SIZE = 40; // use css style instead

    private final List<Hint> hints = new ArrayList();
    private int at = -1;
    private final Text text = new Text();
    private final PopOver<VBox> p = new PopOver(new VBox(15,text));
    private Subscription action_monitoring;
    final Label infoL = new Label();

    public Guide() {

        text.setWrappingWidth(350);
        text.prefWidth(350);

        p.getContentNode().setPadding(new Insets(30));
        p.setAutoHide(false);
        p.setHideOnClick(false);
        p.setHideOnEscape(true);
        p.getSkinn().setContentPadding(new Insets(8));
        p.setArrowSize(0);
        p.detached.set(true);
        p.setOnHiding(e -> run(20,() -> APP.actionStream.push("Guide closing")));
        p.getHeaderIcons().addAll(
            new Icon(ARROW_LEFT,11,"Previus",this::goToPrevious),
            infoL,
            new Icon(ARROW_RIGHT,11,"Next",this::goToNext)
        );

        hint("Intro", "Hi, this is automatic guide for this application. It will show you around. " +
             "\n\nBut first some music, right?",
             new Icon(MUSIC, ICON_SIZE, null, e -> {
                // find spot
                SwitchPane la = Window.getFocused().getSwitchPane();
                Container con = la.getActive().getAllContainers(true).filter(c->c.getEmptySpot()!=null).findFirst()
                                .orElse(la.getComponents().values().stream().filter(c->c.getEmptySpot()!=null).findFirst()
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
                APP.actionStream.push("Intro");
            }
        ));
        hint("Guide offer", "Hi, app guide here. It will show you around. Interested ?",
                new Icon(CHECKBOX_MARKED_OUTLINE,ICON_SIZE,null,this::goToNext),
                new Icon(CLOSE_BOX_OUTLINE,ICON_SIZE,null,this::close)
        );
        hint("Guide hints", "Guide consists of hints. Complete hint to proceed or "
           + "go to next hint manually." +
             "\n\nShow next hint by clicking the next button in the header of this guide.");
        hint("Guide closing", "Guide can be closed simply by closing the popup.\n\n"
           + "Closing guide by clicking the close button in the header of this guide. Or press ESC.");
        hint("Guide opening", "Guide can be opened from app window header. It will resume "
           + "from where you left off" +
             "\n\nGo to header of the app window and click on the guide button. It looks like: ",
             new Icon(GRADUATION_CAP,ICON_SIZE));
        hint("Info popup", "The app has info buttons explaining functionalities for app "
           + "sections and how to use them." +
             "\n\nGo to header of the app window and click the help button. It looks like:",
              new Icon(INFO,ICON_SIZE));
        hint("New widget", "The aplication consists of:\n"
           + "\n\t• Core"
           + "\n\t• Behavior"
           + "\n\t\t• Widgets"
           + "\n\t\t• Services"
           + "\n\t• UI (user interface)"
           + "\n\t\t• Widget Layout"
           + "\n\t\t\t• Windows"
           + "\n\t\t\t• Containers"
           + "\n\t\t\t• Widgets");
        hint("New widget", "Widget is a graphical component with some functionality." +
             "\n\nClick anywhere within empty space in the layout and choose 'Place widget' to add new widget. " +
             "All available widgets will display for you to choose from, sorted lexicographically. There is a " +
             "widget called 'Empty' which contains no content. Select it.");
        hint("Widget info", "To read more about the widget and its functionality, you can open widget's info " +
             "popup. It contains information about the widget, its intention, functions and more. The popup " +
             "opens after click on info button located in the widget header. The widget header is displayed automatically " +
             "when mouse cursor enters right top corner of the widget." +
             "\n\nMove the mouse cursor over the top right corner of the widget, " +
             "wait for the widget header to appear and click on the info button");
        hint("Close widget", "Widgets can be opened and closed." +
             "\n\nTry closing the widget by moving the mouse cursor over the top right corner of the widget, " +
             "wait for the widget header to appear and click on the 'x' button");
        hint("Divide layout", "In order to create layout that best suits your needs, you need to create " +
             "more containers for storing the widgets, by dividing the layout - horizontally or vertically." +
             "\nThe orientation determines how the layout gets split by the divider and can be changed later. " +
             "The divider can be dragged by mouse to change the sizes of the sub containers." +
             "\n\nClick anywhere within empty space and choose one of the 'Split' choices.");
        hint("Layout mode", "When widget header is visible, the widget is in layout mode. Layout mode is used " +
             "for advanced manipulation with the widget. In order to quickly make changes to the layout, layout " +
             "mode can be activated by shortcut." +
             "\n\nPress '" + Action.get("Manage Layout").getKeys() + "' to enter/leave layout mode");
        hint("Layout mode", "For layout mode, there is also fast-shortcut reacting on key press and release." +
             "\n\nPress '" + Action.Shortcut_ALTERNATE + "' to temporarily enter layout mode. (If the shortcut " +
             "is empty (disabled) go to next hint manually).");
        hint("Layout lock", "Because automatic layout mode for widgets and containers can be intrusive, " +
             "the layout can be locked. Locked layout will enter full layout mode with shortcuts, but not " +
             "individual widgets. You may want to lock the layout after configuring it to your needs." +
             "\n\nClick on the lock button in the window header or press '" + Action.get("Toggle layout lock.").getKeys() +
             "' to temporarily enter layout mode.");
        hint("Widget layout lock", "Note that individual widgets and containers can be locked as well to " +
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
            if (!p.isShowing()) p.show(PopOver.ScreenPos.App_Center);
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
        if (action_monitoring==null)
            action_monitoring = APP.actionStream.subscribe(action -> {
//                if(p.isShowing())
                    handleAction(action);
            });
        goToStart();
    }

    public void stop() {
        if (p.isShowing()) p.hideImmediatelly();
        if (action_monitoring!=null) {
            action_monitoring.unsubscribe();
            action_monitoring = null;
        }
    }

    public void open() {
        APP.actionStream.push("Guide opening");
        if (action_monitoring==null) start();
        else proceed();
    }

    public void close() {
        p.hide();
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

    public void hint(String action, String text) {
        hints.add(new Hint(action, text));
    }

    public void hint(String action, String text, Node... graphics) {
        hints.add(new Hint(action, text, layHorizontally(5,CENTER,graphics)));
    }


    class Hint {
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
