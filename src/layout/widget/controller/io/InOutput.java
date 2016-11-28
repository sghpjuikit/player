package layout.widget.controller.io;

import java.util.UUID;

import layout.area.IOLayer;

/**
 * Special {XPut}, a composition of {@link layout.widget.controller.io.Input} and
 * {@link layout.widget.controller.io.Output}.
 */
public class InOutput<T> implements XPut<T> {
	public final Input<T> i;
	public final Output<T> o;

	public InOutput(UUID id, String name, Class<? super T> c) {
		o = new Output<>(id, name, c);
		i = new Input<>(name, c, o::setValue) {
			// This solves the problem of binding ot itself and this can be
			// used to notify the graphics as well, so its on a user level - he
			// can be prevented from even trying.
			@Override
			public boolean canBind(Output<?> output) {
				return output!=o && super.canBind(output);
			}
		};

		IOLayer.all_inoutputs.add(this);
	}
}