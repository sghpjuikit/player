/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

/**
 *
 * @author uranium
 */
public class VerticalContextMenu extends Context_Menu{
    
    @Override
    public double getHeight() {
        return elements.size()*elements.get(0).getHeight();
    }
    @Override
    public double getWidth() {
        if (elements.isEmpty()) return 0;
        return elements.get(0).getWidth();
    }

    @Override
    void open(double X, double Y) {
        int amount = elements.size();
        double height = elements.get(0).getHeight();
        double angle = 0;
        // populate
        for (ContextElement e: elements) {
            int index = elements.indexOf(e);
            double xx = 0;
            double yy = index*height;
            e.relocate(xx, yy, angle);
        }
        // shift whole menu:
        menu.relocate(X, Y-(amount/2*height));
        offScreenFix();
    }
}
