/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Balancer;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import java.util.Collections;

/**
 *
 * @author uranium
 */
public class BalancerBehavior extends BehaviorBase<Balancer> {
    public BalancerBehavior(Balancer control) {
        super(control, Collections.<KeyBinding> emptyList());
    }
}
