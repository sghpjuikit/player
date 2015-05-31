/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import GUI.objects.Tree.PropertyTree;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.Controller;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import javafx.scene.Parent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.AnchorPane;
import main.App;
import static util.Util.setAnchors;
import static util.async.Async.run;

@IsWidget
@Layout.Widgets.Widget.Info(
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
    group = Widget.Group.APP
)
public class GuiInspector extends AnchorPane implements Controller<ClassWidget> {
    
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

    @Override
    public void refresh() {
    }

    @Override
    public void close() {
    }
    
/******************************************************************************/
    
    private ClassWidget w;

    @Override
    public void setWidget(ClassWidget w) {
        this.w = w;
    }

    @Override
    public ClassWidget getWidget() {
        return w;
    }

}
