package soot.jimple.infoflow.android.source;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.StatementSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * SourceSinkManager for Android applications. This class uses precise access
 * path-based source and sink definitions.
 * 
 * @author Steven Arzt
 *
 */
public class AccessPathBasedSourceSinkManager extends AndroidSourceSinkManager {

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * either strong or weak matching.
	 * 
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 * @param config
	 *            The configuration of the data flow analyzer
	 */
	public AccessPathBasedSourceSinkManager(Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks,
			InfoflowAndroidConfiguration config) {
		super(sources, sinks, config);
	}

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * strong matching, i.e. the methods in the code must exactly match those in the
	 * list.
	 * 
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 * @param callbackMethods
	 *            The list of callback methods whose parameters are sources through
	 *            which the application receives data from the operating system
	 * @param config
	 *            The configuration of the data flow analyzer
	 * @param layoutControls
	 *            A map from reference identifiers to the respective Android layout
	 *            controls
	 */
	public AccessPathBasedSourceSinkManager(Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks,
			Set<CallbackDefinition> callbackMethods, InfoflowAndroidConfiguration config,
			Map<Integer, AndroidLayoutControl> layoutControls) {
		super(sources, sinks, callbackMethods, config, layoutControls);
	}

	@Override
	protected SourceInfo createSourceInfo(Stmt sCallSite, InfoflowManager manager, SourceSinkDefinition def) {
		// Do we have data at all?
		if (null == def)
			return null;
		if (def.isEmpty())
			return super.createSourceInfo(sCallSite, manager, def);

		// We have real access path definitions, so we can construct precise
		// source information objects
		Set<AccessPath> aps = new HashSet<>();

		if (def instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;

			// For parameters in callback methods, we need special handling
			switch (methodDef.getCallType()) {
			case Callback:
				if (sCallSite instanceof IdentityStmt) {
					IdentityStmt is = (IdentityStmt) sCallSite;
					if (is.getRightOp() instanceof ParameterRef) {
						ParameterRef paramRef = (ParameterRef) is.getRightOp();
						if (methodDef.getParameters() != null && methodDef.getParameters().length > paramRef.getIndex())
							for (AccessPathTuple apt : methodDef.getParameters()[paramRef.getIndex()])
								aps.add(apt.toAccessPath(is.getLeftOp(), manager, false));
					}
				}
				break;
			case MethodCall:
				// Check whether we need to taint the base object
				if (sCallSite instanceof InvokeStmt && sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr
						&& methodDef.getBaseObjects() != null) {
					Value baseVal = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
					for (AccessPathTuple apt : methodDef.getBaseObjects())
						if (apt.getSourceSinkType().isSource())
							aps.add(apt.toAccessPath(baseVal, manager, true));
				}

				// Check whether we need to taint the return object
				if (sCallSite instanceof DefinitionStmt && methodDef.getReturnValues() != null) {
					Value returnVal = ((DefinitionStmt) sCallSite).getLeftOp();
					for (AccessPathTuple apt : methodDef.getReturnValues())
						if (apt.getSourceSinkType().isSource())
							aps.add(apt.toAccessPath(returnVal, manager, false));
				}

				// Check whether we need to taint parameters
				if (sCallSite.containsInvokeExpr() && methodDef.getParameters() != null
						&& methodDef.getParameters().length > 0)
					for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++)
						if (methodDef.getParameters().length > i)
							for (AccessPathTuple apt : methodDef.getParameters()[i])
								if (apt.getSourceSinkType().isSource())
									aps.add(apt.toAccessPath(sCallSite.getInvokeExpr().getArg(i), manager, true));
				break;
			default:
				return null;
			}
		} else if (def instanceof FieldSourceSinkDefinition) {
			// Check whether we need to taint the left side of the assignment
			FieldSourceSinkDefinition fieldDef = (FieldSourceSinkDefinition) def;
			if (sCallSite instanceof AssignStmt && fieldDef.getAccessPaths() != null) {
				AssignStmt assignStmt = (AssignStmt) sCallSite;
				for (AccessPathTuple apt : fieldDef.getAccessPaths())
					if (apt.getSourceSinkType().isSource())
						aps.add(apt.toAccessPath(assignStmt.getLeftOp(), manager, false));
			}
		} else if (def instanceof StatementSourceSinkDefinition) {
			StatementSourceSinkDefinition ssdef = (StatementSourceSinkDefinition) def;
			for (AccessPathTuple apt : ssdef.getAccessPaths())
				if (apt.getSourceSinkType().isSource())
					aps.add(apt.toAccessPath(ssdef.getLocal(), manager, true));
		}

