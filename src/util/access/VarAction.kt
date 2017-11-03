package util.access

import util.action.Action
import java.util.function.Consumer

// TODO: "Action.class should be implemented so this class is unneeded. It involves separating toS and toString methods.
/** Accessor for Action. Lists all available actions. */
class VarAction: VarEnum<String> {

    @JvmOverloads
    constructor(a: Action, applier: Consumer<Action> = Consumer {})
            : super(a.name, { Action.getActions().map { it.name } }, Consumer { applier.accept(Action.get(it)) })

    @JvmOverloads
    constructor(action_name: String, applier: Consumer<Action> = Consumer {})
            : super(action_name, { Action.getActions().map { it.name } }, Consumer { applier.accept(Action.get(it)) })

    fun getValueAction(): Action = Action.get(value)

}