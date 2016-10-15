package util.hotkey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.scene.input.KeyCode;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Util;
import util.action.Action;

import static util.functional.Util.list;
import static util.functional.Util.stream;

/**
 * Global hotkey manager, implemented on top of https://github.com/kwhat/jnativehook library
 */
public class Hotkeys {

	private static final Logger LOGGER = LoggerFactory.getLogger(Hotkeys.class);

	private final Consumer<Runnable> executor;
	private final Map<Integer,KeyCombo> keyCombos = new HashMap<>();
	private NativeKeyListener keyListener;

	public Hotkeys(Consumer<Runnable> executor) {
		this.executor = executor;
	}

	private NativeKeyListener buildNativeKeyListener() {
		return new NativeKeyListener() {
			@Override
			public void nativeKeyPressed(NativeKeyEvent e) {
				// LOGGER.info("Global key press event: " + e.paramString());

//				R<Boolean> shouldConsume = new R<>(false);
				keyCombos.forEach((actionId, keyCombo) -> {
					Action a = Action.get(actionId);
					if (keyCombo.key.getCode()==e.getRawCode() && keyCombo.modifier==e.getModifiers()) {
						keyCombo.press(a);
//						shouldConsume.set(true);
					}
				});

//				if (shouldConsume.get())
//					// consume native event
//					try {
//						Field f = NativeInputEvent.class.getDeclaredField("reserved");
//						f.setAccessible(true);
//						f.setShort(e, (short) 0x01);
//					} catch (Exception x) {
//						LOGGER.error("Failed to consume native key event.", e);
//					}
			}

			@Override
			public void nativeKeyReleased(NativeKeyEvent e) {
				//LOGGER.info("Global key release event: " + e.paramString());
				keyCombos.values().forEach(KeyCombo::release);
			}

			@Override
			public void nativeKeyTyped(NativeKeyEvent e) {}
		};
	}

	public void start() {
		if (!isRunning()) {
			// Disable library logging.
			java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
			logger.setLevel(java.util.logging.Level.OFF);
			logger.setUseParentHandlers(false); // Don't forget to disable the parent handlers.

			try {
				GlobalScreen.setEventDispatcher(new AbstractExecutorService() {
					private boolean running = true;

					@Override
					public void shutdown() {
						this.running = false;
					}

					@Override
					public List<Runnable> shutdownNow() {
						this.running = false;
						return list();
					}

					@Override
					public boolean isShutdown() {
						return !this.running;
					}

					@Override
					public boolean isTerminated() {
						return !this.running;
					}

					@Override
					public boolean awaitTermination(long amount, TimeUnit units) throws InterruptedException {
						return true;
					}

					@Override
					public void execute(Runnable action) {
						executor.accept(action);
					}
				});
				GlobalScreen.registerNativeHook();
				keyListener = buildNativeKeyListener();
				GlobalScreen.addNativeKeyListener(keyListener);
			} catch (NativeHookException e) {
				LOGGER.error("Failed to register global hotkeys", e);
			}
		}
	}

	public boolean isRunning() {
		return GlobalScreen.isNativeHookRegistered();
	}

	public void stop() {
		if (isRunning()) {
			try {
				GlobalScreen.removeNativeKeyListener(keyListener);
				keyListener = null;
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e) {
				LOGGER.error("Failed to unregister global hotkeys", e);
			}
		}
	}

	public void register(Action action, String keys) {
		int i = keys.lastIndexOf('+');
		String keyString = (i<0 ? keys : keys.substring(i+1)).trim().toLowerCase().replace('_', ' ');
		KeyCode key = stream(KeyCode.values()).findFirst(k -> k.getName().toLowerCase().equals(keyString)).get();
		register(
			action, key,
			stream(KeyCode.ALT, KeyCode.ALT_GRAPH, KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.WINDOWS, KeyCode.COMMAND, KeyCode.META)
				.filter(k -> Util.containsNoCase(keys, k.name()) || (k==KeyCode.CONTROL && keys.toUpperCase().contains("CTRL")))
				.toArray(KeyCode[]::new)
		);
	}

	public void register(Action action, KeyCode key, KeyCode... modifiers) {
		keyCombos.put(action.getID(), new KeyCombo(key, modifiers));
	}

	public void unregister(Action action) {
		keyCombos.remove(action.getID());
	}

	private static final class KeyCombo {
		public final KeyCode key;
		public final KeyCode[] modifiers;
		public final int modifier;
		private boolean isPressed = false;

		public KeyCombo(KeyCode key, KeyCode[] modifiers) {
			this.key = key;
			this.modifiers = modifiers;

			int m = 0;
			if(stream(modifiers).has(KeyCode.SHIFT)) m += 1;   // right shift
			if(stream(modifiers).has(KeyCode.CONTROL)) m += 2; // left ctrl
			if(stream(modifiers).has(KeyCode.WINDOWS)) m += 4;
			if(stream(modifiers).has(KeyCode.ALT)) m += 8;
//			if(stream(modifiers).has(KeyCode.SHIFT)) m += 16;   // right shift
//			if(stream(modifiers).has(KeyCode.CONTROL)) m += 32; // right ctrl
			if(stream(modifiers).has(KeyCode.COMMAND)) m += 64;
			if(stream(modifiers).has(KeyCode.ALT_GRAPH)) m += 128;
//			if(stream(modifiers).has(KeyCode.META)) m += 128;
			this.modifier = m;
		}

		public void press(Action a) {
			boolean isPressedFirst = !isPressed;
			isPressed = true;
			if (a.isContinuous() || isPressedFirst)
				a.run();
		}
		public void release() {
			isPressed = false;
		}
	}
}