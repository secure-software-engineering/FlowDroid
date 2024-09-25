package soot.jimple.infoflow.collections.util;

import java.util.HashSet;

import soot.PrimType;
import soot.Type;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.typing.TypeUtils;

/**
 * Set that only adds abstractions that are valid in the alias flow
 *
 * @author Tim Lange
 */
public class AliasAbstractionSet extends HashSet<Abstraction> {

	private static final long serialVersionUID = -2347968480500330461L;

	private Abstraction checkAbstraction(Abstraction abs) {
		if (abs == null)
			return null;

		Type t;
		if (abs.getAccessPath().isStaticFieldRef())
			t = abs.getAccessPath().getFirstFieldType();
		else
			t = abs.getAccessPath().getBaseType();

		if (t instanceof PrimType || (TypeUtils.isStringType(t) && !abs.getAccessPath().getCanHaveImmutableAliases()))
			return null;

		return abs;
	}

	@Override
	public boolean add(Abstraction abs) {
		abs = checkAbstraction(abs);
		if (abs != null)
			return super.add(abs);
		return false;
	}
}
