package sp.it.pl.gui.itemnode;

import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.util.access.V;
import sp.it.util.reactive.Handler1;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.ISNTØ;
import static sp.it.util.functional.Util.repeat;

/**
 * {@link ValueNode} containing editable list of {@link ValueNode}.
 *
 * @param <VAL> type of value that is chained. Each value is contained in a chained C - a {@link ValueNode} of the same
 * type. The exact type is specified as generic parameter.
 * @param <C> type of chained. This chain will be made of links of chained of exactly this type, either provided
 * manually or constructed using factory.
 */
public abstract class ChainValueNode<VAL, C extends ValueNode<VAL>> extends ValueNode<VAL> {

	private static final Tooltip addTooltip = appTooltip("Add");
	private static final Tooltip remTooltip = appTooltip("Remove");
	private static final Tooltip onTooltip = appTooltip("Enable. Disabled elements will not be in the list.");

	protected final VBox root = new VBox();
	@SuppressWarnings("unchecked")
	protected final ObservableList<Link> chain = (ObservableList) root.getChildren();
	public final IntegerProperty maxChainLength = new SimpleIntegerProperty(Integer.MAX_VALUE);
	public final V<Boolean> editable = new V<>(true);
	protected Supplier<C> chainedFactory; // final
	protected boolean homogeneous = true;
	public boolean inconsistent_state = true;
	protected BiPredicate<Integer,VAL> isHomogeneous = (i, v) -> false;
	public final Handler1<Link> onUserItemAdded = new Handler1<>();
	public final Handler1<Link> onUserItemRemoved = new Handler1<>();
	public final Handler1<Link> onUserItemEnabled = new Handler1<>();
	public final Handler1<Link> onUserItemDisabled = new Handler1<>();

	/** Creates unlimited chain of 1 initial chained element. */
	public ChainValueNode(VAL initialValue) {
		super(initialValue);
		editable.attachC(it -> System.out.println("editable " + it));
		editable.attachC(it -> chain.forEach(Link::updateIcons));
	}

	/** Creates unlimited chain of 1 initial chained element. */
	public ChainValueNode(VAL initialValue, Supplier<C> chainedFactory) {
		this(1, initialValue, chainedFactory);
	}

	/** Creates unlimited chain of i initial chained elements. */
	public ChainValueNode(int i, VAL initialValue, Supplier<C> chainedFactory) {
		this(i, Integer.MAX_VALUE, initialValue, chainedFactory);
	}

	/** Creates limited chain of i initial chained elements. */
	public ChainValueNode(int len, int max_len, VAL initialValue, Supplier<C> chainedFactory) {
		this(initialValue);

		maxChainLength.set(max_len);
		this.chainedFactory = chainedFactory;
		growTo(len);
		maxChainLength.addListener((o, ov, nv) -> {
			if (nv.intValue()<ov.intValue())
				shrinkTo(nv.intValue());
		});
		chain.addListener((Change<? extends Link> c) -> chain.forEach(Link::updateIcons));
		inconsistent_state = false;
	}

	public Link addChained() {
		return addChained(chain.size(), chainedFactory.get());
	}

	public Link addChained(int i) {
		return addChained(i, chainedFactory.get());
	}

	public Link addChained(C chained) {
		return addChained(chain.size(), chained);
	}

	public Link addChained(int i, C chained) {
		if (chain.size()<maxChainLength.get()) {
			Link c = new Link(i, chained);
			chain.add(i, c);
			generateValue();
			return c;
		} else {
			return null;
		}
	}

	/**
	 * Grows the chain to 1.
	 * It is important for the chain length to be at least 1, otherwise it will
	 * become non-editable and always empty!
	 * <p/>
	 * Equivalent to {@code growTo(1); }
	 *
	 * @see #growTo(int)
	 */
	public void growTo1() {
		growTo(1);
	}

	/**
	 * Grows the chain (adds chained to it using the chained factory) so there is
	 * at least n chained elements.
	 * If the chain is already at least as long as n, no elements are added.
	 */
	public void growTo(int n) {
		failIf(n<0, () -> "Chain length must not be negative");
		failIf(n>maxChainLength.get(), () -> "Chain length must not be larger than max length");

		if (n>chain.size()) {
			repeat(n - chain.size(), (Runnable) this::addChained);
			generateValue();
		}
	}

	public void shrinkTo(int n) {
		failIf(n<0, () -> "Chain length must not be negative");
		failIf(n>maxChainLength.get(), () -> "Chain length must not be larger than max length");

		if (n<chain.size()) {
			chain.setAll(chain.stream().limit(n).collect(toList()));
			generateValue();
			chain.forEach(Link::updateIcons);
		}
	}

	public int length() {
		return chain.size();
	}

