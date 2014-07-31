
package Layout;

import javafx.geometry.Orientation;

/**
 * Pure container implementation of {@link BiContainer BiContainer}.
 * <p>
 * This implementation can not handle non-Container children and instead
 * delegates their handling by wrapping them into {@link UniContainer Unicontainer}
 * <p>
 * @author uranium
 */
public class BiContainerPure extends BiContainer {

    public BiContainerPure(Orientation orientation) {
        super(orientation);
    }
    
    /**
     * Adds the widget as child.
     * Since there is only one child, the index parameter is ignored.
     * @param c widget or container. Null value will clear all children.
     * @param index is can only take 1 or 2 value. other values will do
     * nothing.
     */
    @Override
    public void addChild(Integer index, Component c) {
//        if(index<1 || index>2) return; // make sure index == 1 | 2
//        
//        // wrap if component is widget
//        if(!(c instanceof Container)) {
//            UniContainer wrapper = null;
//            if(index==1) wrapper = new UniContainer(gui.getChild1Pane());
//            else if(index==2) wrapper = new UniContainer(gui.getChild2Pane());
//            wrapper.child=c;
//            c = wrapper;
//        }
//        
//        // continue normally
//        super.addChild(index, c);
        
        
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA " + index + " " + c);
        if(index == null) return;
        
        if (index<1 || index>2)
            throw new IndexOutOfBoundsException("Index " + index + " not supported. Only null,1,2 values supported.");
        System.out.println(!(c instanceof  Container));
        // wrap if component is widget
        if(!(c instanceof Container))
            getChildren().put(index, null);
        else getChildren().put(index, c);
        load();
        initialize();
                
//        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA " + index + " " + c);
//        if(index == null) return;
//        
//        if (index<1 || index>2)
//            throw new IndexOutOfBoundsException("Index " + index + " not supported. Only null,1,2 values supported.");
//        System.out.println(!(c instanceof  Container));
//        // wrap if component is widget
//        if(!(c instanceof Container)) {
//            UniContainer wrapper = null;
//            if(index==1) wrapper = new UniContainer(gui.getChild1Pane());
//            else if(index==2) wrapper = new UniContainer(gui.getChild2Pane());
//            getChildren().put(index, wrapper);
//            wrapper.child = c;
//            wrapper.parent = this;
//            wrapper.load();
//            c = wrapper;
//        }
//        
//        getChildren().put(index, c);
//        load();
//        parent.load();
        
    }
}