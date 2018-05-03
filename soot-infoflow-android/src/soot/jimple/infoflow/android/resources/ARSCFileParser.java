/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.resources;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for reading out the contents of Android's resource.arsc file.
 * Structure declarations and comments taken from the Android source code and
 * ported from C to Java.
 * 
 * @author Steven Arzt
 */
public class ARSCFileParser extends AbstractResourceParser {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final static int RES_STRING_POOL_TYPE = 0x0001;
	protected final static int RES_TABLE_TYPE = 0x0002;
	protected final static int RES_TABLE_PACKAGE_TYPE = 0x0200;
	protected final static int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
	protected final static int RES_TABLE_TYPE_TYPE = 0x0201;

	protected final static int SORTED_FLAG = 1 << 0;
	protected final static int UTF8_FLAG = 1 << 8;

	protected final static int SPEC_PUBLIC = 0x40000000;

	/**
	 * Contains no data
	 */
	protected final static int TYPE_NULL = 0x00;
	/**
	 * The 'data' holds a ResTable_ref, a reference to another resource table entry.
	 */
	protected final static int TYPE_REFERENCE = 0x01;
	/**
	 * The 'data' holds an attribute resource identifier.
	 */
	protected final static int TYPE_ATTRIBUTE = 0x02;
	/**
	 * The 'data' holds an index into the containing resource table's global value
	 * string pool.
	 */
	protected final static int TYPE_STRING = 0x03;
	/**
	 * The 'data' holds a single-precision floating point number.
	 */
	protected final static int TYPE_FLOAT = 0x04;
	/**
	 * The 'data' holds a complex number encoding a dimension value, such as
	 * "100in".
	 */
	protected final static int TYPE_DIMENSION = 0x05;
	/**
	 * The 'data' holds a complex number encoding a fraction of a container.
	 */
	protected final static int TYPE_FRACTION = 0x06;
	/**
	 * Beginning of integer flavors...
	 */
	protected final static int TYPE_FIRST_INT = 0x10;
	/**
	 * The 'data' is a raw integer value of the form n..n.
	 */
	protected final static int TYPE_INT_DEC = 0x10;
	/**
	 * The 'data' is a raw integer value of the form 0xn..n.
	 */
	protected final static int TYPE_INT_HEX = 0x11;
	/**
	 * The 'data' is either 0 or 1, for input "false" or "true" respectively.
	 */
	protected final static int TYPE_INT_BOOLEAN = 0x12;
	/**
	 * Beginning of color integer flavors...
	 */
	protected final static int TYPE_FIRST_COLOR_INT = 0x1c;
	/**
	 * The 'data' is a raw integer value of the form #aarrggbb.
	 */
	protected final static int TYPE_INT_COLOR_ARGB8 = 0x1c;
	/**
	 * The 'data' is a raw integer value of the form #rrggbb.
	 */
	protected final static int TYPE_INT_COLOR_RGB8 = 0x1d;
	/**
	 * The 'data' is a raw integer value of the form #argb.
	 */
	protected final static int TYPE_INT_COLOR_ARGB4 = 0x1e;
	/**
	 * The 'data' is a raw integer value of the form #rgb.
	 */
	protected final static int TYPE_INT_COLOR_RGB4 = 0x1f;
	/**
	 * ...end of integer flavors.
	 */
	protected final static int TYPE_LAST_COLOR_INT = 0x1f;
	/**
	 * ...end of integer flavors.
	 */
	protected final static int TYPE_LAST_INT = 0x1f;

	/**
	 * This entry holds the attribute's type code.
	 */
	protected final static int ATTR_TYPE = (0x01000000 | (0 & 0xFFFF));
	/**
	 * For integral attributes, this is the minimum value it can hold.
	 */
	protected final static int ATTR_MIN = (0x01000000 | (1 & 0xFFFF));
	/**
	 * For integral attributes, this is the maximum value it can hold.
	 */
	protected final static int ATTR_MAX = (0x01000000 | (2 & 0xFFFF));
	/**
	 * Localization of this resource is can be encouraged or required with an aapt
	 * flag if this is set
	 */
	protected final static int ATTR_L10N = (0x01000000 | (3 & 0xFFFF));

	// for plural support, see
	// android.content.res.PluralRules#attrForQuantity(int)
	protected final static int ATTR_OTHER = (0x01000000 | (4 & 0xFFFF));
	protected final static int ATTR_ZERO = (0x01000000 | (5 & 0xFFFF));
	protected final static int ATTR_ONE = (0x01000000 | (6 & 0xFFFF));
	protected final static int ATTR_TWO = (0x01000000 | (7 & 0xFFFF));
	protected final static int ATTR_FEW = (0x01000000 | (8 & 0xFFFF));
	protected final static int ATTR_MANY = (0x01000000 | (9 & 0xFFFF));

	protected final static int NO_ENTRY = 0xFFFFFFFF;

	/**
	 * Where the unit type information is. This gives us 16 possible types, as
	 * defined below.
	 */
	protected final static int COMPLEX_UNIT_SHIFT = 0x0;
	protected final static int COMPLEX_UNIT_MASK = 0xf;
	/**
	 * TYPE_DIMENSION: Value is raw pixels.
	 */
	protected final static int COMPLEX_UNIT_PX = 0;
	/**
	 * TYPE_DIMENSION: Value is Device Independent Pixels.
	 */
	protected final static int COMPLEX_UNIT_DIP = 1;
	/**
	 * TYPE_DIMENSION: Value is a Scaled device independent Pixels.
	 */
	protected final static int COMPLEX_UNIT_SP = 2;
	/**
	 * TYPE_DIMENSION: Value is in points.
	 */
	protected final static int COMPLEX_UNIT_PT = 3;
	/**
	 * TYPE_DIMENSION: Value is in inches.
	 */
	protected final static int COMPLEX_UNIT_IN = 4;
	/**
	 * TYPE_DIMENSION: Value is in millimeters.
	 */
	protected final static int COMPLEX_UNIT_MM = 5;
	/**
	 * TYPE_FRACTION: A basic fraction of the overall size.
	 */
	protected final static int COMPLEX_UNIT_FRACTION = 0;
	/**
	 * TYPE_FRACTION: A fraction of the parent size.
	 */
	protected final static int COMPLEX_UNIT_FRACTION_PARENT = 1;
	/**
	 * Where the radix information is, telling where the decimal place appears in
	 * the mantissa. This give us 4 possible fixed point representations as defined
	 * below.
	 */
	protected final static int COMPLEX_RADIX_SHIFT = 4;
	protected final static int COMPLEX_RADIX_MASK = 0x3;
	/**
	 * The mantissa is an integral number -- i.e., 0xnnnnnn.0
	 */
	protected final static int COMPLEX_RADIX_23p0 = 0;
	/**
	 * The mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
	 */
	protected final static int COMPLEX_RADIX_16p7 = 1;
	/**
	 * The mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
	 */
	protected final static int COMPLEX_RADIX_8p15 = 2;
	/**
	 * The mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
	 */
	protected final static int COMPLEX_RADIX_0p23 = 3;
	/**
	 * Where the actual value is. This gives us 23 bits of precision. The top bit is
	 * the sign.
	 */
	protected final static int COMPLEX_MANTISSA_SHIFT = 8;
	protected final static int COMPLEX_MANTISSA_MASK = 0xffffff;

	protected static final float MANTISSA_MULT = 1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
	protected static final float[] RADIX_MULTS = new float[] { 1.0f * MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT,
			1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT };

	/**
	 * If set, this is a complex entry, holding a set of name/value mappings. It is
	 * followed by an array of ResTable_Map structures.
	 */
	public final static int FLAG_COMPLEX = 0x0001;
	/**
	 * If set, this resource has been declared public, so libraries are allowed to
	 * reference it.
	 */
	public final static int FLAG_PUBLIC = 0x0002;

	private final Map<Integer, String> stringTable = new HashMap<Integer, String>();
	private final List<ResPackage> packages = new ArrayList<ResPackage>();

	public static class ResPackage {
		private int packageId;
		private String packageName;
		private List<ResType> types = new ArrayList<>();

