package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import soot.jimple.infoflow.data.SootMethodAndClass;

/**
 * A class to handle all access paths of sources and sinks for a certain method.
 * 
 * @author Daniel Magin
 * @author Steven Arzt
 *
 */
public class MethodSourceSinkDefinition extends SourceSinkDefinition {

	private static MethodSourceSinkDefinition BASE_OBJ_SOURCE;
	private static MethodSourceSinkDefinition BASE_OBJ_SINK;
	private static MethodSourceSinkDefinition[] PARAM_OBJ_SOURCE = new MethodSourceSinkDefinition[5];

	protected final SootMethodAndClass method;
	protected final CallType callType;
	protected Set<AccessPathTuple> baseObjects;
	protected Set<AccessPathTuple>[] parameters;
	protected Set<AccessPathTuple> returnValues;

	/**
	 * Enumeration containing the different types of method invocations that can be
	 * defined as sources or sinks
	 * 
	 * @author Steven Arzt
	 *
	 */
	public enum CallType {
		/**
		 * The app calls the method
		 */
		MethodCall,

		/**
		 * The method is a callback that is invoked by the Android operating system
		 */
		Callback,

		/**
		 * All return values in the method are considered (only supported as sink)
		 */
		Return
	}

	/**
	 * Creates a new instance of the {@link SourceSinkDefinition} class without a
	 * method. This constructor is intended to be used for sources that arise from
	 * UI elements or other programming constructs that are not directly associated
	 * with a single method being called.
	 */
	public MethodSourceSinkDefinition(Set<AccessPathTuple> baseObjects, Set<AccessPathTuple>[] parameters,
			Set<AccessPathTuple> returnValues, CallType callType) {
		this(null, baseObjects, parameters, returnValues, callType);
	}

	/**
	 * Creates a new instance of the {@link SourceSinkDefinition} class
	 */
	public MethodSourceSinkDefinition(SootMethodAndClass am) {
		this(am, null, null, null, CallType.MethodCall);
	}

	/**
	 * Creates a new instance of the MethodSourceSinkDefinition class
	 * 
	 * @param am
	 *            The method for which this object defines sources and sinks
	 * @param baseObjects
	 *            The source and sink definitions for the base object on which a
	 *            method of this class is invoked
	 * @param parameters
	 *            The source and sink definitions for parameters of the current
	 *            method
	 * @param callType
	 *            The type of calls to define as sources or sinks
	 * @param returnValues
	 *            The source definitions for the return value of the current method
	 */
	public MethodSourceSinkDefinition(SootMethodAndClass am, Set<AccessPathTuple> baseObjects,
			Set<AccessPathTuple>[] parameters, Set<AccessPathTuple> returnValues, CallType callType) {
		this.method = am;
		this.baseObjects = baseObjects == null || baseObjects.isEmpty() ? null : baseObjects;
		this.parameters = parameters;
		this.returnValues = returnValues == null || returnValues.isEmpty() ? null : returnValues;
		this.callType = callType;
	}

	/**
	 * Gets the method for which this object defines sources and sinks
	 * 
	 * @return The method for which this object defines sources and sinks
	 */
	public SootMethodAndClass getMethod() {
		return this.method;
	}

	/**
	 * Gets the type of method invocations that are denoted by this source or sink
	 * 
	 * @return The type of method invocations that are denoted by this source or
	 *         sink
	 */
	public CallType getCallType() {
		return this.callType;
	}

	/**
	 * Gets the source and sink definitions for the base object on which a method of
	 * this class is invoked
	 * 
	 * @return The source and sink definitions for the base object
	 */
	public Set<AccessPathTuple> getBaseObjects() {
		return this.baseObjects;
	}

	/**
	 * Gets the number of access paths defined as sources or sinks on base objects
	 * 
	 * @return The number of access paths defined as sources or sinks on base
	 *         objects
	 */
	public int getBaseObjectCount() {
		return this.baseObjects == null ? 0 : this.baseObjects.size();
	}

	/**
	 * Gets the source and sink definitions for parameters of the current method
	 * 
	 * @return The source and sink definitions for parameters
	 */
	public Set<AccessPathTuple>[] getParameters() {
		return this.parameters;
	}

	/**
	 * Gets the number of access paths defined as sources or sinks on parameters
	 * 
	 * @return The number of access paths defined as sources or sinks on parameters
	 */
	public int getParameterCount() {
		if (this.parameters == null || this.parameters.length == 0)
			return 0;

		int cnt = 0;
		for (Set<AccessPathTuple> apt : this.parameters)
			cnt += apt.size();
		return cnt;
	}

	/**
	 * Gets the source definitions for the return value of the current method
	 * 
	 * @return The source definitions for the return value
	 */
	public Set<AccessPathTuple> getReturnValues() {
		return this.returnValues;
	}

