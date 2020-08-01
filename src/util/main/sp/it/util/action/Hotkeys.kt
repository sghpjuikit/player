package sp.it.util.action

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ALT
import javafx.scene.input.KeyCode.ALT_GRAPH
import javafx.scene.input.KeyCode.COMMAND
import javafx.scene.input.KeyCode.CONTROL
import javafx.scene.input.KeyCode.META
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.WINDOWS
import mu.KLogging
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeInputEvent
import org.jnativehook.NativeInputEvent.BUTTON1_MASK
import org.jnativehook.NativeInputEvent.BUTTON2_MASK
import org.jnativehook.NativeInputEvent.BUTTON3_MASK
import org.jnativehook.NativeInputEvent.BUTTON4_MASK
import org.jnativehook.NativeInputEvent.BUTTON5_MASK
import org.jnativehook.NativeInputEvent.CAPS_LOCK_MASK
import org.jnativehook.NativeInputEvent.NUM_LOCK_MASK
import org.jnativehook.NativeInputEvent.SCROLL_LOCK_MASK
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import sp.it.util.dev.fail
import sp.it.util.functional.Util.list
import sp.it.util.functional.runTry
import sp.it.util.type.atomic
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Global hotkey manager, implemented on top of JNativeHook library. */
class Hotkeys(private val executor: (Runnable) -> Unit) {
   private val keyCombos = ConcurrentHashMap<String, KeyCombo>()
   private var keyListener: NativeKeyListener? = null
   private var isRunning = false

   init {
      // Only log warnings from JNativeHook
      java.util.logging.Logger.getLogger(GlobalScreen::class.java.getPackage().name).apply {
         level = java.util.logging.Level.WARNING
         useParentHandlers = false
      }
   }

   fun isRunning(): Boolean = isRunning

   fun start() {
      if (!isRunning) {
         logger.info { "Starting global hotkeys" }
         isRunning = true

         val eventDispatcher = object: AbstractExecutorService() {
            private var running by atomic(true)

            override fun shutdown() {
               running = false
            }
            override fun shutdownNow(): List<Runnable> {
               running = false
               return list()
            }
            override fun isShutdown() = !running
            override fun isTerminated() = !running
            override fun awaitTermination(amount: Long, units: TimeUnit) = true
            override fun execute(action: Runnable) = action.run()
         }
         val keyListener = object: NativeKeyListener {
            override fun nativeKeyPressed(e: NativeKeyEvent) {
               val modifiers = e.modifiers.withoutIgnoredModifiers()

               keyCombos.forEach { (actionId, keyCombo) ->
                  // For some reason left BACK_SLASH key (left of the Z key) is not recognized, recognize manually
                  if (e.rawCode==226) {
                     e.keyCode = 43
                     e.keyChar = 43.toChar()
                  }

                  // Unfortunately, JavaFX key codes and the library raw codes do not match for some keys, so we also
                  // check key name. This combination should be enough for all but rare cases
                  val modifiersMatch = keyCombo.modifier==modifiers
                  val keysMatch by lazy { keyCombo.key.code==e.rawCode || keyCombo.key.getName().equals(NativeKeyEvent.getKeyText(e.keyCode), ignoreCase = true) }
                  if (keysMatch && modifiersMatch) {
                     val action = ActionRegistrar[actionId]
                     keyCombo.press(action, e)
                  }
               }
            }

            override fun nativeKeyReleased(e: NativeKeyEvent) {
               keyCombos.values.forEach { if (it.isPressed) it.release() }
            }

            override fun nativeKeyTyped(e: NativeKeyEvent) {}
         }

         runTry {
            GlobalScreen.setEventDispatcher(eventDispatcher)
            GlobalScreen.addNativeKeyListener(keyListener)
            GlobalScreen.registerNativeHook()
            this.keyListener = keyListener
         }.ifError {
            logger.error(it) { "Failed to register global hotkeys" }
         }
      }
   }

   fun stop() {
      if (isRunning) {
         runTry {
            logger.info { "Stopping global hotkeys" }
            GlobalScreen.removeNativeKeyListener(keyListener)
            keyListener = null
            GlobalScreen.unregisterNativeHook()
         }.ifError {
            logger.error(it) { "Failed to unregister global hotkeys" }
         }
         isRunning = false
      }
   }

   fun register(action: Action, keys: String) {
      val keyString = keys.substringAfterLast('+').trim().toLowerCase().replace('_', ' ')
      val key = KeyCode.values().find { it.getName().equals(keyString, true) } ?: fail { "No KeyCode for ${action.keys}" }
      register(
         action,
         key,
         *sequenceOf(ALT, ALT_GRAPH, SHIFT, CONTROL, WINDOWS, COMMAND, META)
            .filter { keys.contains(it.name, true) || (it==CONTROL && keys.contains("CTRL", true)) }
            .toList().toTypedArray()
      )
   }

   fun register(action: Action, key: KeyCode, vararg modifiers: KeyCode) {
      keyCombos[action.name] = KeyCombo(key, *modifiers)
   }

   fun unregister(action: Action) {
      keyCombos.remove(action.name)
   }

   private inner class KeyCombo {
      val key: KeyCode
      val modifier: Int
      var isPressed = false
         private set

      constructor(key: KeyCode, vararg modifiers: KeyCode) {
         this.key = key
         this.modifier = run {
            var m = 0
            infix fun KeyCode.toMask(mask: Int) {
               if (this in modifiers)
                  m += mask
            }
            SHIFT toMask NativeInputEvent.SHIFT_L_MASK
            CONTROL toMask NativeInputEvent.CTRL_L_MASK
            ALT toMask NativeInputEvent.ALT_L_MASK
            COMMAND toMask NativeInputEvent.META_R_MASK
            ALT_GRAPH toMask NativeInputEvent.ALT_R_MASK
            WINDOWS toMask NativeInputEvent.META_L_MASK
            META toMask NativeInputEvent.META_L_MASK
            m
         }
      }

      fun press(a: Action, e: NativeKeyEvent) {
         val isPressedFirst = !isPressed
         isPressed = true
         if (a.isContinuous || isPressedFirst) {
            // consume event (must be on the jNativeHook thread)
            try {
               val f = NativeInputEvent::class.java.getDeclaredField("reserved")
               f.isAccessible = true
               f.setShort(e, 0x01.toShort())
            } catch (x: Exception) {
               logger.error(x) { "Failed to consume native key event" }
            }

            executor(a)
         }
      }

      fun release() {
         isPressed = false
      }
   }

   companion object: KLogging() {
      private var ignoredModifiers = listOf(
         SCROLL_LOCK_MASK,
         CAPS_LOCK_MASK,
         NUM_LOCK_MASK,
         BUTTON1_MASK,
         BUTTON2_MASK,
         BUTTON3_MASK,
         BUTTON4_MASK,
         BUTTON5_MASK
      )
      private var ignoredModifiersInvMask = ignoredModifiers.reduce(Int::or).inv()

      private fun Int.withoutIgnoredModifiers(): Int = this and ignoredModifiersInvMask
   }

}