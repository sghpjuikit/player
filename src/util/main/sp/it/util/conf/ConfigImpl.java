package sp.it.util.conf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.collections.ObservableList;
import kotlin.jvm.functions.Function1;
import sp.it.util.conf.Constraint.HasNonNullElements;
import sp.it.util.conf.Constraint.ObjectNonNull;
import sp.it.util.conf.Constraint.ReadOnlyIf;
import sp.it.util.file.properties.PropVal;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.file.properties.PropVal.PropVal1;
import static sp.it.util.file.properties.PropVal.PropValN;
import static sp.it.util.functional.Util.forEachBoth;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.split;
import static sp.it.util.functional.UtilKt.compose;
import static sp.it.util.type.Util.unPrimitivize;

public interface ConfigImpl {

	abstract class ConfigBase<T> extends Config<T> {

		private final Class<T> type;
		private final String gui_name;
		private final String name;
		private final String group;
		private final String info;
		private final EditMode editable;
		private final T defaultValue;
		private Set<Constraint<? super T>> constraints;

		ConfigBase(Class<T> type, String name, String gui_name, T val, String group, String info, EditMode editable) {
			this.type = unPrimitivize(type);
			this.gui_name = gui_name;
			this.name = name;
			this.defaultValue = val;
			this.group = group;
			this.info = info==null || info.isEmpty() ? gui_name : info;
			this.editable = editable;
		}

		/**
		 * @throws NullPointerException if val parameter null. The wrapped value must no be null.
		 */
		ConfigBase(Class<T> type, String name, ConfigDefinition c, T val, String group) {
			this(type, name, c.getName().isEmpty() ? name : c.getName(), val, group, c.getInfo(), c.getEditable());
		}

		ConfigBase(Class<T> type, String name, ConfigDefinition c, Set<Constraint<? super T>> constraints, T val, String group) {
			this(type, name, c.getName().isEmpty() ? name : c.getName(), val, group, c.getInfo(), c.getEditable());
			this.constraints = new HashSet<>(constraints);
		}

		@Override
		public final String getNameUi() {
			return gui_name;
		}

		@Override
		public final String getName() {
			return name;
		}

		@Override
		public final String getGroup() {
			return group;
		}

		@Override
		public Class<T> getType() {
			return type;
		}

		@Override
		public final String getInfo() {
			return info;
		}

		@Override
		public final EditMode isEditable() {
			return editable;
		}

		@SuppressWarnings({"unchecked", "RedundantCast"})
		@Override
		public Set<Constraint<T>> getConstraints() {
			return constraints==null ? Set.of() : (Set) constraints;
		}

		@SafeVarargs
		@Override
		public final ConfigBase<T> addConstraints(Constraint<? super T>... constraints) {
			if (this.constraints==null) this.constraints = new HashSet<>(constraints.length);
			this.constraints.addAll(asList(constraints));
			return this;
		}

		@Override
		public T getDefaultValue() {
			return defaultValue;
		}
	}

	class ListConfig<T> extends ConfigBase<ObservableList<T>> {

		public final ConfList<T> a;
		public final Function1<? super T,? extends Configurable<?>> toConfigurable;
		private final List<T> defaultItems;

		@SuppressWarnings("unchecked")
		public ListConfig(String name, ConfigDefinition c, ConfList<T> list, String group, Set<Constraint<? super ObservableList<T>>> constraints, Set<Constraint<? super T>> elementConstraints) {
			super((Class) ObservableList.class, name, c, constraints, list.list, group);
			failIf(isReadOnly(list.list)!=c.getEditable().isByNone());

			a = list;
			defaultItems = isFixedSizeAndHasConfigurableItems() ? null : list(list.list);
			toConfigurable = compose(list.toConfigurable, configurable -> {
				if (configurable instanceof Config) {
					var config = (Config) configurable;
					config.addConstraints(elementConstraints);
					if (!list.isNullable) config.addConstraints(ObjectNonNull.INSTANCE);
					if (!isEditable().isByUser()) config.addConstraints(new ReadOnlyIf(true));
				}
				return configurable;
			});

			if (!list.isNullable) addConstraints(HasNonNullElements.INSTANCE);
		}

		@Override
		public ObservableList<T> getValue() {
			return a.list;
		}

		@Override
		public void setValue(ObservableList<T> val) {}

		@Override
		public void setValueToDefault() {
			if (isFixedSizeAndHasConfigurableItems())
				a.list.setAll(defaultItems);
		}

		@Override
		public PropVal getValueAsProperty() {
			return new PropValN(
				map(
					getValue(),
					t -> a.toConfigurable.invoke(t).getFields().stream()
						.map(c -> {
							var p = c.getValueAsProperty();
							failIf(p.getValN().size()!=1, () -> "Only single-value within multi value is supported"); // TODO: support multi-value
							return p.getVal1();
						})
						.collect(joining(";"))
				)
			);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setValueAsProperty(PropVal property) {
			boolean isFixedSizeAndHasConfigurableItems = isFixedSizeAndHasConfigurableItems();
			AtomicInteger i = isFixedSizeAndHasConfigurableItems ? new AtomicInteger(0) : null;
			var values = property.getValN().stream()
				.map(s -> {
					T t = isFixedSizeAndHasConfigurableItems ? a.list.get(i.getAndIncrement()) : a.factory.get();
					List<Config> configs = list(a.toConfigurable.invoke(t).getFields());
					List<String> cValues = split(s, ";");
					if (configs.size()==cValues.size())
						forEachBoth(configs, cValues, (c, v) -> c.setValueAsProperty(new PropVal1(v))); // TODO: support multi-value

					return (T) (a.itemType.isAssignableFrom(configs.get(0).getType()) ? configs.get(0).getValue() : t);
				})
				.filter(a.isNullable ? (it -> true) : (it -> it!=null))
				.collect(toList());
			setTo(a.list, values);
		}

		private boolean isFixedSizeAndHasConfigurableItems() {
			return a.factory==ConfList.FailFactory;
		}

		static boolean isReadOnly(ObservableList<?> list) {
			return list.getClass().getSimpleName().toLowerCase().contains("unmodifiable");
		}

	}

}