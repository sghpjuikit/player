package util.conf;

import java.util.Collection;
import java.util.Map;

public interface CachedConfigurable<T> extends Configurable<T> {

	Map<String,Config<T>> getFieldsMap();

	@Override
	default Config<T> getField(String n) {
		return getFieldsMap().computeIfAbsent(n, Configurable.super::getField);
	}

	@Override
	default Collection<Config<T>> getFields() {
		Configurable.super.getFields().forEach(c -> getFieldsMap().putIfAbsent(c.getName(), c));
		return getFieldsMap().values();
	}

}