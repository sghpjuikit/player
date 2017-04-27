package layout.widget.feature;

import java.util.Collection;
import util.conf.Config;
import util.conf.Configurable;

/**
 * Editor for {@link Configurable}.
 */
@Feature(
	name = "Configurator",
	description = "Provides settings and configurations",
	type = ConfiguringFeature.class
)
public interface ConfiguringFeature {

	/** Displays configs of the specified configurable object for user to edit. */
	default void configure(Configurable c) {
		configure(c==null ? null : c.getFields());
	}

	/** Displays specified configs for user to edit. */
	void configure(Collection<Config> c);

}