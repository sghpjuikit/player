/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import Action.Action;
import java.util.function.Consumer;
import util.dev.TODO;
import static util.functional.Util.listM;

/**
 * Accessor for Action. Lists all available actions. ComboBox should be used as
 * gui.
 */
@TODO(purpose = TODO.Purpose.UNIMPLEMENTED, note = "Action.class should be implemented so "
        + "this class is unneeded. It involves separating toS and toString methods. Research.")
public class AccessorAction extends AccessorEnum<String> {
    
    
    public AccessorAction(Action a, Consumer<Action> applier) {
        super(a.getName(), applier==null ? null : name -> applier.accept(Action.getAction(name)), () -> listM(Action.getActions(), Action::getName));
    }
    
    public AccessorAction(String action_name, Consumer<Action> applier) {
        super(action_name, applier==null ? null : name -> applier.accept(Action.getAction(name)), () -> listM(Action.getActions(), Action::getName));
    }
    
    public Action getValueAction() {
        return Action.getAction(getValue());
    }
}
