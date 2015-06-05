package Layout.Areas;

import gui.objects.Text;
import Layout.Component;
import java.util.HashSet;
import java.util.Set;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import static javafx.stage.StageStyle.TRANSPARENT;
 
/**
 * A draggable tab that can optionally be detached from its tab pane and shown
 * in a separate window. This can be added to any normal TabPane, however a
 * TabPane with draggable tabs must *only* have DraggableTabs, normal tabs and
 * DrragableTabs mixed will cause issues!
 * <p>
 * @author Michael Berry, Uranium
 */
public class DraggableTab extends Tab {
 
    static final Set<TabPane> tabPanes = new HashSet();
    private static final Stage markerStage;
    
    private Label nameLabel;
    private Text dragText;
    private Stage dragStage;
    private boolean detachable;
 
    static {
        markerStage = new Stage();
        markerStage.initStyle(StageStyle.UNDECORATED);
        Rectangle dummy = new Rectangle(3, 10, Color.web("#555555"));
        StackPane markerStack = new StackPane();
                  markerStack.getChildren().add(dummy);
        markerStage.setScene(new Scene(markerStack));
    }
 
    /**
     * Create a new draggable tab. This can be added to any normal TabPane,
     * however a TabPane with draggable tabs must *only* have DraggableTabs,
     * normal tabs and DrragableTabs mixed will cause issues!
     * <p>
     * @param text the text to appear on the tag label.
     */
    public DraggableTab(String text) {
        nameLabel = new Label(text);
        setGraphic(nameLabel);
        detachable = true;
        
        dragText = new Text(text);
        StackPane dragStagePane = new StackPane(dragText);
                  dragStagePane.getStyleClass().add("tooltip");
                  
        dragStage = new Stage();
        dragStage.initStyle(TRANSPARENT);
        dragStage.setScene(new Scene(dragStagePane));
        
        nameLabel.setOnMouseDragged( e -> {
            // support only left mouse drag
            if(e.getButton()!=PRIMARY) return;
            
            dragStage.setWidth(nameLabel.getWidth() + 10);
            dragStage.setHeight(nameLabel.getHeight() + 10);
            dragStage.setX(e.getScreenX());
            dragStage.setY(e.getScreenY());
            dragStage.show();
            Point2D screenPoint = new Point2D(e.getScreenX(), e.getScreenY());
            tabPanes.add(getTabPane());
            InsertData data = getInsertData(screenPoint);
            if(data == null || data.getInsertPane().getTabs().isEmpty()) {
                markerStage.hide();
            } else {
                int index = data.getIndex();
                boolean end = false;
                if(index == data.getInsertPane().getTabs().size()) {
                    end = true;
                    index--;
                }
                Rectangle2D rect = getAbsoluteRect(data.getInsertPane().getTabs().get(index));
                markerStage.setX( end ? rect.getMaxX() : rect.getMinX());
                markerStage.setY(rect.getMaxY() + 10);
                markerStage.show();
            }
        });
        
        
//        nameLabel.setOnDragDetected( e -> {System.out.println("drag started " + e.getSource());
//            Dragboard db = getTabPane().startDragAndDrop(TransferMode.MOVE);
//            Component component = (Component)getUserData();
//            DragUtil.setComponent(getTabArea().container,component, db);
//            db.setDragView(null);
//            
//            
//            getTabPane().setOnDragDone( ee -> {
//                dragStage.hide();
//                markerStage.hide();
//            });
//        });

        
        nameLabel.setOnMouseReleased( e -> {
            // support only left mouse drag
            if(e.getButton()!=PRIMARY) return;
            
            markerStage.hide();
            dragStage.hide();
            if(!e.isStillSincePress()) {
                Point2D screenPoint = new Point2D(e.getScreenX(), e.getScreenY());
                InsertData insertData = getInsertData(screenPoint);                
                
                // if dropped at table
                if(insertData != null) {
                    TabPane fromPane = getTabPane();
                    TabPane toPane = insertData.getInsertPane();
                    int from = fromPane.getTabs().indexOf(DraggableTab.this);
                    int to = Math.min(insertData.getIndex(), fromPane.getTabs().size());
                    
                    // if dropped at the same table reorder
                    if(fromPane == toPane) {
                        // return if nothing to do
                        if (fromPane.getTabs().size() == 1) return;System.out.println("MMMMMMMMOving " + from + " " + to);
                        // move child at position
                        getTabArea().container.moveChild(from, to);
                    }
                    // if dropped at some other table move over
                    else {                   
                        // remove from source
                        TabArea c1 = getTabArea();
                        Component comp = c1.container.getChildren().get(from);
                        c1.container.getChildren().remove(from);
                        c1.container.load();
                        // add to target
                        TabArea c2 = (TabArea) toPane.getUserData();
                        c2.container.addChild(to, comp);
                    }
                }
                // if not dropped at any table detach to window
                else {
                    if(!detachable) return;
                    // detach component
                    int index = getTabPane().getTabs().indexOf(this);
                    getTabArea().detachComponent(index);
                }
            }
        });
    }

