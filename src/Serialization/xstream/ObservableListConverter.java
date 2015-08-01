package Serialization.xstream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.sun.javafx.collections.ObservableListWrapper;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

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
    
    @Override
    protected Object createCollection(Class type) {
        if (type == ObservableListWrapper.class) {
            return FXCollections.observableArrayList();
        }
        return super.createCollection(type);
    }
}