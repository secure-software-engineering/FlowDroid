package soot.jimple.infoflow.android.axml.parsers;

/**
 * Class that contains constant definitions from the Android SDK
 * 
 * @author Steven Arzt
 *
 */
public class AXmlConstants {

	public static final int TYPE_NULL = 0x00;
	public static final int TYPE_REFERENCE = 0x01;
	public static final int TYPE_ATTRIBUTE = 0x02;
	public static final int TYPE_STRING = 0x03;
	public static final int TYPE_FLOAT = 0x04;
	public static final int TYPE_DIMENSION = 0x05;
	public static final int TYPE_FRACTION = 0x06;
	public static final int TYPE_FIRST_INT = 0x10;
	public static final int TYPE_INT_DEC = 0x10;
	public static final int TYPE_INT_HEX = 0x11;
	public static final int TYPE_INT_BOOLEAN = 0x12;

	public static final int TYPE_FIRST_COLOR_INT = 0x1c;
	public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
	public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
	public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
	public static final int TYPE_INT_COLOR_RGB4 = 0x1f;
	public static final int TYPE_LAST_COLOR_INT = 0x1f;

	public static final int TYPE_LAST_INT = 0x1f;

	public static final int COMPLEX_MANTISSA_MASK = 0x00ffffff;
	public static final int COMPLEX_MANTISSA_SHIFT = 0x00000008;
	public static final int COMPLEX_RADIX_0p23 = 0x00000003;
	public static final int COMPLEX_RADIX_16p7 = 0x00000001;
	public static final int COMPLEX_RADIX_23p0 = 0x00000000;
	public static final int COMPLEX_RADIX_8p15 = 0x00000002;
	public static final int COMPLEX_RADIX_MASK = 0x00000003;
	public static final int COMPLEX_RADIX_SHIFT = 0x00000004;
	public static final int COMPLEX_UNIT_DIP = 0x00000001;
	public static final int COMPLEX_UNIT_FRACTION = 0x00000000;
	public static final int COMPLEX_UNIT_FRACTION_PARENT = 0x00000001;
	public static final int COMPLEX_UNIT_IN = 0x00000004;
	public static final int COMPLEX_UNIT_MASK = 0x0000000f;
	public static final int COMPLEX_UNIT_MM = 0x00000005;
	public static final int COMPLEX_UNIT_PT = 0x00000003;
	public static final int COMPLEX_UNIT_PX = 0x00000000;
	public static final int COMPLEX_UNIT_SHIFT = 0x00000000;
	public static final int COMPLEX_UNIT_SP = 0x00000002;

	public static final int DATA_NULL_EMPTY = 0x00000001;
	public static final int DATA_NULL_UNDEFINED = 0x00000000;

	public static final int DENSITY_DEFAULT = 0x00000000;
	public static final int DENSITY_NONE = 0x0000ffff;

}
