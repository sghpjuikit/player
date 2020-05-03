package sp.it.pl.ui.itemnode;

import java.util.List;
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
import sp.it.pl.ui.objects.icon.CheckIcon;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.util.access.V;
import sp.it.util.reactive.Handler0;
import sp.it.util.reactive.Handler1;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.ISNT0;
import static sp.it.util.functional.Util.repeat;

/**
 * {@link ValueNode} containing editable list of {@link ValueNode}.
 *
 * @param <VAL> type of value that is chained. Each value is contained in a chained C - a {@link ValueNode} of the same
 * type. The exact type is specified as generic parameter.
 * @param <C> type of chained. This chain will be made of links of chained of exactly this type, either provided
 * manually or constructed using factory.
 */
public abstract class ChainValueNode<VAL, C extends ValueNode<VAL>, REDUCED_VAL> extends ValueNode<REDUCED_VAL> {

	private static final Tooltip addTooltip = appTooltip("Add new item after this item");
	private static final Tooltip remTooltip = appTooltip("Remove this item");
	private static final Tooltip remAllTooltip = appTooltip("Remove all items");
	private static final Tooltip onTooltip = appTooltip("Enable. Disabled elements will not be in the list.");

	protected final VBox rootLinks = new VBox();
	protected final VBox root = new VBox(rootLinks);
	@SuppressWarnings("unchecked")
	protected final ObservableList<Link> chain = (ObservableList) rootLinks.getChildren();
	public final IntegerProperty maxChainLength = new SimpleIntegerProperty(Integer.MAX_VALUE);
	public final V<Boolean> editable = new V<>(true);
	protected Supplier<C> chainedFactory; // final
	public boolean inconsistentState = true;
	/** Whether the link at the index can be removed from this chain. Default always true. */
	protected BiPredicate<Integer,VAL> isHomogeneousRem = (i, v) -> true;
	/** Whether new link can be added into this chain after the link at the index. Default always true. */
	protected BiPredicate<Integer,VAL> isHomogeneousAdd = (i, v) -> true;
	/** Whether the link at the index can have its enable icon enabled. Default always true. */
	protected BiPredicate<Integer,VAL> isHomogeneousOn = (i, v) -> true;
	/** Whether the link at the index can have its content enabled. Default always true. */
	protected BiPredicate<Integer,VAL> isHomogeneousEdit = (i, v) -> true;
	public final Handler1<Link> onUserItemAdded = new Handler1<>();
	public final Handler1<Link> onUserItemRemoved = new Handler1<>();
	public final Handler0 onUserItemsCleared = new Handler0();
	public final Handler1<Link> onUserItemEnabled = new Handler1<>();
	public final Handler1<Link> onUserItemDisabled = new Handler1<>();

	/** Creates unlimited chain of 1 initial chained element. */
	public ChainValueNode(REDUCED_VAL initialValue) {
		super(initialValue);
		editable.attachC(it -> chain.forEach(Link::updateIcons));
		editable.attachC(it -> root.getChildren().stream().filter(i -> i instanceof ChainValueNode.NullLink).map(k -> (ChainValueNode.NullLink) k).forEach(j -> j.updateIcons()));
	}

	/** Creates unlimited chain of 1 initial chained element. */
	public ChainValueNode(REDUCED_VAL initialValue, Supplier<C> chainedFactory) {
		this(1, initialValue, chainedFactory);
	}

	/** Creates unlimited chain of i initial chained elements. */
	public ChainValueNode(int initialLength, REDUCED_VAL initialValue, Supplier<C> chainedFactory) {
		this(initialLength, Integer.MAX_VALUE, initialValue, chainedFactory);
	}

