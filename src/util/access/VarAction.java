/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import java.util.function.Consumer;

import action.Action;
import util.dev.TODO;

import static util.functional.Util.map;

/**
 * Accessor for Action. Lists all available actions. ComboBox should be used as
 * gui.
 */
@TODO(purpose = TODO.Purpose.UNIMPLEMENTED, note = "Action.class should be implemented so "
        + "this class is unneeded. It involves separating toS and toString methods. Research.")
public class VarAction extends VarEnum<String> {

    public VarAction(Action a, Consumer<Action> applier) {
        super(a.getName(), c(applier), () -> map(Action.getActions(), Action::getName));
    }

    public VarAction(String action_name, Consumer<Action> applier) {
        super(action_name, c(applier), () -> map(Action.getActions(), Action::getName));
    }

    public Action getValueAction() {
        return Action.get(getValue());
    }

    private static Consumer<String> c(Consumer<Action> applier) {
        return applier==null ? null : name -> applier.accept(Action.get(name));
    }
}
