
package gui.objects.Window.stage;

import Configuration.IsConfigurable;
import gui.objects.icon.Icon;
import gui.objects.PopOver.PopOver;
import gui.objects.SimpleConfigurator;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.COGS;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import static javafx.stage.WindowEvent.WINDOW_HIDING;

/**
 *
 * @author uranium
 */
@IsConfigurable
public final class ContextManager {    
    private static double X;
    private static double Y;
    

    /** Set last mouse click x coordinate. */
    static void setX(double screenX) { X = screenX; }
    
    /** Set last mouse click y coordinate. */
    static void setY(double screenY) { Y = screenY; }
    
    /** Get last mouse click x coordinate. */
    public static double getX() {
        return Window.getActive().getX()+X;
    }
    /** Get last mouse click y coordinate. */
    public static double getY() {
        return Window.getActive().getY()+Y;
    }
    
    /** 
     * @param widget widget to open, does nothing when null.
     */
    public static Window showWindow(Widget widget) {
        Window w = Window.create();
               w.setContent(widget);
               w.show();
               w.setScreen(Window.getActive().getScreen());
               w.setXyCenter();
        return w;
    }
    
    public static PopOver showFloating(Widget w) {
        Objects.requireNonNull(w);
        
        // build popup content
        Icon propB = new Icon(COGS,12,"Settings", e -> {
                  SimpleConfigurator c = new SimpleConfigurator(w);
                  PopOver ph = new PopOver(c);
                          ph.title.set(w.getName() + " Settings");
                          ph.setAutoFix(false);
                          ph.setAutoHide(true);
                          ph.show((Node)e.getSource());
                  e.consume();
              });
        // build popup
        PopOver p = new PopOver(w.load());
                p.title.set(w.getInfo().name());
                p.setAutoFix(false);
                p.getHeaderIcons().addAll(propB);
                p.show(Window.getActive().getStage(),getX(),getY());
                // unregister the widget from active eidgets manually
                p.addEventFilter(WINDOW_HIDING, we -> WidgetManager.standaloneWidgets.remove(w));
        return p;
    }
    
    public static PopOver showFloating(Node content, String title) {
        Objects.requireNonNull(content);
        Objects.requireNonNull(title);  // we could use null, but disallow
        
        PopOver p = new PopOver(content);
                p.title.set(title);
                p.setAutoFix(false);
                p.show(Window.getActive().getStage(),getX(),getY());
        return p;
    }
    
/******************************************************************************/   
    
    public static WritableImage makeSnapshot(Node n) {
        return n.snapshot(new SnapshotParameters(), null);
    }
    
}