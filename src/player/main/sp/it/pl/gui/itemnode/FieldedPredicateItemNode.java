package sp.it.pl.gui.itemnode;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import sp.it.pl.gui.objects.combobox.ImprovedComboBox;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.collections.list.PrefList;
import sp.it.util.functional.Functors.PF;
import sp.it.util.functional.Util;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.Util.IS0;
import static sp.it.util.functional.Util.ISNT;
import static sp.it.util.functional.Util.ISNT0;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.stream;

/**
 * Filter node producing {@link sp.it.util.access.fieldvalue.ObjectField} predicate.
 */
public class FieldedPredicateItemNode<V, F extends ObjectField<V,?>> extends ValueNode<Predicate<V>> {

	// Normally we would use this predicate builder:
	//     (field,filter) -> element -> filter.test(element.getField(field));
	// But element.getField(field) can return null!, which Predicate can not handle on its own.
	// We end up with null safe alternative:
	//     (field,filter) -> element -> {
	//         Object o = element.getField(field);
	//         return o==null ? false : filter.test(o);
	//     };
	// Problem:
	//    Predicate testing null (o -> o==null) will get bypassed and wont have any effect
	// leading us to predicate identity preservation and ultimate solution below.
	//
	// One could argue predicate isNull is useless in OOP, particularly for filtering, like here,
	// but ultimately, it has it's place and we should'nt ignore it out of convenience.
	// In this particular case, where we are filtering FieldedValue, isNull should not be used, rather
	// isEmpty() predicate should check: element.getField(field).equals(EMPTY_ELEMENT.getField(field))
	// where null.equals(null) would return true, basically: element.hasDefaultValue(field).
	// However, in my opinion, isNull predicate does not lose its value completely.
	private static <V, T> Predicate<V> predicate(ObjectField<V,T> field, Function<? super T, ? extends Boolean> filter) {
		return Util.isAny(filter, IS0, ISNT0, IS, ISNT)
				? element -> filter.apply(field.getOf(element))
				: element -> {
					T o = field.getOf(element);
					return o==null ? false : filter.apply(o);
				};
	}

	private static final Tooltip negTooltip = new Tooltip("Negate");

	private final ComboBox<PredicateData<F>> typeCB = new ImprovedComboBox<>(t -> t.name);
	private FItemNode<Object,Boolean> config;
	private final CheckIcon negB = (CheckIcon) new CheckIcon(false).styleclass("filter-negate-icon");
	private final HBox root = new HBox(5, negB, typeCB);

	private Supplier<PredicateData<F>> prefTypeSupplier;
	private boolean inconsistentState = false;
	private boolean empty = true;

	@SuppressWarnings("unchecked")
	public FieldedPredicateItemNode(Callback<Class,PrefList<PF<Object,Boolean>>> predicatePool, Callback<Class,PF<Object,Boolean>> prefPredicatePool) {
		super((Predicate) IS);

		root.setAlignment(CENTER_LEFT);
		typeCB.setVisibleRowCount(25);
		typeCB.valueProperty().addListener((o, ov, nv) -> {
			if (inconsistentState) return;
			if (config!=null) root.getChildren().remove(config.getNode());
			config = new FItemNode(() -> predicatePool.call(nv.type));
			root.getChildren().add(config.getNode());
			HBox.setHgrow(config.getNode(), ALWAYS);
			config.onItemChange = v -> generatePredicate();
			generatePredicate();
		});
		negB.selected.addListener((o, nv, ov) -> generatePredicate());
		negB.tooltip(negTooltip);
	}

	/** Set initially selected value. Supplier can be null and return null, in which case 1st value is selected. */
	public void setPrefTypeSupplier(Supplier<PredicateData<F>> initialValueSupplier) {
		prefTypeSupplier = initialValueSupplier;
	}

	/**
	 * Sets chosable data specifying what filter can be generated in form of list
	 * of tri-tuples : displayed name, class, passed object.
	 * <pre>
	 * The name is what will be displayed in the combobox to choose from
	 * The class specifies the type of object the filter is generated for.
	 * The passed object's purpose is to be returned along with the filter, mostly to be used in the generated filter
	 * </pre>
	 * <p/>
	 * If there is no object to pass, use null.
	 */
	public void setData(List<PredicateData<F>> classes) {
		List<PredicateData<F>> cs = stream(classes).sorted(by(pd -> pd.name)).collect(toList());
		inconsistentState = true;
		typeCB.getItems().setAll(cs);
		inconsistentState = false;

		PredicateData<F> v = Optional.ofNullable(prefTypeSupplier)
				.map(Supplier::get)
				.flatMap(pd -> cs.stream().filter(d -> d.value.equals(pd.value)).findAny())
				.or(() -> cs.stream().findFirst())
				.orElse(null);

		typeCB.setValue(v);
		empty = true;   // we can do this since we know we are in 'default' state
	}

	/**
	 * Focuses the filter's first parameter's config field if any.
	 * <p/>
	 * {@inheritDoc }
	 */
	@Override
	public void focus() {
		if (config!=null) config.focus();
	}

	public boolean isEmpty() {
		return empty;
	}

	@SuppressWarnings("unchecked")
	public void clear() {
		inconsistentState = true;
		if (config!=null) config.clear();
		inconsistentState = false;
		empty = true;
		changeValue((Predicate<V>) IS);
	}

	@SuppressWarnings("unchecked")
	private void generatePredicate() {
		if (inconsistentState) return;
		empty = config==null;

		if (config==null) {
			changeValue((Predicate<V>) IS);
		} else {
			Function<? super Object, ? extends Boolean> p = config.getVal();
			F o = typeCB.getValue()==null ? null : typeCB.getValue().value;
			if (p!=null && o!=null) {
				Predicate<V> pr = predicate((ObjectField) o, p);
				if (negB.selected.getValue()) pr = pr.negate();
				changeValue(pr);
			}
		}
	}

	@Override
	public Node getNode() {
		return root;
	}

	public static class PredicateData<T> {
		public final String name;
		public final Class type;
		public final T value;

		public static <V, T> PredicateData<ObjectField<V,T>> ofField(ObjectField<V,T> field) {
			return new PredicateData<>(field.name(), field.getType(), field);
		}

		public PredicateData(String name, Class type, T value) {
			noNull(name);
			noNull(type);
			noNull(value);
			this.name = name;
			this.type = type;
			this.value = value;
		}
	}
}