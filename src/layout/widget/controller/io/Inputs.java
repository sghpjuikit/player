package layout.widget.controller.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Martin Polakovic
 */
public class Inputs {
    private final Map<String,Input<?>> m;

    public Inputs() {
        m = new HashMap<>();
    }

    public <T> Input<T> create(String name, Class<? super T> type, Consumer<? super T> action) {
        Input<T> o = new Input<>(name,type,action);
        m.put(name, o);
        return o;
    }
    public <T> Input<T> create(String name, Class<? super T> type, T init_val, Consumer<? super T> action) {
        Input<T> o = new Input<>(name,type,init_val,action);
        m.put(name, o);
        return o;
    }

    public int getSize() {
        return m.size();
    }

    public Input getInput(String name) {
        return m.get(name);
    }

    public boolean contains(String name) {
        return m.containsKey(name);
    }

    public boolean contains(Input i) {
         return m.containsValue(i); // slow impl./ w can rely on the below:
//        return m.containsKey(i.name); // does not guarantee correctness
    }

    public Collection<Input<?>> getInputs() {
        return m.values();
    }
}