    /**
     * Set whether it's possible to detach the tab from its pane and move it to
     * another pane or another window. Defaults to true.
     * <p>
     * @param detachable true if the tab should be detachable, false otherwise.
     */
    public void setDetachable(boolean detachable) {
        this.detachable = detachable;
    }
 
    /**
     * Set the label text on this draggable tab. This must be used instead of
     * setText() to set the label, otherwise weird side effects will result!
     * <p>
     * @param text the label text for this tab.
     */
    public void setLabelText(String text) {
        nameLabel.setText(text);
        dragText.setText(text);
    }
    
    public final TabArea getTabArea() {
        return ((TabArea)getTabPane().getUserData());
    }
 
    private InsertData getInsertData(Point2D screenPoint) {
        for(TabPane tabPane : tabPanes) {
            Rectangle2D tabAbsolute = getAbsoluteRect(tabPane);
            if(tabAbsolute.contains(screenPoint)) {
                int tabInsertIndex = 0;
                if(!tabPane.getTabs().isEmpty()) {
                    Rectangle2D firstTabRect = getAbsoluteRect(tabPane.getTabs().get(0));
                    if(firstTabRect.getMaxY()+60 < screenPoint.getY() || firstTabRect.getMinY() > screenPoint.getY()) {
                        return null;
                    }
                    Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));
                    if(screenPoint.getX() < (firstTabRect.getMinX() + firstTabRect.getWidth() / 2)) {
                        tabInsertIndex = 0;
                    }
                    else if(screenPoint.getX() > (lastTabRect.getMaxX() - lastTabRect.getWidth() / 2)) {
                        tabInsertIndex = tabPane.getTabs().size();
                    }
                    else {
                        for(int i = 0; i < tabPane.getTabs().size() - 1; i++) {
                            Tab leftTab = tabPane.getTabs().get(i);
                            Tab rightTab = tabPane.getTabs().get(i + 1);
                            if(leftTab instanceof DraggableTab && rightTab instanceof DraggableTab) {
                                Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                if(betweenX(leftTabRect, rightTabRect, screenPoint.getX())) {
                                    tabInsertIndex = i + 1;
                                    break;
                                }
                            }
                        }
                    }
                }
                return new InsertData(tabInsertIndex, tabPane);
            }
        }
        return null;
    }
 
    private Rectangle2D getAbsoluteRect(Control node) {
        return new Rectangle2D(node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() + node.getScene().getWindow().getX(),
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() + node.getScene().getWindow().getY(),
                node.getWidth(),
                node.getHeight());
    }
 
    private Rectangle2D getAbsoluteRect(Tab tab) {
        Control node = ((DraggableTab) tab).getLabel();
        return getAbsoluteRect(node);
    }
 
    private Label getLabel() {
        return nameLabel;
    }
 
    private boolean betweenX(Rectangle2D r1, Rectangle2D r2, double xPoint) {
        double lowerBound = r1.getMinX() + r1.getWidth() / 2;
        double upperBound = r2.getMaxX() - r2.getWidth() / 2;
        return xPoint >= lowerBound && xPoint <= upperBound;
    }
 
    private static class InsertData {
 
        private final int index;
        private final TabPane insertPane;
 
        public InsertData(int index, TabPane insertPane) {
            this.index = index;
            this.insertPane = insertPane;
        }
 
        public int getIndex() {
            return index;
        }
 
        public TabPane getInsertPane() {
            return insertPane;
        }
 
    }
}