		// If we don't have any information, we cannot continue
		if (aps.isEmpty())
			return null;

		return new SourceInfo(def, aps);
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath sourceAccessPath) {
		SourceSinkDefinition def = getSinkDefinition(sCallSite, manager, sourceAccessPath);
		if (def == null)
			return null;

		// If we have no precise information, we conservatively assume that
		// everything is tainted without looking at the access path. Only
		// exception: separate compilation assumption
		if (def.isEmpty() && sCallSite.containsInvokeExpr()) {
			if (SystemClassHandler.isTaintVisible(sourceAccessPath, sCallSite.getInvokeExpr().getMethod()))
				return new SinkInfo(def);
			else
				return null;
		}

		// If we are only checking whether this statement can be a sink in
		// general, we know this by now
		if (sourceAccessPath == null)
			return new SinkInfo(def);

		if (def instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
			if (methodDef.getCallType() == CallType.Return)
				return new SinkInfo(def);

			// Check whether the base object matches our definition
			if (sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr && methodDef.getBaseObjects() != null) {
				for (AccessPathTuple apt : methodDef.getBaseObjects())
					if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt))
						return new SinkInfo(def);
			}

			// Check whether a parameter matches our definition
			if (methodDef.getParameters() != null && methodDef.getParameters().length > 0) {
				// Get the tainted parameter index
				for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++)
					if (sCallSite.getInvokeExpr().getArg(i) == sourceAccessPath.getPlainValue()) {
						// Check whether we have a sink on that parameter
						if (methodDef.getParameters().length > i)
							for (AccessPathTuple apt : methodDef.getParameters()[i])
								if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt))
									return new SinkInfo(def);
					}
			}
		} else if (def instanceof FieldSourceSinkDefinition) {
			FieldSourceSinkDefinition fieldDef = (FieldSourceSinkDefinition) def;

			// Check whether we need to taint the right side of the assignment
			if (sCallSite instanceof AssignStmt && fieldDef.getAccessPaths() != null) {
				for (AccessPathTuple apt : fieldDef.getAccessPaths())
					if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt))
						return new SinkInfo(def);
			}
		} else if (def instanceof StatementSourceSinkDefinition) {
			StatementSourceSinkDefinition ssdef = (StatementSourceSinkDefinition) def;
			for (AccessPathTuple apt : ssdef.getAccessPaths())
				if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt))
					return new SinkInfo(def);
		}

		// No matching access path found
		return null;
	}

	/**
	 * Checks whether the given access path matches the given definition
	 * 
	 * @param sourceAccessPath
	 *            The access path to check
	 * @param apt
	 *            The definition against which to check the access path
	 * @return True if the given access path matches the given definition, otherwise
	 *         false
	 */
	private boolean accessPathMatches(AccessPath sourceAccessPath, AccessPathTuple apt) {
		// If the source or sink definitions does not specify any fields, it
		// always matches
		if (apt.getFields() == null || apt.getFields().length == 0 || sourceAccessPath == null)
			return true;

		for (int i = 0; i < apt.getFields().length; i++) {
			// If a.b.c.* is our defined sink and a.b is tainted, this is not a
			// leak. If a.b.* is tainted, it is.
			if (i >= sourceAccessPath.getFieldCount())
				return sourceAccessPath.getTaintSubFields();

			// Compare the fields
			if (!sourceAccessPath.getFields()[i].getName().equals(apt.getFields()[i]))
				return false;
		}
		return true;
	}

}