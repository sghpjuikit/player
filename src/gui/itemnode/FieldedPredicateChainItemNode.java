package gui.itemnode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import gui.itemnode.FieldedPredicateItemNode.PredicateData;
import util.access.fieldvalue.ObjectField;

import static util.functional.Util.IS;

/**
 * Chained filter node producing {@link util.access.fieldvalue.ObjectField} predicate.
 *
 * @author Martin Polakovic
 */
public class FieldedPredicateChainItemNode<T,F extends ObjectField<T>> extends ChainValueNode<Predicate<T>,FieldedPredicateItemNode<T,F>> {

    private final List<PredicateData<F>> data = new ArrayList<>();

    public FieldedPredicateChainItemNode(Supplier<FieldedPredicateItemNode<T,F>> chainedFactory) {
        this(1,chainedFactory);
    }

    public FieldedPredicateChainItemNode(int i, Supplier<FieldedPredicateItemNode<T,F>> chainedFactory) {
        super(i, chainedFactory);

        inconsistent_state = false;
        generateValue();
    }

    protected List<PredicateData<F>> getData() {
        return data;
    }

    public void setData(List<PredicateData<F>> data) {
        inconsistent_state = true;
        this.data.clear(); // causes serious problems, unknown
        this.data.addAll(data);
        chain.forEach(g -> g.chained.setData(data));
        clear(); // bug fix, not sure if it does not cause problems
    }

    public void setPrefTypeSupplier(Supplier<PredicateData<F>> supplier) {
        chain.forEach(g -> g.chained.setPrefTypeSupplier(supplier));
    }

    public boolean isEmpty() {
        return chain.stream().allMatch(c -> c.chained.isEmpty());
    }

    public void clear() {
        inconsistent_state = true;
        chain.setAll(chain.get(0));
        chain.forEach(c -> c.chained.clear());
        inconsistent_state = false;
        generateValue();
    }

    @Override
    protected Predicate<T> reduce(Stream<Predicate<T>> values) {
        return values.reduce(Predicate::and).orElse((Predicate)IS);
    }

}