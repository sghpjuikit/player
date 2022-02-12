package sp.it.util.action

import java.util.concurrent.ConcurrentHashMap
import javafx.application.Platform
import javafx.scene.input.KeyCode.ALT_GRAPH
import javafx.scene.input.KeyCode.COMMA
import javafx.scene.input.KeyCode.PERIOD
import javafx.scene.input.KeyCode.SLASH
import javafx.stage.Stage
import sp.it.util.access.v
import sp.it.util.action.ActionRegistrar.hotkeys
import sp.it.util.collections.mapset.MapSet
import sp.it.util.conf.EditMode.NONE
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyUnless
import sp.it.util.dev.fail
import sp.it.util.functional.orNull
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.syncNonNullWhile

object ActionManager: GlobalSubConfigDelegator(Action.CONFIG_GROUP) {

   val keyManageLayout by c(ALT_GRAPH).def(name = "Manage Layout (fast) Shortcut", info = "Enables layout management mode.", editable = NONE)
   val keyShortcuts by c(COMMA).def(name = "Show shortcuts", info = "Display all available shortcuts.", editable = NONE)
   val keyShortcutsComponent by c(PERIOD).def(name = "Show component shortcuts", info = "Display all available component shortcuts.", editable = NONE)
   val keyActionsComponent by c(SLASH).def(name = "Show component actions", info = "Display all available component actions.", editable = NONE)

   // @IsConfig(name = "Media shortcuts supported", editable = NONE, info = "Whether media shortcuts are supported on this system")
   // private val isMediaShortcutsSupported by c(true)

   // @IsConfig(name = "Media shortcuts enabled", info = "Allows using shortcuts for media keys on the keyboard.")
   // val globalMediaShortcutsEnabled by cv(true)

   /**
    * Whether global shortcuts are supported by the active platform.
    * If not, global shortcuts will run as local and [startGlobalListening]
    * and [stopGlobalListening] will have no effect.
    *
    * @return true iff global shortcuts are supported at running platform
    */
   val isGlobalShortcutsSupported by c(true).def(
      name = "Global shortcuts supported",
      editable = NONE,
      info = "Whether global shortcuts are supported on this system"
   )
   val globalShortcutsEnabled by cv(true) { v(it && isGlobalShortcutsSupported) }
      .readOnlyUnless(isGlobalShortcutsSupported)
      .def(
         name = "Global shortcuts enabled",
         info = "Allows using the shortcuts even if application is not focused."
      ) sync {
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

   /** @return whether the action listening is running */
   var isActionListening = false
      private set


   /* ---------- HELPER METHODS ---------------------------------------------------------------------------------------- */

   /**
    * Activates listening process for hotkeys. Not running this method will cause hotkeys to not
    * get invoked.
    * Must not be run more than once.
    * Does nothing if not supported.
    *
    * @param ignoreGlobal disables global shortcut listening (global shortcuts will become dysfunctional - no-op)
    * @throws IllegalStateException if ran more than once without calling [stopActionListening] in between
    */
   fun startActionListening(ignoreGlobal: Boolean = false) {
      if (isActionListening) throw IllegalStateException("Action listening already running")
      startLocalListening()
      if (globalShortcutsEnabled.value && !ignoreGlobal) startGlobalListening()
      isActionListening = true
   }

   /**
    * Deactivates listening process for hotkeys (global and local), causing them to stop working.
    * Frees resources. This method should always be run when [startActionListening]
    * was invoked. Not doing so may prevent the application from closing successfully, due to non
    * daemon thread involved here.
    */
   fun stopActionListening() {
      stopLocalListening()
      stopGlobalListening()
      isActionListening = false
   }

   private val localActionRegisterer = Subscribed {
      Stage.getWindows().onItemSyncWhile {
         it.sceneProperty().syncNonNullWhile { scene ->
            ActionRegistrar.getActions().forEach { it.registerInScene(scene) }
            Subscription { ActionRegistrar.getActions().forEach { it.unregisterInScene(scene) } }
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
      hotkeys.value.start()
   }

   /**
    * Deactivates listening process for global hotkeys. Frees resources. This
    * method should always be run at the end of application's life cycle
    * if [startGlobalListening] was invoked at least once.
    * Not doing so might prevent from the application to close successfully,
    * because bgr listening thread will not close.
    */
   private fun stopGlobalListening() {
      hotkeys.orNull()?.stop()
   }

   /** Invokes immediately before [Action.run]. */
   val onActionRunPre = Handler1<Action>()
   /** Invokes immediately after [Action.run]. */
   val onActionRunPost = Handler1<Action>()
}

object ActionRegistrar {
   val hotkeys = lazy { Hotkeys { Platform.runLater(it) } }

   private val actions = MapSet<String, Action>(ConcurrentHashMap()) { it.name }.apply {
      this += Action.NONE
   }

   /**
    * Returns the MutableCollection of all actions mapped by their name.
    *
    * @return all actions
    */
   fun getActions(): MutableCollection<Action> = actions

   /**
    * @return the action with the given [name]
    * @throws RuntimeException if no action with that name exists (programmatic error)
    */
   @JvmStatic operator fun get(name: String): Action = actions[name] ?: fail { "No action: '$name' found. " }

   /** @return the action with the given [name] or null */
   @JvmStatic fun getOrNull(name: String): Action? = actions[name]

}
