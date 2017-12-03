package sp.it.pl.util.parsing;

import org.jetbrains.annotations.NotNull;
import sp.it.pl.util.conf.Configurable;
import sp.it.pl.util.functional.Try;
import static java.util.stream.Collectors.joining;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.functional.Try.error;
import static sp.it.pl.util.functional.Try.ok;
import static sp.it.pl.util.functional.Util.stream;

/** Converter for javaFX bean convention. */
public class ConverterFX extends Converter {

    private final String DELIMITER_CONFIG_VALUE = "-";
    private final String DELIMITER_CONFIG_NAME = ":";

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> Try<T,String> ofS(@NotNull Class<T> type, @NotNull String text) {
        try {
            String[] values = text.split(DELIMITER_CONFIG_VALUE);
            Class<?> typeValue = Class.forName(values[0]);
            if (!type.isAssignableFrom(typeValue))
                throw new Exception(); // optimization, avoids next line

            T v = (T) typeValue.getConstructor().newInstance();
            Configurable c = Configurable.configsFromFxPropertiesOf(v);
            stream(values).skip(1)
                .forEach(str -> {
                    try {
                        String[] nameValue = str.split(DELIMITER_CONFIG_NAME);
                        if (nameValue.length!=2) return; // ignore
                        String name = nameValue[0], val = nameValue[1];
                        c.setField(name, val);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
            return ok(v);
        } catch (Exception e) {
            log(ConverterFX.class).warn("Parsing failed, class={} text={}", type, text, e);
            return error(e.getMessage());
        }
    }

    @NotNull
    @Override
    public <T> String toS(T o) {
        return o.getClass().getName() + DELIMITER_CONFIG_VALUE +
            Configurable.configsFromFxPropertiesOf(o).getFields().stream()
                .map(c -> c.getName() + DELIMITER_CONFIG_NAME + c.getValueS())
                .collect(joining(DELIMITER_CONFIG_VALUE));
    }

}