	/** {@inheritDoc} */
	@Override
	public VBox getNode() {
		return root;
	}

	/**
	 * Invokes focus on last chained element.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public void focus() {
		if (!chain.isEmpty())
			chain.get(chain.size() - 1).chained.focus();
	}

	protected void generateValue() {
		if (inconsistent_state) return;
		changeValue(reduce(getValues()));
	}

	abstract protected VAL reduce(Stream<VAL> values);

	public VAL getValueAt(int i) {
		return i>=0 && i<chain.size() && chain.get(i).chained!=null && chain.get(i).chained.getVal()!=null
				? chain.get(i).chained.getVal()
				: null;
	}

	/** Return individual chained values that are enabled and non null. */
	public Stream<VAL> getValues() {
		return chain.stream().filter(g -> g.on.getValue())
				.map(g -> g.chained.getVal())
				.filter(ISNTØ);
	}

	/** Adjusts remove button of the first element of the chain (which is disabled by default). */
	public final ObjectProperty<Function1<Icon,Unit>> buttonAdjuster = new SimpleObjectProperty<>(null) {{
		addListener((o,ov,nv) -> {
			var icon = chain.isEmpty() ? null : chain.get(0).rem;
			if (nv!=null && icon!=null) nv.invoke(icon);
		});
	}};

	public class Link extends HBox {
		private final CheckIcon onB = new CheckIcon(true);
		private final Icon rem = new Icon(MINUS, 13);
		private final Icon add = new Icon(PLUS, 13);
		private final int initialIndex;
		private boolean wasAdjusted = false;
		public final C chained;
		public final Property<Boolean> on = onB.selected;

		Link(int initialIndex, C c) {
			this.initialIndex = initialIndex;
			chained = c;
			chained.onItemChange = f -> generateValue();
			setSpacing(5);
			setPadding(new Insets(0, 0, 0, 5));
			setAlignment(CENTER_LEFT);
			HBox.setHgrow(chained.getNode(), ALWAYS);

			on.addListener((o, ov, nv) -> generateValue());
			on.addListener((o, ov, nv) -> { if (nv) onUserItemEnabled.invoke(this); else onUserItemDisabled.invoke(this); });
			rem.setOnMouseClicked(e -> { onRem(); onUserItemRemoved.invoke(this); });
			add.setOnMouseClicked(e -> { onUserItemAdded.invoke(addChained(getIndex() + 1)); });
			rem.tooltip(remTooltip);
			add.tooltip(addTooltip);
			onB.tooltip(onTooltip);
			updateIcons();
		}

		/** @return position index within the chain from 0 to chain.size() */
		public final int getIndex() {
			var i =  chain.indexOf(this);
			return i==-1 ? initialIndex : i;
		}

		public void updateIcons() {
			int i = getIndex();
			boolean h = isHomogeneous();

			rem.setDisable(h && i==0);
			add.setDisable(h && i>=maxChainLength.get()-1);
			setDisable(!h);

			if (i==0 && buttonAdjuster.get()!=null) {
				rem.setDisable(false);
				if (!wasAdjusted) buttonAdjuster.get().invoke(rem);
				wasAdjusted = true;
			}

			if (editable.getValue()) getChildren().setAll(rem, add, onB, chained.getNode());
			else getChildren().setAll(chained.getNode());
		}

		public void onRem() {
			chain.remove(this);
			if (chain.isEmpty()) {
				growTo1();
			} else {
				generateValue();
			}
		}

		private boolean isHomogeneous() {
			int i = getIndex();
			boolean hi = false;
			if (chained!=null && chained.getVal()!=null) {
				hi = isHomogeneous.test(i, chained.getVal());
			}
			return homogeneous || hi || i>=chain.size() - 1;

			// optimization, but does not work?
//            int i = getIndex();
//
//            if (i==0) return false;
//            if (i>=chain.size()-1) return true;
//            if (homogeneous) return true;
//
//            if (i>0 && chained!=null && chained.getValue()!=null)
//                return isHomogeneous.test(chained.getValue());
//            return false;

		}
	}

	public static class ListConfigField<V, IN extends ValueNode<V>> extends ChainValueNode<V,IN> {

		public ListConfigField(Supplier<IN> chainedFactory) {
			super(null, chainedFactory);
			inconsistent_state = false;
			generateValue();
		}

		public ListConfigField(int length, Supplier<IN> chainedFactory) {
			super(length, null, chainedFactory);
			inconsistent_state = false;
			generateValue();
		}

		@Override
		protected void generateValue() {
			changeValue(null);
		}

		@Override
		protected void changeValue(V nv) {
			value = nv;
			onItemChange.accept(nv);
		}

		@Override
		public V getVal() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected V reduce(Stream<V> values) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

	}
}