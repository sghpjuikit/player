/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import Action.Action;
import java.util.function.Consumer;
import util.TODO;
import static util.functional.FunctUtil.listM;

/**
 * Accessor for Action. Lists all available actions. ComboBox should be used as
 * gui.
 */
@TODO(purpose = TODO.Purpose.UNIMPLEMENTED, note = "Action.class should be implemented so "
        + "this class is unneeded. It involves separating toS and toString methods. Research.")
public class AccessorAction extends AccessorEnum<String> {
    
    private AccessorAction(String val, Consumer<String> applier) {
        super(val, applier, () -> listM(Action.getActions(), Action::getName));
    }
    
    public AccessorAction(Action a, Consumer<Action> applier) {
        this(a.getName(), applier==null ? null : name -> applier.accept(Action.getAction(name)));
    }
    
    public Action getValueAction() {
        return Action.getAction(getValue());
    }
}
