/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import org.reactfx.Subscription;

import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.container.bicontainer.BiContainer;
import Layout.container.bicontainer.BiContainerPure;
import Layout.container.switchcontainer.SwitchContainer;
import Layout.widget.WidgetManager;
import action.Action;
import gui.objects.PopOver.PopOver;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import gui.objects.icon.Icon;
import main.Guide.Hint;
import util.animation.Anim;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DEBUG_STEP_OVER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.GAMEPAD_VARIANT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.HAND_POINTING_RIGHT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.RUN;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WALK;
import static gui.objects.icon.Icon.createInfoIcon;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
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
    private final String STYLECLASS_TEXT = "guide-text";

    private final List<Hint> hints = new ArrayList();
    private int prev_at = -1;
    @IsConfig(name = "Position")
    private int at = -1;
    private final Text text = new Text();
    private final PopOver<VBox> p = new PopOver(new VBox(15,text));
    private Subscription action_monitoring;
    final Label infoL = new Label();

    public Guide() {

        text.setWrappingWidth(350);
        text.prefWidth(350);
        text.getStyleClass().add(STYLECLASS_TEXT);

        p.getContentNode().setPadding(new Insets(30));
        p.setAutoHide(false);
        p.setHideOnClick(false);
        p.setHideOnEscape(true);
        p.getSkinn().setContentPadding(new Insets(8));
        p.setArrowSize(0);
        p.detached.set(true);
        p.setOnHiding(e -> run(20,() -> APP.actionStream.push("Guide closing")));
        p.getHeaderIcons().addAll(
            // new Icon(ARROW_LEFT,11,"Previus",this::goToPrevious),
            createInfoIcon(
                 "Guide info popup."
               + "\n\nThere are many others. If you see one for the first time, check it out."
               + "\n\nThis popup will close on its own when you clock somewhere. ESCAPE works too."
            ),
            infoL,
            new Label()
            // new Icon(ARROW_RIGHT,11,"Next",this::goToNext)
        );
        p.getContentNode().addEventHandler(MOUSE_CLICKED, e -> {
            if(e.getButton()==PRIMARY) goToNext();
            if(e.getButton()==SECONDARY) goToPrevious();
        });

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
        hint("Guide hints", "Guide consists of hints. You can proceed manually or by completing the "
           + "hint. To navigate, use left and right mouse button."
           + "\n\nClick anywhere on this guide.");
        Icon<?> ni = new Icon(DEBUG_STEP_OVER,ICON_SIZE);
        Icon<?> pi = new Icon(DEBUG_STEP_OVER,ICON_SIZE){{setScaleX(-1);}};
             pi.setOpacity(0.5);
             ni.onClick(e -> {
                 if(e.getButton()==PRIMARY) {
                     ni.setOpacity(0.5);
                     pi.setOpacity(1);
                 }
                 e.consume();
             });
             pi.onClick(e -> {
                 if(e.getButton()==SECONDARY) {
                     pi.setOpacity(0.5);
                     ni.setOpacity(1);
                 }
                 e.consume();
             });
        hint("Navigation", "Navigation with mouse buttons is used across the entire app. Remember:\n"
           + "\n\t• Left click: go next"
           + "\n\t• Right click: go back"
           + "\n\nTry it out below:",
           ni,pi
        );
        hint("Guide closing", "Know your ESCAPEs. It will help you. With what? Completing this guide. "
           + "And closing windows. And table selections. And searching... You get the idea."
           + "\n\nClose guide to proceed (the guide will continue after you close it). Use close "
           + "button or ESC key."
        );
        hint("Guide opening", "Guide can be opened from app window header. It will resume "
           + "from where you left off."
           + "\nNow you will know where to go when you lose your way."
           + "\n\nClick on the guide button in the app window top header. It looks like: ",
             new Icon(GRADUATION_CAP,ICON_SIZE)
        );
        hint("Icons", "Icons, icons everywhere. Picture is worth a thousand words, they say."
           + "\nThe icons try to tell their function visually."
           + "\n\nOne icon leads to the next hint.",
              new Icon(WHEELCHAIR,ICON_SIZE).onClick(e -> ((Icon)e.getSource()).setDisable(true)),
              new Icon(WALK,ICON_SIZE).onClick(e -> ((Icon)e.getSource()).setDisable(true)),
              new Icon(RUN,ICON_SIZE).onClick(() -> runFX(1500,() -> APP.actionStream.push("Icons")))
        );
        Icon hi = new Icon(TROPHY,ICON_SIZE)
                .tooltip("Click to claim the trophy")
                .onClick(() -> runFX(1000,() -> APP.actionStream.push("Tooltips")));
             hi.setVisible(false);
        hint("Tooltips", "Tooltips will teach you the way if the icon is not enough. Use them well."
           + "\n\nThere can be a meaning where you don't see it. Hover above the icons to find out.",
              new Icon(GAMEPAD_VARIANT,ICON_SIZE).tooltip(new Tooltip("Playing")),
              new Icon(HAND_POINTING_RIGHT,ICON_SIZE).tooltip(new Tooltip("To")),
              new Icon(GRADUATION_CAP,ICON_SIZE).tooltip(new Tooltip("Learn"){{
                  setOnHidden(e -> runFX(1500, () -> hi.setVisible(true)));
              }}),
              hi
        );
        hint("Info popup", "There is more... Info buttons explain various app sections and how to "
           + "use them in more detail."
           + "\n\nSee the corner of this hint? Click the help button. It looks like:",
              new Icon(INFO,ICON_SIZE)
        );
        Runnable hider = this::close;
        Runnable shower = () -> { open(); runFX(1000, this::goToNext); };
        hint("Shortcuts", "Shortcuts are keys and key combinations that invoke some action."
           + "\n\nTo configure shortcuts, visit Settings > Shortcuts. Shortcuts can be global "
           + "or local. Global shortcuts will work even if the application has no focus."
           + "\n\nTo see all the available shortcuts, simply press '"
           + Action.get("Show shortcuts").getKeys() + "'.",
            () -> {
                APP.shortcutPane.onShown.add(hider);
                APP.shortcutPane.onHidden.add(shower);
            },
            () -> {
                APP.shortcutPane.onShown.remove(hider);
                APP.shortcutPane.onHidden.remove(shower);
            }
        );
        hint("New widget", "The aplication consists of:\n"
           + "\n\t• Core"
           + "\n\t• Behavior"
           + "\n\t\t• Widgets"
           + "\n\t\t• Services"
           + "\n\t• UI (user interface) layout"
           + "\n\t\t• Windows"
           + "\n\t\t• Components"
           + "\n\t\t\t• Containers"
           + "\n\t\t\t• Widgets");
        hint("Component", "Components compose the layout. There are graphical Widgets and "
           + "virtual Containers."
           + "\n\nComponents are:\n"
           + "\n\t• Independent - multiple instances of the same component type can run at the same time."
           + "\n\t• Exportable - each component can be exported into a small file and then launched "
           + "as a standalone application. More on this later."
        );
        hint("New widget", "Widget is a graphical component with some functionality, for example "
           + "Playlist widget. Using the application mostly about using the widgets.\n"
           + "\n\nWidgets are:\n"
           + "\n\t• Configurable - each widget instance has its own state and settings."
           + "\n\t• Functional units - Widget can have inputs and outputs and communicate with other widgets."
           + "\n\t• Pluggable - It is possible to load custom developed widgets easily."
        );
        hint("Container", "Container is a non-graphical component for positioning the widget. "
           + "Containers are invisible and create nested hierarchy of components"
           + "\n\nTo create an UI and use widgets, containers are necessary. Every window "
           + "comes with a top level container. So it is only a matter of filling it with "
           + "content.");
        hint("New widget", "Creating widget involves:\n"
           + "\n\t• Deciding where to put it"
           + "\n\t• Creating appropriate container"
           + "\n\t• Adding widget to the container"
           + "\n\nClick anywhere within empty space (not inside widget) and choose 'Place widget' "
           + "to add new widget. All available widgets will display for you to choose from. "
           + "There is a widget called 'Empty' which contains no content. Select it.");
        hint("Widget control", "Widget controls are located in the widget header. It is displayed "
           + "automatically when mouse cursor enters right top corner of the widget."
           + "\nThis is known as layout mode. It is an overlay UI for manupilating the content. "
           + "There is an info button in the widget controls, so use it to learn more about what "
           + "the controls can do."
           + "\n\nMove the mouse cursor to the top right corner of the widget to display the controls.");
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
        hint("Container control", "You can control container similarly to a widget, but you need "
           + "to navigate to its controls first. Use mouse buttons:"
           + "\n\t• Right click: go 'up' - visit parent container"
           + "\n\t• Left click: go 'down' - visit children"
        );
        hint("Layout lock", "Because automatic layout mode can be intrusive, the layout can be "
           + "locked. Locked layout will enter layout mode only with shortcut."
           + "\nYou may want to lock the layout after configuring it to your needs." +
             "\n\nClick on the lock button in the window header or press '" + Action.get("Toggle layout lock.").getKeys() +
             "' to lock layout.");
        hint("Widget layout lock", "Widgets and containers can be locked as well. Locking widget "
           + "is useful if automatic layout mode gets in the way of the particular widget. Locking "
           + "a container will lock all its children."
           + "\nWidget will behave as locked if it or any of its parent containers or whole layout "
           + "is locked. Thus, you can combine locks however you wish. "
           + "other controls in the corner activation area." +
             "\n\nClick on the lock button in the widget's header.");
        hint("Table columns", "Table columns are customizable:"
            + "\n\t• Width - drag column by its border. Some columns do not support this and "
            + "resize automatically."
            + "\n\t• Order - drag column to left or right"
            + "\n\t• Visibility - use table column menu to show/hide columns"
            + "\n\n Some tables group items by some column (similar to SQL). In such case, the "
            + "column to group by can be specified in the table column menu"
            + "\n\n To show table column menu click on any table column with right mouse button"
        );
        hint("Table search", "To look up item in a table, focus the table and simply type what "
            + "you are looking for."
            + "\n\nTable search is customizable (Settings > Table). Search works as follows:\n"
            + "\n\t• Search looks for matches in specified column. The column can be set for "
            + "each table in the table column menu (right click on the table column headers). "
            + "Only columns that display text areavailable."
            + "\n\t• Search is closed after specific period of inactivity or on ESCAPE press"
            + "\n\t• Search query is reset after a specific delay after the last key press"
            + "\n\t• Table will scroll the row with the first match to the center"
            + "\n\t• All rows with a match will be highlighted"
            + "\n\t• Search algorithm can be chosen"
            + "\n\t• Searching can be case sensitive or insensitive"
        );
        hint("Table filter", "Tables support powerful filters. Use CTRL+F (or ESCAPE) to show/hide "
            + "the filter."
            + "\nFiltering is different from searching. Filtering affects the actual table content."
            + "\nFiltering works by specifying a condition/rule. Multiple conditions can be created "
            + "and chained."
            + "\nSome of the functions to be aware of:\n"
            + "\n\t• Each filter can be individually disabled and enabled"
            + "\n\t• Each filter can be negated - inverted so it has the opposite effect"
            + "\n\t• Each filter filters items by specified column using a rule, chosen from rule "
            + "set of available rules. The rule set depends on the type of content."
        );
        hint("Table ESCAPE", "Here is a comprehensive guide to using ESCAPE key in a table. When "
            + "ESCAPE is pressed, one of the following scenarios will happen (in this order):\n"
            + "\n\t• If searching is active, it will be closed"
            + "\n\t• If filtering is active and not empty it will be cleared. Table will show all items."
            + "\n\t• If filtering is active and  empty, it will be hidden"
            + "\n\t• All selected items will be unselected."
        );
    }

    private final EventHandler consumer = Event::consume;
    private final Anim proceed_anim = new Anim(millis(400),x -> p.getContentNode().setOpacity(-(x*x-1)));

    private void proceed() {
        if (at<0) at=0;
        if (at<hints.size()) {
            proceed_anim.playOpenDoClose(this::proceedDo);
        } else {
            stop();
        }
    }
    private void proceedDo() {
        // exit old hint
        if(prev_at>=0 && prev_at<hints.size()) {
            Hint ph = hints.get(prev_at);
            if(ph.onExit!=null) ph.onExit.run();
        }

        Hint h = hints.get(at);

        // enter new hint
        if(h.onEnter!=null) h.onEnter.run();

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
            h.graphics.removeEventHandler(MOUSE_CLICKED, consumer);
            h.graphics.addEventHandler(MOUSE_CLICKED, consumer);
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
        prev_at = at;
        at = 0;
        proceed();
    }
    public void goToPrevious() {
        prev_at = at;
        at--;
        proceed();
    }
    public void goToNext() {
        prev_at = at;
        at++;
        proceed();
    }

    public void hint(String action, String text) {
        hints.add(new Hint(action, text));
    }

    public void hint(String action, String text, Node... graphics) {
        hints.add(new Hint(action, text, layHorizontally(10,CENTER,graphics)));
    }

    public void hint(String action, String text, Runnable onEnter, Runnable onExit, Node... graphics) {
        hints.add(new Hint(action, text, layHorizontally(10,CENTER,graphics), onEnter, onExit));
    }


    class Hint {
        public final String text;
        public final String action;
        public final Node graphics;
        public final Runnable onEnter;
        public final Runnable onExit;

        public Hint(String action, String text) {
            this(action, text, null);
        }

        public Hint(String action, String text, Node graphics) {
            this(action, text, graphics, null, null);
        }
        public Hint(String action, String text, Node graphics, Runnable onEnter, Runnable onExit) {
            this.action = action;
            this.text = text;
            this.graphics = graphics;
            this.onEnter = onEnter;
            this.onExit = onExit;
        }
    }
}
