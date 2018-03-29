package soot.jimple.infoflow.android.axml;

/**
 * Value representing a color in a binary Android XML file
 * 
 * @author Steven Arzt
 *
 */
public class AXmlColorValue {

	private final int a;
	private final int r;
	private final int g;
	private final int b;

	public AXmlColorValue(int a, int r, int g, int b) {
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public AXmlColorValue(int r, int g, int b) {
		this.a = -1;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public int getA() {
		return a;
	}

	public int getR() {
		return r;
	}

	public int getG() {
		return g;
	}

	public int getB() {
		return b;
	}

	/**
	 * Creates a string representation of the ARGB values as #aarrggbb
	 * 
	 * @return A string representation of the ARGB values
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('#');
		if (a >= 0)
			sb.append(toHexComponent(a));
		sb.append(toHexComponent(r));
		sb.append(toHexComponent(g));
		sb.append(toHexComponent(b));
		return sb.toString();
	}

	/**
	 * Creates a string representation of the RGB values as #rrggbb
	 * 
	 * @return A string representation of the RGB values
	 */
	public String toRGBString() {
		StringBuilder sb = new StringBuilder();
		sb.append('#');
		sb.append(toHexComponent(r));
		sb.append(toHexComponent(g));
		sb.append(toHexComponent(b));
		return sb.toString();
	}

	private static String toHexComponent(int val) {
		String s = Integer.toHexString(val);
		if (s.length() < 2)
			s = "0" + s;
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + a;
		result = prime * result + b;
		result = prime * result + g;
		result = prime * result + r;
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
		AXmlColorValue other = (AXmlColorValue) obj;
		if (a != other.a)
			return false;
		if (b != other.b)
			return false;
		if (g != other.g)
			return false;
		if (r != other.r)
			return false;
		return true;
	}

}
