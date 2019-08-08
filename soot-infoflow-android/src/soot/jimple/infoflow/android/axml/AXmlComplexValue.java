package soot.jimple.infoflow.android.axml;

/**
 * A complex value from an Android binary XML file
 * 
 * @author Steven Arzt
 *
 */
public class AXmlComplexValue {

	public enum Unit {
		/**
		 * Device-independent pixels
		 */
		DIP,

		/**
		 * A basic fraction of the overall size
		 */
		Fraction,

		/**
		 * A fraction of the parent size
		 */
		FractionParent,

		/**
		 * Inches
		 */
		IN,

		/**
		 * Millimeters
		 */
		MM,

		/**
		 * Points
		 */
		PT,

		/**
		 * Raw pixels
		 */
		PX,

		/**
		 * Scaled pixels
		 */
		SP;

		@Override
		public String toString() {
			switch (this) {
			case DIP:
				return "dip";
			case Fraction:
			case FractionParent:
				return "%";
			case IN:
				return "in";
			case MM:
				return "mm";
			case PT:
				return "pt";
			case PX:
				return "px";
			case SP:
				return "sp";
			default:
				return "";
			}
		}
	}

	private final Unit unit;
	private final int mantissa;
	private final int radix;

	public AXmlComplexValue(Unit unit, int mantissa, int radix) {
		this.unit = unit;
		this.mantissa = mantissa;
		this.radix = radix;
	}

	public Unit getUnit() {
		return unit;
	}

	public int getMantissa() {
		return mantissa;
	}

	public int getRadix() {
		return radix;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Integer.toString(mantissa));
		sb.append('.');
		sb.append(Integer.toString(radix));
		sb.append(unit.toString());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mantissa;
		result = prime * result + radix;
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AXmlComplexValue other = (AXmlComplexValue) obj;
		if (mantissa != other.mantissa)
			return false;
		if (radix != other.radix)
			return false;
		if (unit != other.unit)
			return false;
		return true;
	}

}
