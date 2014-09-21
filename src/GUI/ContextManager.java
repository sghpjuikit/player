
package GUI;

import Configuration.IsConfigurable;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.COGS;
import java.util.ArrayList;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
               w.setLocationCenter();
        return w;
    }
    
    public static PopOver showFloating(Widget w) {
        Objects.requireNonNull(w);
        
        // build popup content
        Label propB = AwesomeDude.createIconLabel(COGS,"","12","12",CENTER);
              propB.setTooltip(new Tooltip("Settings"));
              propB.setOnMouseClicked( e -> {
                  SimpleConfigurator c = new SimpleConfigurator(w);
                  PopOver ph = new PopOver(c);
                          ph.setTitle(w.getName() + " Settings");
                          ph.setAutoFix(false);
                          ph.setAutoHide(true);
                          ph.show(propB);
                  e.consume();
              });
        // build popup
        PopOver p = new PopOver(w.load());
                p.setTitle(w.name());
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
                p.setTitle(title);
                p.setAutoFix(false);
                p.show(Window.getActive().getStage(),getX(),getY());
        return p;
    }
    
/******************************************************************************/   
    
    public static WritableImage makeSnapshot(Node n) {
        return n.snapshot(new SnapshotParameters(), null);
    }
    
}