	/**
	 * Gets the number of access paths defined as sources or sinks on return values
	 * 
	 * @return The number of access paths defined as sources or sinks on return
	 *         values
	 */
	public int getReturnValueCount() {
		return this.returnValues == null ? 0 : this.returnValues.size();
	}

	@Override
	public boolean isEmpty() {
		boolean parametersEmpty = true;
		if (parameters != null)
			for (Set<AccessPathTuple> paramSet : this.parameters)
				if (paramSet != null && !paramSet.isEmpty()) {
					parametersEmpty = false;
					break;
				}

		return (baseObjects == null || baseObjects.isEmpty()) && parametersEmpty
				&& (returnValues == null || returnValues.isEmpty());
	}

	@Override
	public String toString() {
		return method == null ? "<no method>" : method.getSignature();
	}

	@Override
	public SourceSinkDefinition getSourceOnlyDefinition() {
		// Collect all base sources
		Set<AccessPathTuple> baseSources = null;
		if (baseObjects != null) {
			baseSources = new HashSet<>(baseObjects.size());
			for (AccessPathTuple apt : baseObjects)
				if (apt.getSourceSinkType().isSource())
					baseSources.add(apt);
		}

		// Collect all parameter sources
		@SuppressWarnings("unchecked")
		Set<AccessPathTuple>[] paramSources = new Set[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Set<AccessPathTuple> aptSet = parameters[i];
			Set<AccessPathTuple> thisParam = new HashSet<>(aptSet.size());
			paramSources[i] = thisParam;
			for (AccessPathTuple apt : aptSet)
				if (apt.getSourceSinkType().isSource())
					thisParam.add(apt);
		}

		// Collect all return sources
		Set<AccessPathTuple> returnSources = null;
		if (returnValues != null) {
			returnSources = new HashSet<>(returnValues.size());
			for (AccessPathTuple apt : returnValues)
				if (apt.getSourceSinkType().isSource())
					returnSources.add(apt);
		}

		MethodSourceSinkDefinition mssd = buildNewDefinition(baseSources, paramSources, returnSources);
		return mssd;
	}

	@Override
	public SourceSinkDefinition getSinkOnlyDefinition() {
		// Collect all base sinks
		Set<AccessPathTuple> baseSinks = null;
		if (baseObjects != null) {
			baseSinks = new HashSet<>(baseObjects.size());
			for (AccessPathTuple apt : baseObjects)
				if (apt.getSourceSinkType().isSink())
					baseSinks.add(apt);
		}

		// Collect all parameter sinks
		@SuppressWarnings("unchecked")
		Set<AccessPathTuple>[] paramSinks = new Set[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Set<AccessPathTuple> aptSet = parameters[i];
			Set<AccessPathTuple> thisParam = new HashSet<>(aptSet.size());
			paramSinks[i] = thisParam;
			for (AccessPathTuple apt : aptSet)
				if (apt.getSourceSinkType().isSink())
					thisParam.add(apt);
		}

		// Collect all return sinks
		Set<AccessPathTuple> returnSinks = null;
		if (returnValues != null) {
			returnSinks = new HashSet<>(returnValues.size());
			for (AccessPathTuple apt : returnValues)
				if (apt.getSourceSinkType().isSink())
					returnSinks.add(apt);
		}

		MethodSourceSinkDefinition mssd = buildNewDefinition(baseSinks, paramSinks, returnSinks);
		return mssd;
	}

