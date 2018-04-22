package sp.it.pl.util.action

import javafx.application.Platform
import javafx.scene.input.KeyCode.ALT_GRAPH
import javafx.stage.Stage
import sp.it.pl.main.c
import sp.it.pl.main.cv
import sp.it.pl.main.readOnlyUnless
import sp.it.pl.util.access.initSync
import sp.it.pl.util.access.v
import sp.it.pl.util.action.ActionRegistrar.hotkeys
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.hotkey.Hotkeys
import sp.it.pl.util.reactive.Subscribed
import sp.it.pl.util.reactive.onItemAdded
import sp.it.pl.util.reactive.onItemRemoved
import sp.it.pl.util.reactive.sync1IfNonNull
import sp.it.pl.util.system.Os
import java.util.concurrent.ConcurrentHashMap

@IsConfigurable(Action.CONFIG_GROUP)
object ActionManager {

    @IsConfig(name = "Media shortcuts enabled", info = "Allows using shortcuts for media keys on the keyboard.")
    val globalMediaShortcuts by cv(true)

    //    @IsConfig(name = "Allow in-app shortcuts", info = "Allows using standard shortcuts.", group = "Shortcuts")
    //    public static final V<Boolean> local_shortcuts = new V<>(true, v -> {
    //        if (isLocalShortcutsSupported()) {
    //            if (v){
    //            } else {
    //
    //            }
    //        }
    //    });

    @IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout management mode.")
    var Shortcut_ALTERNATE by c(ALT_GRAPH)

    @IsConfig(name = "Collapse layout", info = "Collapses focused container within layout.", editable = EditMode.NONE)
    val Shortcut_COLAPSE by c("Shift+C")

    /**
     * Returns true iff global shortcuts are supported at running platform.
     * Otherwise false. In such case, global shortcuts will run as local and
     * [.startGlobalListening] and [.stopGlobalListening] will
     * have no effect.
     */
    @IsConfig(name = "Global shortcuts supported", editable = EditMode.NONE, info = "Whether global shortcuts are supported on this system")
    val isGlobalShortcutsSupported by c(true)

    @IsConfig(name = "Media shortcuts supported", editable = EditMode.NONE, info = "Whether media shortcuts are supported on this system")
    private val isMediaShortcutsSupported by c(true)

    /** @return whether the action listening is running */
    var isActionListening = false
        private set

    @IsConfig(name = "Global shortcuts enabled", info = "Allows using the shortcuts even if application is not focused.")
    val globalShortcuts by cv(Os.WINDOWS.isCurrent) {
        v(it).initSync {
            if (isGlobalShortcutsSupported) {
                if (it) {
                    startGlobalListening()
                    // re-register shortcuts to switch from local
                    ActionRegistrar.getActions().forEach { a ->
                        a.unregister()
                        a.register()
                    }
                } else {
                    stopGlobalListening()
                    // re-register shortcuts to switch to local
                    ActionRegistrar.getActions().forEach { a ->
                        a.unregister()
                        a.register()
                    }
                }
            }
        }
    }.readOnlyUnless { isGlobalShortcutsSupported }


    /* ---------- HELPER METHODS ---------------------------------------------------------------------------------------- */

    // TODO: Subscriptions are leaking here
    private val localActionRegisterer1 = Subscribed {
        Stage.getWindows().onItemAdded { it.sceneProperty().sync1IfNonNull {
                scene -> ActionRegistrar.getActions().forEach { if (!it.isGlobal) it.registerInScene(scene) }
        } }
    }
    private val localActionRegisterer2 = Subscribed {
        Stage.getWindows().onItemRemoved { it.scene?.let {
                scene -> ActionRegistrar.getActions().forEach { if (!it.isGlobal) it.unregisterInScene(scene) }
        } }
    }

    /**
     * Activates listening process for hotkeys. Not running this method will cause hotkeys to not
     * get invoked.
     * Must not be ran more than once.
     * Does nothing if not supported.
     *
     * @throws IllegalStateException if ran more than once without calling [.stopActionListening]
     */
    fun startActionListening() {
        if (isActionListening) throw IllegalStateException("Action listening already running")
        startLocalListening()
        if (isGlobalShortcutsSupported && globalShortcuts.get()) startGlobalListening()
        isActionListening = true
    }

    /**
     * Deactivates listening process for hotkeys (global and local), causing them to stop working.
     * Frees resources. This method should should always be ran when [.startActionListening]
     * was invoked. Not doing so may prevent the application from closing successfully, due to non
     * daemon thread involved here.
     */
    fun stopActionListening() {
        stopLocalListening()
        stopGlobalListening()
        isActionListening = false
    }

    /** Activates listening process for local hotkeys.  */
    private fun startLocalListening() {
        localActionRegisterer1.subscribe(true)
        localActionRegisterer2.subscribe(true)
    }

    /** Deactivates listening process for local hotkeys. */
    private fun stopLocalListening() {
        localActionRegisterer1.subscribe(false)
        localActionRegisterer2.subscribe(false)
        Stage.getWindows().forEach { window ->
            window.scene?.let { scene -> ActionRegistrar.getActions().forEach { it.unregisterInScene(scene) } }
        }
    }

    /**
     * Activates listening process for global hotkeys. Not running this method
     * will cause registered global hotkeys to not get invoked. Use once when
     * application initializes.
     * Does nothing if not supported.
     */
    private fun startGlobalListening() {
        hotkeys.start()
    }

    /**
     * Deactivates listening process for global hotkeys. Frees resources. This
     * method should should always be ran at the end of application's life cycle
     * if [.startGlobalListening] ()} was invoked at least once.
     * Not doing so might prevent from the application to close successfully,
     * because bgr listening thread will not close.
     */
    private fun stopGlobalListening() {
        hotkeys.stop()
    }

}

object ActionRegistrar {
    val hotkeys by lazy { Hotkeys { Platform.runLater(it) } }

    private val actions = MapSet<Int, Action>(ConcurrentHashMap()) { it.id }
            .apply { this += Action.EMPTY }

    /**
     * Returns modifiable collection of all actions mapped by their name. Actions
     * can be added and removed, which modifies the underlying collection.
     *
     * @return all actions.
     */
    fun getActions(): MutableCollection<Action> = actions

    /**
     * @throws RuntimeException if no such action
     */
    operator fun get(id: Int): Action = actions[id]
            ?: throw IllegalArgumentException("No such action: '$id'. Make sure the action is declared and annotation processing is enabled and functioning properly.")

    /**
     *
     * @param name name of the action
     * @return the action with specified name or throws an exception (it is a programmatic error if an action does not exist)
     * @throws RuntimeException if no such action
     */
    operator fun get(name: String): Action = actions[idOf(name)]
            ?: throw IllegalArgumentException("No such action: '$name'. Make sure the action is declared and annotation processing is enabled and functioning properly.")

    // Guarantees consistency
    fun idOf(actionName: String): Int {
        return actionName.hashCode()
    }
}