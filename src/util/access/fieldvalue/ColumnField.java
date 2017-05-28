package util.access.fieldvalue;

import java.util.HashSet;
import java.util.Set;
import util.SwitchException;

public class ColumnField implements ObjectField<Object,Integer> {

	public static final Set<ColumnField> FIELDS = new HashSet<>();
	public static final ColumnField INDEX = new ColumnField("#", "Index of the item in the list");

	private final String name;
	private final String description;

	ColumnField(String name, String description) {
		this.name = name;
		this.description = description;
		FIELDS.add(this);
	}

	public static ColumnField valueOf(String s) {
		if (INDEX.name().equals(s)) return INDEX;
		throw new SwitchException(s);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Integer getOf(Object value) {
		return null;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public String toS(Integer o, String substitute) {
		return "";
	}

	@Override
	public Class<Integer> getType() {
		return Integer.class;
	}
}