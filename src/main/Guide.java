package main;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

import org.reactfx.Subscription;

import gui.Gui;
import gui.objects.Text;
import gui.objects.icon.Icon;
import gui.objects.popover.PopOver;
import gui.objects.window.stage.Window;
import layout.container.bicontainer.BiContainer;
import layout.container.switchcontainer.SwitchContainer;
import layout.widget.Widget;
import util.access.V;
import util.action.Action;
import util.animation.Anim;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.graphics.drag.DragUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TROPHY;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.*;
import static gui.objects.icon.Icon.createInfoIcon;
import static java.util.Collections.singletonMap;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.util.Duration.millis;
import static layout.container.Container.testControlContainer;
import static main.App.APP;
import static util.async.Async.run;
import static util.async.Async.runFX;
import static util.graphics.Util.layHorizontally;
import static util.graphics.drag.DragUtil.installDrag;

/**
 *
 * @author Martin Polakovic
 */
@IsConfigurable
public final class Guide implements Configurable {

    @IsConfig(name = "Show guide on app start", info = "Show guide when application "
            + "starts. Default true, but when guide is shown, it is set to false "
            + "so the guide will never appear again on its own.")
    public final V<Boolean> first_time = new V<>(true);
    private static final String STYLECLASS_TEXT = "guide-text";
    private final double ICON_SIZE = 40; // use css style instead