	/**
	 * Factory method for creating a new method-based source/sink definition based
	 * on the current one. This method is used when transforming the current
	 * definition. Derived classes can override this method to create instances of
	 * the correct class.
	 * 
	 * @param baseAPTs
	 *            The access paths rooted in the base object
	 * @param paramAPTs
	 *            The access paths rooted in the method's parameters
	 * @param returnAPTs
	 *            The access paths rooted in the return value
	 * @return The new source/sink definition object
	 */
	protected MethodSourceSinkDefinition buildNewDefinition(Set<AccessPathTuple> baseAPTs,
			Set<AccessPathTuple>[] paramAPTs, Set<AccessPathTuple> returnAPTs) {
		MethodSourceSinkDefinition def = new MethodSourceSinkDefinition(method, baseAPTs, paramAPTs, returnAPTs,
				callType);
		def.setCategory(category);
		return def;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void merge(SourceSinkDefinition other) {
		if (other instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition otherMethod = (MethodSourceSinkDefinition) other;

			// Merge the base object definitions
			if (otherMethod.baseObjects != null && !otherMethod.baseObjects.isEmpty()) {
				if (this.baseObjects == null)
					this.baseObjects = new HashSet<>();
				for (AccessPathTuple apt : otherMethod.baseObjects)
					this.baseObjects.add(apt);
			}

			// Merge the parameter definitions
			if (otherMethod.parameters != null && otherMethod.parameters.length > 0) {
				if (this.parameters == null)
					this.parameters = new Set[this.method.getParameters().size()];
				for (int i = 0; i < otherMethod.parameters.length; i++) {
					this.parameters[i].addAll(otherMethod.parameters[i]);
				}
			}

			// Merge the return value definitions
			if (otherMethod.returnValues != null && !otherMethod.returnValues.isEmpty()) {
				if (this.returnValues == null)
					this.returnValues = new HashSet<>();
				for (AccessPathTuple apt : otherMethod.returnValues)
					this.returnValues.add(apt);
			}
		}
	}

	/**
	 * Gets the shared source definition that is not associated with any method and
	 * taints the base object
	 * 
	 * @return The shared blank source definition that is not associated with any
	 *         method and taints the base object
	 */
	public static MethodSourceSinkDefinition getBaseObjectSource() {
		if (BASE_OBJ_SOURCE == null)
			BASE_OBJ_SOURCE = new MethodSourceSinkDefinition(
					Collections.singleton(AccessPathTuple.getBlankSourceTuple()), null, null, CallType.MethodCall);
		return BASE_OBJ_SOURCE;
	}

	/**
	 * Gets the shared sink definition that is not associated with any method and
	 * taints the base object
	 * 
	 * @return The shared blank sink definition that is not associated with any
	 *         method and taints the base object
	 */
	public static MethodSourceSinkDefinition getBaseObjectSink() {
		if (BASE_OBJ_SINK == null)
			BASE_OBJ_SINK = new MethodSourceSinkDefinition(Collections.singleton(AccessPathTuple.getBlankSinkTuple()),
					null, null, CallType.MethodCall);
		return BASE_OBJ_SINK;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseObjects == null) ? 0 : baseObjects.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((callType == null) ? 0 : callType.hashCode());
		result = prime * result + ((parameters == null) ? 0 : Arrays.hashCode(parameters));
		result = prime * result + ((returnValues == null) ? 0 : returnValues.hashCode());
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
		MethodSourceSinkDefinition other = (MethodSourceSinkDefinition) obj;
		if (baseObjects == null) {
			if (other.baseObjects != null)
				return false;
		} else if (!baseObjects.equals(other.baseObjects))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (callType == null) {
			if (other.callType != null)
				return false;
		} else if (!callType.equals(other.callType))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!Arrays.equals(parameters, other.parameters))
			return false;
		if (returnValues == null) {
			if (other.returnValues != null)
				return false;
		} else if (!returnValues.equals(other.returnValues))
			return false;
		return true;
	}

	/**
	 * Creates a source definition that considers a parameter as tainted
	 * 
	 * @param index
	 *            The index of the parameter to consider as tainted
	 * @param callType
	 *            The type of call
	 * @return The newly created source definition
	 */
	@SuppressWarnings("unchecked")
	public static MethodSourceSinkDefinition createParameterSource(int index, CallType callType) {
		// For small indices, we have cached shared objects
		if (index < 5 && callType == CallType.MethodCall) {
			MethodSourceSinkDefinition def = PARAM_OBJ_SOURCE[index];
			if (def == null) {
				def = new MethodSourceSinkDefinition(null, (Set<AccessPathTuple>[]) new Set<?>[] {
						Collections.singleton(AccessPathTuple.getBlankSourceTuple()) }, null, callType);
				PARAM_OBJ_SOURCE[index] = def;
			}
			return def;
		}

		return new MethodSourceSinkDefinition(null,
				(Set<AccessPathTuple>[]) new Set<?>[] { Collections.singleton(AccessPathTuple.getBlankSourceTuple()) },
				null, callType);
	}

	/**
	 * Creates a source definition that considers the return value as tainted
	 * 
	 * @param callType
	 *            The type of call
	 * @return The newly created source definition
	 */
	public static SourceSinkDefinition createReturnSource(CallType callType) {
		return new MethodSourceSinkDefinition(null, null, Collections.singleton(AccessPathTuple.getBlankSourceTuple()),
				callType);
	}

	/**
	 * Checks whether this definition is equivalent to one of the simple predefined
	 * ones. If so, it returns the shared predefined object. Otherwise, it returns
	 * this object.
	 * 
	 * @return A shared object that is equal to this one if possible, otherwise this
	 *         object
	 */
	public MethodSourceSinkDefinition simplify() {
		MethodSourceSinkDefinition baseObjSource = getBaseObjectSource();
		MethodSourceSinkDefinition baseObjSink = getBaseObjectSink();

		if (this.equals(baseObjSource))
			return baseObjSource;
		else if (this.equals(baseObjSink))
			return baseObjSink;
		else {
			for (int i = 0; i < PARAM_OBJ_SOURCE.length; i++) {
				MethodSourceSinkDefinition def = createParameterSource(i, getCallType());
				if (this.equals(def))
					return def;
			}
			return this;
		}
	}

}
