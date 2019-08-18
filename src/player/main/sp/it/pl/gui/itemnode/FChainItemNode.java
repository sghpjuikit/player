package sp.it.pl.gui.itemnode;

import java.util.function.Function;
import java.util.stream.Stream;
import javafx.collections.ListChangeListener;
import sp.it.util.collections.list.PrefList;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Functors.NullIn;
import sp.it.util.functional.Functors.NullOut;
import sp.it.util.functional.Functors.PF;
import sp.it.util.functional.Functors.TypeAwareF;
import static sp.it.util.functional.Util.IDENTITY;

/**
 * Function editor with function chaining.
 * Provides ui for creating functions.
 * <p/>
 * This editor provides editable and scalable chain of stackable functions. The
 * chain:
 * <ul>
 * <li> Is always reducible to single function called reduction function. Applying
 * reduction function is equivalent to applying all functions in the chain in
 * their chain order.
 * <li> can be used as a reduction function {@link #getVal()} or a stream of
 * the standalone functions can be obtained {@link #getValues()}.
 * <li> begins with an input type. It determines the type of input of the first
 * function in the chain and input type of the reduction function. By default it
 * is Void.class. {@link #getTypeIn()}
 * <li> produces output of type determined by the last function in the chain
 * {@link #getTypeOut()}
 * </ul>
 * <p>
 * Supported are also:
 * <ul>
 * <li> null. Functions in the chain  are wrapped to function wrappers to
 * cases of producing or receiving null. The strategy can be customized
 * {@link #setNullHandling(sp.it.util.functional.Functors.NullIn, sp.it.util.functional.Functors.NullOut)}
 * <li> Void. Functions with void parameter can simulate supplier (no input) or
 * consumers (no output) functions. They can be anywhere within the chain.
 * </ul>
 */
public class FChainItemNode extends ChainValueNode<F1<? super Object, ? extends Object>, FItemNode<Object,Object>> {

	private final Function<Class,PrefList<PF<? super Object, ?>>> fp;
	private Class type_in = Void.class;
	private NullIn handleNullIn = NullIn.NULL;
	private NullOut handleNullOut = NullOut.NULL;

	/** Creates unlimited chain starting with Void.class. */
	public FChainItemNode(Function<Class,PrefList<PF<? super Object, ?>>> functionPool) {
		this(Void.class, Integer.MAX_VALUE, functionPool);
	}

	/** Creates unlimited chain starting with specified type. */
	public FChainItemNode(Class in, Function<Class,PrefList<PF<? super Object, ?>>> functionPool) {
		this(in, Integer.MAX_VALUE, functionPool);
	}

	/** Creates limited chain starting with specified type. */
	@SuppressWarnings("SimplifiableConditionalExpression")
	public FChainItemNode(Class in, int max_len, Function<Class,PrefList<PF<? super Object, ?>>> functionPool) {
		super(null);

//        super(len, max_len, () -> new ƑItemNode(functionPool));
		fp = functionPool;
		chainedFactory = () -> new FItemNode<>(() -> fp.apply(getTypeOut()));
		isHomogeneous = (i, f) -> {
			// Link is homogeneous if removing the function poses no problem.
			// For function f this is when previous function output type is same as next function input type

			// Identity function always produces homogeneous link.
			// Workaround for identity function erasing its own return type to Object.class
			// Identity function (x -> x) has same input type and output type, but this is
			// generalized to Object since it works for any object. We either handle this manually
			// here, or guarantee that the identity function input type will not be erased
			// (basically we need instance of identity function per each class)
			if (f==IDENTITY) return true;
			if (f instanceof TypeAwareF && ((TypeAwareF) f).f==IDENTITY) return true; // just in case

			// If two subsequent functions have same output type (or input type) one of them is safe
			// to remove (which one depends on whether we check inputs or outputs).
			//
			// The exceptional case is the first and the last link of the chain, which lack the
			// previous (respectively following) function.
			// However, they can still be removed. Checking for output types will result in the first
			// link being an exceptional case. Below we check for inputs, which results in the last
			// link to be exceptional case. We do this, because the last link can always be removed
			// hence we do not have to handle the case.
			F1 next_f = getValueAt(i + 1);
			return next_f instanceof TypeAwareF && f instanceof TypeAwareF
					? ((TypeAwareF) f).in.equals(((TypeAwareF) next_f).in)
					: false;
		};
		homogeneous = false;
		setTypeIn(in);  // initializes value, don't fire update yet

		maxChainLength.addListener((o, ov, nv) -> {
			int m = nv.intValue();
			if (m<chain.size()) {
				chain.setAll(chain.subList(0, m));
				generateValue();
			}
			chain.forEach(Link::updateIcons);
		});
		chain.addListener((ListChangeListener.Change<? extends Link> c) -> chain.forEach(Link::updateIcons));
		maxChainLength.set(max_len);

		inconsistent_state = false;
		generateValue();
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
	 * <li> determined by object type this chain's reduction function will be applied to
	 * <li> determines available functions in the dropdown box or the 1st chain element
	 * <ul>
	 * <p/>
	 * If the class changes, entire transformation chain is cleared.
	 * Callign this method always results in an update event.
	 * <p/>
	 * Default Void.class
	 */
	public void setTypeIn(Class c) {
		if (type_in.equals(c))
			generateValue();
		else {
			type_in = c;
			chain.clear();
			addChained();
		}
	}

	/**
	 * Sets null handling during chain function reduction.
	 * <p/>
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
		return chain.isEmpty() ? type_in : chain.get(chain.size() - 1).chained.getTypeOut();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected F1<? super Object, Object> reduce(Stream<F1<? super Object, ?>> ƒs) {
		return ƒs.map(ƒ -> ((F1) ƒ).wrap(handleNullIn, handleNullOut))
				.reduce(F1::andThen)
				.orElse(x -> x);
	}

}