	/** Creates limited chain of i initial chained elements. */
	public ChainValueNode(int initialLength, int maxChainLength, REDUCED_VAL initialValue, Supplier<C> chainedFactory) {
		this(initialValue);

		this.maxChainLength.addListener((o,ov,nv) -> failIf(nv.intValue()<=0, () -> "Max chain length must be > 0"));
		this.maxChainLength.set(maxChainLength);
		this.chainedFactory = chainedFactory;
		growTo(initialLength);
		this.maxChainLength.addListener((o, ov, nv) -> {
			if (nv.intValue()<ov.intValue())
				shrinkTo(nv.intValue());
		});
		chain.addListener((Change<? extends Link> c) -> chain.forEach(Link::updateIcons));
		inconsistentState = false;
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
			chain.forEach(Link::updateIcons);
			generateValue();
			return c;
		} else {
			return null;
		}
	}

	public Link setChained(int i, C chained) {
		Link c = new Link(i, chained);
		chain.set(i, c);
		chain.forEach(Link::updateIcons);
		generateValue();
		return c;
	}

	public void setHeaderVisible(boolean visible) {
		if (visible && !isHeaderVisible()) {
			root.getChildren().add(0, new NullLink());
			chain.forEach(Link::updateIcons);
		}
		if (!visible && isHeaderVisible()) {
			root.getChildren().remove(0);
			chain.forEach(Link::updateIcons);
		}
	}

	public boolean isHeaderVisible() {
		return !root.getChildren().isEmpty() && root.getChildren().get(0) instanceof ChainValueNode.NullLink;
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
			inconsistentState = true;
			repeat(n - chain.size(), (Runnable) this::addChained);
			chain.forEach(Link::updateIcons);
			inconsistentState = false;
			generateValue();
		}
	}

	public void shrinkTo(int n) {
		failIf(n<0, () -> "Chain length must not be negative");
		failIf(n>maxChainLength.get(), () -> "Chain length must not be larger than max length");

		if (n<chain.size()) {
			inconsistentState = true;
			chain.setAll(chain.stream().limit(n).collect(toList()));
			chain.forEach(Link::updateIcons);
			inconsistentState = false;
			generateValue();
		}
	}

	public void convergeTo(int n) {
		shrinkTo(n);
		growTo(n);
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

	protected final void generateValue() {
		if (inconsistentState) return;
		changeValue(reduce(getValues()));
	}

	abstract protected REDUCED_VAL reduce(Stream<VAL> values);

	public VAL getValueAt(int i) {
		return i>=0 && i<chain.size() && chain.get(i).chained!=null ? chain.get(i).chained.getVal() : null;
	}

	/** Return individual chained values that are enabled and non null. */
	public Stream<VAL> getValues() {
		return chain.stream().filter(g -> g.on.getValue())
				.map(g -> g.chained.getVal())
				.filter(ISNT0);
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
			on.addListener((o, ov, nv) -> ChainValueNode.this.chain.forEach(it -> it.updateIcons()));
			on.addListener((o, ov, nv) -> { if (nv) onUserItemEnabled.invoke(this); else onUserItemDisabled.invoke(this); });
			rem.setOnMouseClicked(e -> { onRem(); onUserItemRemoved.invoke(this); });
			add.setOnMouseClicked(e -> { onUserItemAdded.invoke(addChained(getIndex() + 1)); });
			rem.tooltip(remTooltip);
			add.tooltip(addTooltip);
			onB.tooltip(onTooltip);
		}

		/** @return position index within the chain from 0 to chain.size() */
		public int getIndex() {
			var i =  chain.indexOf(this);
			return i==-1 ? initialIndex : i;
		}

		public void updateIcons() {
			var i = getIndex();
			var minChainLength = isHeaderVisible() ? 1 : 0;
			var noRem = chain.size()<=minChainLength || !isHomogeneous(i, isHomogeneousRem);
			var noAdd = chain.size()>=maxChainLength.getValue() || !isHomogeneous(i, isHomogeneousAdd);
			var noEdit = !isHomogeneous(i, isHomogeneousEdit);
			var isCriticalLength = (chain.size()<=minChainLength && on.getValue()) || (chain.size()>=maxChainLength.getValue() && !on.getValue());
			var noOn = noRem || noEdit || !isHomogeneous(i, isHomogeneousOn) || isCriticalLength;

			rem.setDisable(noRem);
			add.setDisable(noAdd);
			onB.setDisable(noOn);
			chained.getNode().setDisable(noEdit);

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

			if (!isHeaderVisible() && chain.isEmpty()) growTo1();
			else generateValue();
		}

		private boolean isHomogeneous(int index, BiPredicate<Integer,VAL> isHomogeneous) {
			if (chained!=null) return isHomogeneous.test(index, chained.getVal());
			return false;
		}

	}

	public class NullLink extends HBox {
		private final Icon rem = new Icon(MINUS, 13);
		private final Icon add = new Icon(PLUS, 13);

		public NullLink() {
			setSpacing(5);
			setPadding(new Insets(0, 0, 0, 5));
			setAlignment(CENTER_LEFT);
			getChildren().addAll(rem, add);
			rem.setOnMouseClicked(e -> { onClear(); onUserItemsCleared.invoke(); });
			add.setOnMouseClicked(e -> { onUserItemAdded.invoke(addChained(getIndex() + 1)); });
			rem.tooltip(remAllTooltip);
			add.tooltip(addTooltip);
			updateIcons();
		}

		public int getIndex() {
			return -1;
		}

		public void updateIcons() {
			if (editable.getValue()) getChildren().setAll(rem, add);
			else getChildren().clear();
		}

		public void onClear() {
			chain.clear();
			if (!isHeaderVisible()) growTo1();
			else generateValue();
		}
	}

	public static class ListChainValueNode<V, IN extends ValueNode<V>> extends ChainValueNode<V,IN,List<V>> {

		public ListChainValueNode(Supplier<IN> chainedFactory) {
			this(0, chainedFactory);
		}

		public ListChainValueNode(int initialLength, Supplier<IN> chainedFactory) {
			super(initialLength, List.of(), chainedFactory);
			inconsistentState = false;
			generateValue();
		}

		@Override
		protected List<V> reduce(Stream<V> values) {
			return values.collect(toList());
		}

	}
}