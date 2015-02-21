package Serialization.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;

/**
 * Created at 17/09/11 11:10.<br>
 *
 * @author Antoine Mischler <antoine@dooapp.com>
 */
public class ObjectPropertyConverter extends AbstractPropertyConverter<Object> implements Converter {

    public ObjectPropertyConverter(Mapper mapper) {
        super(ObjectProperty.class, mapper);
    }

    @Override
    protected WritableValue<Object> createProperty() {
        return new SimpleObjectProperty();
    }

    @Override
    protected Class readType(HierarchicalStreamReader reader) {
        return mapper.realClass(reader.getAttribute("propertyClass"));
    }

    @Override
    protected void writeValue(HierarchicalStreamWriter writer, MarshallingContext context, Object value) {
        final Class clazz = value.getClass();
        final String propertyClass = mapper.serializedClass(clazz);
        writer.addAttribute("propertyClass", propertyClass);
        context.convertAnother(value);
    }
}