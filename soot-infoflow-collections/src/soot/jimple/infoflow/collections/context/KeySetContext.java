package soot.jimple.infoflow.collections.context;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import soot.jimple.infoflow.collections.util.ImmutableArraySet;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * Representation of map keys using a set possible keys
 *
 * @param <C> key type
 */
public class KeySetContext<C> implements ValueBasedContext<KeySetContext<?>> {
	private final Set<C> keys;
	private final boolean impreciseValue;

	public KeySetContext(C key) {
		this(key, false);
	}

	public KeySetContext(C key, boolean impreciseValue) {
		this.keys = new ImmutableArraySet<>(key);
		this.impreciseValue = impreciseValue;
	}

	public KeySetContext(Set<C> keys) {
		this(keys, false);
	}

	public KeySetContext(Set<C> keys, boolean impreciseValue) {
		this.keys = new ImmutableArraySet<>(keys);
		this.impreciseValue = impreciseValue;
	}

	@Override
	public Tristate intersect(KeySetContext<?> other) {
		boolean imprecise = this.isImprecise() || other.isImprecise();
		boolean all = true;
		boolean any = false;
		for (C c : this.keys) {
			if (other.keys.contains(c)) {
				any = true;
				all = all && !imprecise;
			} else {
				all = false;
			}

			if (any && !all)
				return Tristate.MAYBE();
		}

		return Tristate.fromBoolean(any && all);
	}

	public boolean entails(ContainerContext other) {
		if (!(other instanceof KeySetContext))
			return false;

		for (C key : keys)
			if (!((KeySetContext<?>) other).keys.contains(key))
				return false;

		return true;
	}

	@Override
	public boolean containsInformation() {
		return !keys.isEmpty();
	}

	@Override
	public boolean isImprecise() {
		return impreciseValue || keys.size() > 1;
	}

	@Override
	public String toString() {
		return keys.stream().map(Object::toString).collect(Collectors.joining(", "));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KeySetContext<?> that = (KeySetContext<?>) o;
		return Objects.equals(keys, that.keys) && isImprecise() == that.isImprecise();
	}

	@Override
	public int hashCode() {
		return Objects.hash(keys);
	}
}