		public int getPackageId() {
			return this.packageId;
		}

		public String getPackageName() {
			return this.packageName;
		}

		public List<ResType> getDeclaredTypes() {
			return this.types;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + packageId;
			result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
			result = prime * result + ((types == null) ? 0 : types.hashCode());
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
			ResPackage other = (ResPackage) obj;
			if (packageId != other.packageId)
				return false;
			if (packageName == null) {
				if (other.packageName != null)
					return false;
			} else if (!packageName.equals(other.packageName))
				return false;
			if (types == null) {
				if (other.types != null)
					return false;
			} else if (!types.equals(other.types))
				return false;
			return true;
		}

	}

	/**
	 * A resource type in an Android resource file. All resources are associated
	 * with a type.
	 */
	public static class ResType {
		private int id;
		private String typeName;
		private List<ResConfig> configurations = new ArrayList<ResConfig>();

		public String getTypeName() {
			return this.typeName;
		}

		public List<ResConfig> getConfigurations() {
			return this.configurations;
		}

		/**
		 * Gets a list of all resources in this type regardless of the configuration.
		 * Resources sharing the same ID will only be returned once, taking the value
		 * from the first applicable configuration.
		 * 
		 * @return A list of all resources of this type.
		 */
		public Collection<AbstractResource> getAllResources() {
			Map<String, AbstractResource> resources = new HashMap<String, AbstractResource>();
			for (ResConfig rc : this.configurations)
				for (AbstractResource res : rc.getResources())
					if (!resources.containsKey(res.resourceName))
						resources.put(res.resourceName, res);
			return resources.values();
		}

		/**
		 * Gets all resource of the current type that have the given id
		 * 
		 * @param resourceID
		 *            The resource id to look for
		 * @return A list containing all resources with the given id
		 */
		public List<AbstractResource> getAllResources(int resourceID) {
			List<AbstractResource> resourceList = new ArrayList<>();
			for (ResConfig rc : this.configurations)
				for (AbstractResource res : rc.getResources())
					if (res.resourceID == resourceID)
						resourceList.add(res);
			return resourceList;
		}

		/**
		 * Gets the first resource with the given name or null if no such resource
		 * exists
		 * 
		 * @param resourceName
		 *            The resource name to look for
		 * @return The first resource with the given name or null if no such resource
		 *         exists
		 */
		public AbstractResource getResourceByName(String resourceName) {
			for (ResConfig rc : this.configurations)
				for (AbstractResource res : rc.getResources())
					if (res.getResourceName().equals(resourceName))
						return res;
			return null;
		}

		/**
		 * Gets the first resource of the current type that has the given name
		 * 
		 * @param resourceName
		 *            The resource name to look for
		 * @return The resource with the given name if it exists, otherwise null
		 */
		public AbstractResource getFirstResource(String resourceName) {
			for (ResConfig rc : this.configurations)
				for (AbstractResource res : rc.getResources())
					if (res.resourceName.equals(resourceName))
						return res;
			return null;
		}

		/**
		 * Gets the first resource of the current type with the given ID
		 * 
		 * @param resourceID
		 *            The resource ID to look for
		 * @return The resource with the given ID if it exists, otherwise null
		 */
		public AbstractResource getFirstResource(int resourceID) {
			for (ResConfig rc : this.configurations)
				for (AbstractResource res : rc.getResources())
					if (res.resourceID == resourceID)
						return res;
			return null;
		}

		@Override
		public String toString() {
			return this.typeName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((configurations == null) ? 0 : configurations.hashCode());
			result = prime * result + id;
			result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
			ResType other = (ResType) obj;
			if (configurations == null) {
				if (other.configurations != null)
					return false;
			} else if (!configurations.equals(other.configurations))
				return false;
			if (id != other.id)
				return false;
			if (typeName == null) {
				if (other.typeName != null)
					return false;
			} else if (!typeName.equals(other.typeName))
				return false;
			return true;
		}
	}

	/**
	 * A configuration in an Android resource file. All resources are associated
	 * with a configuration (which may be the default one).
	 */
	public static class ResConfig {
		private ResTable_Config config;
		private List<AbstractResource> resources = new ArrayList<AbstractResource>();

		public ResTable_Config getConfig() {
			return config;
		}

		public List<AbstractResource> getResources() {
			return this.resources;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((config == null) ? 0 : config.hashCode());
			result = prime * result + ((resources == null) ? 0 : resources.hashCode());
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
			ResConfig other = (ResConfig) obj;
			if (config == null) {
				if (other.config != null)
					return false;
			} else if (!config.equals(other.config))
				return false;
			if (resources == null) {
				if (other.resources != null)
					return false;
			} else if (!resources.equals(other.resources))
				return false;
			return true;
		}
	}

	/**
	 * Abstract base class for all Android resources.
	 */
	public static abstract class AbstractResource {
		private String resourceName;
		private int resourceID;

		public String getResourceName() {
			return this.resourceName;
		}

		public int getResourceID() {
			return this.resourceID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + resourceID;
			result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
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
			AbstractResource other = (AbstractResource) obj;
			if (resourceID != other.resourceID)
				return false;
			if (resourceName == null) {
				if (other.resourceName != null)
					return false;
			} else if (!resourceName.equals(other.resourceName))
				return false;
			return true;
		}
	}

	/**
	 * Android resource that does not contain any data
	 */
	public static class NullResource extends AbstractResource {
	}

	/**
	 * Android resource containing a reference to another resource.
	 */
	public static class ReferenceResource extends AbstractResource {
		private int referenceID;

		public ReferenceResource(int id) {
			this.referenceID = id;
		}

		public int getReferenceID() {
			return this.referenceID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + referenceID;
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
			ReferenceResource other = (ReferenceResource) obj;
			if (referenceID != other.referenceID)
				return false;
			return true;
		}
	}

	/**
	 * Android resource containing an attribute resource identifier.
	 */
	public static class AttributeResource extends AbstractResource {
		private int attributeID;

		public AttributeResource(int id) {
			this.attributeID = id;
		}

		public int getAttributeID() {
			return this.attributeID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + attributeID;
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
			AttributeResource other = (AttributeResource) obj;
			if (attributeID != other.attributeID)
				return false;
			return true;
		}
	}

	/**
	 * Android resource containing string data.
	 */
	public static class StringResource extends AbstractResource {
		private String value;

		public StringResource(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			StringResource other = (StringResource) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}

	/**
	 * Android resource containing integer data.
	 */
	public static class IntegerResource extends AbstractResource {
		private int value;

		public IntegerResource(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + value;
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
			IntegerResource other = (IntegerResource) obj;
			if (value != other.value)
				return false;
			return true;
		}
	}

	/**
	 * Android resource containing a single-precision floating point number
	 */
	public static class FloatResource extends AbstractResource {
		private float value;

		public FloatResource(float value) {
			this.value = value;
		}

		public float getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return Float.toString(value);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Float.floatToIntBits(value);
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
			FloatResource other = (FloatResource) obj;
			if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
				return false;
			return true;
		}
	}

	/**
	 * Android resource containing boolean data.
	 */
	public static class BooleanResource extends AbstractResource {
		private boolean value;

		public BooleanResource(int value) {
			this.value = (value != 0);
		}

