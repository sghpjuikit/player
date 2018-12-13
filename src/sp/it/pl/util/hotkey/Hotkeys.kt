package sp.it.pl.util.hotkey

import javafx.scene.input.KeyCode
import mu.KLogging
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.NativeInputEvent
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import sp.it.pl.util.action.Action
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.Util.list
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Global hotkey manager, implemented on top of https://github.com/kwhat/jnativehook library. */
class Hotkeys {
    private val executor: (Runnable) -> Unit
    private val keyCombos = ConcurrentHashMap<Int, KeyCombo>()
    private var keyListener: NativeKeyListener? = null
    private var isRunning = false

    constructor(executor: (Runnable) -> Unit) {
        this.executor = executor

        // Disable library logging.
         java.util.logging.Logger.getLogger(GlobalScreen::class.java.getPackage().name).apply {
             level = java.util.logging.Level.OFF
             useParentHandlers = false
         }
    }

    fun isRunning(): Boolean = isRunning

    fun start() {
        if (!isRunning) {
            logger.info { "Starting global hotkeys" }
            isRunning = true

            val eventDispatcher = object: AbstractExecutorService() {
                private var running = true

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
                    keyCombos.forEach { actionId, keyCombo ->
                        val action = Action.get(actionId)

                        // For some reason left BACK_SLASH key (left of the Z key) is not recognized, recognize manually
                        if (e.rawCode==226) {
                            e.keyCode = 43
                            e.keyChar = 43.toChar()
                        }

                        // Unfortunately, JavaFX key codes and the library raw codes do not match for some keys, so we also
                        // check key name. This combination should be enough for all but rare cases
                        val modifiersMatch = keyCombo.modifier==e.modifiers
                        val keysMatch = keyCombo.key.code==e.rawCode || keyCombo.key.getName().equals(NativeKeyEvent.getKeyText(e.keyCode), ignoreCase = true)
                        if (keysMatch && modifiersMatch)
                            keyCombo.press(action, e)
                    }
                }

                override fun nativeKeyReleased(e: NativeKeyEvent) {
                    keyCombos.values.forEach { if (it.isPressed) it.release() }
                }

                override fun nativeKeyTyped(e: NativeKeyEvent) {}
            }
            try {
                GlobalScreen.setEventDispatcher(eventDispatcher)
                GlobalScreen.addNativeKeyListener(keyListener)
                GlobalScreen.registerNativeHook()
                this.keyListener = keyListener
            } catch (e: NativeHookException) {
                logger.error("Failed to register global hotkeys", e)
            }

        }
    }

    fun stop() {
        if (isRunning) {
            try {
                logger.info { "Stopping global hotkeys" }
                GlobalScreen.removeNativeKeyListener(keyListener)
                keyListener = null
                GlobalScreen.unregisterNativeHook()
            } catch (e: NativeHookException) {
                logger.error(e) { "Failed to unregister global hotkeys" }
            }
            isRunning = false
        }
    }

    fun register(action: Action, keys: String) {
        val keyString = keys.substringAfterLast('+').trim().toLowerCase().replace('_', ' ')
        val key = KeyCode.values().find { k -> k.getName().equals(keyString, true) }
                ?: fail { "No KeyCode for ${action.keys}" }
        register(
                action,
                key,
                *sequenceOf(KeyCode.ALT, KeyCode.ALT_GRAPH, KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.WINDOWS, KeyCode.COMMAND, KeyCode.META)
                        .filter { k -> keys.contains(k.name, true) || (k==KeyCode.CONTROL && keys.contains("CTRL", true)) }
                        .toList().toTypedArray()
        )
    }

    fun register(action: Action, key: KeyCode, vararg modifiers: KeyCode) {
        keyCombos[action.id] = KeyCombo(key, *modifiers)
    }

    fun unregister(action: Action) {
        keyCombos.remove(action.id)
    }

    private inner class KeyCombo {
        val key: KeyCode
        val modifier: Int
        var isPressed = false
            private set(value) {
                field = value
            }

        constructor(key: KeyCode, vararg modifiers: KeyCode) {
            this.key = key
            this.modifier = run {
                var m = 0
                infix fun KeyCode.toMask(mask: Int) {
                    if (this in modifiers)
                        m += mask
                }
                KeyCode.SHIFT toMask NativeInputEvent.SHIFT_L_MASK
                KeyCode.CONTROL toMask NativeInputEvent.CTRL_L_MASK
                KeyCode.ALT toMask NativeInputEvent.ALT_L_MASK
                KeyCode.COMMAND toMask NativeInputEvent.META_R_MASK
                KeyCode.ALT_GRAPH toMask NativeInputEvent.ALT_R_MASK
                KeyCode.WINDOWS toMask NativeInputEvent.META_L_MASK
                KeyCode.META toMask NativeInputEvent.META_L_MASK
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

    companion object: KLogging()

}