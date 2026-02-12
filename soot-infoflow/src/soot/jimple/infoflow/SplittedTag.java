package soot.jimple.infoflow;

import soot.tagkit.Tag;

public class SplittedTag implements Tag {

	public static final String NAME = "Splitted";
	private static final Tag INSTANCE = new SplittedTag();

	@Override
	public String getName() {
		return NAME;
	}

	public static Tag v() {
		return INSTANCE;
	}

}
