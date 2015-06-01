/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import GUI.objects.Tree.PropertyTree;
import Layout.Widgets.ClassWidgetController;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.APP;
import javafx.scene.Parent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import main.App;
import static util.Util.setAnchors;
import static util.async.Async.run;

@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Gui Inspector",
    description = "Displays gui hierarchy",
    howto = "Available actions:\n"
    + "    Select category\n"
    + "    Change setting value: Automatically takes change\n"
    + "    OK : Applies any unapplied change\n"
    + "    Default : Set default value for this setting\n",
    notes = "To do: generate active widget settings, allow subcategories.",
    version = "1",
    year = "2015",
    group = APP
)
public class GuiInspector extends ClassWidgetController {
    
    public GuiInspector() {
        PropertyTree pt = new PropertyTree();
        getChildren().add(pt);
        setAnchors(pt,0);
        
        run(5000, () -> {
            Parent r = App.getWindow().getStage().getScene().getRoot();
            r.addEventFilter(MOUSE_CLICKED, e -> {
//                Parent o = r;
//                Node t;
//                do{System.out.println("x");
//                    t = o.getChildrenUnmodifiable().stream().filter(c -> c.contains(c.sceneToLocal(e.getSceneX(),e.getSceneY())))
//                         .findAny().orElse(null);
//                    if(t instanceof Parent) o = (Parent)t;
//                }while(t!=null);

                if(e.getPickResult().getIntersectedNode()!=null) 
                    pt.expand(e.getPickResult().getIntersectedNode());
            });
            pt.setRoot(PropertyTree.createTreeItem(r));
        });
        
    }
}
