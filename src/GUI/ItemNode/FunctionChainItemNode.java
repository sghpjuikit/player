/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import java.util.function.*;
import util.collections.PrefList;
import util.functional.Functors.F1;
import util.functional.Functors.PF;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class FunctionChainItemNode<T> extends ChainConfigField<F1<T,T>,FunctionItemNode<T,T>> {

    public FunctionChainItemNode(Supplier<PrefList<PF<T,T>>> functionPool) {
        super(() -> new FunctionItemNode(functionPool));
    }
    
    public FunctionChainItemNode(int i, Supplier<PrefList<PF<T,T>>> functionPool) {
        super(i,() -> new FunctionItemNode(functionPool));
    }
    
    @Override
    protected void generateValue() {
        F1<T,T> v = generators.stream().filter(g->g.on.selected.get())
                                    .map(g -> g.chained.getValue()).filter(isNotNULL)
                                    .map(F1::nonNull)
                                    .reduce(F1::andThen).orElse(x->x);
        changeValue(v);
    }
}