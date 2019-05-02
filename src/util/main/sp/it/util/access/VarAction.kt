package sp.it.util.access

import sp.it.util.action.Action
import sp.it.util.action.ActionRegistrar
import sp.it.util.reactive.attach

/** Accessor for Action. Lists all available actions. */
class VarAction: VarEnum<String> {

    constructor(action: Action, onChange: (Action) -> Unit = {}): super(action.name, { ActionRegistrar.getActions().map { it.name } }) {
        apply {
            attach { onChange(ActionRegistrar[it]) }
        }
    }

    constructor(actionName: String, onChange: (Action) -> Unit = {}): super(actionName, { ActionRegistrar.getActions().map { it.name } }) {
        apply {
            attach { onChange(ActionRegistrar[it]) }
        }
    }

    val valueAsAction: Action
        get() = ActionRegistrar[value]

}