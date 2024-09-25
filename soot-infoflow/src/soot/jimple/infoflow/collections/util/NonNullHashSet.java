package soot.jimple.infoflow.collections.util;

import java.util.HashSet;

public class NonNullHashSet<E> extends HashSet<E> {

	private static final long serialVersionUID = -7700007534552087413L;

	@Override
	public boolean add(E e) {
		if (e == null)
			return false;
		return super.add(e);
	}

}
