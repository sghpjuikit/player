package main;

import util.conf.Configurable;

public interface Plugin extends Configurable<Object> {
	String CONFIG_GROUP = "Plugins";

	void start();

	void stop();

	default void activate(boolean active) {
		if (active) start();
		else stop();
	}

	boolean isActive();

	String getName();

	abstract class SimplePlugin implements Plugin {
		private final String name;
		private boolean isActive = false;

		public SimplePlugin(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void start() {
			if (!isActive()) onStart();
			isActive = true;
		}

		abstract void onStart();

		@Override
		public void stop() {
			if (isActive()) onStop();
			isActive = false;
		}

		abstract void onStop();

		@Override
		public boolean isActive() {
			return isActive;
		}
	}
}