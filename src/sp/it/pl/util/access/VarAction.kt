package sp.it.pl.util.access

import sp.it.pl.util.action.Action
import sp.it.pl.util.action.ActionRegistrar
import sp.it.pl.util.functional.invoke
import java.util.function.Consumer

// TODO: "Action.class should be implemented so this class is unneeded. It involves separating toS and toString methods.
/** Accessor for Action. Lists all available actions. */
class VarAction: VarEnum<String> {

    @JvmOverloads
    constructor(a: Action, applier: Consumer<in Action> = Consumer {})
            : super(a.name, { ActionRegistrar.getActions().map { it.name } }, Consumer { applier(ActionRegistrar[it]) })

    @JvmOverloads
    constructor(action_name: String, applier: Consumer<in Action> = Consumer {})
            : super(action_name, { ActionRegistrar.getActions().map { it.name } }, Consumer { applier(ActionRegistrar[it]) })

    fun getValueAction(): Action = ActionRegistrar[value]

}