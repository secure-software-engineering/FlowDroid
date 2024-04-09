package soot.jimple.infoflow.util;

public class ConstantByReferenceBoolean extends ByReferenceBoolean {

	public static final ByReferenceBoolean TRUE = new ConstantByReferenceBoolean(true);
	public static final ByReferenceBoolean FALSE = new ConstantByReferenceBoolean(false);

	private ConstantByReferenceBoolean(boolean b) {
		super(b);
	}

	@Override
	public ByReferenceBoolean and(boolean b) {
		throw new RuntimeException("This is a constant value; modification not possible");
	}

	@Override
	public ByReferenceBoolean xor(boolean b) {
		throw new RuntimeException("This is a constant value; modification not possible");
	}

	@Override
	public ByReferenceBoolean or(boolean b) {
		throw new RuntimeException("This is a constant value; modification not possible");
	}

}
