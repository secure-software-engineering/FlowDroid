package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Arrays;
import java.util.Collection;
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
public class MethodSourceSinkDefinition extends AbstractSourceSinkDefinition
		implements IAccessPathBasedSourceSinkDefinition {

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
	 * Creates a new instance of the {@link MethodSourceSinkDefinition} class
	 * without a method. This constructor is intended to be used for sources that
	 * arise from UI elements or other programming constructs that are not directly
	 * associated with a single method being called.
	 */
	public MethodSourceSinkDefinition(Set<AccessPathTuple> baseObjects, Set<AccessPathTuple>[] parameters,
			Set<AccessPathTuple> returnValues, CallType callType) {
		this(null, baseObjects, parameters, returnValues, callType);
	}

	/**
	 * Creates a new instance of the {@link MethodSourceSinkDefinition} class
	 * 
	 * @param am The method for which this object defines sources and sinks
	 */
	public MethodSourceSinkDefinition(SootMethodAndClass am) {
		this(am, null, null, null, CallType.MethodCall);
	}

	/**
	 * Creates a new instance of the {@link MethodSourceSinkDefinition} class
	 * 
	 * @param am       The method for which this object defines sources and sinks
	 * @param callType The type of calls to define as sources or sinks
	 */
	public MethodSourceSinkDefinition(SootMethodAndClass am, CallType callType) {
		this(am, null, null, null, callType);
	}

	/**
	 * Creates a new instance of the MethodSourceSinkDefinition class
	 * 
	 * @param am           The method for which this object defines sources and
	 *                     sinks
	 * @param baseObjects  The source and sink definitions for the base object on
	 *                     which a method of this class is invoked
	 * @param parameters   The source and sink definitions for parameters of the
	 *                     current method
	 * @param callType     The type of calls to define as sources or sinks
	 * @param returnValues The source definitions for the return value of the
	 *                     current method
	 */
	public MethodSourceSinkDefinition(SootMethodAndClass am, Set<AccessPathTuple> baseObjects,
			Set<AccessPathTuple>[] parameters, Set<AccessPathTuple> returnValues, CallType callType) {
		this(am, baseObjects, parameters, returnValues, callType, null);
	}

	/**
	 * Creates a new instance of the MethodSourceSinkDefinition class
	 * 
	 * @param am           The method for which this object defines sources and
	 *                     sinks
	 * @param baseObjects  The source and sink definitions for the base object on
	 *                     which a method of this class is invoked
	 * @param parameters   The source and sink definitions for parameters of the
	 *                     current method
	 * @param returnValues The source definitions for the return value of the
	 *                     current method
	 * @param callType     The type of calls to define as sources or sinks
	 * @param category     The category to which this source or sink belongs
	 */
	public MethodSourceSinkDefinition(SootMethodAndClass am, Set<AccessPathTuple> baseObjects,
			Set<AccessPathTuple>[] parameters, Set<AccessPathTuple> returnValues, CallType callType,
			ISourceSinkCategory category) {
		super(category);
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

	@SuppressWarnings("unchecked")
	@Override
	public MethodSourceSinkDefinition getSourceOnlyDefinition() {
		// Collect all base sources
		Set<AccessPathTuple> baseSources = null;
		if (baseObjects != null) {
			baseSources = new HashSet<>(baseObjects.size());
			for (AccessPathTuple apt : baseObjects)
				if (apt.getSourceSinkType().isSource())
					baseSources.add(apt);
		}

		// Collect all parameter sources
		Set<AccessPathTuple>[] paramSources = null;
		if (parameters != null && parameters.length > 0) {
			paramSources = new Set[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				Set<AccessPathTuple> aptSet = parameters[i];
				if (aptSet != null) {
					Set<AccessPathTuple> thisParam = new HashSet<>(aptSet.size());
					paramSources[i] = thisParam;
					for (AccessPathTuple apt : aptSet)
						if (apt.getSourceSinkType().isSource())
							thisParam.add(apt);
				}
			}
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

	@SuppressWarnings("unchecked")
	@Override
	public MethodSourceSinkDefinition getSinkOnlyDefinition() {
		// Collect all base sinks
		Set<AccessPathTuple> baseSinks = null;
		if (baseObjects != null) {
			baseSinks = new HashSet<>(baseObjects.size());
			for (AccessPathTuple apt : baseObjects)
				if (apt.getSourceSinkType().isSink())
					baseSinks.add(apt);
		}

		// Collect all parameter sinks
		Set<AccessPathTuple>[] paramSinks = null;
		if (parameters != null) {
			paramSinks = new Set[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				Set<AccessPathTuple> aptSet = parameters[i];
				if (aptSet != null) {
					Set<AccessPathTuple> thisParam = new HashSet<>(aptSet.size());
					paramSinks[i] = thisParam;
					for (AccessPathTuple apt : aptSet)
						if (apt.getSourceSinkType().isSink())
							thisParam.add(apt);
				}
			}
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
	 * @param baseAPTs   The access paths rooted in the base object
	 * @param paramAPTs  The access paths rooted in the method's parameters
	 * @param returnAPTs The access paths rooted in the return value
	 * @return The new source/sink definition object
	 */
	protected MethodSourceSinkDefinition buildNewDefinition(Set<AccessPathTuple> baseAPTs,
			Set<AccessPathTuple>[] paramAPTs, Set<AccessPathTuple> returnAPTs) {
		MethodSourceSinkDefinition def = buildNewDefinition(method, baseAPTs, paramAPTs, returnAPTs, callType);
		def.category = category;
		def.conditions = conditions;
		return def;
	}

	protected MethodSourceSinkDefinition buildNewDefinition(SootMethodAndClass methodAndclass,
			Set<AccessPathTuple> filteredBaseObjects, Set<AccessPathTuple>[] filteredParameters,
			Set<AccessPathTuple> filteredReturnValues, CallType callType) {
		return new MethodSourceSinkDefinition(methodAndclass, filteredBaseObjects, filteredParameters,
				filteredReturnValues, callType);
	}

	/**
	 * Adds the given access path tuples to this source/sink definition for the
	 * given parameter index
	 * 
	 * @param paramIdx  The parameter index
	 * @param paramDefs The access path tuples
	 */
	@SuppressWarnings("unchecked")
	public void addParameterDefinition(int paramIdx, Set<AccessPathTuple> paramDefs) {
		if (paramDefs != null && !paramDefs.isEmpty()) {
			// We may need to widen our parameter array
			Set<AccessPathTuple>[] oldSet = this.parameters;
			if (oldSet.length <= paramIdx) {
				Set<AccessPathTuple>[] newSet = (Set<AccessPathTuple>[]) new Set<?>[paramIdx + 1];
				System.arraycopy(oldSet, 0, newSet, 0, paramIdx);
				this.parameters = newSet;
			}

			// We may not have a set of access path tuples yet
			Set<AccessPathTuple> aps = this.parameters[paramIdx];
			if (aps == null) {
				aps = new HashSet<>(paramDefs.size());
				this.parameters[paramIdx] = aps;
			}
			aps.addAll(paramDefs);
		}
	}

	/**
	 * Gets the shared source definition that is not associated with any method and
	 * taints the base object
	 * 
	 * @return The shared blank source definition that is not associated with any
	 *         method and taints the base object
	 */
	public MethodSourceSinkDefinition getBaseObjectSource() {
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
	public MethodSourceSinkDefinition getBaseObjectSink() {
		if (BASE_OBJ_SINK == null)
			BASE_OBJ_SINK = new MethodSourceSinkDefinition(Collections.singleton(AccessPathTuple.getBlankSinkTuple()),
					null, null, CallType.MethodCall);
		return BASE_OBJ_SINK;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((baseObjects == null) ? 0 : baseObjects.hashCode());
		result = prime * result + ((callType == null) ? 0 : callType.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + Arrays.hashCode(parameters);
		result = prime * result + ((returnValues == null) ? 0 : returnValues.hashCode());
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
		MethodSourceSinkDefinition other = (MethodSourceSinkDefinition) obj;
		if (baseObjects == null) {
			if (other.baseObjects != null)
				return false;
		} else if (!baseObjects.equals(other.baseObjects))
			return false;
		if (callType != other.callType)
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (!Arrays.equals(parameters, other.parameters))
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
	 * @param index    The index of the parameter to consider as tainted
	 * @param callType The type of call
	 * @return The newly created source definition
	 */
	@SuppressWarnings("unchecked")
	public static MethodSourceSinkDefinition createParameterSource(int index, CallType callType) {
		// For small indices, we have cached shared objects
		if (index < 5 && callType == CallType.MethodCall) {
			MethodSourceSinkDefinition def = PARAM_OBJ_SOURCE[index];
			if (def == null) {
				Set<AccessPathTuple>[] params = (Set<AccessPathTuple>[]) new Set<?>[index + 1];
				params[index] = Collections.singleton(AccessPathTuple.getBlankSourceTuple());
				def = new MethodSourceSinkDefinition(null, params, null, callType);
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
	 * @param callType The type of call
	 * @return The newly created source definition
	 */
	public static MethodSourceSinkDefinition createReturnSource(CallType callType) {
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

	@Override
	public Set<AccessPathTuple> getAllAccessPaths() {
		Set<AccessPathTuple> aps = new HashSet<>();
		if (baseObjects != null && !baseObjects.isEmpty())
			aps.addAll(baseObjects);
		if (returnValues != null && !returnValues.isEmpty())
			aps.addAll(returnValues);
		if (parameters != null && parameters.length > 0) {
			for (Set<AccessPathTuple> paramAPs : parameters) {
				if (paramAPs != null && !paramAPs.isEmpty())
					aps.addAll(paramAPs);
			}
		}
		return aps;
	}

	@SuppressWarnings("unchecked")
	@Override
	public MethodSourceSinkDefinition filter(Collection<AccessPathTuple> accessPaths) {
		// Filter the base objects
		Set<AccessPathTuple> filteredBaseObjects = null;
		if (baseObjects != null && !baseObjects.isEmpty()) {
			filteredBaseObjects = new HashSet<>(baseObjects.size());
			for (AccessPathTuple ap : baseObjects)
				if (accessPaths.contains(ap))
					filteredBaseObjects.add(ap);
		}

		// Filter the return values
		Set<AccessPathTuple> filteredReturnValues = null;
		if (returnValues != null && !returnValues.isEmpty()) {
			filteredReturnValues = new HashSet<>(returnValues.size());
			for (AccessPathTuple ap : returnValues)
				if (accessPaths.contains(ap))
					filteredReturnValues.add(ap);
		}

		// Filter the parameters
		Set<AccessPathTuple>[] filteredParameters = null;
		if (parameters != null && parameters.length > 0) {
			filteredParameters = new Set[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i] != null && !parameters[i].isEmpty()) {
					filteredParameters[i] = new HashSet<>();
					for (AccessPathTuple ap : parameters[i])
						if (accessPaths.contains(ap))
							filteredParameters[i].add(ap);
				}
			}
		}

		MethodSourceSinkDefinition def = buildNewDefinition(method, filteredBaseObjects, filteredParameters,
				filteredReturnValues, callType);
		def.setCategory(category);
		def.setConditions(conditions);
		return def;
	}

}
