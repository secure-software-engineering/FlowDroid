package soot.jimple.infoflow.sourcesSinks.definitions;

/**
 * Enumeration for defining whether an element is a source, a sink, both, or
 * neither
 * 
 * @author Steven Arzt
 *
 */
public enum SourceSinkType {
	Undefined, Source, Sink, Neither, Both;

	/**
	 * Converts boolean flags (is source, is sink) into a source/sink type
	 * 
	 * @param isSink
	 *            True if this method is a sink
	 * @param isSource
	 *            True if this method is a source
	 * @return The source/sink type of the method according to the given flags
	 */
	public static SourceSinkType fromFlags(boolean isSink, boolean isSource) {
		if (isSink && isSource)
			return SourceSinkType.Both;
		else if (!isSink && !isSource)
			return SourceSinkType.Neither;
		else if (isSource)
			return SourceSinkType.Source;
		else if (isSink)
			return SourceSinkType.Sink;
		return SourceSinkType.Undefined;
	}

	/**
	 * Gets whether this type represents a source
	 * 
	 * @return True if this type represents a source, otherwise false
	 */
	public boolean isSource() {
		return this == SourceSinkType.Source || this == SourceSinkType.Both;
	}

	/**
	 * Gets whether this type represents a sink
	 * 
	 * @return True if this type represents a sink, otherwise false
	 */
	public boolean isSink() {
		return this == SourceSinkType.Sink || this == SourceSinkType.Both;
	}

	/**
	 * Removes the given source / sink type from the combined type. In other
	 * words, this method converts, e.g., "Both" to "Source", if "Sink" shall be
	 * removed.
	 * 
	 * @param toRemove
	 *            The element to remove
	 * @return The new combined type from which the given element was removed
	 */
	public SourceSinkType removeType(SourceSinkType toRemove) {
		switch (this) {
		case Undefined:
		case Neither:
			return this;
		case Source:
			return toRemove == SourceSinkType.Source || toRemove == SourceSinkType.Both ? SourceSinkType.Neither : this;
		case Sink:
			return toRemove == SourceSinkType.Sink || toRemove == SourceSinkType.Both ? SourceSinkType.Neither : this;
		case Both:
			switch (toRemove) {
			case Neither:
			case Undefined:
				return this;
			case Source:
				return SourceSinkType.Sink;
			case Sink:
				return SourceSinkType.Source;
			case Both:
				return SourceSinkType.Neither;
			}
		}
		return this;
	}

	/**
	 * Adds the given source / sink type to the combined type. In other words,
	 * this method converts, e.g., "Source" to "Both", if "Source" shall be
	 * added.
	 * 
	 * @param toAdd
	 *            The element to add
	 * @return The new combined type to which the given element was added
	 */
	public SourceSinkType addType(SourceSinkType toAdd) {
		switch (this) {
		case Undefined:
		case Neither:
			return toAdd;
		case Source:
			return toAdd == SourceSinkType.Sink || toAdd == SourceSinkType.Both ? SourceSinkType.Both : this;
		case Sink:
			return toAdd == SourceSinkType.Source || toAdd == SourceSinkType.Both ? SourceSinkType.Both : this;
		case Both:
			return this;
		}
		return this;
	}

}
