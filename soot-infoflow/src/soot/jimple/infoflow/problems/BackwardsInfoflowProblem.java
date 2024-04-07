package soot.jimple.infoflow.problems;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.KillAll;
import soot.ArrayType;
import soot.Local;
import soot.NullType;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.callmappers.CallerCalleeManager;
import soot.jimple.infoflow.callmappers.ICallerCalleeArgumentMapper;
import soot.jimple.infoflow.cfg.FlowDroidSinkStatement;
import soot.jimple.infoflow.cfg.FlowDroidSourceStatement;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Class which contains the flow functions for the backwards analysis. Not to be
 * confused with the AliasProblem, which is used for finding aliases.
 *
 * @author Tim Lange
 */
public class BackwardsInfoflowProblem extends AbstractInfoflowProblem {

	public BackwardsInfoflowProblem(InfoflowManager manager, Abstraction zeroValue,
			IPropagationRuleManagerFactory ruleManagerFactory) {
		super(manager, zeroValue, ruleManagerFactory);
	}

	@Override
	protected FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Abstraction, SootMethod>() {
			@Override
			public FlowFunction<Abstraction> getNormalFlowFunction(Unit srcUnit, Unit destUnit) {
				if (!(srcUnit instanceof Stmt))
					return KillAll.v();

				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return KillAll.v();

				return new SolverNormalFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(srcUnit, source, manager,
									TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

						Set<Abstraction> res = computeTargetsInternal(d1, source);
						return notifyOutFlowHandlers(srcUnit, d1, source, res,
								TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						Set<Abstraction> res = null;
						ByReferenceBoolean killSource = new ByReferenceBoolean();
						ByReferenceBoolean killAll = new ByReferenceBoolean();
						// If we have a RuleManager, apply the rules
						if (propagationRules != null) {
							res = propagationRules.applyNormalFlowFunction(d1, source, (Stmt) srcUnit, (Stmt) destUnit,
									killSource, killAll);
						}
						// On killAll, we do not propagate anything and can stop here
						if (killAll.value)
							return null;

						// Instanciate res in case the RuleManager did return null
						if (res == null)
							res = new HashSet<>();

						// Shortcut: propagate implicit taint over the statement.
						if (source.getAccessPath().isEmpty()) {
							if (killSource.value)
								res.remove(source);
							return res;
						}

						if (!(srcUnit instanceof AssignStmt))
							return res;

						final AssignStmt assignStmt = (AssignStmt) srcUnit;
						// left can not be an expr
						final Value leftVal = assignStmt.getLeftOp();
						final Value rightOp = assignStmt.getRightOp();
						final Value[] rightVals = BaseSelector.selectBaseList(rightOp, true);

						AccessPath ap = source.getAccessPath();
						Local sourceBase = ap.getPlainValue();
						boolean keepSource = false;
						// Statements such as c = a + b with the taint c can produce multiple taints
						// because we can not
						// decide which one originated from a source at this point.
						for (Value rightVal : rightVals) {
							boolean addLeftValue = false;
							boolean cutFirstFieldLeft = false;
							boolean createNewVal = false;
							Type leftType = null;

							if (rightVal instanceof StaticFieldRef) {
								StaticFieldRef staticRef = (StaticFieldRef) rightVal;

								AccessPath mappedAp = aliasing.mayAlias(ap, staticRef);
								if (manager.getConfig()
										.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
										&& mappedAp != null) {
									addLeftValue = true;
									cutFirstFieldLeft = true;
									if (!mappedAp.equals(ap)) {
										ap = mappedAp;
										source = source.deriveNewAbstraction(ap, null);
									}
								}
							} else if (rightVal instanceof InstanceFieldRef) {
								InstanceFieldRef instRef = (InstanceFieldRef) rightVal;

								// Kill the taint if o = null
								if (instRef.getBase().getType() instanceof NullType)
									return null;

								AccessPath mappedAp = aliasing.mayAlias(ap, instRef);
								// field ref match
								if (mappedAp != null) {
									addLeftValue = true;
									// $stack1 = o.x with t=o.x -> T={$stack1}.
									cutFirstFieldLeft = (mappedAp.getFragmentCount() > 0
											&& mappedAp.getFirstField() == instRef.getField());
									// We can't really get more precise typewise
									// leftType = leftVal.getType();
									if (!mappedAp.equals(ap)) {
										ap = mappedAp;
										// source = source.deriveNewAbstraction(ap, null);
									}
								}
								// whole object tainted
								else if (aliasing.mayAlias(instRef.getBase(), sourceBase) && ap.getTaintSubFields()
										&& ap.getFragmentCount() == 0) {
									// $stack1 = o.x with t=o.* -> T={$stack1}.
									addLeftValue = true;
									createNewVal = true;
									// leftType = leftVal.getType();
								}
							} else if (rightVal instanceof ArrayRef) {
								if (!getManager().getConfig().getEnableArrayTracking()
										|| ap.getArrayTaintType() == AccessPath.ArrayTaintType.Length)
									continue;

								ArrayRef arrayRef = (ArrayRef) rightVal;
								// do we track indices...
								if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
									if (arrayRef.getIndex() == sourceBase) {
										addLeftValue = true;
										leftType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
									}
								}
								// ...or only the whole array?
								else if (aliasing.mayAlias(arrayRef.getBase(), sourceBase)) {
									addLeftValue = true;
									leftType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
								}
							}
							if (rightVal == sourceBase) {
								addLeftValue = true;
								leftType = ap.getBaseType();

								if (leftVal instanceof ArrayRef) {
									ArrayRef arrayRef = (ArrayRef) leftVal;
									leftType = TypeUtils.buildArrayOrAddDimension(leftType,
											arrayRef.getType().getArrayType());
								} else if (rightOp instanceof InstanceOfExpr) {
									createNewVal = true;
								} else if (rightOp instanceof LengthExpr) {
									if (ap.getArrayTaintType() == AccessPath.ArrayTaintType.Contents)
										addLeftValue = false;
									createNewVal = true;
								} else if (rightOp instanceof NewArrayExpr) {
									createNewVal = true;
								} else {
									if (!manager.getTypeUtils().checkCast(source.getAccessPath(), leftVal.getType()))
										return null;
								}

								if (rightVal instanceof CastExpr) {
									CastExpr ce = (CastExpr) rightOp;
									if (!manager.getHierarchy().canStoreType(leftType, ce.getCastType()))
										leftType = ce.getCastType();
								}
							}

							if (addLeftValue) {
								AccessPath newAp;
								if (createNewVal)
									newAp = manager.getAccessPathFactory().createAccessPath(leftVal, true);
								else
									newAp = manager.getAccessPathFactory().copyWithNewValue(ap, leftVal, leftType,
											cutFirstFieldLeft);
								Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);

								if (newAbs != null) {
									if (aliasing.canHaveAliasesRightSide(assignStmt, leftVal, newAbs)) {
										for (Unit pred : manager.getICFG().getPredsOf(srcUnit))
											aliasing.computeAliases(d1, (Stmt) pred, leftVal,
													Collections.singleton(newAbs),
													interproceduralCFG().getMethodOf(pred), newAbs);
									}
								}
							}

							boolean addRightValue = false;
							boolean cutFirstField = false;
							Type rightType = null;

							// S.x
							if (leftVal instanceof StaticFieldRef) {
								StaticFieldRef staticRef = (StaticFieldRef) leftVal;

								AccessPath mappedAp = aliasing.mayAlias(ap, staticRef);
								// If we do not track statics, just skip this rightVal
								if (getManager().getConfig()
										.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
										&& mappedAp != null) {
									addRightValue = true;
									cutFirstField = true;
									rightType = mappedAp.getFirstFieldType();
									if (!mappedAp.equals(ap)) {
										ap = mappedAp;
										source = source.deriveNewAbstraction(ap, null);
									}
								}
							}
							// o.x
							else if (leftVal instanceof InstanceFieldRef) {
								InstanceFieldRef instRef = ((InstanceFieldRef) leftVal);

								// Kill the taint if o = null
								if (instRef.getBase().getType() instanceof NullType)
									return null;

								AccessPath mappedAp = aliasing.mayAlias(ap, instRef);
								// field reference match
								if (mappedAp != null) {
									addRightValue = true;
									// o.x = $stack1 with t=o.x -> T={$stack1}. Without it would be $stack1.x.
									cutFirstField = (mappedAp.getFragmentCount() > 0
											&& mappedAp.getFirstField() == instRef.getField());
									// If there was a path expansion (cutFirstField = false), we can not
									// precise the type using the left field
									rightType = mappedAp.getFirstFieldType();
									if (!mappedAp.equals(ap)) {
										ap = mappedAp;
										source = source.deriveNewAbstraction(ap, null);
									}
								}
								// whole object tainted
								else if (aliasing.mayAlias(instRef.getBase(), sourceBase) && ap.getTaintSubFields()
										&& ap.getFragmentCount() == 0) {
									// o.x = $stack1 with t=o.* -> T={$stack1}. No cut as source has no fields.
									addRightValue = true;
									rightType = instRef.getField().getType();
									// Because the whole object is tainted, we can not kill our source
									// as we do not know in which field the tainted value lies.
									keepSource = true;
								}
							} else if (leftVal instanceof ArrayRef) {
								// If we don't track arrays or just the length is tainted we have nothing to do.
								if (!getManager().getConfig().getEnableArrayTracking()
										|| ap.getArrayTaintType() == AccessPath.ArrayTaintType.Length)
									continue;

								ArrayRef arrayRef = (ArrayRef) leftVal;
								// do we track indices...
								if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
									if (arrayRef.getIndex() == sourceBase) {
										addRightValue = true;
										rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
									}
								}
								// ...or only the whole array?
								else if (aliasing.mayAlias(arrayRef.getBase(), sourceBase)) {
									addRightValue = true;
									rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
									// We don't track indices => we don't know if the tainted value was at this
									// index
									keepSource = true;
								}
							}
							// default case
							else if (aliasing.mayAlias(leftVal, sourceBase)) {
								if (rightOp instanceof InstanceOfExpr) {
									// Left side is a boolean but the resulting taint
									// needs to be the object type
									rightType = rightVal.getType();
								} else if (rightOp instanceof CastExpr) {
									// CastExpr only make the types more imprecise backwards
									// but we need to kill impossible casts
									CastExpr ce = (CastExpr) rightOp;
									if (!manager.getTypeUtils().checkCast(ce.getCastType(), rightVal.getType()))
										return null;
								}

								// LengthExpr/RHS ArrayRef and NewArrayExpr handled in ArrayPropagationRule.
								// We allow NewArrayExpr to pass for removing the source but not creating a new
								// taint below.
								if (rightOp instanceof InstanceOfExpr
										&& manager.getConfig().getEnableInstanceOfTainting())
									addRightValue = true;
								else
									addRightValue = !(rightOp instanceof LengthExpr || rightOp instanceof ArrayRef
											|| rightOp instanceof InstanceOfExpr);
							}

							if (addRightValue) {
								// keepSource is true if
								// ... the whole object is tainted
								// ... the left side is an ArrayRef
								if (!keepSource)
									res.remove(source);

								boolean isImplicit = source.getDominator() != null;
								if (isImplicit)
									res.add(source.deriveConditionalUpdate(assignStmt));

								if (rightVal instanceof Constant)
									continue;

								// NewExpr's can not be tainted
								// so we can stop here
								if (rightOp instanceof AnyNewExpr)
									continue;

								AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(ap, rightVal,
										rightType, cutFirstField);
								Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);
								if (newAbs != null) {
									if (rightVal instanceof StaticFieldRef && manager.getConfig()
											.getStaticFieldTrackingMode() == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
										manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
									else {
										enterConditional(newAbs, assignStmt, destUnit);

										if (isPrimitiveOrStringBase(source)) {
											newAbs = newAbs.deriveNewAbstractionWithTurnUnit(srcUnit);
										} else if (leftVal instanceof FieldRef
												&& isPrimitiveOrStringType(((FieldRef) leftVal).getField().getType())
												&& !ap.getCanHaveImmutableAliases()) {
											newAbs = newAbs.deriveNewAbstractionWithTurnUnit(srcUnit);
										} else {
											if (aliasing.canHaveAliasesRightSide(assignStmt, rightVal, newAbs)) {
												for (Unit pred : manager.getICFG().getPredsOf(assignStmt))
													aliasing.computeAliases(d1, (Stmt) pred, rightVal, res,
															interproceduralCFG().getMethodOf(pred), newAbs);
											}
										}
										res.add(newAbs);
									}
								}
							}
						}

						return res;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getCallFlowFunction(final Unit callStmt, final SootMethod dest) {
				if (!dest.hasActiveBody()) {
					logger.debug("Call skipped because target has no body: {} -> {}", callStmt, dest);
					return KillAll.v();
				}

				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return KillAll.v();

				if (!(callStmt instanceof Stmt))
					return KillAll.v();

				final Stmt stmt = (Stmt) callStmt;
				final InvokeExpr ie = stmt.containsInvokeExpr() ? stmt.getInvokeExpr() : null;

				final Local[] paramLocals = dest.getActiveBody().getParameterLocals().toArray(new Local[0]);
				final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();

				final boolean isSink = stmt.hasTag(FlowDroidSinkStatement.TAG_NAME);
				final boolean isSource = stmt.hasTag(FlowDroidSourceStatement.TAG_NAME);

				final ICallerCalleeArgumentMapper mapper = CallerCalleeManager.getMapper(manager, stmt, dest);
				final boolean isReflectiveCallSite = mapper != null ? mapper.isReflectiveMapper() : false;

				return new SolverCallFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (source == getZeroValue())
							return null;

						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(stmt, source, manager,
									TaintPropagationHandler.FlowFunctionType.CallFlowFunction);

						Set<Abstraction> res = computeTargetsInternal(d1, source);
						if (res != null) {
							for (Abstraction abs : res)
								aliasing.getAliasingStrategy().injectCallingContext(abs, solver, dest, callStmt, source,
										d1);
						}
						return notifyOutFlowHandlers(stmt, d1, source, res,
								TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						// Respect user settings
						if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
							return null;
						if (!manager.getConfig().getInspectSources() && isSource)
							return null;
						if (!manager.getConfig().getInspectSinks() && isSink)
							return null;
						if (manager.getConfig()
								.getStaticFieldTrackingMode() == InfoflowConfiguration.StaticFieldTrackingMode.None
								&& dest.isStaticInitializer())
							return null;

						// Do not propagate into Soot library classes if that optimization is enabled
						// CallToReturn handles the propagation over the excluded statement
						if (isExcluded(dest))
							return null;

						// not used static fields do not need to be propagated
						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef()) {
							// static fields first get pushed onto the stack before used,
							// so we check for a read on the base class
							if (!(interproceduralCFG().isStaticFieldUsed(dest, source.getAccessPath().getFirstField())
									|| interproceduralCFG().isStaticFieldRead(dest,
											source.getAccessPath().getFirstField())))
								return null;
						}

						Set<Abstraction> res = null;
						ByReferenceBoolean killAll = new ByReferenceBoolean();
						if (propagationRules != null)
							res = propagationRules.applyCallFlowFunction(d1, source, stmt, dest, killAll);
						if (killAll.value)
							return null;

						// Instanciate in case RuleManager did not produce an object
						if (res == null)
							res = new HashSet<>();

						// x = o.m(a1, ..., an)
						// Taints the return if needed
						if (callStmt instanceof AssignStmt) {
							AssignStmt assignStmt = (AssignStmt) callStmt;
							Value left = assignStmt.getLeftOp();

							boolean isImplicit = source.getDominator() != null;

							// we only taint the return statement(s) if x is tainted
							if (aliasing.mayAlias(left, source.getAccessPath().getPlainValue()) && !isImplicit) {
								for (Unit unit : dest.getActiveBody().getUnits()) {
									if (unit instanceof ReturnStmt) {
										ReturnStmt returnStmt = (ReturnStmt) unit;
										Value retVal = returnStmt.getOp();
										if (retVal instanceof Local) {
											// if types are incompatible, stop here
											if (!manager.getTypeUtils().checkCast(source.getAccessPath().getBaseType(),
													retVal.getType()))
												continue;

											AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
													source.getAccessPath(), retVal, returnStmt.getOp().getType(),
													false);
											Abstraction abs = source.deriveNewAbstraction(ap, stmt);
											if (abs != null) {
												if (isPrimitiveOrStringBase(source))
													abs = abs.deriveNewAbstractionWithTurnUnit(stmt);

												if (abs.getDominator() == null && manager.getConfig()
														.getImplicitFlowMode().trackControlFlowDependencies()) {
													List<Unit> condUnits = manager.getICFG()
															.getConditionalBranchIntraprocedural(returnStmt);
													if (condUnits.size() >= 1) {
														abs.setDominator(condUnits.get(0));
														for (int i = 1; i < condUnits.size(); i++)
															res.add(abs.deriveNewAbstractionWithDominator(
																	condUnits.get(i)));
													}
												}

												res.add(abs);
											}
										}
									}
								}
							}
						}

						// static fields access path stay the same
						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef()) {
							Abstraction abs = source.deriveNewAbstraction(source.getAccessPath(), stmt);
							if (abs != null)
								res.add(abs);
						}

