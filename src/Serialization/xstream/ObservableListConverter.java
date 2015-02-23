package Serialization.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.collections.ObservableList;

/**
 * TODO write documentation<br>
 * <br>
 * Created at 21/09/11 09:32.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 * @since 2.2
 */
public class ObservableListConverter extends CollectionConverter implements Converter {

    public ObservableListConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return ObservableList.class.isAssignableFrom(type);
    }
}