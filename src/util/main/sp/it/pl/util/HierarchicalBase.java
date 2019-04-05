package sp.it.pl.util;

public abstract class HierarchicalBase<T, H extends Hierarchical<H>> implements Hierarchical<H> {

	public final T value;
	public final H parent;

	public HierarchicalBase(T value, H parent) {
		this.value = value;
		this.parent = parent;
	}

	@Override
	public H getHParent() {
		return parent;
	}

}