package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

import java.util.Objects;

public class IntervalContext implements PositionBasedContext<IntervalContext> {
	private final int min;
	private final int max;
	private final boolean impreciseValue;

	public IntervalContext(int i) {
		this(i, i, false);
	}

	public IntervalContext(int min, int max) {
		this(min, max, false);
	}

	public IntervalContext(int min, int max, boolean impreciseValue) {
		this.min = min;
		this.max = max;
		this.impreciseValue = impreciseValue;
	}

	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	@Override
	public Tristate intersects(IntervalContext other) {
		if (this.equals(other))
			return isImprecise() ? Tristate.MAYBE() : Tristate.TRUE();
		if (Integer.max(min, other.min) <= Integer.min(max, other.max))
			return Tristate.MAYBE();
		return Tristate.FALSE();
	}

	@Override
	public boolean entails(ContainerContext obj) {
		if (!(obj instanceof IntervalContext))
			return false;
		IntervalContext other = (IntervalContext) obj;
		return this.min <= other.min && other.max <= this.max;
	}

	@Override
	public IntervalContext exactShift(IntervalContext n) {
		// Never increase the upper bound above the max value
		int newMax = (n.max > 0 && Integer.MAX_VALUE - max < n.max) ? Integer.MAX_VALUE : max + n.max;
		int newMin = min + n.min;
		if (newMin < 0) {
			// We cannot have indices less than zero
			if (newMax < 0)
				return this;
			// If the max is above zero, keep the minimum at zero
			newMin = 0;
		}
		return new IntervalContext(newMin, newMin);
	}

	@Override
	public IntervalContext union(IntervalContext other) {
		return new IntervalContext(Math.min(this.min, other.min), Math.max(this.max, other.max));
	}

	@Override
	public Tristate lessThanEqual(IntervalContext other) {
		if (max <= other.min)
			return Tristate.TRUE();
		if (other.max < min)
			return Tristate.FALSE();
		return Tristate.MAYBE();
	}

	public boolean containsInformation() {
		return min != 0 || max != Integer.MAX_VALUE;
	}

	@Override
	public boolean isImprecise() {
		return min != max || impreciseValue;
	}

	public int size() {
		return max - min;
	}

	@Override
	public int hashCode() {
		return Objects.hash(min, max);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IntervalContext other = (IntervalContext) o;
		return min == other.min && max == other.max && isImprecise() == other.isImprecise();
	}

	@Override
	public String toString() {
		return min + "-" + max;
	}
}
