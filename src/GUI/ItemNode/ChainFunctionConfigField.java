/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import java.util.function.*;
import util.collections.PrefList;
import util.functional.Functors.PF;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class ChainFunctionConfigField<T> extends ChainConfigField<Function<T,T>,FunctionConfigField<T,T>> {

    public ChainFunctionConfigField(Supplier<PrefList<PF<T,T>>> functionPool) {
        super(() -> new FunctionConfigField(functionPool));
    }
    
    public ChainFunctionConfigField(int i, Supplier<PrefList<PF<T,T>>> functionPool) {
        super(i,() -> new FunctionConfigField(functionPool));
    }
    
    @Override
    protected void generateValue() {
        Function<T,T> f = generators.stream().filter(g->g.on.selected.get())
                                    .map(g->g.chained.getValue()).filter(isNotNULL)
                                    .reduce(Function::andThen).orElse(x->x);
        changeValue(f);
    }
}