package util.serialize.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import javafx.beans.value.WritableValue;
import util.access.V;

/**
 * @author Martin Polakovic
 */
public class VConverter extends AbstractPropertyConverter<Object> implements Converter {

	public VConverter(Mapper mapper) {
		super(V.class, mapper);
	}

	@Override
	protected WritableValue<Object> createProperty() {
		return new V<>(null);
	}

	@Override
	protected Class<?> readType(HierarchicalStreamReader reader) {
		return mapper.realClass(reader.getAttribute("propertyClass"));
	}

	@Override
	protected void writeValue(HierarchicalStreamWriter writer, MarshallingContext context, Object value) {
		final Class<?> clazz = value.getClass();
		final String propertyClass = mapper.serializedClass(clazz);
		writer.addAttribute("propertyClass", propertyClass);
		context.convertAnother(value);
	}
}