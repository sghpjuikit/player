/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.util.function.Consumer;

import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

import gui.objects.window.stage.Window;
import util.graphics.Util;

import static javafx.stage.StageStyle.DECORATED;
import static javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST;
import static main.App.APP;
import static util.async.Async.runLater;
import static util.dev.Util.noØ;
import static util.functional.Util.list;

/**
 *
 * @author Martin Polakovic
 */
public class TaskBar {

    private boolean iconified;
    private Image icn;
    private String titl;
    private Screen scr;
    private Consumer<Boolean> onMinimized;
    private Runnable onAltTab;
    private Runnable onClose;

    private Stage s;

    public void setTitle(String title) {
        titl = title;
        if(s!=null) s.setTitle(titl==null ? "" : titl);
    }

    public void setIcon(Image icon) {
        icn = icon;
        if(s!=null) s.getIcons().setAll(icn);
    }

    public void setScreen(Screen screen) {
        noØ(screen);
        if(scr==screen) return;

        scr = screen;

        // The below is not working for some reason, even though I have successfully used this
        // approach elsewhere. Moving the stage to are of the new screen will reposition the
        // taskbar icon. I mean should. In this case, the taskbar "update" does not happen without
        // refreshing the stage's in some unspecified way (clicking manually on the taskbar to
        // minimize the stage repositiones the tasbar correctly, but im not going to try emulating
        // that)
         if(s!=null) {
             s.setX(scr.getBounds().getMinX());
             s.setY(scr.getBounds().getMinY());
         }
        // rather, just reinitialize the whole thing, problem solved
        if(s!=null){
//            setVisible(false);
//            setVisible(true);
        }
    }

    public void setOnMinimize(Consumer<Boolean> action) {
        onMinimized = v -> {
            System.out.println("Taskbar minimize event, value: " + v);
            action.accept(v);
        };
            // this is the only way of detecting minimization events - by using focus
            // when stage is de/minimized by user clicking on the taskbar, the iconified property
            // somehow is not updated (you can try attaching listener to s.iconifiedProperty() and
            // see for yourself.
            // What does happen though, is focus change. Minimizing window makes it lose focus and
            // vice versa. Thus if we do not cause focus change in any other way, we have a reliable
            // way to detect minimization events.
            // As an added bonus, we detect both minimize and deminimize events. Unexpectedly,
            // deminimization by user click on taskbar icon does not work for javaFX applications!
            // Rejoice, this solution fixes that.
    }

    /**
     * When user right clicks on taskbar icon and chooses "close window" (Windows platform)
     * the window associated with the icon closes.
     * <p/>
     * We detect the event and let dev handle it appropriately. For example application can
     * terminate or certain window/s closed.
     */
    public void setOnClose(Runnable action) {
        onClose = action;
    }

    public void setOnAltTab(Runnable action) {
        onAltTab = action;
    }

    public void setVisible(boolean v) {
        if(v) {
            if(s==null) {
                s = new Stage();
                s.initStyle(DECORATED);
                s.setOpacity(0);
                s.setWidth(20);
                s.setHeight(20);
                s.setScene(new Scene(new Region()));
                ((Region)s.getScene().getRoot()).setBackground(null);
                s.getScene().setFill(null);
                // we continue everything just like it was on close
                s.addEventFilter(WINDOW_CLOSE_REQUEST, e -> {
                    if(onClose!=null) onClose.run();
                    setVisible(false);
                    setVisible(true);
                });
                s.focusedProperty().addListener((o,ov,nv) -> {
                    if(inconsistent || forbidIconify) return;

                    // FIXME: only works when taskbar at the bottom of the screen!
                    Point2D mouse = APP.mouseCapture.getMousePosition();
                    if(mouse.getX()< Util.getScreen(mouse).getBounds().getMaxY()-50) {
                        if(nv) {
                            System.out.println("ALT TAB event");
                            inconsistent = true;
                            if(onAltTab!=null) onAltTab.run();
                            runLater(() -> inconsistent = false);
                        }
                    } else {
                    // Allright, trust me when I say I have no idea how this works...
                    // Here is what happened when I did this:
                    // Every time focus changes (and this handler is called), the onMinimized
                    // action causes somehow another focus change and refiring of this
                    // handler. Yes that requires the onMinimized action to do something very specific
                    // and Im not exactly sure what it is. It has something to do with the window
                    // the action deminimizes steals focus, which affects focus of this stage.
                    //
                    // Point is, if this handler is called once, as per javaFX, minimization does not
                    // work, only deminimization. However, due to this listener being called twice
                    // and the interference with focus, SOMEHOW, no idea how, minimization works too.
                    //
                    // The problem was that if onMinimized action restored multiple windows, this
                    // handler is called twice, but from within itself, before it finishes. Thus
                    // the 1st handled window causes the 2nd to not be handled properly anymore.
                    //
                    // For this I introduced this weird double lock. We disallow firing action while
                    // one action is already running, but we do fire it 2nd time, only with a delay
                    // (runLater) so they dont interfere. And we use another lock to prevent any
                    // more actions.
                    //
                    // That is nice, but my testing shows, the action is now called only once and
                    // stuff works, but sometimes during initializations, it is actually called twice
                    // and we have to keep handling it like this to make sure it all works corectly.
                        if(onMinimized!=null) {
                            if(!ignore2) {
                                if(!ignore) {
                                    ignore = true;
                                    System.out.println("start1");
                                    if(s!=null) onMinimized.accept(nv);
                                    System.out.println("end1");
                                    ignore = false;
                                } else {
                                    ignore2 = true;
                                    System.out.println("start2");
                                    runLater(() -> {
                                        if(s!=null) onMinimized.accept(nv);
                                        System.out.println("end2");
                                        ignore2 = false;
                                    });
                                }
                            }
                        }
                    }
                });
            }
            if(scr==null) scr = Screen.getPrimary();
            s.setTitle(titl==null ? "" : titl);
            s.getIcons().setAll(icn==null ? list() : list(icn));
            s.setIconified(iconified);

            // we need to return focus, stolen by this stage on show
            inconsistent = true;
            Window w = APP.windowManager.getFocused();
            s.show();
            if(w!=null) w.focus();
            runLater(() -> inconsistent = false);

            s.setX(scr.getBounds().getMinX());
            s.setY(scr.getBounds().getMinY());

            // debug
            // s.addEventFilter(Event.ANY, e -> System.out.println(e.getEventType()));
            // s.iconifiedProperty().addListener((o,ov,nv) -> System.out.println("iconified " + nv));
            // s.focusedProperty().addListener((o,ov,nv) -> System.out.println("focused " + nv));
        } else {
            if(s!=null) {
                Stage dispose = s;
                s = null;
                dispose.hide();
            }
        }
    }


    public boolean forbidIconify = false;   // prevents synthetic events when affecting focus programmatically
    public boolean inconsistent = false;   // prevents synthetic events when affecting focus programmatically
    private boolean ignore = false;
    private boolean ignore2 = false;

    public void iconify(boolean v) {
        if(ignore || ignore2) return;      // no idea if this is necessary
        iconified = v;
        if(s!=null) {
            inconsistent = true;
            s.setIconified(iconified);
            runLater(() -> inconsistent = false);
        }
    }

}
