/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused;

/**
 * Circular context menu.
 * 
 * @author Martin Polakovic
 */
public class CircleContextMenu {
    private static double radius = 200;
    private static final double PI = Math.PI; // 360 degrees = 2PI
    private static double minAngle = 0;       // min degree
    private static double maxAngle = 2*PI;    // max degree
    
    public CircleContextMenu() {
        
    }
    
//    void open(double X, double Y) {
//        if (elements.isEmpty()) return;
//        minAngle = 0;
//        maxAngle = 2*PI;
//
//        position(X,Y);
//        
//        // populate
//        double angle_interval = (maxAngle>minAngle) ? maxAngle-minAngle : 2*PI-minAngle+maxAngle;
//        double angle_unit = angle_interval/elements.size();
//        if(angle_interval!=2*PI) angle_unit += angle_unit/(elements.size()-1); // occupy empty last position in semi-circles
//        for (ContextElement e: elements) {
//            int index = elements.indexOf(e);
//            double angle = index * angle_unit + minAngle;
//            double polarX = radius * Math.cos(angle); // calculate math. position
//            double polarY = radius * Math.sin(angle); // calculate math. position
//            polarY *= -1;// mirror vertically coordinates (math->gui implementation)
//            polarX += getWidth()/2; // shift to gui [0;0] (eliminate negative coords)
//            polarY += getHeight()/2; // shift to gui [0;0] (eliminate negative coords)
//            e.relocateCenter(polarX, polarY, angle); //position and centre
//        }
//    }
//     
//    public double getHeight() {
//        if (elements.isEmpty()) return 0;
//        return 2*radius+elements.get(0).getHeight();
//    }
//    public double getWidth() {
//        if (elements.isEmpty()) return 0;
//        return 2*radius+elements.get(0).getWidth();
//    }
//    public void setRadius(double r) {
//        radius = r;
//    }
//    
//    public void offScreenFix() {
//        position(getLayoutX(), getLayoutY());
//    }
//    
//    /** Positions menu at coordinates. */
//    private void position(double X, double Y) {
//        double xOffset = elements.get(0).getWidth()/2;
//        double yOffset = elements.get(0).getHeight()/2;
//        
//        menu.resize(getWidth(), getHeight());
//        menu.setMaxSize(getWidth(), getHeight());
//        menu.setMinSize(getWidth(), getHeight());
//        menu.relocate(X-getWidth()/2+xOffset, Y-getHeight()/2+yOffset);// shift whole menu: to center on [X;Y]
//        calculateScreenFix();
//    }
//    
//    /** recalculates max/minAngle based on position within boundaries of display */
//    private void calculateScreenFix() {
//        double x = getLayoutX();    // leftmost corner
//        double y = getLayoutY();    // leftmost corner
//        double h = getHeight();     // height
//        double w = getWidth();      // width
//        double hr = h/2;            // hight radius
//        double wr = w/2;            // width radius
//        double H = getDisplay().getHeight();    // display height
//        double W = getDisplay().getWidth();     // display width
//        double dx = offScreenFixOFFSET + getElementWidth()/2;   // offset x
//        double dy = offScreenFixOFFSET+ getElementHeight()/2;   // offset y
//
//        if (x<0 && y<0) {
//            double diffx = -x + dx;
//            minAngle = PI + Math.acos((wr-diffx)/radius);
//            double diffy = -y + dy;
//            maxAngle = Math.asin((hr-diffy)/radius);
//        } else
//        if (x<0 && y+h>H) {
//            double diffy = h+y-H + dy;
//            minAngle = -Math.asin((hr-diffy)/radius);
//            double diffx = -x + dx;
//            maxAngle = PI - Math.acos((wr-diffx)/radius);
//        } else
//        if (x+w>W && y<0) {
//            double diffy = -y + dy;
//            minAngle = PI-Math.asin((hr-diffy)/radius);
//            double diffx = w+x-W + dx;
//            maxAngle = -Math.acos((wr-diffx)/radius);
//        } else
//        if (x+w>W && y+h>H) {
//            double diffx = w+x-W + dx;
//            minAngle = Math.acos((wr-diffx)/radius);
//            double diffy = h+y-H + dy;
//            maxAngle = PI+Math.asin((hr-diffy)/radius);
//        } else
//        if (x<0) {
//            double diffx = -x + dx;
//            minAngle = PI+Math.acos((wr-diffx)/radius);
//            maxAngle = PI-Math.acos((wr-diffx)/radius);
//        } else
//        if (y<0) {
//            double diffy = -y + dy;
//            minAngle = PI-Math.asin((hr-diffy)/radius);
//            maxAngle = Math.asin((hr-diffy)/radius);
//        } else
//        if (x+w>W) {
//            double diffx = w+x-W + dx;
//            minAngle = Math.acos((wr-diffx)/radius);
//            maxAngle = -Math.acos((wr-diffx)/radius);
//        } else
//        if (y+h>H) {
//            double diffy = h+y-H + dy;
//            minAngle = Math.asin(-(hr-diffy)/radius);
//            maxAngle = PI-Math.asin(-(hr-diffy)/radius);
//        }
//    }
}
