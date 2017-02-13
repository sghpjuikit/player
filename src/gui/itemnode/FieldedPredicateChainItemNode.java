package gui.itemnode;

import gui.itemnode.FieldedPredicateItemNode.PredicateData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import util.access.fieldvalue.ObjectField;
import util.functional.Functors.Ƒ1;
import static util.functional.Util.IS;

/**
 * Chained filter node producing {@link util.access.fieldvalue.ObjectField} predicate.
 *
 * @author Martin Polakovic
 */
public class FieldedPredicateChainItemNode<T,F extends ObjectField<T,Object>> extends ChainValueNode<Predicate<T>,FieldedPredicateItemNode<T,F>> {

    protected Supplier<PredicateData<F>> supplier;
    protected final List<PredicateData<F>> data = new ArrayList<>();

    public FieldedPredicateChainItemNode(Ƒ1<FieldedPredicateChainItemNode<T,F>,FieldedPredicateItemNode<T,F>> chainedFactory) {
        super(0, Integer.MAX_VALUE, null);
        this.chainedFactory = () -> chainedFactory.apply(this);
        inconsistent_state = false;
        generateValue();
    }

    public List<PredicateData<F>> getData() {
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
        this.supplier = supplier;
        chain.forEach(g -> g.chained.setPrefTypeSupplier(supplier));
    }

    public Supplier<PredicateData<F>> getPrefTypeSupplier() {
        return supplier;
    }

    public boolean isEmpty() {
        return chain.stream().allMatch(c -> c.chained.isEmpty());
    }

    public void clear() {
        inconsistent_state = true;
        if (!chain.isEmpty()) chain.setAll(chain.get(0));   // TODO: handle properly
        chain.forEach(c -> c.chained.clear());
        inconsistent_state = false;
        generateValue();
    }

    @Override
    protected Predicate<T> reduce(Stream<Predicate<T>> values) {
        return values.reduce(Predicate::and).orElse((Predicate)IS);
    }

}