						// o.m(a1, ..., an)
						// map o.f to this.f
						if (!source.getAccessPath().isStaticFieldRef() && !dest.isStatic()) {
							Value callBase = mapper.getCallerValueOfCalleeParameter(ie,
									ICallerCalleeArgumentMapper.BASE_OBJECT);

							if (callBase != null) {
								Value sourceBase = source.getAccessPath().getPlainValue();
								if (aliasing.mayAlias(callBase, sourceBase) && manager.getTypeUtils()
										.hasCompatibleTypesForCall(source.getAccessPath(), dest.getDeclaringClass())) {
									// second condition prevents mapping o if it is also a parameter
									if (isReflectiveCallSite || !hasAnotherReferenceOnBase(ie, sourceBase,
											mapper.getCallerIndexOfCalleeParameter(
													ICallerCalleeArgumentMapper.BASE_OBJECT))) {
										AccessPath ap = manager.getAccessPathFactory()
												.copyWithNewValue(source.getAccessPath(), thisLocal);
										Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) callStmt);
										if (abs != null)
											res.add(abs);
									}
								}
							}
						}

						// map arguments to parameter
						if (ie != null && dest.getParameterCount() > 0) {
							for (int i = 0; i < ie.getArgCount(); i++) {
								if (!aliasing.mayAlias(ie.getArg(i), source.getAccessPath().getPlainValue()))
									continue;
								if (isPrimitiveOrStringBase(source))
									continue;
								if (!source.getAccessPath().getTaintSubFields())
									continue;
								int calleeIndex = mapper.getCalleeIndexOfCallerParameter(i);
								if (calleeIndex == ICallerCalleeArgumentMapper.UNKNOWN)
									continue;

								// If the variable was overwritten
								// somewehere in the callee, we assume
								// it to overwritten on all paths (yeah,
								// I know ...) Otherwise, we need SSA
								// or lots of bookkeeping to avoid FPs
								// (BytecodeTests.flowSensitivityTest1).
								if (!isReflectiveCallSite
										&& interproceduralCFG().methodWritesValue(dest, paramLocals[calleeIndex]))
									continue;

								if (calleeIndex == ICallerCalleeArgumentMapper.ALL_PARAMS) {
									// taint all parameters if the arg array of an reflective
									// call site is tainted
									for (Value param : paramLocals) {
										AccessPath ap = manager.getAccessPathFactory()
												.copyWithNewValue(source.getAccessPath(), param, null, false);
										Abstraction abs = source.deriveNewAbstraction(ap, stmt);
										if (abs != null)
											res.add(abs);
									}
								} else {
									// taint just the tainted parameter
									AccessPath ap = manager.getAccessPathFactory()
											.copyWithNewValue(source.getAccessPath(), paramLocals[i]);
									Abstraction abs = source.deriveNewAbstraction(ap, stmt);
									if (abs != null) {
										res.add(abs);
									}
								}
							}
						}

						// Sometimes callers have more arguments than the callee parameters, e.g.
						// because one argument is resolved in native code. A concrete example is
						// sendMessageDelayed(android.os.Message, int)
						// -> handleMessage(android.os.Message message)
						// TODO: handle argument/parameter mismatch for some special cases

						return res;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getReturnFlowFunction(Unit callSite, SootMethod callee, Unit exitSite,
					Unit returnSite) {
				if (callSite != null && !(callSite instanceof Stmt))
					return KillAll.v();

				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return KillAll.v();

				final Value[] paramLocals = new Value[callee.getParameterCount()];
				for (int i = 0; i < callee.getParameterCount(); i++)
					paramLocals[i] = callee.getActiveBody().getParameterLocal(i);

				final Stmt stmt = (Stmt) callSite;
				final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr()) ? stmt.getInvokeExpr() : null;
				final Stmt callStmt = (Stmt) callSite;
				final Stmt exitStmt = (Stmt) exitSite;

				final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
				final ICallerCalleeArgumentMapper mapper = CallerCalleeManager.getMapper(manager, stmt, callee);
				final boolean isReflectiveCallSite = mapper != null ? mapper.isReflectiveMapper() : false;

				return new SolverReturnFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction source, Abstraction calleeD1,
							Collection<Abstraction> callerD1s) {
						if (source == getZeroValue())
							return null;
						if (callSite == null)
							return null;

						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(stmt, source, manager,
									TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);

						Set<Abstraction> res = computeTargetsInternal(source, calleeD1, callerD1s);
						return notifyOutFlowHandlers(exitSite, calleeD1, source, res,
								TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction source, Abstraction calleeD1,
							Collection<Abstraction> callerD1s) {
						if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
							return null;

						Set<Abstraction> res = null;
						ByReferenceBoolean killAll = new ByReferenceBoolean();
						if (propagationRules != null)
							res = propagationRules.applyReturnFlowFunction(callerD1s, calleeD1, source, (Stmt) exitSite,
									(Stmt) returnSite, (Stmt) callSite, killAll);
						if (killAll.value)
							return null;

						// Already handled in the rule
						if (source.getAccessPath().isEmpty())
							return res;

						if (res == null)
							res = new HashSet<>();

						// Static fields get propagated unchanged
						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef()) {
							res.add(source);
						}

						// o.m(a1, ..., an)
						// map o.f to this.f
						if (!callee.isStatic()) {
							Value sourceBase = source.getAccessPath().getPlainValue();
							if (aliasing.mayAlias(thisLocal, sourceBase) && manager.getTypeUtils()
									.hasCompatibleTypesForCall(source.getAccessPath(), callee.getDeclaringClass())) {
								Value callBase = mapper.getCallerValueOfCalleeParameter(ie,
										ICallerCalleeArgumentMapper.BASE_OBJECT);

								// Either the callBase is from a reflective call site
								// or the source base doesn't match with any parameters
								if (callBase != null) {
									if (isReflectiveCallSite || !hasAnotherReferenceOnBase(ie, sourceBase,
											mapper.getCallerIndexOfCalleeParameter(
													ICallerCalleeArgumentMapper.BASE_OBJECT))) {
										AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
												source.getAccessPath(), callBase,
												isReflectiveCallSite ? null : source.getAccessPath().getBaseType(),
												false);
										Abstraction abs = source.deriveNewAbstraction(ap, exitStmt);
										if (abs != null) {
											enterConditional(abs, callSite, returnSite);
											res.add(abs);
										}
									}
								}
							}
						}

						// map arguments to parameter
						if (ie != null) {
							for (int paramIndex = 0; paramIndex < callee.getParameterCount(); paramIndex++) {
								if (!aliasing.mayAlias(source.getAccessPath().getPlainValue(), paramLocals[paramIndex]))
									continue;

								Value originalCallArg = mapper.getCallerValueOfCalleeParameter(ie, paramIndex);
								if (originalCallArg == null)
									continue;
								if (!AccessPath.canContainValue(originalCallArg))
									continue;
								if (!isReflectiveCallSite && !manager.getTypeUtils().checkCast(source.getAccessPath(),
										originalCallArg.getType()))
									continue;

								AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
										originalCallArg,
										isReflectiveCallSite ? null : source.getAccessPath().getBaseType(), false);
								Abstraction abs = source.deriveNewAbstraction(ap, exitStmt);
								if (abs != null) {
									enterConditional(abs, callSite, returnSite);
									res.add(abs);

									if (!isReflectiveCallSite
											&& aliasing.canHaveAliasesRightSide(callStmt, originalCallArg, abs)) {
										// see HeapTests#testAliases
										// If two arguments are the same, we maybe missed one parameter
										// so we fully revisit the callSite using aliasing
										SootMethod caller = manager.getICFG().getMethodOf(callStmt);
										boolean foundDuplicate = false;
										// Look if we have a duplicate argument to originalCallArg
										for (int argIndex = 0; argIndex < ie.getArgCount(); argIndex++) {
											if (paramIndex != argIndex && originalCallArg == ie.getArg(argIndex)) {
												foundDuplicate = true;
												break;
											}
										}
										// trigger aliasing on all args that are equal
										// to originalCallArg including itself
										if (foundDuplicate) {
											for (Value arg : ie.getArgs()) {
												if (arg == originalCallArg) {
													for (Abstraction d1 : callerD1s) {
														aliasing.computeAliases(d1, callStmt, arg, res, caller, abs);
													}
												}
											}
										}

										// A foo(A a) {
										// return a;
										// }
										// A b = foo(a);
										// An alias is created using the returned value. If no assignment
										// happen inside the method, also no alias analysis is triggered.
										// Thus, here we trigger a manual alias analysis for all return
										// values which equal a param if the param is on the heap.
										for (Unit u : callee.getActiveBody().getUnits()) {
											if (!(u instanceof ReturnStmt))
												continue;
											Value retOp = ((ReturnStmt) u).getOp();

											if (paramLocals[paramIndex] == retOp) {
												for (Unit pred : manager.getICFG().getPredsOf(callStmt)) {
													for (Abstraction d1 : callerD1s) {
														aliasing.computeAliases(d1, stmt, originalCallArg,
																Collections.singleton(abs),
																manager.getICFG().getMethodOf(pred), abs);
													}
												}
											}
										}
									}
								}
							}
						}

						for (Abstraction abs : res) {
							if (abs != source)
								abs.setCorrespondingCallSite(callStmt);
						}

						return res;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
				if (!(callSite instanceof Stmt)) {
					return KillAll.v();
				}

				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return KillAll.v();

				final Stmt callStmt = (Stmt) callSite;
				final InvokeExpr invExpr = callStmt.getInvokeExpr();

				final Value[] callArgs = new Value[invExpr.getArgCount()];
				for (int i = 0; i < invExpr.getArgCount(); i++) {
					callArgs[i] = invExpr.getArg(i);
				}

				final SootMethod callee = invExpr.getMethod();

				final boolean isSink = manager.getSourceSinkManager() != null
						&& manager.getSourceSinkManager().getSinkInfo(callStmt, manager, null) != null;

				return new SolverCallToReturnFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(callSite, source, manager,
									TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);

						Set<Abstraction> res = computeTargetsInternal(d1, source);
						return notifyOutFlowHandlers(callSite, d1, source, res,
								TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
							return null;

						Set<Abstraction> res = null;
						ByReferenceBoolean killSource = new ByReferenceBoolean();
						ByReferenceBoolean killAll = new ByReferenceBoolean();
						// if we have a RuleManager, apply the rules
						if (propagationRules != null) {
							res = propagationRules.applyCallToReturnFlowFunction(d1, source, callStmt, killSource,
									killAll, true);
						}
						// On killAll, we do not propagate and can stop here
						if (killAll.value)
							return null;

						// Instanciate res if RuleManager did return null
						if (res == null)
							res = new HashSet<>();

						// If left side is tainted, the return value overwrites the taint
						// CallFlow takes care of tainting the return value
						if (callStmt instanceof AssignStmt && aliasing.mayAlias(((AssignStmt) callStmt).getLeftOp(),
								source.getAccessPath().getPlainValue())) {
							return res;
						}

						// If we do not know the callees, we can not reason
						// To not break anything, propagate over
						if (!killSource.value && source != zeroValue) {
							boolean hasConcreteCallees = false;
							for (SootMethod callee : interproceduralCFG().getCalleesOfCallAt(callSite))
								if (callee.isConcrete()) {
									hasConcreteCallees = true;
									break;
								}
							if (!hasConcreteCallees)
								res.add(source);
						}

						// Assumption: Sinks only leak taints but never
						// overwrite them. This is needed e.g. if an heap object
						// is an argument and leaked twice in the same path.
						// See also Android Source Sink Tests
						if (isSink && !manager.getConfig().getInspectSinks() && source != zeroValue
								&& !killSource.value)
							res.add(source);

						// If method is excluded, add the taint to not break anything
						// unless one of the rules doesn't want so
						if (isExcluded(callee) && source != zeroValue && !killSource.value)
							res.add(source);

						// Static values can be propagated over methods if
						// the value isn't written inside the method.
						// Otherwise CallFlowFunction already does the job.
						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef() && interproceduralCFG()
										.isStaticFieldUsed(callee, source.getAccessPath().getFirstField()))
							return res;

						if (callee.isNative() && ncHandler != null) {
							for (Value arg : callArgs) {
								if (aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())) {
									Set<Abstraction> nativeAbs = ncHandler.getTaintedValues(callStmt, source, callArgs);
									if (nativeAbs != null) {
										res.addAll(nativeAbs);

										// Compute the aliases
										for (Abstraction abs : nativeAbs) {
											enterConditional(abs, callStmt, returnSite);
											if (abs.getAccessPath().isStaticFieldRef()
													|| aliasing.canHaveAliasesRightSide(callStmt,
															abs.getAccessPath().getPlainValue(), abs)) {
												for (Unit pred : manager.getICFG().getPredsOf(callStmt))
													aliasing.computeAliases(d1, (Stmt) pred,
															abs.getAccessPath().getPlainValue(), res,
															interproceduralCFG().getMethodOf(pred), abs);
											}
										}
									}
									break;
								}
							}
						}
						// Do not pass base if tainted
						// CallFlow passes this into the callee
						// unless the callee is native and can not be visited
						// The third condition represents a tainted reference without any fields tainted
						// of which the callee only can overwrite the reference to the object but not
						// its contents
						if (invExpr instanceof InstanceInvokeExpr
								&& aliasing.mayAlias(((InstanceInvokeExpr) invExpr).getBase(),
										source.getAccessPath().getPlainValue())
								&& (source.getAccessPath().getTaintSubFields()
										|| source.getAccessPath().getFragmentCount() > 0)
								&& !callee.isNative())
							return res;

						// Do not pass over reference parameters
						// CallFlow passes this into the callee
						if (Arrays.stream(callArgs).anyMatch(arg -> !isPrimitiveOrStringBase(source)
								&& aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())))
							return res;

						if (!killSource.value && source != zeroValue)
							res.add(source);

						if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies()
								&& source.getDominator() == null && res.contains(source)) {
							IInfoflowCFG.UnitContainer dom = manager.getICFG().getDominatorOf(callSite);
							if (dom.getUnit() != null && dom.getUnit() != returnSite) {
								res.remove(source);
								res.add(source.deriveNewAbstractionWithDominator(dom.getUnit()));
							}
						}

						return res;
					}
				};
			}

			/**
			 * Sets the dominator if the taint enters a conditional
			 *
			 * @param abs      target abstraction
			 * @param stmt     Current statement
			 * @param destStmt Destination statement
			 */
			private void enterConditional(Abstraction abs, Unit stmt, Unit destStmt) {
				if (!manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies()
						|| abs.getDominator() != null)
					return;
				IInfoflowCFG.UnitContainer dom = manager.getICFG().getDominatorOf(stmt);
				if (dom.getUnit() != null && dom.getUnit() != destStmt) {
					abs.setDominator(dom.getUnit());
				}
			}

			private boolean isPrimitiveOrStringBase(Abstraction abs) {
				Type t = abs.getAccessPath().getBaseType();
				return t instanceof PrimType
						|| (TypeUtils.isStringType(t) && !abs.getAccessPath().getCanHaveImmutableAliases());
			}

			private boolean isPrimitiveOrStringType(Type t) {
				return t instanceof PrimType || TypeUtils.isStringType(t);
			}
		};
	}

}
