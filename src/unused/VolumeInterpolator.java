/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package unused;

/**
 *
 * @author Plutonium_
 */
public enum VolumeInterpolator {
    
    LINEAR {
        @Override
        public double interpolate(double t) {
            return t;
        }
    },
//    LOGARITHMIC {
//        @Override
//        public double interpolate(double t) {
////            y = a·exp(b·x)
//        }
//    },
    QUADRATIC {
        @Override
        public double interpolate(double t) {
//            return t * t * t * t;
            return Math.sqrt(t);
        }
    };
    abstract public double interpolate(double t);    
}
