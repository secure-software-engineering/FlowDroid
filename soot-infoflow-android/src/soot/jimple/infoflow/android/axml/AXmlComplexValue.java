package soot.jimple.infoflow.android.axml;

import java.nio.ByteBuffer;

import soot.jimple.infoflow.android.axml.parsers.AXmlConstants;

/**
 * A complex value from an Android binary XML file
 * 
 * @author Steven Arzt
 *
 */

public class AXmlComplexValue implements pxb.android.Item {

	public enum Unit {
		/**
		 * Device-independent pixels
		 */
		DIP(AXmlConstants.COMPLEX_UNIT_DIP),

		/**
		 * A basic fraction of the overall size
		 */
		Fraction(AXmlConstants.COMPLEX_UNIT_FRACTION),

		/**
		 * A fraction of the parent size
		 */
		FractionParent(AXmlConstants.COMPLEX_UNIT_FRACTION_PARENT),

		/**
		 * Inches
		 */
		IN(AXmlConstants.COMPLEX_UNIT_IN),

		/**
		 * Millimeters
		 */
		MM(AXmlConstants.COMPLEX_UNIT_MM),

		/**
		 * Points
		 */
		PT(AXmlConstants.COMPLEX_UNIT_PT),

		/**
		 * Raw pixels
		 */
		PX(AXmlConstants.COMPLEX_UNIT_PX),

		/**
		 * Scaled pixels
		 */
		SP(AXmlConstants.COMPLEX_UNIT_SP);

		private int unit;

		Unit(int c) {
			this.unit = c;
		}

		public int getUnit() {
			return unit;
		}

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

	@Override
	public void writeout(ByteBuffer out) {
		out.putInt(getInt());
	}

	public int getInt() {
		int val = 0;
		val |= mantissa;
		val = val << (AXmlConstants.COMPLEX_MANTISSA_SHIFT - AXmlConstants.COMPLEX_RADIX_SHIFT);

		val |= radix;
		val = val << AXmlConstants.COMPLEX_RADIX_SHIFT;

		val |= unit.getUnit();
		return val;
	}

	/**
	 * Parses the given Android complex value
	 * 
	 * @param complexValue The numeric complex value to parse
	 * @return A data object that contains the information from the complex value
	 */
	public static AXmlComplexValue parseComplexValue(int complexValue) {
		// Get the unit
		int unitVal = complexValue >> AXmlConstants.COMPLEX_UNIT_SHIFT;
		unitVal &= AXmlConstants.COMPLEX_UNIT_MASK;
		Unit complexUnit = parseComplexUnit(unitVal);

		// Get the radix
		int radixVal = complexValue >> AXmlConstants.COMPLEX_RADIX_SHIFT;
		radixVal &= AXmlConstants.COMPLEX_RADIX_MASK;

		// Get the mantissa
		int mantissa = complexValue >> AXmlConstants.COMPLEX_MANTISSA_SHIFT;
		mantissa &= AXmlConstants.COMPLEX_MANTISSA_MASK;

		return new AXmlComplexValue(complexUnit, mantissa, radixVal);
	}

	/**
	 * Parses the given numeric complex unit into one of the well-known enum
	 * constants
	 * 
	 * @param unitVal The numeric complex unit
	 * @return One of the well-known constants for complex units
	 */
	public static Unit parseComplexUnit(int unitVal) {
		switch (unitVal) {
		// Not all cases are listed here, because some of them have the same numeric
		// value
		case AXmlConstants.COMPLEX_UNIT_DIP:
			return Unit.DIP;
		case AXmlConstants.COMPLEX_UNIT_IN:
			return Unit.IN;
		case AXmlConstants.COMPLEX_UNIT_MM:
			return Unit.MM;
		case AXmlConstants.COMPLEX_UNIT_PT:
			return Unit.PT;
		case AXmlConstants.COMPLEX_UNIT_PX:
			return Unit.PX;
		case AXmlConstants.COMPLEX_UNIT_SP:
			return Unit.SP;
		default:
			throw new RuntimeException(String.format("Unknown complex unit %d", unitVal));
		}
	}

}
