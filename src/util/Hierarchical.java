package util;

import java.util.List;

/**
 * Object in a tree hierarchy, having an optional parent and any number of children. All objects in the hierarchy
 * inherit from <i>hierarchy</i> type H.
 *
 * @param <H> hierarchy type
 */
public interface Hierarchical<H extends Hierarchical<H>> {

	H getHParent();

	List<H> getHChildren();

	// TODO: implement
//	@SuppressWarnings("unchecked")
//	default Stream<H> getHChildrenR() {
//		return StreamEx.ofTree((H) this, (H h) -> h.getHChildren().stream());
//	}

	default boolean isHLeaf() {
		return getHChildren().isEmpty();
	}

	// TODO: implement
//	default Stream<H> getHLeafChildrenR() {
//		return getHChildrenR().filter(Hierarchical::isHLeaf);
//	}

	default boolean isHRoot() {
		return getHParent()==null;
	}

	@SuppressWarnings("unchecked")
	default H getHRoot() {
		H i = (H) this, p;

		// Depending on implementation, this may perform sub-optimally as it calls getHParent twice
		// Hence, we optimize it to the below
		// while (!i.isHRoot())
		//	  i = i.getHParent();

		while ((p = i.getHParent())!=null)
			i = p;

		return i;
	}

	@SuppressWarnings("unchecked")
	default boolean isHChildOf(H h) {
		if (this==h) return false;
		H i = (H) this;

		while ((i = i.getHParent())!=null)
			if (i==h) return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	default boolean isHParentOf(H h) {
		return h!=null && h.isHChildOf((H) this);
	}

}