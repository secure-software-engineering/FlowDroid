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
package soot.jimple.infoflow.rifl;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a full RIFL specification document
 * 
 * @author Steven Arzt
 */
public class RIFLDocument {

	private InterfaceSpec interfaceSpec = new InterfaceSpec();
	private List<DomainSpec> domains = new ArrayList<DomainSpec>();
	private List<DomainAssignment> domainAssignment = new ArrayList<DomainAssignment>();
	private List<FlowPair> flowPolicy = new ArrayList<FlowPair>();
	
	public class Assignable {
		private final String handle;
		private final SourceSinkSpec element;
		
		/**
		 * Creates a new instance of the Assignable class
		 * @param handle The handle by which this interface can be referenced
		 * @param element The element to which the handle refers
		 */
		public Assignable(String handle, SourceSinkSpec element) {
			this.handle = handle;
			this.element = element;
		}
		
		/**
		 * Gets the handle by which this interface can be referenced
		 * @return The handle by which this interface can be referenced
		 */
		public String getHandle() {
			return this.handle;
		}
		
		/**
		 * Gets the element to which this assignable refers
		 * @return The element to which this assignable refers
		 */
		public SourceSinkSpec getElement() {
			return this.element;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((handle == null) ? 0 : handle.hashCode());
			result = prime * result
					+ ((element == null) ? 0 : element.hashCode());
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
			Assignable other = (Assignable) obj;
			if (handle == null) {
				if (other.handle != null)
					return false;
			} else if (!handle.equals(other.handle))
				return false;
			if (element == null) {
				if (other.element != null)
					return false;
			} else if (!element.equals(other.element))
				return false;
			return true;
		}
	}
	
	/**
	 * The interface specification in a RIFL document (the IO channels
	 * through which an attacker can communicate with an application)
	 */
	public class InterfaceSpec {
		private final List<Assignable> sourcesSinks = new ArrayList<Assignable>();
		
		/**
		 * Gets the list of sources and sinks defined in this interface
		 * specification
		 * @return The list of sources and sinks defined in this interface
		 * specification
		 */
		public List<Assignable> getSourcesSinks() {
			return this.sourcesSinks;
		}
		
		/**
		 * Gets the assignable with the given handle
		 * @param handle The handle of the assignable to retrieve
		 * @return The assignable with the given handle if it has been found,
		 * otherwise false
		 */
		public Assignable getElementByHandle(String handle) {
			for (Assignable assign : this.sourcesSinks)
				if (assign.getHandle().equals(handle))
					return assign;
			return null;
		}
		
