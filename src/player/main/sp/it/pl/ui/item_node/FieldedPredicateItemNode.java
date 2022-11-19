package sp.it.pl.ui.item_node;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.main.AppTexts;
import sp.it.pl.ui.objects.SpitComboBox;
import sp.it.pl.ui.objects.icon.CheckIcon;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.collections.list.PrefList;
import sp.it.util.functional.Functors;
import sp.it.util.functional.PF;
import sp.it.util.type.VType;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.TypesKt.notnull;

/**
 * Filter node producing {@link sp.it.util.access.fieldvalue.ObjectField} predicate.
 */
public class FieldedPredicateItemNode<V, F extends ObjectField<V,?>> extends ValueNode<Predicate<V>> {

	private static final Tooltip negTooltip = new Tooltip("Negate");

	private final ComboBox<PredicateData<F>> typeCB = new SpitComboBox<>(t -> t.name, AppTexts.textNoVal);
	private FItemNode<Object,Boolean> config;
	private final CheckIcon negB = (CheckIcon) new CheckIcon(false).styleclass("filter-negate-icon");
	private final HBox root = new HBox(5, negB, typeCB);

	private Supplier<PredicateData<F>> prefTypeSupplier;
	private boolean inconsistentState = false;
	private boolean empty = true;

	@SuppressWarnings({"unchecked", "rawtypes", "UseBulkOperation"})
	public FieldedPredicateItemNode() {
		this(inRaw -> {
			var in = notnull(inRaw);
			var fsIO = Functors.pool.getIO(in, new VType<>(Boolean.class, false));
			var fsI = Functors.pool.getI(in);
			var fsAll = new PrefList();
			fsIO.forEach(fsAll::add);
			fsI.stream().filter(it -> it.getParameters().size()==0 && !fsAll.contains(it)).forEach(fsAll::add);
			fsAll.setPreferred(fsIO.getPreferred());
			return fsAll;
		});
	}

	@SuppressWarnings("unchecked")
	public FieldedPredicateItemNode(Function1<VType<?>,PrefList<PF<Object,?>>> predicatePool) {
		super((Predicate<V>) IS);

		root.setAlignment(CENTER_LEFT);
		typeCB.setVisibleRowCount(25);
		typeCB.valueProperty().addListener((o, ov, nv) -> {
			if (inconsistentState) return;
			if (config!=null) root.getChildren().remove(config.getNode());
			config = new FItemNode<>(nv.type, new VType<>(Boolean.class, false), predicatePool, null);
			root.getChildren().add(config.getNode());
			HBox.setHgrow(config.getNode(), ALWAYS);
			config.onItemChange = v -> generatePredicate();
			generatePredicate();
		});
		negB.selected.addListener((o, nv, ov) -> generatePredicate());
		negB.tooltip(negTooltip);
	}

	/** Set initially selected value. Supplier can be null and return null, in which case 1st value is selected. */
	@SuppressWarnings("unchecked")
	public void setPrefTypeSupplier(Supplier<? extends PredicateData<F>> initialValueSupplier) {
		prefTypeSupplier = (Supplier<PredicateData<F>>) initialValueSupplier;
	}

	/**
	 * Sets combo box data specifying what filter can be generated in form of list
	 * of tri-tuples : displayed name, class, passed object.
	 * <pre>
	 * The name is what will be displayed in the combobox to choose from
	 * The class specifies the type of object the filter is generated for.
	 * The passed object's purpose is to be returned along with the filter, mostly to be used in the generated filter
	 * </pre>
	 * <p/>
	 * If there is no object to pass, use null.
	 */
	public void setData(List<? extends PredicateData<F>> classes) {
		List<PredicateData<F>> cs = stream(classes).sorted(by(pd -> pd.name())).collect(toList());
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
		changeValue((Predicate<V>) IS);
		empty = true;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void generatePredicate() {
		if (inconsistentState) return;
		empty = config==null;

		if (config==null) {
			changeValue((Predicate<V>) IS);
		} else {
			Function1<? super Object, ? extends Boolean> p = config.getVal();
			F o = typeCB.getValue()==null ? null : typeCB.getValue().value;
			if (p!=null && o!=null) {
				Predicate<V> pr = FieldedPredicateItemNodeCompanion.INSTANCE.predicate((ObjectField) o, (Function1) p);
				if (negB.selected.getValue()) pr = pr.negate();
				changeValue(pr);
			}
		}
	}

	@Override
	public @NotNull Node getNode() {
		return root;
	}

	public record PredicateData<T>(@NotNull String name, @NotNull VType<?> type, @NotNull T value) {

		public static <V, T> @NotNull PredicateData<ObjectField<V,T>> ofField(ObjectField<V,T> field) {
			return new PredicateData<>(field.name(), field.getType(), field);
		}

	}
}