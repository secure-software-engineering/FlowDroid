package soot.jimple.infoflow.methodSummary.data.sourceSink;

import java.util.HashMap;
import java.util.Map;

import soot.Local;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;
import soot.jimple.infoflow.methodSummary.xml.XMLConstants;

/**
 * Data class which stores the data associated to a Sink or a Source of a method
 * flow.
 */
public abstract class AbstractFlowSinkSource {
	protected final SourceSinkType type;
	protected final int parameterIdx;
	protected final String baseType;
	protected final AccessPathFragment accessPath;
	protected final GapDefinition gap;
	protected final Object userData;
	protected final boolean matchStrict;
	protected ConstraintType isConstrained;


	public AbstractFlowSinkSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
								  boolean matchStrict, ConstraintType isConstrained) {
		this(type, parameterIdx, baseType, accessPath, null, matchStrict, isConstrained);
	}

	public AbstractFlowSinkSource(SourceSinkType type, String baseType, AccessPathFragment accessPath,
								  GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		this(type, -1, baseType, accessPath, gap, matchStrict, isConstrained);
	}

	public AbstractFlowSinkSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
								  GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		this(type, parameterIdx, baseType, accessPath, gap, null, matchStrict, isConstrained);
	}

	public AbstractFlowSinkSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
								  GapDefinition gap, Object userData, boolean matchStrict, ConstraintType isConstrained) {
		this.type = type;
		this.parameterIdx = parameterIdx;
		this.baseType = baseType;
		this.accessPath = accessPath;
		this.gap = gap;
		this.userData = userData;
		this.matchStrict = matchStrict;
		this.isConstrained = isConstrained;
	}


	/**
	 * Checks whether the current source or sink is coarser than the given one,
	 * i.e., if all elements referenced by the given source or sink are also
	 * referenced by this one
	 * 
	 * @param other The source or sink with which to compare the current one
	 * @return True if the current source or sink is coarser than the given one,
	 *         otherwise false
	 */
	public boolean isCoarserThan(AbstractFlowSinkSource other) {
		if (this.equals(other))
			return true;

		if (this.type != other.type || this.parameterIdx != other.parameterIdx
				|| !safeCompare(this.baseType, other.baseType) || !safeCompare(this.gap, other.gap))
			return false;
		if (this.accessPath != null && other.accessPath != null) {
			if (this.accessPath.length() > other.accessPath.length())
				return false;
			for (int i = 0; i < this.accessPath.length(); i++)
				if (!this.accessPath.getField(i).equals(other.accessPath.getField(i)))
					return false;
		}
		return true;
	}

	public boolean isParameter() {
		return type == SourceSinkType.Parameter;
	}

	public boolean isThis() {
		return type == SourceSinkType.Field && !hasAccessPath();
	}

	public boolean isCustom() {
		return type == SourceSinkType.Custom;
	}

	public int getParameterIndex() {
		return parameterIdx;
	}

	public String getBaseType() {
		return baseType;
	}

	/**
	 * Gets whether this taint is on a *base* field. Note that this does not include
	 * fields starting on parameters or return values.
	 * 
	 * @return True if this taint references a base field, false otherwise
	 */
	public boolean isField() {
		return type == SourceSinkType.Field;
	}

	public AccessPathFragment getAccessPath() {
		return accessPath;
	}

	public boolean isReturn() {
		return type == SourceSinkType.Return;
	}

	public boolean isGapBaseObject() {
		return type == SourceSinkType.GapBaseObject;
	}

	public boolean hasAccessPath() {
		return accessPath != null && !accessPath.isEmpty();
	}

	public int getAccessPathLength() {
		return accessPath == null ? 0 : accessPath.length();
	}

	public SourceSinkType getType() {
		return this.type;
	}

	public GapDefinition getGap() {
		return this.gap;
	}

	public boolean hasGap() {
		return this.gap != null;
	}

	public String getLastFieldType() {
		if (accessPath == null || accessPath.isEmpty())
			return baseType;
		return accessPath.getLastFieldType();
	}

	/**
	 * Gets whether strict access path matching shall be applied to this source
	 * definition. Strict matching means that a taint on a.* does NOT match a source
	 * defined for a.foo.
	 * 
	 * @return True if strict access path matching shall be applied, otherwise false
	 */
	public boolean isMatchStrict() {
		return matchStrict;
	}

	public boolean isConstrained() {
		return isConstrained == ConstraintType.TRUE || isConstrained == ConstraintType.NO_MATCH;
	}

	public boolean shiftRight() {
		return isConstrained == ConstraintType.SHIFT_RIGHT;
	}

	public boolean shiftLeft() {
		return isConstrained == ConstraintType.SHIFT_LEFT;
	}

	public boolean anyShift() {
		return shiftLeft() || shiftRight();
	}

	public boolean keepConstraint() {
		return isConstrained == ConstraintType.KEEP;
	}

	public boolean keepOnRO() {
		return isConstrained == ConstraintType.READONLY;
	}

	public boolean append() { return isConstrained == ConstraintType.APPEND; }

	public ConstraintType getConstraintType() {
		return isConstrained;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());
		result = prime * result + ((gap == null) ? 0 : gap.hashCode());
		result = prime * result + (matchStrict ? 1231 : 1237);
		result = prime * result + isConstrained.hashCode();
		result = prime * result + parameterIdx;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
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
		AbstractFlowSinkSource other = (AbstractFlowSinkSource) obj;
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		if (baseType == null) {
			if (other.baseType != null)
				return false;
		} else if (!baseType.equals(other.baseType))
			return false;
		if (gap == null) {
			if (other.gap != null)
				return false;
		} else if (!gap.equals(other.gap))
			return false;
		if (matchStrict != other.matchStrict)
			return false;
		if (isConstrained != other.isConstrained)
			return false;
		if (parameterIdx != other.parameterIdx)
			return false;
		if (type != other.type)
			return false;
		if (userData == null) {
			if (other.userData != null)
				return false;
		} else if (!userData.equals(other.userData))
			return false;
		return true;
	}

	protected boolean safeCompare(Object o1, Object o2) {
		if (o1 == null)
			return o2 == null;
		if (o2 == null)
			return o1 == null;
		return o1.equals(o2);
	}

	public Map<String, String> xmlAttributes() {
		Map<String, String> res = new HashMap<String, String>();
		if (isParameter()) {
			res.put(XMLConstants.ATTRIBUTE_FLOWTYPE, XMLConstants.VALUE_PARAMETER);
			res.put(XMLConstants.ATTRIBUTE_PARAMETER_INDEX, getParameterIndex() + "");
		} else if (isField())
			res.put(XMLConstants.ATTRIBUTE_FLOWTYPE, XMLConstants.VALUE_FIELD);
		else if (isReturn())
			res.put(XMLConstants.ATTRIBUTE_FLOWTYPE, XMLConstants.VALUE_RETURN);
		else
			throw new RuntimeException("Invalid source type");

		if (baseType != null)
			res.put(XMLConstants.ATTRIBUTE_BASETYPE, baseType);
		if (hasAccessPath())
			res.put(XMLConstants.ATTRIBUTE_ACCESSPATH, getAccessPath().toString());
		if (gap != null)
			res.put(XMLConstants.ATTRIBUTE_GAP, getGap().getID() + "");

		return res;
	}

	/**
	 * Gets the custom user data associated with this sink
	 * 
	 * @return The custom user data associated with this sink
	 */
	public Object getUserData() {
		return this.userData;
	}

	/**
	 * Replaces the gaps in this definition according to the given map
	 * 
	 * @param replacementMap A mapping from gap id to new gap data object
	 * @return A copy of this definition in which the gaps that also occur in the
	 *         given map have been replaced with the values from the map
	 */
	public abstract AbstractFlowSinkSource replaceGaps(Map<Integer, GapDefinition> replacementMap);

	/**
	 * Gets the base local for the access path that this source or sink definition
	 * references
	 * 
	 * @param stmt The context in which to map the definition to concrete locals
	 * @return The local that resembles the base local of this source or sink
	 *         definition in the given context or <code>null</code> if this
	 *         definition is not valid in the given context
	 */
	public Local getBaseLocal(Stmt stmt) {
		if (stmt.containsInvokeExpr()) {
			InvokeExpr iexpr = stmt.getInvokeExpr();
			if (isField()) {
				if (iexpr instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) iexpr;
					return (Local) iiexpr.getBase();
				}
			} else if (isParameter()) {
				int idx = getParameterIndex();
				if (idx < iexpr.getArgCount()) {
					Value val = iexpr.getArg(idx);
					if (val instanceof Local)
						return (Local) val;
				}
			} else if (isReturn()) {
				if (stmt instanceof AssignStmt) {
					AssignStmt assignStmt = (AssignStmt) stmt;
					Value lop = assignStmt.getLeftOp();
					if (lop instanceof Local)
						return (Local) lop;
				}
			}
		}
		return null;
	}

}
