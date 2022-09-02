package sp.it.util.action

import java.util.concurrent.ConcurrentHashMap
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ALT
import javafx.scene.input.KeyCode.ALT_GRAPH
import javafx.scene.input.KeyCode.COMMAND
import javafx.scene.input.KeyCode.CONTROL
import javafx.scene.input.KeyCode.META
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.WINDOWS
import mu.KLogging
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeInputEvent
import com.github.kwhat.jnativehook.NativeInputEvent.BUTTON1_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.BUTTON2_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.BUTTON3_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.BUTTON4_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.BUTTON5_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.CAPS_LOCK_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.NUM_LOCK_MASK
import com.github.kwhat.jnativehook.NativeInputEvent.SCROLL_LOCK_MASK
import com.github.kwhat.jnativehook.dispatcher.VoidDispatchService
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import sp.it.util.dev.fail
import sp.it.util.functional.runTry

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

         val eventDispatcher = VoidDispatchService()
         val keyListener = object: NativeKeyListener {
            var modifiers = 0
            override fun nativeKeyPressed(e: NativeKeyEvent) {
               // For some reason left BACK_SLASH key (left of the Z key) is not recognized, recognize manually
               if (e.rawCode==226) {
                  e.keyCode = NativeKeyEvent.VC_BACK_SLASH
                  e.keyChar = '\\'
               }
               val key = nativeToFx[e.keyCode]
               val modifiers = e.modifiers.withoutIgnoredModifiers()

               keyCombos.forEach { (actionId, keyCombo) ->
                  if (keyCombo.modifier==modifiers && keyCombo.key==key) {
                     keyCombo.press(ActionRegistrar[actionId], e)
                  }
               }
            }

            override fun nativeKeyReleased(e: NativeKeyEvent) {
               keyCombos.values.forEach { if (it.isPressed) it.release(e) }
            }

            override fun nativeKeyTyped(e: NativeKeyEvent) {}
         }

         runTry {
            GlobalScreen.setEventDispatcher(eventDispatcher)
            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(keyListener)
            this.keyListener = keyListener
         }.ifError {
            logger.error(it) { "Failed to start global hotkeys" }
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
            logger.error(it) { "Failed to stop global hotkeys" }
         }
         isRunning = false
      }
   }

   fun register(action: Action, keys: String) {
      val keyString = keys.substringAfterLast('+').trim().lowercase().replace('_', ' ')
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
      val modifiers: Set<KeyCode>
      var isPressed = false
         private set

      constructor(key: KeyCode, vararg modifiers: KeyCode) {
         this.key = key
         this.modifiers = modifiers.toSet()
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
         e.consume()
         if (a.isContinuous || isPressedFirst) executor(a)
      }

      fun release(e: NativeKeyEvent) {
         isPressed = false
         e.consume()
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

      /** Consume event (must be on the jNativeHook thread) */
      private fun NativeKeyEvent.consume() {
         try {
            val f = NativeInputEvent::class.java.getDeclaredField("reserved")
            f.isAccessible = true
            f.setShort(this, 0x01.toShort())
         } catch (x: Exception) {
            logger.error(x) { "Failed to consume native key event" }
         }
      }

      private fun Int.withoutIgnoredModifiers(): Int = this and ignoredModifiersInvMask

      @Suppress("RemoveRedundantQualifierName")
      private val nativeToFx = mapOf(
         NativeKeyEvent.VC_ESCAPE to KeyCode.ESCAPE,
         NativeKeyEvent.VC_F1 to KeyCode.F1,
         NativeKeyEvent.VC_F2 to KeyCode.F2,
         NativeKeyEvent.VC_F3 to KeyCode.F3,
         NativeKeyEvent.VC_F4 to KeyCode.F4,
         NativeKeyEvent.VC_F5 to KeyCode.F5,
         NativeKeyEvent.VC_F6 to KeyCode.F6,
         NativeKeyEvent.VC_F7 to KeyCode.F7,
         NativeKeyEvent.VC_F8 to KeyCode.F8,
         NativeKeyEvent.VC_F9 to KeyCode.F9,
         NativeKeyEvent.VC_F10 to KeyCode.F10,
         NativeKeyEvent.VC_F11 to KeyCode.F11,
         NativeKeyEvent.VC_F12 to KeyCode.F12,
         NativeKeyEvent.VC_F13 to KeyCode.F13,
         NativeKeyEvent.VC_F14 to KeyCode.F14,
         NativeKeyEvent.VC_F15 to KeyCode.F15,
         NativeKeyEvent.VC_F16 to KeyCode.F16,
         NativeKeyEvent.VC_F17 to KeyCode.F17,
         NativeKeyEvent.VC_F18 to KeyCode.F18,
         NativeKeyEvent.VC_F19 to KeyCode.F19,
         NativeKeyEvent.VC_F20 to KeyCode.F20,
         NativeKeyEvent.VC_F21 to KeyCode.F21,
         NativeKeyEvent.VC_F22 to KeyCode.F22,
         NativeKeyEvent.VC_F23 to KeyCode.F23,
         NativeKeyEvent.VC_F24 to KeyCode.F24,
         NativeKeyEvent.VC_BACKQUOTE to KeyCode.BACK_QUOTE,
         NativeKeyEvent.VC_1 to KeyCode.DIGIT1,
         NativeKeyEvent.VC_2 to KeyCode.DIGIT2,
         NativeKeyEvent.VC_3 to KeyCode.DIGIT3,
         NativeKeyEvent.VC_4 to KeyCode.DIGIT4,
         NativeKeyEvent.VC_5 to KeyCode.DIGIT5,
         NativeKeyEvent.VC_6 to KeyCode.DIGIT6,
         NativeKeyEvent.VC_7 to KeyCode.DIGIT7,
         NativeKeyEvent.VC_8 to KeyCode.DIGIT8,
         NativeKeyEvent.VC_9 to KeyCode.DIGIT9,
         NativeKeyEvent.VC_0 to KeyCode.DIGIT0,
         NativeKeyEvent.VC_MINUS to KeyCode.MINUS,
         NativeKeyEvent.VC_EQUALS to KeyCode.EQUALS,
         NativeKeyEvent.VC_BACKSPACE to KeyCode.BACK_SPACE,
         NativeKeyEvent.VC_TAB to KeyCode.TAB,
         NativeKeyEvent.VC_CAPS_LOCK to KeyCode.CAPS,
         NativeKeyEvent.VC_A to KeyCode.A,
         NativeKeyEvent.VC_B to KeyCode.B,
         NativeKeyEvent.VC_C to KeyCode.C,
         NativeKeyEvent.VC_D to KeyCode.D,
         NativeKeyEvent.VC_E to KeyCode.E,
         NativeKeyEvent.VC_F to KeyCode.F,
         NativeKeyEvent.VC_G to KeyCode.G,
         NativeKeyEvent.VC_H to KeyCode.H,
         NativeKeyEvent.VC_I to KeyCode.I,
         NativeKeyEvent.VC_J to KeyCode.J,
         NativeKeyEvent.VC_K to KeyCode.K,
         NativeKeyEvent.VC_L to KeyCode.L,
         NativeKeyEvent.VC_M to KeyCode.M,
         NativeKeyEvent.VC_N to KeyCode.N,
         NativeKeyEvent.VC_O to KeyCode.O,
         NativeKeyEvent.VC_P to KeyCode.P,
         NativeKeyEvent.VC_Q to KeyCode.Q,
         NativeKeyEvent.VC_R to KeyCode.R,
         NativeKeyEvent.VC_S to KeyCode.S,
         NativeKeyEvent.VC_T to KeyCode.T,
         NativeKeyEvent.VC_U to KeyCode.U,
         NativeKeyEvent.VC_V to KeyCode.V,
         NativeKeyEvent.VC_W to KeyCode.W,
         NativeKeyEvent.VC_X to KeyCode.X,
         NativeKeyEvent.VC_Y to KeyCode.Y,
         NativeKeyEvent.VC_Z to KeyCode.Z,
         NativeKeyEvent.VC_OPEN_BRACKET to KeyCode.OPEN_BRACKET,
         NativeKeyEvent.VC_CLOSE_BRACKET to KeyCode.CLOSE_BRACKET,
         NativeKeyEvent.VC_BACK_SLASH to KeyCode.BACK_SLASH,
         NativeKeyEvent.VC_SEMICOLON to KeyCode.SEMICOLON,
         NativeKeyEvent.VC_QUOTE to KeyCode.QUOTE,
         NativeKeyEvent.VC_ENTER to KeyCode.ENTER,
         NativeKeyEvent.VC_COMMA to KeyCode.COMMA,
         NativeKeyEvent.VC_PERIOD to KeyCode.PERIOD,
         NativeKeyEvent.VC_SLASH to KeyCode.SLASH,
         NativeKeyEvent.VC_SPACE to KeyCode.SPACE,
         NativeKeyEvent.VC_PRINTSCREEN to KeyCode.PRINTSCREEN,
         NativeKeyEvent.VC_SCROLL_LOCK to KeyCode.SCROLL_LOCK,
         NativeKeyEvent.VC_PAUSE to KeyCode.PAUSE,
         NativeKeyEvent.VC_INSERT to KeyCode.INSERT,
         NativeKeyEvent.VC_DELETE to KeyCode.DELETE,
         NativeKeyEvent.VC_HOME to KeyCode.HOME,
         NativeKeyEvent.VC_END to KeyCode.END,
         NativeKeyEvent.VC_PAGE_UP to KeyCode.PAGE_UP,
         NativeKeyEvent.VC_PAGE_DOWN to KeyCode.PAGE_DOWN,
         NativeKeyEvent.VC_UP to KeyCode.UP,
         NativeKeyEvent.VC_LEFT to KeyCode.LEFT,
         NativeKeyEvent.VC_CLEAR to KeyCode.CLEAR,
         NativeKeyEvent.VC_RIGHT to KeyCode.RIGHT,
         NativeKeyEvent.VC_DOWN to KeyCode.DOWN,
         NativeKeyEvent.VC_NUM_LOCK to KeyCode.NUM_LOCK,
         NativeKeyEvent.VC_SEPARATOR to KeyCode.SEPARATOR,
         NativeKeyEvent.VC_SHIFT to KeyCode.SHIFT,
         NativeKeyEvent.VC_CONTROL to KeyCode.CONTROL,
         NativeKeyEvent.VC_ALT to KeyCode.ALT,
         NativeKeyEvent.VC_META to KeyCode.META,
         NativeKeyEvent.VC_CONTEXT_MENU to KeyCode.CONTEXT_MENU,
         NativeKeyEvent.VC_POWER to KeyCode.POWER,
         NativeKeyEvent.VC_MEDIA_PLAY to KeyCode.PLAY,
         NativeKeyEvent.VC_MEDIA_STOP to KeyCode.STOP,
         NativeKeyEvent.VC_MEDIA_EJECT to KeyCode.EJECT_TOGGLE,
         NativeKeyEvent.VC_VOLUME_MUTE to KeyCode.MUTE,
         NativeKeyEvent.VC_VOLUME_UP to KeyCode.UP,
         NativeKeyEvent.VC_VOLUME_DOWN to KeyCode.DOWN,
         NativeKeyEvent.VC_KATAKANA to KeyCode.KATAKANA,
         NativeKeyEvent.VC_UNDERSCORE to KeyCode.UNDERSCORE,
         NativeKeyEvent.VC_FURIGANA to KeyCode.UNDERSCORE,
         NativeKeyEvent.VC_KANJI to KeyCode.KANJI,
         NativeKeyEvent.VC_HIRAGANA to KeyCode.HIRAGANA,
         NativeKeyEvent.VC_SUN_HELP to KeyCode.HELP,
         NativeKeyEvent.VC_SUN_STOP to KeyCode.STOP,
         NativeKeyEvent.VC_SUN_FIND to KeyCode.FIND,
         NativeKeyEvent.VC_SUN_AGAIN to KeyCode.AGAIN,
         NativeKeyEvent.VC_SUN_UNDO to KeyCode.UNDO,
         NativeKeyEvent.VC_SUN_COPY to KeyCode.COPY,
         NativeKeyEvent.VC_SUN_INSERT to KeyCode.INSERT,
         NativeKeyEvent.VC_SUN_CUT to KeyCode.CUT
      )
   }

}