		@Override
		public int hashCode() {
			return 31 * this.sourcesSinks.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof InterfaceSpec))
				return false;
			InterfaceSpec otherIO = (InterfaceSpec) other;
			return this.sourcesSinks.equals(otherIO.sourcesSinks);
		}
	}
	
	/**
	 * An enumeration specifying whether an assignable is a source or a sink
	 */
	public enum SourceSinkType {
		Category,
		Source,
		Sink
	}
	
	/**
	 * Abstract base class for all source and sink specifications in RIFL
	 */
	public abstract class SourceSinkSpec {
		protected final SourceSinkType type;
		
		public SourceSinkSpec(SourceSinkType type) {
			this.type = type;
		}
		
		/**
		 * Gets the type of this specification, i.e., whether it's a source or
		 * a sink
		 * @return The type of this specification
		 */
		public SourceSinkType getType() {
			return this.type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			SourceSinkSpec other = (SourceSinkSpec) obj;
			if (type != other.type)
				return false;
			return true;
		}
		
	}
	
	/**
	 * Instance of the {@link SourceSinkSpec} class for Java 
	 */
	public abstract class JavaSourceSinkSpec extends SourceSinkSpec {
		private final String className;
		
		/**
		 * Creates a new instance of the JavaSourceSinkSpec class
		 * @param type Specifies whether this element is a source or a sink
		 * @param className The name of the class containing the parameter to
		 * be defined as a source or sink.
		 */
		public JavaSourceSinkSpec(SourceSinkType type, String className) {
			super(type);
			this.className = className;
		}
		
		/**
		 * Gets the name of the class containing the parameter to be defined as
		 * a source or sink.
		 * @return The class name
		 */
		public String getClassName() {
			return this.className;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((className == null) ? 0 : className.hashCode());
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
			JavaSourceSinkSpec other = (JavaSourceSinkSpec) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			return true;
		}
		
	}
	
	/**
	 * Abstract base class for source/sink specifications on methods
	 */
	public abstract class JavaMethodSourceSinkSpec extends JavaSourceSinkSpec {
		private final String halfSignature;

		/**
		 * Creates a new instance of the JavaSourceSinkSpec class
		 * @param type Specifies whether this element is a source or a sink
		 * @param className The name of the class containing the parameter to
		 * be defined as a source or sink.
		 */
		public JavaMethodSourceSinkSpec(SourceSinkType type, String className,
				String halfSignature) {
			super(type, className);
			this.halfSignature = halfSignature;
		}
		
		/**
		 * Gets the method name and the formal parameters with fully-qualified
		 * names in brackets. This is like a Soot subsignature, except for the
		 * return type which is omitted.
		 * @return The method name and the method's fully qualified parameter
		 * list
		 */
		public String getHalfSignature() {
			return this.halfSignature;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((halfSignature == null) ? 0 : halfSignature.hashCode());
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
			JavaMethodSourceSinkSpec other = (JavaMethodSourceSinkSpec) obj;
			if (halfSignature == null) {
				if (other.halfSignature != null)
					return false;
			} else if (!halfSignature.equals(other.halfSignature))
				return false;
			return true;
		}
		
	}
	
	/**
	 * Class that models a method parameter in Java specified as a source or
	 * sink in RIFL
	 */
	public class JavaParameterSpec extends JavaMethodSourceSinkSpec {
		private final int paramIdx;
		
		/**
		 * Creates a new instance of the {@link JavaParameterSpec} class
		 * @param type Specifies whether this element is a source or a sink
		 * @param className The name of the class containing the parameter to
		 * be defined as a source or sink.
		 * @param halfSignature The method name and the formal parameters with
		 * fully-qualified names in brackets. This is like a Soot subsignature,
		 * except for the return type which is omitted.
		 * @param paramIdx The index of the parameter to be defined as a source
		 * or sink. 0 refers to the return value, so the list is 1-based.
		 */
		public JavaParameterSpec(SourceSinkType type, String className,
				String halfSignature, int paramIdx) {
			super(type, className, halfSignature);
			this.paramIdx = paramIdx;
		}
		
		/**
		 * Gets the index of the parameter to be defined as a source or sink. 0
		 * refers to the return value, so the list is 1-based.
		 * @return The parameter index of the tainted parameter
		 */
		public int getParamIdx() {
			return this.paramIdx;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + paramIdx;
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
			JavaParameterSpec other = (JavaParameterSpec) obj;
			if (paramIdx != other.paramIdx)
				return false;
			return true;
		}

	}
	
	/**
	 * Class that models the return value of a Java method specified as a source
	 * or a sink in a RIFL document
	 */
	public class JavaReturnValueSpec extends JavaMethodSourceSinkSpec {
		
		/**
		 * Creates a new instance of the {@link JavaReturnValueSpec} class
		 * @param type Specifies whether this element is a source or a sink
		 * @param className The name of the class containing the method whose
		 * return value shall be defined as a source or a sink
		 * @param halfSignature The method name and the formal parameters with
		 * fully-qualified names in brackets. This is like a Soot subsignature,
		 * except for the return type which is omitted.
		 */
		public JavaReturnValueSpec(SourceSinkType type, String className,
				String halfSignature) {
			super(type, className, halfSignature);
		}
		
	}
	
	/**
	 * Class that models a static field in Java specified as a source or sink
	 * in RIFL
	 */
	public class JavaFieldSpec extends JavaSourceSinkSpec {
		private final String fieldName;
		
		/**
		 * Creates a new instance of the {@link JavaFieldSpec} class
		 * @param type Specifies whether this element is a source or a sink
		 * @param className The name of the class containing the static field to
		 * be defined as a source or sink.
		 * @param fieldName The name of the static field to be treated as a source
		 * or sink
		 */
		public JavaFieldSpec(SourceSinkType type, String className,
				String fieldName) {
			super(type, className);
			this.fieldName = fieldName;
		}
		
		/**
		 * Gets the name of the static field to be treated as a source or sink
		 * @return The name of the static field to be treated as a source or sink
		 */
		public String getFieldName() {
			return this.fieldName;
		}

		@Override
		public int hashCode() {
			return 31 * this.fieldName.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof JavaFieldSpec))
				return false;
			JavaFieldSpec otherSpec = (JavaFieldSpec) other;
			return this.fieldName.equals(otherSpec.fieldName);
		}
	}

	/**
	 * Class representing a domain in the RIFL specification
	 */
	public class DomainSpec {
		private final String name;
		
		/**
		 * Creates a new instance of the DomainSpec class
		 * @param name The name of this domain
		 */
		public DomainSpec(String name) {
			this.name = name;
		}
		
		/**
		 * Gets the name of this domain
		 * @return The name of this domain
		 */
		public String getName() {
			return this.name;
		}
	}
	
	/**
	 * A category based on a named domain
	 */
	public class Category extends SourceSinkSpec {
		private final String name;
		private final List<SourceSinkSpec> elements = new ArrayList<>();
		
		/**
		 * Creates a new instance of the {@link Category} class
		 * @param name The name of the category
		 */
		public Category(String name) {
			super(SourceSinkType.Category);
			this.name = name;
		}
		
		/**
		 * Gets the name of this category
		 * @return The name of this category
		 */
		public String getName() {
			return this.name;
		}
		
		/**
		 * Gets the sources and sinks in this category
		 * @return The sources and sinks in this category
		 */
		public List<SourceSinkSpec> getElements() {
			return this.elements;
		}

		@Override
		public int hashCode() {
			return 31 * this.name.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof Category))
				return false;
			Category otherSpec = (Category) other;
			return this.name.equals(otherSpec.name);
		}
	}
	
	/**
	 * A link between an assignable element and a domain
	 */
	public class DomainAssignment {
		private final Assignable sourceOrSink;
		private final DomainSpec domain;
		
		/**
		 * Creates a new instance of the {@link DomainAssignment} class
		 * @param sourceOrSink The source or sink to be associated with a domain
		 * @param domain The domain to associate the source or sink with
		 */
		public DomainAssignment(Assignable sourceOrSink, DomainSpec domain) {
			this.sourceOrSink = sourceOrSink;
			this.domain = domain;
		}
		
		/**
		 * Gets the source or sink associated with a domain
		 * @return The source or sink associated with a domain
		 */
		public Assignable getSourceOrSink() {
			return this.sourceOrSink;
		}
		
		/**
		 * Gets the domain the source or sink is associated with
		 * @return The domain the source or sink is associated with
		 */
		public DomainSpec getDomain() {
			return this.domain;
		}
		
		@Override
		public int hashCode() {
			return 31 * this.sourceOrSink.hashCode()
					+ 31 * this.domain.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof DomainAssignment))
				return false;
			DomainAssignment otherPair = (DomainAssignment) other;
			return this.sourceOrSink.equals(otherPair.sourceOrSink)
					&& this.domain.equals(otherPair.domain);
		}
	}
	
	/**
	 * Class representing a pair of domains between which information flow is
	 * allowed
	 */
	public class FlowPair {
		private final DomainSpec firstDomain;
		private final DomainSpec secondDomain;
		
		/**
		 * Creates a new instance of the {@link FlowPair} class
		 * @param firstDomain The first domain in the pair, i.e. the start
		 * domain of the information flow
		 * @param secondDomain The second domain in the pair, i.e. the target
		 * domain of the information flow
		 */
		public FlowPair(DomainSpec firstDomain, DomainSpec secondDomain) {
			this.firstDomain = firstDomain;
			this.secondDomain = secondDomain;
		}
		
		/**
		 * Gets the first domain in the pair, i.e. the start domain of the
		 * information flow
		 * @return The first domain in the pair
		 */
		public DomainSpec getFirstDomain() {
			return this.firstDomain;
		}

		/**
		 * Gets the second domain in the pair, i.e. the target domain of the
		 * information flow
		 * @return The second domain in the pair
		 */
		public DomainSpec getSecondDomain() {
			return this.secondDomain;
		}
		
		@Override
		public int hashCode() {
			return 31 * this.firstDomain.hashCode()
					+ 31 * this.secondDomain.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof FlowPair))
				return false;
			FlowPair otherPair = (FlowPair) other;
			return this.firstDomain.equals(otherPair.firstDomain)
					&& this.secondDomain.equals(otherPair.secondDomain);
		}
	}

	/**
	 * Gets the interface specification
	 * @return The interface specification
	 */
	public InterfaceSpec getInterfaceSpec() {
		return this.interfaceSpec;
	}
	
	/**
	 * Gets the list of domains
	 * @return The list of domains
	 */
	public List<DomainSpec> getDomains() {
		return this.domains;
	}
	
	/**
	 * Gets the flow domain with the given name
	 * @param domainName The name of the flow domain to retrieve
	 * @return The flow domain with the given name. If no domain with the given
	 * name can be found, null is returned.
	 */
	public DomainSpec getDomainByName(String domainName) {
		for (DomainSpec ds : domains)
			if (ds.getName().equals(domainName))
				return ds;
		return null;
	}
	
	/**
	 * Gets the list of domain assignments
	 * @return The list of domain assignments
	 */
	public List<DomainAssignment> getDomainAssignment() {
		return this.domainAssignment;
	}
	
	/**
	 * Gets the flow policy as a list of pair of domains between each data
	 * flows are allowed
	 * @return The flow policy
	 */
	public List<FlowPair> getFlowPolicy() {
		return this.flowPolicy;
	}
	
	/**
	 * Gets the version number of the RIFL specification modeled by these data
	 * classes
	 * @return The version number of the RIFL specification
	 */
	public static String getRIFLSpecVersion() {
		return "1.0";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((interfaceSpec == null) ? 0 : interfaceSpec.hashCode());
		result = prime
				* result
				+ ((domainAssignment == null) ? 0 : domainAssignment.hashCode());
		result = prime * result + ((domains == null) ? 0 : domains.hashCode());
		result = prime * result
				+ ((flowPolicy == null) ? 0 : flowPolicy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof RIFLDocument))
			return false;
		RIFLDocument other = (RIFLDocument) obj;
		return interfaceSpec.equals(other.interfaceSpec)
				&& domainAssignment.equals(other.domainAssignment)
				&& domains.equals(other.domains)
				&& flowPolicy.equals(other.flowPolicy);
	}

}
