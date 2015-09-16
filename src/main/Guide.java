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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import org.reactfx.Subscription;

import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.BiContainer;
import Layout.BiContainerPure;
import Layout.SwitchContainer;
import Layout.Widgets.WidgetManager;
import action.Action;
import gui.objects.PopOver.PopOver;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import gui.objects.icon.Icon;
import main.Guide.Hint;
import util.animation.Anim;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.GAMEPAD_VARIANT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.HAND_POINTING_RIGHT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RUN;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WALK;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.async.Async.run;
import static util.async.Async.runFX;
import static util.graphics.Util.layHorizontally;

/**
 *
 * @author Plutonium_
 */
@IsConfigurable
public final class Guide implements Configurable {

    @IsConfig(name = "Show guide on app start", info = "Show guide when application "
            + "starts. Default true, but when guide is shown, it is set to false "
            + "so the guide will never appear again on its own.")
    public boolean first_time = true;
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

        hint("Intro", "Hi, this is guide for this application. It will show you around. " +
             "\n\nBut first some music, right?",
             new Icon(MUSIC, ICON_SIZE, null, e -> {
                // find spot
                SwitchContainer la = Window.getFocused().getSwitchPane().container;
                // prepare container
                BiContainer bc = new BiContainerPure(VERTICAL);
                la.addChild(la.getEmptySpot(), bc);
                // load widgets
                bc.addChild(1,WidgetManager.getFactory("Playlist").create());
                bc.addChild(2,WidgetManager.getFactory("PlayerControls").create());
                // go to layout
                la.getGraphics().alignTab(bc);
                // go to next guide
                APP.actionStream.push("Intro");
            }
        ));
        hint("Guide hints", "Guide consists of hints. Complete hint to proceed or "
           + "go to next hint manually." +
             "\n\nShow next hint by clicking the next button in the header of this guide.");
        hint("Guide closing", "Guide can be closed simply by closing the popup.\n\n"
           + "Close guide by clicking the close button in the header of this guide. Or press ESC.");
        hint("Guide opening", "Guide can be opened from app window header. It will resume "
           + "from where you left off" +
             "\n\nGo to header of the app window and click on the guide button. It looks like: ",
             new Icon(GRADUATION_CAP,ICON_SIZE));
        int i = 3; // randomize this
        hint("Icons", "Icons, icons everywhere. Picture is worth a thousand words, they say."
           + "\n\nOnly one icon leads to next hint. Can you guess which?",
              new Icon(WHEELCHAIR,ICON_SIZE).onClick(e -> ((Icon)e.getSource()).icon(CLOSE)),
              new Icon(WALK,ICON_SIZE).onClick(e -> ((Icon)e.getSource()).icon(CLOSE)),
              new Icon(RUN,ICON_SIZE).onClick(() -> runFX(1500,() -> APP.actionStream.push("Icons")))
        );
        Icon hi = new Icon(TROPHY,ICON_SIZE)
                .tooltip("Click to claim the trophy")
                .onClick(() -> runFX(1500,() -> APP.actionStream.push("Tooltips")));
             hi.setVisible(false);
        hint("Tooltips", "Tooltips can provide additional action description. Use them well."
           + "\n\nThere can be a meaning where you don't see it. Hover above the icons to find out.",
              new Icon(GAMEPAD_VARIANT,ICON_SIZE).tooltip(new Tooltip("Play")),
              new Icon(HAND_POINTING_RIGHT,ICON_SIZE).tooltip(new Tooltip("To")),
              new Icon(GRADUATION_CAP,ICON_SIZE).tooltip(new Tooltip("Learn"){{
                  setOnHidden(e -> runFX(1500, () -> hi.setVisible(true)));
              }}),
              hi
        );
        hint("Info popup", "The app has info buttons explaining functionalities for app "
           + "sections and how to use them." +
             "\n\nGo to header of the app window and click the help button. It looks like:",
              new Icon(INFO,ICON_SIZE));
        hint("New widget", "The aplication consists of:\n"
           + "\n\t• Core"
           + "\n\t• Behavior"
           + "\n\t\t• Widgets"
           + "\n\t\t• Services"
           + "\n\t• UI (user interface) layout"
           + "\n\t\t• Windows"
           + "\n\t\t• Containers"
           + "\n\t\t• Widgets");
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

    private final Anim proceed_anim = new Anim(millis(500),x -> p.getContentNode().setOpacity(1-x*x));
    private void proceed() {
        if (at<0) at=0;
        if (at<hints.size()) {
            proceed_anim.playOpenDoClose(this::proceedFinish);
        } else {
            stop();
        }
    }

    private void proceedFinish() {
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
        if(h.graphics!=null) {
            p.getContentNode().getChildren().add(h.graphics);
            VBox.setMargin(h.graphics, new Insets(10,0,0,0));
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
        first_time = false;
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
        hints.add(new Hint(action, text, layHorizontally(10,CENTER,graphics)));
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
