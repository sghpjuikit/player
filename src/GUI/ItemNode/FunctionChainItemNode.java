/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.ItemNode;

import java.util.function.*;
import java.util.stream.Stream;
import util.collections.PrefList;
import util.functional.Functors.F1;
import util.functional.Functors.PF;

/**
 * Function (unary operator) chain editor.
 * Contained value is the composed function - function that is equivalent to 
 * applying the chained functions in their chained order.
 * <p>
 * For example string transformation editor, which allows stacking the
 * transformation and applying it on a string on change.
 *
 * @author Plutonium_
 */
public class FunctionChainItemNode<T> extends ChainConfigField<F1<T,T>,FunctionItemNode<T,T>> {
    
    /** Creates unlimited chain of 1 initial chained element.  */
    public FunctionChainItemNode(Supplier<PrefList<PF<T,T>>> functionPool) {
        super(() -> new FunctionItemNode(functionPool));
    }
    
    /** Creates unlimited chain of i initial chained element.  */
    public FunctionChainItemNode(int i, Supplier<PrefList<PF<T,T>>> functionPool) {
        super(i,() -> new FunctionItemNode(functionPool));
    }

    /** Creates limited chain of 1 initial chained element.  */
    public FunctionChainItemNode(int len, int max_len, Supplier<PrefList<PF<T,T>>> functionPool) {
        super(len, max_len, () -> new FunctionItemNode(functionPool));
    }

    @Override
    protected F1<T, T> reduce(Stream<F1<T, T>> values) {
        return values.map(F1::nonNull).reduce(F1::andThen).orElse(x->x);
    }
    
}