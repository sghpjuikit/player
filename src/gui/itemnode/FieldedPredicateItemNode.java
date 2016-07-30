/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import gui.itemnode.ItemNode.ValueNode;
import gui.objects.combobox.ImprovedComboBox;
import gui.objects.icon.CheckIcon;
import util.access.fieldvalue.ObjectField;
import util.collections.list.PrefList;
import util.functional.Functors.PƑ;
import util.functional.Util;

import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.functional.Util.*;

/**
 * Filter node producing {@link util.access.fieldvalue.ObjectField} predicate.
 *
 * @author Martin Polakovic
 */
public class FieldedPredicateItemNode<V,F extends ObjectField<V>> extends ValueNode<Predicate<V>> {

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
    private static <V,F extends ObjectField> Predicate<V> predicate(F field, Function<Object,Boolean> filter) {
            // debug
            // if (field==Metadata.Field.FIRST_PLAYED) {
            //     System.out.println((filter==ISØ) + " " + (filter==ISNTØ) + " " + (filter==IS) + " " + (filter==ISNT));
            // }
            return Util.isAny(filter, ISØ,ISNTØ,IS,ISNT)
                    // the below could be made more OOP by adding predicate methods to FieldEnum
                    ? element -> filter.apply(field.getOf(element))
                    : element -> {
                          Object o = field.getOf(element);
                          return o==null ? false : filter.apply(o);
                      };
    }

    private static final Tooltip negTooltip = new Tooltip("Negate");

    private final ComboBox<PredicateData<F>> typeCB = new ImprovedComboBox<>(t -> t.name);
    private FItemNode<Object,Boolean> config;
    private final CheckIcon negB = new CheckIcon(false).styleclass("filter-negate-icon");
    private final HBox root = new HBox(5,negB,typeCB);

    private Supplier<PredicateData<F>> prefTypeSupplier;
    private boolean inconsistentState = false;

    public FieldedPredicateItemNode(Callback<Class,PrefList<PƑ<Object,Boolean>>> predicatePool, Callback<Class,PƑ<Object,Boolean>> prefPredicatePool) {
        root.setAlignment(CENTER_LEFT);
        typeCB.setVisibleRowCount(25);
        typeCB.valueProperty().addListener((o,ov,nv) -> {
            if (inconsistentState) return;
            if (config!=null) root.getChildren().remove(config.getNode());
            config = new FItemNode<>(() -> predicatePool.call(nv.type));
            root.getChildren().add(config.getNode());
            HBox.setHgrow(config.getNode(), ALWAYS);
            config.onItemChange = v -> generatePredicate();
            generatePredicate();
        });
        negB.selected.addListener((o,nv,ov) -> generatePredicate());
        negB.tooltip(negTooltip);
    }

    public void setPrefTypeSupplier(Supplier<PredicateData<F>> supplier) {
        prefTypeSupplier = supplier;
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
     * @param classes
     */
    public void setData(List<PredicateData<F>> classes) {
	    List<PredicateData<F>> cs = new ArrayList<>(classes);
        // cs.removeIf(e -> pPool.call(unPrimitivize(e.type)).isEmpty()); // remove unsupported
        inconsistentState = true;
        typeCB.getItems().setAll(cs);
        inconsistentState = false;

	    PredicateData<F> v = prefTypeSupplier == null ? null : prefTypeSupplier.get();
        if (v==null) v = cs.isEmpty() ? null : cs.get(0);
        typeCB.setValue(v);
    }

    /**
     * Focuses the filter's first parameter's config field if any.
     * <p/>
     * {@inheritDoc }
     */
    @Override
    public void focus() {
        config.focus();
    }

    private boolean empty = true;

    public boolean isEmpty() {
        return empty;
    }

    public void clear() {
        empty = true;
        value = (Predicate<V>)IS;
        // TODO: clear config (the predicate values, e.g., )
    }

    private void generatePredicate() {
        if (inconsistentState) return;
        empty = false;
        Function<Object,Boolean> p = config.getValue();
        F o = typeCB.getValue()==null ? null : typeCB.getValue().value;
        if (p!=null && o!=null) {
            Predicate<V> pr = predicate(o, p);
            if (negB.selected.getValue()) pr = pr.negate();
            changeValue(pr);
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

		public static <A, V extends ObjectField<A>> PredicateData<V> ofField(V field) {
			return new PredicateData<>(field.toString(), field.getType(), field);
		}

		public PredicateData(String name, Class type, T value) {
			this.name = name;
			this.type = type;
			this.value = value;
		}
	}
}