package soot.jimple.infoflow.android.source;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.IAccessPathBasedSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
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
	 * @param sources The list of source methods
	 * @param sinks   The list of sink methods
	 * @param config  The configuration of the data flow analyzer
	 */
	public AccessPathBasedSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks, InfoflowAndroidConfiguration config) {
		super(sources, sinks, config);
	}

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * strong matching, i.e. the methods in the code must exactly match those in the
	 * list.
	 * 
	 * @param sources         The list of source methods
	 * @param sinks           The list of sink methods
	 * @param callbackMethods The list of callback methods whose parameters are
	 *                        sources through which the application receives data
	 *                        from the operating system
	 * @param config          The configuration of the data flow analyzer
	 * @param layoutControls  A map from reference identifiers to the respective
	 *                        Android layout controls
	 */
	public AccessPathBasedSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks, Set<AndroidCallbackDefinition> callbackMethods,
			InfoflowAndroidConfiguration config, Map<Integer, AndroidLayoutControl> layoutControls) {
		super(sources, sinks, callbackMethods, config, layoutControls);
	}

	@Override
	protected Collection<Pair<AccessPath, ISourceSinkDefinition>> createSourceInfoPairs(Stmt sCallSite, InfoflowManager manager, Collection<ISourceSinkDefinition> defs) {
		HashSet<ISourceSinkDefinition> delegateToSuper = new HashSet<>();
		HashSet<Pair<AccessPath, ISourceSinkDefinition>> matchingDefs = new HashSet<>();
		for (ISourceSinkDefinition def : defs) {
			// We need to have access path data inside the source/sink definition
			if (!(def instanceof IAccessPathBasedSourceSinkDefinition)) {
				delegateToSuper.add(def);
				continue;
			}

			IAccessPathBasedSourceSinkDefinition apDef = (IAccessPathBasedSourceSinkDefinition) def;
			// If we don't have concrete access paths, we use the default implementation
			if (apDef.isEmpty()) {
				delegateToSuper.add(def);
				continue;
			}

			// We have real access path definitions, so we can construct precise
			// source information objects
			Set<AccessPath> aps = new HashSet<>();
			Set<AccessPathTuple> apTuples = new HashSet<>();

			if (def instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;

				// For parameters in callback methods, we need special handling
				switch (methodDef.getCallType()) {
					case Callback:
						if (sCallSite instanceof IdentityStmt) {
							IdentityStmt is = (IdentityStmt) sCallSite;
							if (is.getRightOp() instanceof ParameterRef) {
								ParameterRef paramRef = (ParameterRef) is.getRightOp();
								if (methodDef.getParameters() != null
										&& methodDef.getParameters().length > paramRef.getIndex()) {
									for (AccessPathTuple apt : methodDef.getParameters()[paramRef.getIndex()]) {
										aps.add(apt.toAccessPath(is.getLeftOp(), manager, false));
										apTuples.add(apt);
									}
								}
							}
						}
						break;
					case MethodCall:
						// Check whether we need to taint the base object
						if (sCallSite instanceof InvokeStmt && sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr
								&& methodDef.getBaseObjects() != null) {
							Value baseVal = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
							for (AccessPathTuple apt : methodDef.getBaseObjects()) {
								if (apt.getSourceSinkType().isSource()) {
									aps.add(apt.toAccessPath(baseVal, manager, true));
									apTuples.add(apt);
								}
							}
						}

						// Check whether we need to taint the return object
						if (sCallSite instanceof DefinitionStmt && methodDef.getReturnValues() != null) {
							Value returnVal = ((DefinitionStmt) sCallSite).getLeftOp();
							for (AccessPathTuple apt : methodDef.getReturnValues()) {
								if (apt.getSourceSinkType().isSource()) {
									aps.add(apt.toAccessPath(returnVal, manager, false));
									apTuples.add(apt);
								}
							}
						}

						// Check whether we need to taint parameters
						if (sCallSite.containsInvokeExpr() && methodDef.getParameters() != null
								&& methodDef.getParameters().length > 0)
							for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++) {
								if (methodDef.getParameters().length > i) {
									for (AccessPathTuple apt : methodDef.getParameters()[i]) {
										if (apt.getSourceSinkType().isSource()) {
											aps.add(apt.toAccessPath(sCallSite.getInvokeExpr().getArg(i), manager, true));
											apTuples.add(apt);
										}
									}
								}
							}
						break;
					default:
						return null;
				}
			} else if (def instanceof FieldSourceSinkDefinition) {
				// Check whether we need to taint the left side of the assignment
				FieldSourceSinkDefinition fieldDef = (FieldSourceSinkDefinition) def;
				if (sCallSite instanceof AssignStmt && fieldDef.getAccessPaths() != null) {
					AssignStmt assignStmt = (AssignStmt) sCallSite;
					for (AccessPathTuple apt : fieldDef.getAccessPaths()) {
						if (apt.getSourceSinkType().isSource()) {
							aps.add(apt.toAccessPath(assignStmt.getLeftOp(), manager, false));
							apTuples.add(apt);
						}
					}
				}
			} else if (def instanceof StatementSourceSinkDefinition) {
				StatementSourceSinkDefinition ssdef = (StatementSourceSinkDefinition) def;
				if (ssdef.getAccessPaths() != null) {
					for (AccessPathTuple apt : ssdef.getAccessPaths()) {
						if (apt.getSourceSinkType().isSource()) {
							aps.add(apt.toAccessPath(ssdef.getLocal(), manager, true));
							apTuples.add(apt);
						}
					}
				}
			}
			// If we don't have any information, we cannot continue
			if (aps.isEmpty())
				return Collections.emptySet();

			apDef = apDef.filter(apTuples);
			for (AccessPath ap : aps) {
				matchingDefs.add(new Pair<>(ap, apDef));
			}
		}
		matchingDefs.addAll(super.createSourceInfoPairs(sCallSite, manager, delegateToSuper));

		return matchingDefs;
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath sourceAccessPath) {
		Collection<ISourceSinkDefinition> defs = getSinkDefinitions(sCallSite, manager, sourceAccessPath);
		HashSet<ISourceSinkDefinition> sinkDefs = new HashSet<>();
		for (ISourceSinkDefinition def : defs) {
			// We need the access paths
			if (!(def instanceof IAccessPathBasedSourceSinkDefinition)) {
				SinkInfo superSinkInfo = super.getSinkInfo(sCallSite, manager, sourceAccessPath);
				// Note: we lose the user data here
				sinkDefs.addAll(superSinkInfo.getDefinitions());
				continue;
			}

			IAccessPathBasedSourceSinkDefinition apDef = (IAccessPathBasedSourceSinkDefinition) def;
			// If we have no precise information, we conservatively assume that
			// everything is tainted without looking at the access path. Only
			// exception: separate compilation assumption
			if (apDef.isEmpty() && sCallSite.containsInvokeExpr()) {
				if (SystemClassHandler.v().isTaintVisible(sourceAccessPath, sCallSite.getInvokeExpr().getMethod()))
					sinkDefs.add(def);
				continue;
			}

			// If we are only checking whether this statement can be a sink in
			// general, we know this by now
			if (sourceAccessPath == null) {
				sinkDefs.add(def);
				continue;
			}

			if (def instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
				if (methodDef.getCallType() == CallType.Return) {
					sinkDefs.add(def);
					continue;
				}

				// Check whether the base object matches our definition
				InvokeExpr iexpr = sCallSite.getInvokeExpr();
				if (iexpr instanceof InstanceInvokeExpr && methodDef.getBaseObjects() != null) {
					InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) iexpr;
					if (iiexpr.getBase() == sourceAccessPath.getPlainValue()) {
						boolean addedDef = false;
						for (AccessPathTuple apt : methodDef.getBaseObjects())
							if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt)) {
								sinkDefs.add(apDef.filter(Collections.singleton(apt)));
								addedDef = true;
								break;
							}
						if (addedDef)
							continue;
					}
				}

				// Check whether a parameter matches our definition
				if (methodDef.getParameters() != null && methodDef.getParameters().length > 0) {
					boolean addedDef = false;
					// Get the tainted parameter index
					for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++)
						if (sCallSite.getInvokeExpr().getArg(i) == sourceAccessPath.getPlainValue()) {
							// Check whether we have a sink on that parameter
							if (methodDef.getParameters().length > i)
								for (AccessPathTuple apt : methodDef.getParameters()[i])
									if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt)) {
										sinkDefs.add(apDef.filter(Collections.singleton(apt)));
										addedDef = true;
										break;
									}
						}
					if (addedDef)
						continue;
				}
			} else if (def instanceof FieldSourceSinkDefinition) {
				FieldSourceSinkDefinition fieldDef = (FieldSourceSinkDefinition) def;

				// Check whether we need to taint the right side of the assignment
				if (sCallSite instanceof AssignStmt && fieldDef.getAccessPaths() != null) {
					boolean addedDef = false;
					for (AccessPathTuple apt : fieldDef.getAccessPaths())
						if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt)) {
							sinkDefs.add(apDef.filter(Collections.singleton(apt)));
							addedDef = true;
							break;
						}
					if (addedDef)
						continue;
				}
			} else if (def instanceof StatementSourceSinkDefinition) {
				StatementSourceSinkDefinition ssdef = (StatementSourceSinkDefinition) def;
				boolean addedDef = false;
				for (AccessPathTuple apt : ssdef.getAccessPaths())
					if (apt.getSourceSinkType().isSink() && accessPathMatches(sourceAccessPath, apt)) {
						sinkDefs.add(apDef.filter(Collections.singleton(apt)));
						addedDef = true;
						break;
					}
				if (addedDef)
					continue;
			}
		}

		return sinkDefs.size() > 0 ? new SinkInfo(sinkDefs) : null;
	}

	/**
	 * Checks whether the given access path matches the given definition
	 * 
	 * @param sourceAccessPath The access path to check
	 * @param apt              The definition against which to check the access path
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
			if (i >= sourceAccessPath.getFragmentCount())
				return sourceAccessPath.getTaintSubFields();

			// Compare the fields
			if (!sourceAccessPath.getFragments()[i].getField().getName().equals(apt.getFields()[i]))
				return false;
		}
		return true;
	}

	@Override
	public SourceInfo getInverseSinkInfo(Stmt sCallSite, InfoflowManager manager) {
		Collection<ISourceSinkDefinition> defs = getInverseSinkDefinition(sCallSite, manager.getICFG());

		Collection<ISourceSinkDefinition> delegateToSuper = new HashSet<>();
		Collection<Pair<AccessPath, ISourceSinkDefinition>> matchingDefs = new HashSet<>();
		for (ISourceSinkDefinition def : defs) {
			// We need the access paths
			if (!(def instanceof IAccessPathBasedSourceSinkDefinition)) {
				delegateToSuper.add(def);
				continue;
			}

			IAccessPathBasedSourceSinkDefinition apDef = (IAccessPathBasedSourceSinkDefinition) def;
			// exception: separate compilation assumption
			// If we don't have concrete access paths, we use the default implementation
			if (apDef.isEmpty()) {
				delegateToSuper.add(def);
				continue;
			}

			if (def instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
				if (methodDef.getCallType() == CallType.Return) {
					Set<AccessPath> aps = new HashSet<>();
					Set<AccessPathTuple> apts = new HashSet<>();
					for (SootMethod dest : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
						if (!dest.hasActiveBody())
							continue;

						for (Unit unit : dest.getActiveBody().getUnits()) {
							if (!(unit instanceof ReturnStmt))
								continue;

							for (AccessPathTuple apt : methodDef.getReturnValues()) {
								if (apt.getSourceSinkType().isSink()) {
									aps.add(apt.toAccessPath(((ReturnStmt) unit).getOp(), manager, false));
									apts.add(apt);
								}
							}
						}
					}
					methodDef = methodDef.filter(apts);
					for (AccessPath ap : aps)
						matchingDefs.add(new Pair<>(ap, methodDef));
				}
				// Check whether the base object matches our definition
				else if (sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr && methodDef.getBaseObjects() != null) {
					for (AccessPathTuple apt : methodDef.getBaseObjects()) {
						if (apt.getSourceSinkType().isSink()) {
							AccessPath ap = apt.toAccessPath(((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase(), manager, true);
							matchingDefs.add(new Pair<>(ap, def));
							break;
						}
					}
				}
				// Check whether a parameter matches our definition
				else if (methodDef.getParameters() != null && methodDef.getParameters().length > 0) {
					Set<AccessPath> aps = new HashSet<>();
					Set<AccessPathTuple> apts = new HashSet<>();
					// Get the tainted parameter index
					for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++) {
						if (sCallSite.getInvokeExpr().getArg(i) instanceof Constant)
							continue;

						if (methodDef.getParameters().length > i) {
							for (AccessPathTuple apt : methodDef.getParameters()[i]) {
								if (apt.getSourceSinkType().isSink()) {
									AccessPath ap = apt.toAccessPath(sCallSite.getInvokeExpr().getArg(i), manager, true);
									aps.add(ap);
									apts.add(apt);
								}
							}
						}
					}
					methodDef = methodDef.filter(apts);
					for (AccessPath ap : aps)
						matchingDefs.add(new Pair<>(ap, methodDef));
				}
			} else if (def instanceof FieldSourceSinkDefinition) {
				FieldSourceSinkDefinition fieldDef = (FieldSourceSinkDefinition) def;
				Set<AccessPath> aps = new HashSet<>();
				Set<AccessPathTuple> apts = new HashSet<>();

				// Check whether we need to taint the right side of the assignment
				if (sCallSite instanceof AssignStmt && fieldDef.getAccessPaths() != null) {
					for (AccessPathTuple apt : fieldDef.getAccessPaths())
						if (apt.getSourceSinkType().isSink()) {
							aps.add(apt.toAccessPath(sCallSite.getFieldRef(), manager, false));
						}
					fieldDef = fieldDef.filter(apts);
					for (AccessPath ap : aps)
						matchingDefs.add(new Pair<>(ap, fieldDef));
				}
			} else if (def instanceof StatementSourceSinkDefinition) {
				StatementSourceSinkDefinition ssdef = (StatementSourceSinkDefinition) def;
				Set<AccessPath> aps = new HashSet<>();
				Set<AccessPathTuple> apsTuple = new HashSet<>();
				for (AccessPathTuple apt : ssdef.getAccessPaths()) {
					if (apt.getSourceSinkType().isSink()) {
						apsTuple.add(apt);
						aps.add(apt.toAccessPath(sCallSite.getFieldRef(), manager, true));
					}
					ssdef = ssdef.filter(apsTuple);
					for (AccessPath ap : aps)
						matchingDefs.add(new Pair<>(ap, ssdef));
				}
			}
		}
		matchingDefs.addAll(super.createInverseSinkInfoPairs(sCallSite, manager, delegateToSuper));

		// No matching access path found
		return matchingDefs.size() > 0 ? new SourceInfo(matchingDefs) : null;
	}

	@Override
	public SinkInfo getInverseSourceInfo(Stmt sCallSite, InfoflowManager manager, AccessPath sourceAccessPath) {
		Collection<ISourceSinkDefinition> defs = getInverseSourceDefinition(sCallSite, manager, sourceAccessPath);

		Collection<ISourceSinkDefinition> matching = new HashSet<>();
		for (ISourceSinkDefinition def : defs) {
			// We need to have access path data inside the source/sink definition
			if (!(def instanceof IAccessPathBasedSourceSinkDefinition)) {
				matching.addAll(super.getInverseSourceDefinition(sCallSite, manager, sourceAccessPath));
				continue;
			}

			IAccessPathBasedSourceSinkDefinition apDef = (IAccessPathBasedSourceSinkDefinition) def;
			if (apDef.isEmpty() && sCallSite.containsInvokeExpr()) {
				if (SystemClassHandler.v().isTaintVisible(sourceAccessPath, sCallSite.getInvokeExpr().getMethod()))
					matching.add(def);
				continue;
			}

			// If we don't have concrete access paths, we use the default implementation
			if (apDef.isEmpty()) {
				matching.addAll(super.getInverseSourceDefinition(sCallSite, manager, sourceAccessPath));
				continue;
			}

			// If we are only checking whether this statement can be a sink in
			// general, we know this by now
			if (sourceAccessPath == null) {
				matching.add(def);
				continue;
			}

			// We have real access path definitions, so we can construct precise
			// source information objects
			Set<AccessPath> aps = new HashSet<>();
			Set<AccessPathTuple> apTuples = new HashSet<>();

			if (def instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;

				// For parameters in callback methods, we need special handling
				switch (methodDef.getCallType()) {
					case Callback:
						if (sCallSite instanceof IdentityStmt) {
							IdentityStmt is = (IdentityStmt) sCallSite;
							if (is.getRightOp() instanceof ParameterRef) {
								ParameterRef paramRef = (ParameterRef) is.getRightOp();
								if (methodDef.getParameters() != null
										&& methodDef.getParameters().length > paramRef.getIndex()) {
									for (AccessPathTuple apt : methodDef.getParameters()[paramRef.getIndex()]) {
										AccessPath ap = apt.toAccessPath(is.getLeftOp(), manager, false);
										if (accessPathMatches(sourceAccessPath, apt)) {
											aps.add(ap);
											apTuples.add(apt);
										}
									}
								}
							}
						}
						break;
					case MethodCall:
						// Check whether we need to taint the base object
						if (sCallSite instanceof InvokeStmt && sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr
								&& methodDef.getBaseObjects() != null) {
							Value baseVal = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
							for (AccessPathTuple apt : methodDef.getBaseObjects()) {
								if (apt.getSourceSinkType().isSource()) {
									AccessPath ap = apt.toAccessPath(baseVal, manager, true);
									if (accessPathMatches(sourceAccessPath, apt)) {
										aps.add(ap);
										apTuples.add(apt);
									}
								}
							}
						}

						// Check whether we need to taint the return object
						if (sCallSite instanceof DefinitionStmt && methodDef.getReturnValues() != null) {
							Value returnVal = ((DefinitionStmt) sCallSite).getLeftOp();
							for (AccessPathTuple apt : methodDef.getReturnValues()) {
								if (apt.getSourceSinkType().isSource()) {
									AccessPath ap = apt.toAccessPath(returnVal, manager, false);
									if (accessPathMatches(sourceAccessPath, apt)) {
										aps.add(ap);
										apTuples.add(apt);
									}
								}
							}
						}

						// Check whether we need to taint parameters
						if (sCallSite.containsInvokeExpr() && methodDef.getParameters() != null
								&& methodDef.getParameters().length > 0)
							for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++) {
								if (methodDef.getParameters().length > i) {
									for (AccessPathTuple apt : methodDef.getParameters()[i]) {
										if (apt.getSourceSinkType().isSource()) {
											AccessPath ap = apt.toAccessPath(sCallSite.getInvokeExpr().getArg(i), manager, true);
											if (accessPathMatches(sourceAccessPath, apt)) {
												aps.add(ap);
												apTuples.add(apt);
											}
										}
									}
								}
							}
						break;
					default:
						return null;
				}
			} else if (def instanceof FieldSourceSinkDefinition) {
				// Check whether we need to taint the left side of the assignment
				FieldSourceSinkDefinition fieldDef = (FieldSourceSinkDefinition) def;
				if (sCallSite instanceof AssignStmt && fieldDef.getAccessPaths() != null) {
					AssignStmt assignStmt = (AssignStmt) sCallSite;
					for (AccessPathTuple apt : fieldDef.getAccessPaths()) {
						if (apt.getSourceSinkType().isSource()) {
							AccessPath ap = apt.toAccessPath(assignStmt.getLeftOp(), manager, false);
							if (accessPathMatches(sourceAccessPath, apt)) {
								aps.add(ap);
								apTuples.add(apt);
							}
						}
					}
				}
			} else if (def instanceof StatementSourceSinkDefinition) {
				StatementSourceSinkDefinition ssdef = (StatementSourceSinkDefinition) def;
				for (AccessPathTuple apt : ssdef.getAccessPaths()) {
					if (apt.getSourceSinkType().isSource()) {
						AccessPath ap = apt.toAccessPath(ssdef.getLocal(), manager, true);
						if (accessPathMatches(sourceAccessPath, apt)) {
							aps.add(ap);
							apTuples.add(apt);
						}
					}
				}
			}

			// If we don't have any information, we cannot continue
			if (aps.isEmpty())
				return null;

			matching.add(apDef.filter(apTuples));
		}

		return matching.size() > 0 ? new SinkInfo(matching) : null;
	}
}