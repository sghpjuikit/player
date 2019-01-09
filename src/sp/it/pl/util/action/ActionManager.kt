package sp.it.pl.util.action

import javafx.application.Platform
import javafx.scene.input.KeyCode.ALT_GRAPH
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.WINDOWS
import javafx.stage.Stage
import javafx.stage.Window
import org.reactfx.Subscription
import sp.it.pl.util.access.initSync
import sp.it.pl.util.access.v
import sp.it.pl.util.action.ActionRegistrar.hotkeys
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.conf.EditMode.NONE
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.readOnlyUnless
import sp.it.pl.service.hotkey.Hotkeys
import sp.it.pl.util.reactive.Subscribed
import sp.it.pl.util.reactive.onItemSync
import sp.it.pl.util.reactive.syncIntoWhile
import sp.it.pl.util.system.Os
import sp.it.pl.util.text.getNamePretty
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

    @IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout management mode.", editable = NONE)
    val keyManageLayout by c(ALT_GRAPH)

    @IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout management mode.", editable = NONE)
    val keyManageWindow by c(WINDOWS.getNamePretty() + " + " + SHIFT.getNamePretty())

    /**
     * Whether global shortcuts are supported by the active platform.
     * If not, global shortcuts will run as local and [startGlobalListening] 
     * and [stopGlobalListening] will have no effect.
     *
     * @return true iff global shortcuts are supported at running platform 
     */
    @IsConfig(name = "Global shortcuts supported", editable = NONE, info = "Whether global shortcuts are supported on this system")
    val isGlobalShortcutsSupported by c(true)

    @IsConfig(name = "Media shortcuts supported", editable = NONE, info = "Whether media shortcuts are supported on this system")
    private val isMediaShortcutsSupported by c(true)

    /** @return whether the action listening is running */
    var isActionListening = false
        private set

    @IsConfig(name = "Global shortcuts enabled", info = "Allows using the shortcuts even if application is not focused.")
    val globalShortcutsEnabled by cv(Os.WINDOWS.isCurrent) {
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
    }.readOnlyUnless(isGlobalShortcutsSupported)


    /* ---------- HELPER METHODS ---------------------------------------------------------------------------------------- */

    /**
     * Activates listening process for hotkeys. Not running this method will cause hotkeys to not
     * get invoked.
     * Must not be ran more than once.
     * Does nothing if not supported.
     *
     * @throws IllegalStateException if ran more than once without calling [stopActionListening] in between
     */
    fun startActionListening() {
        if (isActionListening) throw IllegalStateException("Action listening already running")
        startLocalListening()
        if (isGlobalShortcutsSupported && globalShortcutsEnabled.get()) startGlobalListening()
        isActionListening = true
    }

    /**
     * Deactivates listening process for hotkeys (global and local), causing them to stop working.
     * Frees resources. This method should should always be ran when [startActionListening]
     * was invoked. Not doing so may prevent the application from closing successfully, due to non
     * daemon thread involved here.
     */
    fun stopActionListening() {
        stopLocalListening()
        stopGlobalListening()
        isActionListening = false
    }

    private val localActionRegisterer = Subscribed {
        Stage.getWindows().onItemSync {
            v(it).syncIntoWhile(Window::sceneProperty) { scene ->
                if (scene!=null) {
                    ActionRegistrar.getActions().forEach { it.registerInScene(scene) }
                    Subscription { ActionRegistrar.getActions().forEach { it.unregisterInScene(scene) } }
                } else {
                    Subscription.EMPTY
                }
            }
        }
    }

    /** Activates listening process for local hotkeys.  */
    private fun startLocalListening() {
        localActionRegisterer.subscribe(true)
    }

    /** Deactivates listening process for local hotkeys. */
    private fun stopLocalListening() {
        localActionRegisterer.subscribe(false)
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
     * if [startGlobalListening] ()} was invoked at least once.
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
     * Returns the MutableCollection of all actions mapped by their name.
     * 
     * @return all actions
     */
    fun getActions(): MutableCollection<Action> = actions

    /**
     * @return the action with the given [id]
     * @throws RuntimeException if no action with that id exists (programmatic error)
     */
    operator fun get(id: Int): Action = actions[id]
            ?: throw IllegalArgumentException("No such action: '$id'. Make sure the action is declared and annotation processing is enabled and functioning properly.")

    /**
     * @return the action with the given [name]
     * @throws RuntimeException if no action with that name exists (programmatic error)
     */
    operator fun get(name: String): Action = actions[idOf(name)]
            ?: throw IllegalArgumentException("No such action: '$name'. Make sure the action is declared and annotation processing is enabled and functioning properly.")

    // Guarantees consistency
    fun idOf(actionName: String) = actionName.hashCode()
    
}
