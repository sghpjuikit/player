package sp.it.pl.util.access

import sp.it.pl.util.action.Action
import sp.it.pl.util.action.ActionRegistrar

/** Accessor for Action. Lists all available actions. */
class VarAction: VarEnum<String> {

    constructor(action: Action, onChange: (Action) -> Unit = {}): super(action.name, { ActionRegistrar.getActions().map { it.name } }) {
        initAttach { onChange(ActionRegistrar[it]) }
    }

    constructor(actionName: String, onChange: (Action) -> Unit = {}): super(actionName, { ActionRegistrar.getActions().map { it.name } }) {
        initAttach { onChange(ActionRegistrar[it]) }
    }

    val valueAsAction: Action
        get() = ActionRegistrar[value]

}