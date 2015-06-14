/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.function.Function;
import java.util.stream.Stream;
import javafx.collections.ListChangeListener;
import util.collections.PrefList;
import util.functional.Functors;
import util.functional.Functors.F1;
import util.functional.Functors.NullIn;
import util.functional.Functors.NullOut;

/**
 * Function editor with function chaining.
 * Provides ui for creating functions.
 * <p>
 * This editor provides editable and scalable chain of stackable functions. The
 * chain:
 * <ul>
 * <li> Is always reducible to single function called reduction function. Applying
 * reduction function is equivalent to applying all functions in the chain in
 * their chain order.
 * <li> can be used as a reduction function {@link #getValue()} or a stream of
 * the standalone functions can be obtained {@link #getValues()}.
 * <li> begins with an input type. It determines the type of input of the first
 * function in the chain and input type of the reduction function. By default it
 * is Void.class. {@link #getTypeIn()}
 * <li> produces output of type determined by the last function in the chain 
 * {@link #getTypeOut()}
 * </ul>
 * 
 * Supported are also:
 * <ul>
 * <li> null. Functions in the chain  are wrapped to function wrappers to 
 * cases of producing or receiving null. The strategy can be customized
 * {@link #setNullHandling(util.functional.Functors.NullIn, util.functional.Functors.NullOut)}
 * <li> Void. Functions with void parameter can simulate supplier (no input) or
 * consumers (no output) functions. They can be anywhere within the chain.
 * </ul>
 * 
 *
 * @author Plutonium_
 */
public class FunctionChainItemNode extends ChainConfigField<F1<Object,Object>,FunctionItemNode<Object,Object>> {
    
    private final Function<Class,PrefList<Functors.PF<Object,Object>>> fp;
    private Class type_in = Void.class;
    private NullIn handleNullIn = NullIn.NULL;
    private NullOut handleNullOut = NullOut.NULL;
    
    /** Creates unlimited chain starting with Void.class. */
    public FunctionChainItemNode(Function<Class,PrefList<Functors.PF<Object,Object>>> functionPool) {
        this(Void.class, Integer.MAX_VALUE, functionPool);
    }
    /** Creates unlimited chain starting with specified type. */
    public FunctionChainItemNode(Class in, Function<Class,PrefList<Functors.PF<Object,Object>>> functionPool) {
        this(in, Integer.MAX_VALUE, functionPool);
    }
    
    /** Creates limited chain starting with specified type. */
    public FunctionChainItemNode(Class in, int max_len, Function<Class,PrefList<Functors.PF<Object,Object>>> functionPool) {
//        super(len, max_len, () -> new FunctionItemNode(functionPool));
//        homogeneous = false;
        fp = functionPool;
        homogeneous = false;
        setTypeIn(in);  // initializes value, dont fire update yet
        
        maxChainLength.addListener((o,ov,nv) -> {
            int m = nv.intValue();
            if(m<chain.size()) {
                chain.setAll(chain.subList(0, m));
                generateValue();
            }
            chain.forEach(Chainable::updateIcons);
        });
        chain.addListener((ListChangeListener.Change<? extends Chainable<FunctionItemNode<Object, Object>>> c) -> chain.forEach(Chainable::updateIcons));
        maxChainLength.set(max_len);
    }
    
    /** 
     * Sets input type.
     * Input type determines:
     * <ul>
     * <li> the input type of this chain's reduction function
     * <li>input type of the first transformation function in the chain
     * <ul>
     * This parameter is:
     * <ul>
     * <li> determined by object type this chain's reduction function will be aplied to
     * <li> determines available functions in the dropdown box or the 1st chain element
     * <ul>
     * <p>
     * If the class changes, entire transformation chain is cleared.
     * Callign this method always results in an update event.
     * <p>
     * Default Void.class
     */
    public void setTypeIn(Class c) {
        if(type_in.equals(c)) 
            generateValue();
        else {
            type_in = c;
            chain.clear();
            new Chainable(() -> new FunctionItemNode<>(() -> fp.apply(getTypeOut())));
            generateValue();
        }
    }
    
    /**
     * Sets null handling during chain function reduction.
     * <p>
     * Calling this method results in an update event.
     */
    public void setNullHandling(NullIn ni, NullOut no) {
        handleNullIn = ni;
        handleNullOut = no;
        generateValue();
    }
    

    public Class getTypeIn() {
        return type_in;
    }

    public Class getTypeOut() {
        return chain.isEmpty() ? type_in : chain.get(chain.size()-1).chained.getTypeOut();
    }
    
    @Override
    protected F1<Object,Object> reduce(Stream<Functors.F1<Object,Object>> values) {
        return values.map(f -> f.wrap(handleNullIn, handleNullOut))
                     .reduce(Functors.F1::andThen).orElse(x->x);
    }
    
}