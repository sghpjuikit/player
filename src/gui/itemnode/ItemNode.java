package gui.itemnode;

import java.util.function.Consumer;
import javafx.scene.Node;
import util.conf.Config;

/**
 * Graphics with a value. Mostly an ui editor that allows user to customize the
 * value.
 */
public abstract class ItemNode<T> {

	/** Returns current value. Should not be null if possible. Document. */
	public abstract T getValue();

	/** Returns the root node. Use to attach this to scene graph. */
	public abstract Node getNode();

	/**
	 * Focuses this node's content. Depends on implementation. Usually, the
	 * most important element requiring user input (i.e. text box) will receive
	 * focus. If there is none, does nothing.
	 */
	public void focus() {}

	/** Item node which directly holds the value. */
	public static abstract class ValueNodeBase<T> extends ItemNode<T> {
		protected T value;

		/** {@inheritDoc} */
		@Override
		public T getValue() {
			return value;
		}
	}

	/** Item node which directly holds the value and fires value change events. */
	public static abstract class ValueNode<T> extends ValueNodeBase<T> {

		/**
		 * Value change handler. Executes when value changes. Consumes new value.
		 * If null, is ignored. Can be set or changed anytime.
		 */
		public Consumer<T> onItemChange;

		/** Sets value & fires itemChange if available. Internal use only. */
		protected void changeValue(T nv) {
			if (value==nv) return;
			value = nv;
			if (onItemChange!=null) onItemChange.accept(nv);
		}
	}

	/** Item node which holds the value in a {@link Config}. */
	public static abstract class ConfigNode<T> extends ItemNode<T> {
		protected final Config<T> config;

		public ConfigNode(Config<T> config) {
			this.config = config;
		}

		/** {@inheritDoc} */
		@Override
		public T getValue() {
			return config.getValue();
		}

		/**
		 * @return the underlying config. Never null. The config never holds null either.
		 */
		public Config getConfig() {
			return config;
		}

	}
}