		public boolean getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return Boolean.toString(value);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (value ? 1231 : 1237);
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
			BooleanResource other = (BooleanResource) obj;
			if (value != other.value)
				return false;
			return true;
		}

	}

	/**
	 * Android resource containing color data.
	 */
	public static class ColorResource extends AbstractResource {
		private int a;
		private int r;
		private int g;
		private int b;

		public ColorResource(int a, int r, int g, int b) {
			this.a = a;
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public int getA() {
			return this.a;
		}

		public int getR() {
			return this.r;
		}

		public int getG() {
			return this.g;
		}

		public int getB() {
			return this.b;
		}

		@Override
		public String toString() {
			return String.format("#%02x%02x%02x%02x", a, r, g, b);
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
			ColorResource other = (ColorResource) obj;
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

	/**
	 * Special Android resource that contains an array of other resources
	 */
	public static class ArrayResource extends AbstractResource {

		private final List<AbstractResource> arrayElements;

		public ArrayResource() {
			this.arrayElements = new ArrayList<>();
		}

		public ArrayResource(List<AbstractResource> arrayElements) {
			this.arrayElements = arrayElements;
		}

		public void add(AbstractResource resource) {
			this.arrayElements.add(resource);
		}

		@Override
		public String toString() {
			return this.arrayElements.toString();
		}

		public List<AbstractResource> getArrayElements() {
			return Collections.unmodifiableList(arrayElements);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((arrayElements == null) ? 0 : arrayElements.hashCode());
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
			ArrayResource other = (ArrayResource) obj;
			if (arrayElements == null) {
				if (other.arrayElements != null)
					return false;
			} else if (!arrayElements.equals(other.arrayElements))
				return false;
			return true;
		}

	}

	/**
	 * Enumeration containing the types of fractions supported in Android
	 */
	public enum FractionType {
		/**
		 * A basic fraction of the overall size.
		 */
		Fraction,

		/**
		 * A fraction of the parent size.
		 */
		FractionParent
	}

	/**
	 * Android resource containing fraction data (e.g. element width relative to
	 * some other control).
	 */
	public static class FractionResource extends AbstractResource {
		private FractionType type;
		private float value;

		public FractionResource(FractionType type, float value) {
			this.type = type;
			this.value = value;
		}

		public FractionType getType() {
			return this.type;
		}

		public float getValue() {
			return this.value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + Float.floatToIntBits(value);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			FractionResource other = (FractionResource) obj;
			if (type != other.type)
				return false;
			if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
				return false;
			return true;
		}
	}

	/**
	 * Enumeration containing all dimension units available in Android
	 */
	public enum Dimension {
		PX, DIP, SP, PT, IN, MM
	}

	/**
	 * Android resource containing dimension data like "11pt".
	 */
	public static class DimensionResource extends AbstractResource {
		private int value;
		private Dimension unit;

		public DimensionResource(int value, Dimension unit) {
			this.value = value;
			this.unit = unit;
		}

		DimensionResource(int dimension, int value) {
			this.value = value;
			switch (dimension) {
			case COMPLEX_UNIT_PX:
				this.unit = Dimension.PX;
				break;
			case COMPLEX_UNIT_DIP:
				this.unit = Dimension.DIP;
				break;
			case COMPLEX_UNIT_SP:
				this.unit = Dimension.SP;
				break;
			case COMPLEX_UNIT_PT:
				this.unit = Dimension.PT;
				break;
			case COMPLEX_UNIT_IN:
				this.unit = Dimension.IN;
				break;
			case COMPLEX_UNIT_MM:
				this.unit = Dimension.MM;
				break;
			default:
				throw new RuntimeException("Invalid dimension: " + dimension);
			}
		}

		public int getValue() {
			return this.value;
		}

		public Dimension getUnit() {
			return this.unit;
		}

		@Override
		public String toString() {
			return Integer.toString(this.value) + unit.toString().toLowerCase();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((unit == null) ? 0 : unit.hashCode());
			result = prime * result + value;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			DimensionResource other = (DimensionResource) obj;
			if (unit != other.unit)
				return false;
			if (value != other.value)
				return false;
			return true;
		}
	}

	/**
	 * Android resource containing complex map data.
	 */
	public static class ComplexResource extends AbstractResource {
		private Map<String, AbstractResource> value;

		public ComplexResource() {
			this.value = new HashMap<String, AbstractResource>();
		}

		public ComplexResource(Map<String, AbstractResource> value) {
			this.value = value;
		}

		public Map<String, AbstractResource> getValue() {
			return this.value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			ComplexResource other = (ComplexResource) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}

	protected static class ResTable_Header {
		ResChunk_Header header = new ResChunk_Header();
		/**
		 * The number of ResTable_package structures
		 */
		int packageCount; // uint32

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((header == null) ? 0 : header.hashCode());
			result = prime * result + packageCount;
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
			ResTable_Header other = (ResTable_Header) obj;
			if (header == null) {
				if (other.header != null)
					return false;
			} else if (!header.equals(other.header))
				return false;
			if (packageCount != other.packageCount)
				return false;
			return true;
		}
	}

	/**
	 * Header that appears at the front of every data chunk in a resource
	 */
	protected static class ResChunk_Header {
		/**
		 * Type identifier of this chunk. The meaning of this value depends on the
		 * containing class.
		 */
		int type; // uint16
		/**
		 * Size of the chunk header (in bytes). Adding this value to the address of the
		 * chunk allows you to find the associated data (if any).
		 */
		int headerSize; // uint16
		/**
		 * Total size of this chunk (in bytes). This is the chunkSize plus the size of
		 * any data associated with the chunk. Adding this value to the chunk allows you
		 * to completely skip its contents. If this value is the same as chunkSize,
		 * there is no data associated with the chunk.
		 */
		int size; // uint32

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + headerSize;
			result = prime * result + size;
			result = prime * result + type;
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
			ResChunk_Header other = (ResChunk_Header) obj;
			if (headerSize != other.headerSize)
				return false;
			if (size != other.size)
				return false;
			if (type != other.type)
				return false;
			return true;
		}
	}

	protected static class ResStringPool_Header {
		ResChunk_Header header;

		/**
		 * Number of strings in this pool (number of uint32_t indices that follow in the
		 * data).
		 */
		int stringCount; // uint32
		/**
		 * Number of style span arrays in the pool (number of uint32_t indices follow
		 * the string indices).
		 */
		int styleCount; // uint32
		/**
		 * If set, the string index is sorted by the string values (based on
		 * strcmp16()).
		 */
		boolean flagsSorted; // 1<<0
		/**
		 * String pool is encoded in UTF-8.
		 */
		boolean flagsUTF8; // 1<<8
		/**
		 * Index from the header of the string data.
		 */
		int stringsStart; // uint32
		/**
		 * Index from the header of the style data.
		 */
		int stylesStart; // uint32

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (flagsSorted ? 1231 : 1237);
			result = prime * result + (flagsUTF8 ? 1231 : 1237);
			result = prime * result + ((header == null) ? 0 : header.hashCode());
			result = prime * result + stringCount;
			result = prime * result + stringsStart;
			result = prime * result + styleCount;
			result = prime * result + stylesStart;
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
			ResStringPool_Header other = (ResStringPool_Header) obj;
			if (flagsSorted != other.flagsSorted)
				return false;
			if (flagsUTF8 != other.flagsUTF8)
				return false;
			if (header == null) {
				if (other.header != null)
					return false;
			} else if (!header.equals(other.header))
				return false;
			if (stringCount != other.stringCount)
				return false;
			if (stringsStart != other.stringsStart)
				return false;
			if (styleCount != other.styleCount)
				return false;
			if (stylesStart != other.stylesStart)
				return false;
			return true;
		}
	}

	protected static class ResTable_Package {
		ResChunk_Header header;

		/**
		 * If this is the base package, its ID. Package IDs start at 1 (corresponding to
		 * the value of the package bits in a resource identifier). 0 means that this is
		 * not a base package.
		 */
		int id; // uint32
		/**
		 * Actual name of this package, \0-terminated
		 */
		String name; // char16
		/**
		 * Offset to a ResStringPool_Header defining the resource type symbol table. If
		 * zero, this package is inheriting from another base package (overriding
		 * specific values in it).
		 */
		int typeStrings; // uint32
		/**
		 * Last index into typeStrings that is for public use by others.
		 */
		int lastPublicType; // uint32
		/**
		 * Offset to a ResStringPool_Header defining the resource key symbol table. If
		 * zero, this package is inheriting from another base package (overriding
		 * specific values in it).
		 */
		int keyStrings; // uint32
		/**
		 * Last index into keyStrings that is for public use by others.
		 */
		int lastPublicKey; // uint32

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((header == null) ? 0 : header.hashCode());
			result = prime * result + id;
			result = prime * result + keyStrings;
			result = prime * result + lastPublicKey;
			result = prime * result + lastPublicType;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + typeStrings;
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
			ResTable_Package other = (ResTable_Package) obj;
			if (header == null) {
				if (other.header != null)
					return false;
			} else if (!header.equals(other.header))
				return false;
			if (id != other.id)
				return false;
			if (keyStrings != other.keyStrings)
				return false;
			if (lastPublicKey != other.lastPublicKey)
				return false;
			if (lastPublicType != other.lastPublicType)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (typeStrings != other.typeStrings)
				return false;
			return true;
		}
	}

	/**
	 * A specification of the resources defined by a particular type.
	 * 
	 * There should be one of these chunks for each resource type.
	 * 
	 * This structure is followed by an array of integers providing the set of
	 * configuration change flags (ResTable_Config::CONFIG_*) that have multiple
	 * resources for that configuration. In addition, the high bit is set if that
	 * resource has been made public.
	 */
	protected static class ResTable_TypeSpec {
		ResChunk_Header header;

		/**
		 * The type identifier this chunk is holding. Type IDs start at 1 (corresponding
		 * to the value of the type bits in a resource identifier). 0 is invalid.
		 */
		int id; // uint8
		/**
		 * Must be 0.
		 */
		int res0; // uint8
		/**
		 * Must be 1.
		 */
		int res1; // uint16
		/**
		 * Number of uint32_t entry configuration masks that follow.
		 */
		int entryCount; // uint32

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + entryCount;
			result = prime * result + ((header == null) ? 0 : header.hashCode());
			result = prime * result + id;
			result = prime * result + res0;
			result = prime * result + res1;
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
			ResTable_TypeSpec other = (ResTable_TypeSpec) obj;
			if (entryCount != other.entryCount)
				return false;
			if (header == null) {
				if (other.header != null)
					return false;
			} else if (!header.equals(other.header))
				return false;
			if (id != other.id)
				return false;
			if (res0 != other.res0)
				return false;
			if (res1 != other.res1)
				return false;
			return true;
		}
	}

	/**
	 * A collection of resource entries for a particular resource data type.
	 * Followed by an array of uint32_t defining the resource values, corresponding
	 * to the array of type strings in the ResTable_Package::typeStrings string
	 * block. Each of these hold an index from entriesStart; a value of NO_ENTRY
	 * means that entry is not defined.
	 * 
	 * There may be multiple of these chunks for a particular resource type, supply
	 * different configuration variations for the resource values of that type.
	 *
	 * It would be nice to have an additional ordered index of entries, so we can do
	 * a binary search if trying to find a resource by string name.
	 */
	protected static class ResTable_Type {
		ResChunk_Header header;

		/**
		 * The type identifier this chunk is holding. Type IDs start at 1 (corresponding
		 * to the value of the type bits in a resource identifier). 0 is invalid.
		 */
		int id; // uint8
		/**
		 * Must be 0.
		 */
		int res0; // uint8
		/**
		 * Must be 1.
		 */
		int res1; // uint16

		/**
		 * Number of uint32_t entry indices that follow.
		 */
		int entryCount; // uint32
		/**
		 * Offset from header where ResTable_Entry data starts.
		 */
		int entriesStart; // uint32
		/**
		 * Configuration this collection of entries is designed for,
		 */
		ResTable_Config config = new ResTable_Config();

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((config == null) ? 0 : config.hashCode());
			result = prime * result + entriesStart;
			result = prime * result + entryCount;
			result = prime * result + ((header == null) ? 0 : header.hashCode());
			result = prime * result + id;
			result = prime * result + res0;
			result = prime * result + res1;
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
			ResTable_Type other = (ResTable_Type) obj;
			if (config == null) {
				if (other.config != null)
					return false;
			} else if (!config.equals(other.config))
				return false;
			if (entriesStart != other.entriesStart)
				return false;
			if (entryCount != other.entryCount)
				return false;
			if (header == null) {
				if (other.header != null)
					return false;
			} else if (!header.equals(other.header))
				return false;
			if (id != other.id)
				return false;
			if (res0 != other.res0)
				return false;
			if (res1 != other.res1)
				return false;
			return true;
		}
	}

	/**
	 * Describes a particular resource configuration.
	 */
	public static class ResTable_Config {
		/**
		 * Number of bytes in this structure
		 */
		int size; // uint32
		/**
		 * Mobile country code (from SIM). "0" means any.
		 */
		int mmc; // uint16
		/**
		 * Mobile network code (from SIM). "0" means any.
		 */
		int mnc; // uint16
		/**
		 * \0\0 means "any". Otherwise, en, fr, etc.
		 */
		char[] language = new char[2]; // char[2]
		/**
		 * \0\0 means "any". Otherwise, US, CA, etc.
		 */
		char[] country = new char[2]; // char[2]

		int orientation; // uint8
		int touchscreen; // uint8
		int density; // uint16

		int keyboard; // uint8
		int navigation; // uint8
		int inputFlags; // uint8
		int inputPad0; // uint8

		int screenWidth; // uint16
		int screenHeight; // uint16

		int sdkVersion; // uint16
		int minorVersion; // uint16

		int screenLayout; // uint8
		int uiMode; // uint8
		int smallestScreenWidthDp; // uint16

		int screenWidthDp; // uint16
		int screenHeightDp; // uint16

		char[] localeScript = new char[4]; // char[4]
		char[] localeVariant = new char[8]; // char[8]

		public int getMmc() {
			return mmc;
		}

		public int getMnc() {
			return mnc;
		}

		public String getLanguage() {
			return new String(language);
		}

		public String getCountry() {
			return new String(country);
		}

		public int getOrientation() {
			return orientation;
		}

		public int getTouchscreen() {
			return touchscreen;
		}

		public int getDensity() {
			return density;
		}

		public int getKeyboard() {
			return keyboard;
		}

		public int getNavigation() {
			return navigation;
		}

		public int getInputFlags() {
			return inputFlags;
		}

		public int getInputPad0() {
			return inputPad0;
		}

		public int getScreenWidth() {
			return screenWidth;
		}

		public int getScreenHeight() {
			return screenHeight;
		}

		public int getSdkVersion() {
			return sdkVersion;
		}

		public int getMinorVersion() {
			return minorVersion;
		}

		public int getScreenLayout() {
			return screenLayout;
		}

		public int getUiMode() {
			return uiMode;
		}

		public int getSmallestScreenWidthDp() {
			return smallestScreenWidthDp;
		}

		public int getScreenWidthDp() {
			return screenWidthDp;
		}

		public int getScreenHeightDp() {
			return screenHeightDp;
		}

		public String getLocaleScript() {
			return new String(localeScript);
		}

		public String getLocaleVariant() {
			return new String(localeVariant);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(country);
			result = prime * result + density;
			result = prime * result + inputFlags;
			result = prime * result + inputPad0;
			result = prime * result + keyboard;
			result = prime * result + Arrays.hashCode(language);
			result = prime * result + Arrays.hashCode(localeScript);
			result = prime * result + Arrays.hashCode(localeVariant);
			result = prime * result + minorVersion;
			result = prime * result + mmc;
			result = prime * result + mnc;
			result = prime * result + navigation;
			result = prime * result + orientation;
			result = prime * result + screenHeight;
			result = prime * result + screenHeightDp;
			result = prime * result + screenLayout;
			result = prime * result + screenWidth;
			result = prime * result + screenWidthDp;
			result = prime * result + sdkVersion;
			result = prime * result + size;
			result = prime * result + smallestScreenWidthDp;
			result = prime * result + touchscreen;
			result = prime * result + uiMode;
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
			ResTable_Config other = (ResTable_Config) obj;
			if (!Arrays.equals(country, other.country))
				return false;
			if (density != other.density)
				return false;
			if (inputFlags != other.inputFlags)
				return false;
			if (inputPad0 != other.inputPad0)
				return false;
			if (keyboard != other.keyboard)
				return false;
			if (!Arrays.equals(language, other.language))
				return false;
			if (!Arrays.equals(localeScript, other.localeScript))
				return false;
			if (!Arrays.equals(localeVariant, other.localeVariant))
				return false;
			if (minorVersion != other.minorVersion)
				return false;
			if (mmc != other.mmc)
				return false;
			if (mnc != other.mnc)
				return false;
			if (navigation != other.navigation)
				return false;
			if (orientation != other.orientation)
				return false;
			if (screenHeight != other.screenHeight)
				return false;
			if (screenHeightDp != other.screenHeightDp)
				return false;
			if (screenLayout != other.screenLayout)
				return false;
			if (screenWidth != other.screenWidth)
				return false;
			if (screenWidthDp != other.screenWidthDp)
				return false;
			if (sdkVersion != other.sdkVersion)
				return false;
			if (size != other.size)
				return false;
			if (smallestScreenWidthDp != other.smallestScreenWidthDp)
				return false;
			if (touchscreen != other.touchscreen)
				return false;
			if (uiMode != other.uiMode)
				return false;
			return true;
		}

	}

	/**
	 * This is the beginning of information about an entry in the resource table. It
	 * holds the reference to the name of this entry, and is immediately followed by
	 * one of: * A Res_value structure, if FLAG_COMPLEX is -not- set * An array of
	 * ResTable_Map structures, if FLAG_COMPLEX is set. These supply a set of
	 * name/value mappings of data.
	 */
	protected static class ResTable_Entry {
		/**
		 * Number of bytes in this structure
		 */
		int size; // uint16
		boolean flagsComplex;
		boolean flagsPublic;
		/**
		 * Reference into ResTable_Package::KeyStrings identifying this entry.
		 */
		int key;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (flagsComplex ? 1231 : 1237);
			result = prime * result + (flagsPublic ? 1231 : 1237);
			result = prime * result + key;
			result = prime * result + size;
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
			ResTable_Entry other = (ResTable_Entry) obj;
			if (flagsComplex != other.flagsComplex)
				return false;
			if (flagsPublic != other.flagsPublic)
				return false;
			if (key != other.key)
				return false;
			if (size != other.size)
				return false;
			return true;
		}
	}

	/**
	 * Extended form of a ResTable_Entry for map entries, defining a parent map
	 * resource from which to inherit values.
	 */
	protected static class ResTable_Map_Entry extends ResTable_Entry {
		/**
		 * Resource identifier of the parent mapping, or 0 if there is none.
		 */
		int parent;
		/**
		 * Number of name/value pairs that follow for FLAG_COMPLEX.
		 */
		int count; // uint32

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + count;
			result = prime * result + parent;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResTable_Map_Entry other = (ResTable_Map_Entry) obj;
			if (count != other.count)
				return false;
			if (parent != other.parent)
				return false;
			return true;
		}
	}

	/**
	 * Representation of a value in a resource, supplying type information.
	 */
	protected static class Res_Value {
		/**
		 * Number of bytes in this structure.
		 */
		int size; // uint16

		/**
		 * Always set to 0.
		 */
		int res0; // uint8

		int dataType; // uint8
		/**
		 * The data for this type, as interpreted according to dataType.
		 */
		int data; // uint16

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + data;
			result = prime * result + dataType;
			result = prime * result + res0;
			result = prime * result + size;
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
			Res_Value other = (Res_Value) obj;
			if (data != other.data)
				return false;
			if (dataType != other.dataType)
				return false;
			if (res0 != other.res0)
				return false;
			if (size != other.size)
				return false;
			return true;
		}
	}

	/**
	 * A single name/value mapping that is part of a complex resource entry.
	 */
	protected static class ResTable_Map {
		/**
		 * The resource identifier defining this mapping's name. For attribute
		 * resources, 'name' can be one of the following special resource types to
		 * supply meta-data about the attribute; for all other resource types it must be
		 * an attribute resource.
		 */
		int name; // uint32

		/**
		 * This mapping's value.
		 */
		Res_Value value = new Res_Value();

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + name;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			ResTable_Map other = (ResTable_Map) obj;
			if (name != other.name)
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}

	/**
	 * Class containing the data encoded in an Android resource ID
	 */
	public static class ResourceId {
		private int packageId;
		private int typeId;
		private int itemIndex;

		public ResourceId(int packageId, int typeId, int itemIndex) {
			this.packageId = packageId;
			this.typeId = typeId;
			this.itemIndex = itemIndex;
		}

		public int getPackageId() {
			return this.packageId;
		}

		public int getTypeId() {
			return this.typeId;
		}

		public int getItemIndex() {
			return this.itemIndex;
		}

		@Override
		public String toString() {
			return "Package " + this.packageId + ", type " + this.typeId + ", item " + this.itemIndex;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + itemIndex;
			result = prime * result + packageId;
			result = prime * result + typeId;
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
			ResourceId other = (ResourceId) obj;
			if (itemIndex != other.itemIndex)
				return false;
			if (packageId != other.packageId)
				return false;
			if (typeId != other.typeId)
				return false;
			return true;
		}
	}

	public ARSCFileParser() {
	}

	/**
	 * Parses the resource definition file in the given APK
	 * 
	 * @param apkFile
	 *            The APK file in which to parse the resource definition file
	 * @throws IOException
	 *             Thrown if the given APK file cannot be opened
	 */
	public void parse(String apkFile) throws IOException {
		this.handleAndroidResourceFiles(apkFile, null, new IResourceHandler() {

			@Override
			public void handleResourceFile(String fileName, Set<String> fileNameFilter, InputStream stream) {
				try {
					if (fileName.equals("resources.arsc"))
						parse(stream);
				} catch (IOException ex) {
					logger.error("Could not read resource file", ex);
				}
			}

		});
	}

	public void parse(InputStream stream) throws IOException {
		readResourceHeader(stream);
	}

	private void readResourceHeader(InputStream stream) throws IOException {
		final int BLOCK_SIZE = 2048;

		ResTable_Header resourceHeader = new ResTable_Header();
		readChunkHeader(stream, resourceHeader.header);
		resourceHeader.packageCount = readUInt32(stream);
		logger.debug(String.format("Package Groups (%d)", resourceHeader.packageCount));

		// Do we have any packages to read?
		int remainingSize = resourceHeader.header.size - resourceHeader.header.headerSize;
		if (remainingSize <= 0)
			return;

		// Load the remaining data
		byte[] remainingData = new byte[remainingSize];
		int totalBytesRead = 0;
		while (totalBytesRead < remainingSize) {
			byte[] block = new byte[Math.min(BLOCK_SIZE, remainingSize - totalBytesRead)];
			int bytesRead = stream.read(block);
			if (bytesRead < 0) {
				logger.error("Could not read block from resource file");
				return;
			}
			System.arraycopy(block, 0, remainingData, totalBytesRead, bytesRead);
			totalBytesRead += bytesRead;
		}
		int offset = 0;
		int beforeBlock = 0;

		// Read the next chunk
		int packageCtr = 0;
		Map<Integer, String> keyStrings = new HashMap<Integer, String>();
		Map<Integer, String> typeStrings = new HashMap<Integer, String>();
		while (offset < remainingData.length - 1) {
			beforeBlock = offset;
			ResChunk_Header nextChunkHeader = new ResChunk_Header();
			offset = readChunkHeader(nextChunkHeader, remainingData, offset);
			if (nextChunkHeader.type == RES_STRING_POOL_TYPE) {
				// Read the string pool header
				ResStringPool_Header stringPoolHeader = new ResStringPool_Header();
				stringPoolHeader.header = nextChunkHeader;
				offset = parseStringPoolHeader(stringPoolHeader, remainingData, offset);

				// Read the string data
				offset = readStringTable(remainingData, offset, beforeBlock, stringPoolHeader, this.stringTable);
				assert this.stringTable.size() == stringPoolHeader.stringCount;
			} else if (nextChunkHeader.type == RES_TABLE_PACKAGE_TYPE) {
				// Read the package header
				ResTable_Package packageTable = new ResTable_Package();
				packageTable.header = nextChunkHeader;
				offset = parsePackageTable(packageTable, remainingData, offset);

				logger.debug(
						String.format("\tPackage %s id=%d name=%s", packageCtr, packageTable.id, packageTable.name));

				// Record the end of the object to know then to stop looking for
				// internal records
				int endOfRecord = beforeBlock + nextChunkHeader.size;

				// Create the data object and set the base data
				ResPackage resPackage = new ResPackage();
				this.packages.add(resPackage);
				resPackage.packageId = packageTable.id;
				resPackage.packageName = packageTable.name;

				{
					// Find the type strings
					int typeStringsOffset = beforeBlock + packageTable.typeStrings;
					int beforeStringBlock = typeStringsOffset;
					ResChunk_Header typePoolHeader = new ResChunk_Header();
					typeStringsOffset = readChunkHeader(typePoolHeader, remainingData, typeStringsOffset);
					if (typePoolHeader.type != RES_STRING_POOL_TYPE)
						throw new RuntimeException("Unexpected block type for package type strings");

					ResStringPool_Header typePool = new ResStringPool_Header();
					typePool.header = typePoolHeader;
					typeStringsOffset = parseStringPoolHeader(typePool, remainingData, typeStringsOffset);

					// Attention: String offset starts at the beginning of the
					// StringPool
					// block, not the at the beginning of the Package block
					// referring to it.
					readStringTable(remainingData, typeStringsOffset, beforeStringBlock, typePool, typeStrings);

					// Find the key strings
					int keyStringsOffset = beforeBlock + packageTable.keyStrings;
					beforeStringBlock = keyStringsOffset;
					ResChunk_Header keyPoolHeader = new ResChunk_Header();
					keyStringsOffset = readChunkHeader(keyPoolHeader, remainingData, keyStringsOffset);
					if (keyPoolHeader.type != RES_STRING_POOL_TYPE)
						throw new RuntimeException("Unexpected block type for package key strings");

					ResStringPool_Header keyPool = new ResStringPool_Header();
					keyPool.header = keyPoolHeader;
					keyStringsOffset = parseStringPoolHeader(keyPool, remainingData, keyStringsOffset);

					// Attention: String offset starts at the beginning of the
					// StringPool
					// block, not the at the beginning of the Package block
					// referring to it.
					readStringTable(remainingData, keyStringsOffset, beforeStringBlock, keyPool, keyStrings);

					// Jump to the end of the string block
					offset = beforeStringBlock + keyPoolHeader.size;
				}

				while (offset < endOfRecord) {
					// Read the next inner block
					ResChunk_Header innerHeader = new ResChunk_Header();
					int beforeInnerBlock = offset;
					offset = readChunkHeader(innerHeader, remainingData, offset);
					if (innerHeader.type == RES_TABLE_TYPE_SPEC_TYPE) {
						// Type specification block
						ResTable_TypeSpec typeSpecTable = new ResTable_TypeSpec();
						typeSpecTable.header = innerHeader;
						offset = readTypeSpecTable(typeSpecTable, remainingData, offset);
						assert offset == beforeInnerBlock + typeSpecTable.header.headerSize;

						// Create the data object
						ResType tp = new ResType();
						tp.id = typeSpecTable.id;
						tp.typeName = typeStrings.get(typeSpecTable.id - 1);
						resPackage.types.add(tp);

						// Normally, we also have a set of configurations
						// following, but
						// we don't implement that at the moment
					} else if (innerHeader.type == RES_TABLE_TYPE_TYPE) {
						// Type resource entries. The id field maps to the type
						// for which we have a record. We create a mapping from
						// type IDs to declare resources.
						ResTable_Type typeTable = new ResTable_Type();
						typeTable.header = innerHeader;
						offset = readTypeTable(typeTable, remainingData, offset);
						assert offset == beforeInnerBlock + typeTable.header.headerSize;

						// Create the data object
						ResType resType = null;
						for (ResType rt : resPackage.types)
							if (rt.id == typeTable.id) {
								resType = rt;
								break;
							}
						if (resType == null)
							throw new RuntimeException("Reference to undeclared type found");
						ResConfig config = new ResConfig();
						config.config = typeTable.config;
						resType.configurations.add(config);

						// Read the table entries
						int resourceIdx = 0;
						for (int i = 0; i < typeTable.entryCount; i++) {
							int entryOffset = readUInt32(remainingData, offset);
							offset += 4;
							if (entryOffset == 0xFFFFFFFF) { // NoEntry
								resourceIdx++;
								continue;
							}
							entryOffset += beforeInnerBlock + typeTable.entriesStart;
							ResTable_Entry entry = readEntryTable(remainingData, entryOffset);
							entryOffset += entry.size;

							AbstractResource res;

							// If this is a simple entry, the data structure is
							// followed by RES_VALUE
							if (entry.flagsComplex) {
								ComplexResource cmpRes = new ComplexResource();
								res = cmpRes;

								for (int j = 0; j < ((ResTable_Map_Entry) entry).count; j++) {
									ResTable_Map map = new ResTable_Map();
									entryOffset = readComplexValue(map, remainingData, entryOffset);

									final String mapName = map.name + "";
									AbstractResource value = parseValue(map.value);

									// If we are dealing with an array, we put it into a special array container
									if (resType.typeName != null && resType.typeName.equals("array")
											&& value instanceof StringResource) {

										AbstractResource existingResource = cmpRes.value.get(mapName);
										if (existingResource == null) {
											existingResource = new ArrayResource();
											cmpRes.value.put(mapName, existingResource);
										}

										// We silently ignore inconsistencies at thze moment
										if (existingResource instanceof ArrayResource)
											((ArrayResource) existingResource).add(value);
									} else
										cmpRes.value.put(mapName, value);
								}
							} else {
								Res_Value val = new Res_Value();
								entryOffset = readValue(val, remainingData, entryOffset);
								res = parseValue(val);
								if (res == null) {
									logger.error(String.format("Could not parse resource %s of type %s, skipping entry",
											keyStrings.get(entry.key), Integer.toHexString(val.dataType)));
									continue;
								}
							}

							// Create the data object. For finding the correct
							// ID, we
							// must check whether the entry is really new - if
							// so, it
							// gets a new ID, otherwise, we reuse the old one
							if (keyStrings.containsKey(entry.key))
								res.resourceName = keyStrings.get(entry.key);
							else
								res.resourceName = "<INVALID RESOURCE>";

							if (res.resourceName != null && res.resourceName.length() > 0) {
								// Some obfuscated resources do only contain an
								// empty string as resource name
								// -> We only need to check the name if it is
								// really present
								AbstractResource r = resType.getResourceByName(res.resourceName);
								if (r != null) {
									res.resourceID = r.resourceID;
								}
							}
							if (res.resourceID <= 0) {
								res.resourceID = (packageTable.id << 24) + (typeTable.id << 16) + resourceIdx;
							}
							config.resources.add(res);
							resourceIdx++;
						}
					}
					offset = beforeInnerBlock + innerHeader.size;
				}

				// Create the data objects for the types in the package
				if (logger.isDebugEnabled()) {
					for (ResType resType : resPackage.types) {
						logger.debug(String.format("\t\tType %s (%d), configCount=%d, entryCount=%d", resType.typeName,
								resType.id - 1, resType.configurations.size(),
								resType.configurations.size() > 0 ? resType.configurations.get(0).resources.size()
										: 0));
						for (ResConfig resConfig : resType.configurations) {
							logger.debug("\t\t\tconfig");
							for (AbstractResource res : resConfig.resources)
								logger.debug(String.format("\t\t\t\tresource %s: %s",
										Integer.toHexString(res.resourceID), res.resourceName));
						}
					}
				}
				packageCtr++;
			}

			// Skip the block
			offset = beforeBlock + nextChunkHeader.size;
			remainingSize -= nextChunkHeader.size;
		}
	}

	/**
	 * Checks whether the given complex map entry is one of the well-known
	 * attributes.
	 * 
	 * @param map
	 *            The map entry to check
	 * @return True if the given entry is one of the well-known attributes,
	 *         otherwise false.
	 */
	protected boolean isAttribute(ResTable_Map map) {
		return map.name == ATTR_TYPE || map.name == ATTR_MIN || map.name == ATTR_MAX || map.name == ATTR_L10N
				|| map.name == ATTR_OTHER || map.name == ATTR_ZERO || map.name == ATTR_ONE || map.name == ATTR_TWO
				|| map.name == ATTR_FEW || map.name == ATTR_MANY;
	}

	/**
	 * Taken from
	 * https://github.com/menethil/ApkTool/blob/master/src/android/util/TypedValue.java
	 * 
	 * @param complex
	 * @return
	 */
	protected static float complexToFloat(int complex) {
		return (complex & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
				* RADIX_MULTS[(complex >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK];
	}

	private AbstractResource parseValue(Res_Value val) {
		AbstractResource res;
		switch (val.dataType) {
		case TYPE_NULL:
			res = new NullResource();
			break;
		case TYPE_REFERENCE:
			res = new ReferenceResource(val.data);
			break;
		case TYPE_ATTRIBUTE:
			res = new AttributeResource(val.data);
			break;
		case TYPE_STRING:
			res = new StringResource(stringTable.get(val.data));
			break;
		case TYPE_INT_DEC:
		case TYPE_INT_HEX:
			res = new IntegerResource(val.data);
			break;
		case TYPE_INT_BOOLEAN:
			res = new BooleanResource(val.data);
			break;
		case TYPE_INT_COLOR_ARGB8:
		case TYPE_INT_COLOR_RGB8:
		case TYPE_INT_COLOR_ARGB4:
		case TYPE_INT_COLOR_RGB4:
			res = new ColorResource(val.data & 0xFF000000 >> 3 * 8, val.data & 0x00FF0000 >> 2 * 8,
					val.data & 0x0000FF00 >> 8, val.data & 0x000000FF);
			break;
		case TYPE_DIMENSION:
			res = new DimensionResource(val.data & COMPLEX_UNIT_MASK, val.data >> COMPLEX_UNIT_SHIFT);
			break;
		case TYPE_FLOAT:
			res = new FloatResource(Float.intBitsToFloat(val.data));
			break;
		case TYPE_FRACTION:
			int fracType = (val.data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK;
			float data = complexToFloat(val.data);
			if (fracType == COMPLEX_UNIT_FRACTION)
				res = new FractionResource(FractionType.Fraction, data);
			else
				res = new FractionResource(FractionType.FractionParent, data);
			break;
		default:
			return null;
		}
		return res;
	}

	private int readComplexValue(ResTable_Map map, byte[] remainingData, int offset) throws IOException {
		map.name = readUInt32(remainingData, offset);
		offset += 4;

		return readValue(map.value, remainingData, offset);
	}

	private int readValue(Res_Value val, byte[] remainingData, int offset) throws IOException {
		int initialOffset = offset;

		val.size = readUInt16(remainingData, offset);
		offset += 2;
		if (val.size > 8) // This should always be 8. Check to not fail on
							// broken resources in apps
			return 0;

		val.res0 = readUInt8(remainingData, offset);
		if (val.res0 != 0)
			throw new RuntimeException("File format error, res0 was not zero");
		offset += 1;

		val.dataType = readUInt8(remainingData, offset);
		offset += 1;

		val.data = readUInt32(remainingData, offset);
		offset += 4;

		assert offset == initialOffset + val.size;
		return offset;
	}

	private ResTable_Entry readEntryTable(byte[] data, int offset) throws IOException {
		// The exact type of entry depends on the size
		int size = readUInt16(data, offset);
		offset += 2;
		ResTable_Entry entry;
		if (size == 0x8)
			entry = new ResTable_Entry();
		else if (size == 0x10)
			entry = new ResTable_Map_Entry();
		else
			throw new RuntimeException("Unknown entry type");
		entry.size = size;

		int flags = readUInt16(data, offset);
		offset += 2;
		entry.flagsComplex = (flags & FLAG_COMPLEX) == FLAG_COMPLEX;
		entry.flagsPublic = (flags & FLAG_PUBLIC) == FLAG_PUBLIC;

		entry.key = readUInt32(data, offset);
		offset += 4;

		if (entry instanceof ResTable_Map_Entry) {
			ResTable_Map_Entry mapEntry = (ResTable_Map_Entry) entry;
			mapEntry.parent = readUInt32(data, offset);
			offset += 4;
			mapEntry.count = readUInt32(data, offset);
			offset += 4;
		}

		return entry;
	}

	private int readTypeTable(ResTable_Type typeTable, byte[] data, int offset) throws IOException {
		typeTable.id = readUInt8(data, offset);
		offset += 1;

		typeTable.res0 = readUInt8(data, offset);
		if (typeTable.res0 != 0)
			throw new RuntimeException("File format error, res0 was not zero");
		offset += 1;

		typeTable.res1 = readUInt16(data, offset);
		if (typeTable.res1 != 0)
			throw new RuntimeException("File format error, res1 was not zero");
		offset += 2;

		typeTable.entryCount = readUInt32(data, offset);
		offset += 4;

		typeTable.entriesStart = readUInt32(data, offset);
		offset += 4;

		return readConfigTable(typeTable.config, data, offset);
	}

	private int readConfigTable(ResTable_Config config, byte[] data, int offset) throws IOException {
		config.size = readUInt32(data, offset);
		offset += 4;

		config.mmc = readUInt16(data, offset);
		offset += 2;

		config.mnc = readUInt16(data, offset);
		offset += 2;

		config.language[0] = (char) data[offset];
		config.language[1] = (char) data[offset + 1];
		offset += 2;

		config.country[0] = (char) data[offset];
		config.country[1] = (char) data[offset + 1];
		offset += 2;

		config.orientation = readUInt8(data, offset);
		offset += 1;
		config.touchscreen = readUInt8(data, offset);
		offset += 1;
		config.density = readUInt16(data, offset);
		offset += 2;

		config.keyboard = readUInt8(data, offset);
		offset += 1;
		config.navigation = readUInt8(data, offset);
		offset += 1;
		config.inputFlags = readUInt8(data, offset);
		offset += 1;
		config.inputPad0 = readUInt8(data, offset);
		offset += 1;

		config.screenWidth = readUInt16(data, offset);
		offset += 2;
		config.screenHeight = readUInt16(data, offset);
		offset += 2;

		config.sdkVersion = readUInt16(data, offset);
		offset += 2;
		config.minorVersion = readUInt16(data, offset);
		offset += 2;
		if (config.size <= 28)
			return offset;

		config.screenLayout = readUInt8(data, offset);
		offset += 1;
		config.uiMode = readUInt8(data, offset);
		offset += 1;
		config.smallestScreenWidthDp = readUInt16(data, offset);
		offset += 2;
		if (config.size <= 32)
			return offset;

		config.screenWidthDp = readUInt16(data, offset);
		offset += 2;
		config.screenHeightDp = readUInt16(data, offset);
		offset += 2;
		if (config.size <= 36)
			return offset;

		for (int i = 0; i < 4; i++)
			config.localeScript[i] = (char) data[offset + i];
		offset += 4;
		if (config.size <= 40)
			return offset;

		for (int i = 0; i < 8; i++)
			config.localeVariant[i] = (char) data[offset + i];
		offset += 8;
		if (config.size <= 48)
			return offset;

		// Read in the remaining bytes. If they're all zero, we're fine.
		// Otherwise, we print a warning.
		int remainingSize = config.size - 48;
		if (remainingSize > 0) {
			byte[] remainingBytes = new byte[remainingSize];
			System.arraycopy(data, offset, remainingBytes, 0, remainingSize);
			if (!(new BigInteger(1, remainingBytes).equals(BigInteger.ZERO))) {
				logger.warn("Excessive non-null bytes in ResTable_Config ignored");
				assert false;
			}
			offset += remainingSize;
		}

		return offset;
	}

	private int readTypeSpecTable(ResTable_TypeSpec typeSpecTable, byte[] data, int offset) throws IOException {
		typeSpecTable.id = readUInt8(data, offset);
		offset += 1;

		typeSpecTable.res0 = readUInt8(data, offset);
		offset += 1;
		if (typeSpecTable.res0 != 0)
			throw new RuntimeException("File format violation, res0 was not zero");

		typeSpecTable.res1 = readUInt16(data, offset);
		offset += 2;
		if (typeSpecTable.res1 != 0)
			throw new RuntimeException("File format violation, res1 was not zero");

		typeSpecTable.entryCount = readUInt32(data, offset);
		offset += 4;

		return offset;
	}

	private int readStringTable(byte[] remainingData, int offset, int blockStart, ResStringPool_Header stringPoolHeader,
			Map<Integer, String> stringList) throws IOException {
		// Read the strings
		for (int i = 0; i < stringPoolHeader.stringCount; i++) {
			int stringIdx = readUInt32(remainingData, offset);
			offset += 4;

			// Offset begins at block start
			stringIdx += stringPoolHeader.stringsStart + blockStart;
			String str = "";
			if (stringPoolHeader.flagsUTF8)
				str = readStringUTF8(remainingData, stringIdx).trim();
			else
				str = readString(remainingData, stringIdx).trim();
			stringList.put(i, str);
		}
		return offset;
	}

	private int parsePackageTable(ResTable_Package packageTable, byte[] data, int offset) throws IOException {
		packageTable.id = readUInt32(data, offset);
		offset += 4;

		// Read the package name, zero-terminated string
		StringBuilder bld = new StringBuilder();
		for (int i = 0; i < 128; i++) {
			int curChar = readUInt16(data, offset);
			bld.append((char) curChar);
			offset += 2;
		}
		packageTable.name = bld.toString().trim();

		packageTable.typeStrings = readUInt32(data, offset);
		offset += 4;

		packageTable.lastPublicType = readUInt32(data, offset);
		offset += 4;

		packageTable.keyStrings = readUInt32(data, offset);
		offset += 4;

		packageTable.lastPublicKey = readUInt32(data, offset);
		offset += 4;

		return offset;
	}

	private String readString(byte[] remainingData, int stringIdx) throws IOException {
		int strLen = readUInt16(remainingData, stringIdx);
		if (strLen == 0)
			return "";
		stringIdx += 2;
		byte[] str = new byte[strLen * 2];
		System.arraycopy(remainingData, stringIdx, str, 0, strLen * 2);
		return new String(remainingData, stringIdx, strLen * 2, "UTF-16LE");
	}

	private String readStringUTF8(byte[] remainingData, int stringIdx) throws IOException {
		// skip the length, will usually be 0x1A1A
		// int strLen = readUInt16(remainingData, stringIdx);
		// the length here is somehow weird
		int strLen = readUInt8(remainingData, stringIdx + 1);
		stringIdx += 2;
		String str = new String(remainingData, stringIdx, strLen, "UTF-8");
		return str;
	}

	private int parseStringPoolHeader(ResStringPool_Header stringPoolHeader, byte[] data, int offset)
			throws IOException {
		stringPoolHeader.stringCount = readUInt32(data, offset);
		stringPoolHeader.styleCount = readUInt32(data, offset + 4);

		int flags = readUInt32(data, offset + 8);
		stringPoolHeader.flagsSorted = (flags & SORTED_FLAG) == SORTED_FLAG;
		stringPoolHeader.flagsUTF8 = (flags & UTF8_FLAG) == UTF8_FLAG;

		stringPoolHeader.stringsStart = readUInt32(data, offset + 12);
		stringPoolHeader.stylesStart = readUInt32(data, offset + 16);
		return offset + 20;
	}

	/**
	 * Reads a chunk header from the input stream and stores the data in the given
	 * object.
	 * 
	 * @param stream
	 *            The stream from which to read the chunk header
	 * @param nextChunkHeader
	 *            The data object in which to put the chunk header
	 * @throws IOException
	 *             Thrown if an error occurs during read
	 */
	private void readChunkHeader(InputStream stream, ResChunk_Header nextChunkHeader) throws IOException {
		byte[] header = new byte[8];
		stream.read(header);
		readChunkHeader(nextChunkHeader, header, 0);
	}

	/**
	 * Reads a chunk header from the input stream and stores the data in the given
	 * object.
	 * 
	 * @param nextChunkHeader
	 *            The data object in which to put the chunk header
	 * @param data
	 *            The data array containing the structure
	 * @param offset
	 *            The offset from which to start reading
	 * @throws IOException
	 *             Thrown if an error occurs during read
	 */
	private int readChunkHeader(ResChunk_Header nextChunkHeader, byte[] data, int offset) throws IOException {
		nextChunkHeader.type = readUInt16(data, offset);
		offset += 2;

		nextChunkHeader.headerSize = readUInt16(data, offset);
		offset += 2;

		nextChunkHeader.size = readUInt32(data, offset);
		offset += 4;

		return offset;
	}

	private int readUInt8(byte[] uint16, int offset) throws IOException {
		int b0 = uint16[0 + offset] & 0x000000FF;
		return b0;
	}

	private int readUInt16(byte[] uint16, int offset) throws IOException {
		int b0 = uint16[0 + offset] & 0x000000FF;
		int b1 = uint16[1 + offset] & 0x000000FF;
		return (b1 << 8) + b0;
	}

	private int readUInt32(InputStream stream) throws IOException {
		byte[] uint32 = new byte[4];
		stream.read(uint32);
		return readUInt32(uint32, 0);
	}

	private int readUInt32(byte[] uint32, int offset) throws IOException {
		int b0 = uint32[0 + offset] & 0x000000FF;
		int b1 = uint32[1 + offset] & 0x000000FF;
		int b2 = uint32[2 + offset] & 0x000000FF;
		int b3 = uint32[3 + offset] & 0x000000FF;
		return (Math.abs(b3) << 24) + (Math.abs(b2) << 16) + (Math.abs(b1) << 8) + Math.abs(b0);
	}

	public Map<Integer, String> getGlobalStringPool() {
		return this.stringTable;
	}

	public List<ResPackage> getPackages() {
		return this.packages;
	}

	/**
	 * Finds the resource with the given Android resource ID. This method is
	 * configuration-agnostic and simply returns the first match it finds.
	 * 
	 * @param resourceId
	 *            The Android resource ID for which to the find the resource object
	 * @return The resource object with the given Android resource ID if it has been
	 *         found, otherwise null.
	 */
	public AbstractResource findResource(int resourceId) {
		ResourceId id = parseResourceId(resourceId);
		for (ResPackage resPackage : this.packages)
			if (resPackage.packageId == id.packageId) {
				for (ResType resType : resPackage.types)
					if (resType.id == id.typeId) {
						return resType.getFirstResource(resourceId);
					}
				break;
			}
		return null;
	}

	/**
	 * Finds all resources with the given Android resource ID. This method returns
	 * all matching resources, regardless of their respective configuration.
	 * 
	 * @param resourceId
	 *            The Android resource ID for which to the find the resource object
	 * @return The resource object with the given Android resource ID if it has been
	 *         found, otherwise null.
	 */
	public List<AbstractResource> findAllResources(int resourceId) {
		List<AbstractResource> resourceList = new ArrayList<>();
		ResourceId id = parseResourceId(resourceId);
		for (ResPackage resPackage : this.packages)
			if (resPackage.packageId == id.packageId) {
				for (ResType resType : resPackage.types)
					if (resType.id == id.typeId) {
						resourceList.addAll(resType.getAllResources(resourceId));
					}
				break;
			}
		return resourceList;
	}

	public ResType findResourceType(int resourceId) {
		ResourceId id = parseResourceId(resourceId);
		for (ResPackage resPackage : this.packages)
			if (resPackage.packageId == id.packageId) {
				for (ResType resType : resPackage.types)
					if (resType.id == id.typeId) {
						return resType;
					}
				break;
			}
		return null;
	}

	/**
	 * Parses an Android resource ID into its components
	 * 
	 * @param resourceId
	 *            The numeric resource ID to parse
	 * @return The data contained in the given Android resource ID
	 */
	public ResourceId parseResourceId(int resourceId) {
		return new ResourceId((resourceId & 0xFF000000) >> 24, (resourceId & 0x00FF0000) >> 16,
				resourceId & 0x0000FFFF);
	}

}