    private final List<Hint> hints = new ArrayList<>();
    private int prev_at = -1;
    @IsConfig(name = "Position")
    private int at = -1;
    private final Text text = new Text();
    private final PopOver<VBox> p = new PopOver<>(new VBox(15,text));
    private Subscription action_monitoring;
    private final Label infoL = new Label();

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
            createInfoIcon(
                 "Guide info popup."
               + "\n\nThere are many others. If you see one for the first time, check it out."
               + "\n\nThis popup will close on its own when you clock somewhere. ESCAPE works too."
            ),
            // new Icon(ARROW_LEFT,11,"Previus",this::goToPrevious), // unnecessary, uses left+right mouse button navigation
            infoL,
            // new Icon(ARROW_RIGHT,11,"Next",this::goToNext) // unnecessary, uses left+right mouse button navigation
            new Label()
        );
        p.getContentNode().addEventHandler(MOUSE_CLICKED, e -> {
            if(e.getButton()==PRIMARY) goToNext();
            if(e.getButton()==SECONDARY) goToPrevious();
        });

        hint("Intro", "Hi, this is guide for this application. It will show you around. " +
             "\n\nBut first some music, right?",
             new Icon(MUSIC, ICON_SIZE, null, e -> {
                // find spot
                SwitchContainer la = APP.windowManager.getFocused().getSwitchPane().container;
                // prepare container
                BiContainer bc = new BiContainer(VERTICAL);
                la.addChild(la.getEmptySpot(), bc);
                // load widgets
                bc.addChild(1,APP.widgetManager.factories.get("PlaylistView","EmptyWidget").create());
                bc.addChild(2,APP.widgetManager.factories.get("PlayerControls","EmptyWidget").create());
                // go to layout
                la.ui.alignTab(bc);
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
        hint("Shortcuts", () -> "Shortcuts are keys and key combinations that invoke some action."
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
        hint("New widget", "The application consists of:\n"
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
        hint("Container", "Container is a non-graphical component. Containers group components "
           + "together so they can be manipulated as a group.\n"
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
           + "\nThis is known as layout mode. It is an overlay UI for manipulating the content. "
           + "There is an info button in the widget controls, so use it to learn more about what "
           + "the controls can do."
           + "\n\nMove the mouse cursor to the top right corner of the widget to display the controls.");
        hint("Divide layout", "In order to create layout that best suits your needs, you need to create " +
             "more containers for storing the widgets, by dividing the layout - horizontally or vertically." +
             "\nThe orientation determines how the layout gets split by the divider and can be changed later. " +
             "The divider can be dragged by mouse to change the sizes of the sub containers." +
             "\n\nClick anywhere within empty space and choose one of the 'Split' choices.");
        hint("Layout mode", () -> "When widget header is visible, the widget is in layout mode. Layout mode is used " +
             "for advanced manipulation with the widget. In order to quickly make changes to the layout, layout " +
             "mode can be activated by shortcut." +
             "\n\nPress '" + Action.get("Manage Layout").getKeys() + "' to enter/leave layout mode");
        hint("Layout mode", "For layout mode, there is also fast-shortcut reacting on key press and release." +
             "\n\nPress '" + Action.Shortcut_ALTERNATE + "' to temporarily enter layout mode. (If the shortcut " +
             "is empty (disabled) go to next hint manually).");
        hint("Container control", "Container can be controlled just like a widget, but you need "
           + "to navigate to its controls first. Use mouse buttons:\n"
           + "\n\t• Right click: go 'up' - visit parent container"
           + "\n\t• Left click: go 'down' - visit children"
           + "\n\n Try out container navigation:",
             new Icon(PALETTE_ADVANCED,ICON_SIZE,"",() -> {
                 Window w = APP.window;
                 int i = w.getTopContainer().getEmptySpot();
                 w.getTopContainer().ui.alignTab(i);
                 runFX(1000, () -> w.getTopContainer().addChild(i, testControlContainer()),
                     1000, () -> Gui.setLayoutMode(true)
                 );
             }).withText("Test")
        );
        hint("Layout lock", () -> "Because automatic layout mode can be intrusive, the layout can be "
           + "locked. Locked layout will enter layout mode only with shortcut."
           + "\nYou may want to lock the layout after configuring it to your needs." +
             "\n\nClick on the lock button in the window header or press '" + Action.get("Toggle layout lock.").getKeys() +
             "' to lock layout.");
        hint("Widget layout lock", "Widgets and containers can be locked as well. Locking widget "
           + "is useful if automatic layout mode gets in the way of the particular widget. Locking "
           + "a container will lock all its children."
           + "\n\nWidget will behave as locked if it or any of its parent containers or whole layout "
           + "is locked. Thus, you can combine locks however you wish."
           + "\n\nClick on the lock button in any widget's header.");
        hint("Table columns", "Table columns have customizable:"
            + "\n\t• Width - drag column by its border. Some columns do not support this and "
            + "resize automatically."
            + "\n\t• Order - drag column to left or right"
            + "\n\t• Visibility - use table column menu to show/hide columns"
            + "\n\nSome tables group items by some column (similar to SQL). In such case, the "
            + "column to group by can be specified in the table column menu"
            + "\n\nTo show table column menu click on any table column with right mouse button"
        );
        hint("Table search", "To look up item in a table, focus the table and simply type what "
            + "you are looking for."
            + "\n\nTable search is customizable (Settings > Table). Search works as follows:\n"
            + "\n\t• Search looks for matches in specified column. The column can be set for "
            + "each table in the table column menu (right click on the table column headers). "
            + "Only columns that display text are available."
            + "\n\t• Search is closed after specific period of inactivity or on ESCAPE press"
            + "\n\t• Search query is reset after a specific delay after the last key press"
            + "\n\t• Table will scroll the row with the first match to the center"
            + "\n\t• All rows with a match will be highlighted"
            + "\n\t• Search algorithm can be chosen"
            + "\n\t• Searching can be case sensitive or insensitive"
        );
        hint("Table filter", "Tables support powerful filters. Use CTRL+F to show/hide "
           + "the filter."
           + "\nFiltering is different from searching. Filtering affects the actual table content."
           + "\nFiltering works by specifying a condition/rule. Multiple conditions can be created "
           + "and chained."
           + "\n\nEach filter:\n"
           + "\n\t• Can be individually disabled and enabled"
           + "\n\t• Can be negated, so it has the opposite effect"
           + "\n\t• Filters items by specified column using a rule, chosen from rule "
           + "set of available rules. The rule set depends on the type of content."
        );
        hint("Table ESCAPE", "Here is a comprehensive guide to using ESCAPE key in a table. When "
           + "ESCAPE is pressed, one of the following scenarios will happen (in this order):\n"
           + "\n\t• If search is active, search will be closed"
           + "\n\t• If filtering is active and not empty it will be cleared. Table will show all items."
           + "\n\t• If filtering is active and empty, it will be hidden"
           + "\n\t• All selected items will be unselected."
        );
        hint("Drag & drop", "Many widgets support drag & drop for various content: files, text, etc. "
           + "When you drag some content, any drag supporting area will signal that it can accept the "
           + "content when you drag over it and show the type of action that will execute."
           + "\n\nIn regards to drag & drop, are can:\n"
           + "\n\t• Ignore drag (if area does not accept drags)"
           + "\n\t• Accept drag (if the drag content matches)."
           + "\n\t• Refuse drag (if drag matches, but some condition is not met, e.g., dragging from "
           + "self to self is usually forbidden)."
           + "\n\nDrag areas may cover each other! Therefore, if an accept signal is visible, moving "
           + "the mouse within the area can still activate different area (child area)."
           + "\n\nYou can start the tutorial below:",
             new Icon(PALETTE_ADVANCED,ICON_SIZE,"",() -> {
                 Window wd = APP.window;
                 int i = wd.getTopContainer().getEmptySpot();
                 wd.getTopContainer().ui.alignTab(i);

                 runFX(1000,() -> {
                    Widget w = Widget.EMPTY();
                    BiContainer root = new BiContainer(HORIZONTAL);
                    root.addChild(1,w);
                    wd.getTopContainer().addChild(i, root);

                    installDrag(
                        w.load(),
                        DICE_2,
                        "Accepts text containing digit '2' and does nothing"
                      + "\n\t• Release mouse to drop drag and execute action"
                      + "\n\t• Continue moving to try elsewhere",
                        e -> DragUtil.hasText(e) && DragUtil.getText(e).contains("2"),
                        e -> {}
                    );
                    installDrag(
                        root.getRoot(),
                        DICE_2,
                        "Accepts text containing digit '2' or '3' and does nothing"
                      + "\n\t• Release mouse to drop drag and execute action"
                      + "\n\t• Continue moving to try elsewhere",
                        e -> DragUtil.hasText(e) && (DragUtil.getText(e).contains("2") || DragUtil.getText(e).contains("3")),
                        e -> {}
                    );
                 });
             }).withText("Test"),
             new Icon(DICE_2,ICON_SIZE){{
                 setOnDragDetected(e -> {
                     Dragboard db = this.startDragAndDrop(TransferMode.COPY);
                     db.setContent(singletonMap(DataFormat.PLAIN_TEXT, "2"));
                     e.consume();
                 });
             }}.withText("Drag '2'"),
             new Icon(DICE_3,ICON_SIZE){{
                 setOnDragDetected(e -> {
                     Dragboard db = this.startDragAndDrop(TransferMode.COPY);
                     db.setContent(singletonMap(DataFormat.PLAIN_TEXT, "3"));
                     e.consume();
                 });
             }}.withText("Drag '3'")
        );
        hint("Component switching", "Components (widgets or containers) can be switched among each "
           + "other. This is accomplished simply by drag & drop in layout mode."
           + "\n\nSteps:\n"
           + "\n\t• Enter layout mode of the widget or container."
           + "\n\t• Drag the component."
           + "\n\t• Drop the container on different component."
           + "\n\nThis allows fast content switching and layout customization. To make it faster, "
           + "components can be dragged by their header - you do not have to enter layout mode. "
           + "Simply enter right top corner of the component and drag the component by its header."
        );
    }

    private final EventHandler<Event> consumer = Event::consume;
    private final Anim proceed_anim = new Anim(millis(400),x -> p.getContentNode().setOpacity(-(x*x-1)));

    private void proceed() {
        if(hints.isEmpty()) return;
        if (at<0 || at>=hints.size()) at=0;
        proceed_anim.playOpenDoClose(this::proceedDo);
        first_time.set(false);
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
        // - avoids unneeded show() call
        // - avoids relocating the popups a result of alignment with different popup size
        // - the popup size depends on the text
        if (!p.isShowing()) p.show(PopOver.ScreenPos.App_Center);
        // progress
        infoL.setText((at+1) + "/" + hints.size());
        // title + text
        p.title.set(h.action.isEmpty() ? "Guide" : "Guide - " + h.action);
        text.setText(h.text.get());
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
        proceed();
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
        else {
            if(at>=hints.size()-1) at=0;
            proceed();
        }
    }

    public void close() {
        p.hide();
    }


    public void goToStart() {
        if(hints.isEmpty()) return;
        prev_at = -1;
        at = 0;
        proceed();
    }

    public void goToPrevious() {
        if(at==0) return;
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

    public void hint(String action, Supplier<String> text) {
        hints.add(new Hint(action, text, null, null, null));
    }

    public void hint(String action, String text, Node... graphics) {
        hints.add(new Hint(action, () -> text, layHorizontally(10,CENTER,graphics), null, null));
    }

    public void hint(String action, Supplier<String> text, Runnable onEnter, Runnable onExit, Node... graphics) {
        hints.add(new Hint(action, text, layHorizontally(10,CENTER,graphics), onEnter, onExit));
    }


    private static class Hint {
        public final Supplier<String> text;
        public final String action;
        public final Node graphics;
        public final Runnable onEnter;
        public final Runnable onExit;

        public Hint(String action, String text) {
            this(action, () -> text, null, null, null);
        }

        public Hint(String action, Supplier<String> text, Node graphics, Runnable onEnter, Runnable onExit) {
            this.action = action;
            this.text = text;
            this.graphics = graphics;
            this.onEnter = onEnter;
            this.onExit = onExit;
        }
    }
}
