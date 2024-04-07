package soot.jimple.infoflow.problems;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import heros.solver.PathEdge;
import soot.ArrayType;
import soot.Local;
import soot.PrimType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.UnopExpr;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.callmappers.CallerCalleeManager;
import soot.jimple.infoflow.callmappers.ICallerCalleeArgumentMapper;
import soot.jimple.infoflow.cfg.FlowDroidSinkStatement;
import soot.jimple.infoflow.cfg.FlowDroidSourceStatement;
import soot.jimple.infoflow.collect.MutableTwoElementSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.rules.EmptyPropagationRuleManagerFactory;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.BaseSelector;

/**
 * Class which contains the alias analysis for the backwards analysis.
 *
 * @author Tim Lange
 */
public class BackwardsAliasProblem extends AbstractInfoflowProblem {
	private final static boolean DEBUG_PRINT = false;

	public BackwardsAliasProblem(InfoflowManager manager) {
		super(manager, null, EmptyPropagationRuleManagerFactory.INSTANCE);
	}

	@Override
	protected FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Abstraction, SootMethod>() {
			private Abstraction checkAbstraction(Abstraction abs) {
				if (abs == null)
					return null;

				// Primitive types and strings cannot have aliases and thus
				// never need to be propagated back
				if (!abs.getAccessPath().isStaticFieldRef()) {
					if (abs.getAccessPath().getBaseType() instanceof PrimType)
						return null;
				} else {
					if (abs.getAccessPath().getFirstFieldType() instanceof PrimType)
						return null;
				}
				return abs;
			}

			@Override
			public FlowFunction<Abstraction> getNormalFlowFunction(Unit srcUnit, Unit destUnit) {
				if (!(srcUnit instanceof DefinitionStmt))
					return Identity.v();

				final DefinitionStmt defStmt = (DefinitionStmt) srcUnit;

				return new SolverNormalFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (source == getZeroValue())
							return null;
						assert !source.getAccessPath().isEmpty();

						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(srcUnit, source, manager,
									TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

						// TurnUnit is the sink. Below this stmt, the taint is not valid anymore
						// Therefore we turn around here.
						if (source.getTurnUnit() == srcUnit) {
							return notifyOutFlowHandlers(srcUnit, d1, source, null,
									TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
						}

						Set<Abstraction> res = computeAliases(defStmt, d1, source);
						if (DEBUG_PRINT)
							System.out.println("Alias Normal" + "\n" + "In: " + source.toString() + "\n" + "Stmt: "
									+ srcUnit.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n"
									+ "---------------------------------------");

						return notifyOutFlowHandlers(srcUnit, d1, source, res,
								TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
					}

					private Set<Abstraction> computeAliases(final DefinitionStmt defStmt, Abstraction d1,
							Abstraction source) {
						if (defStmt instanceof IdentityStmt)
							return Collections.singleton(source);
						if (!(defStmt instanceof AssignStmt))
							return null;

						MutableTwoElementSet<Abstraction> res = new MutableTwoElementSet<>();
						final AssignStmt assignStmt = (AssignStmt) defStmt;
						final Value leftOp = assignStmt.getLeftOp();
						final Value rightOp = assignStmt.getRightOp();
						final Value leftVal = BaseSelector.selectBase(leftOp, false);
						final Value rightVal = BaseSelector.selectBase(rightOp, false);

						AccessPath ap = source.getAccessPath();
						Value sourceBase = ap.getPlainValue();
						Type rightType = rightOp.getType();
						boolean handoverLeftValue = false;
						boolean cutSubfield = false;
						boolean leftSideOverwritten = false;
						if (leftOp instanceof StaticFieldRef) {
							if (manager.getConfig()
									.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
									&& ap.firstFieldMatches(((StaticFieldRef) leftOp).getField())) {
								handoverLeftValue = true;
								cutSubfield = true;
							}
						} else if (leftOp instanceof InstanceFieldRef) {
							InstanceFieldRef instRef = (InstanceFieldRef) leftOp;

							// base matches
							if (instRef.getBase() == sourceBase) {
								AccessPath mappedAp = Aliasing.getReferencedAPBase(ap,
										new SootField[] { instRef.getField() }, manager);
								if (mappedAp != null) {
									handoverLeftValue = true;
									cutSubfield = true;
									if (!mappedAp.equals(ap))
										ap = mappedAp;
								}
							}
						} else if (leftVal == sourceBase) {
							// Either the alias is overwritten here or a write to an array element
							handoverLeftValue = leftOp instanceof ArrayRef
									&& ap.getArrayTaintType() != AccessPath.ArrayTaintType.Length;
							leftSideOverwritten = !handoverLeftValue;
						}

						if (handoverLeftValue) {
							Abstraction newAbs = null;
							if (rightVal instanceof Constant) {
								if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies()) {
									newAbs = source.deriveConditionalUpdate(assignStmt);
									for (Unit pred : manager.getICFG().getPredsOf(srcUnit))
										handOver(d1, pred, newAbs);
								}
							} else {
								AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(ap, rightOp,
										rightType, cutSubfield);
								newAbs = source.deriveNewAbstraction(newAp, assignStmt);
							}

							if (newAbs != null && !newAbs.equals(source)) {
								// We found a missed path upwards
								// inject same stmt in infoflow solver
								for (Unit pred : manager.getICFG().getPredsOf(srcUnit))
									handOver(d1, pred, newAbs);
							}
						}

						if (leftSideOverwritten)
							return null;
						res.add(source);

						// BinopExr & UnopExpr operands can not have aliases
						// as both only can have primitives on the right side
						// NewArrayExpr do not produce new aliases
						if (rightOp instanceof BinopExpr || rightOp instanceof UnopExpr
								|| rightOp instanceof NewArrayExpr)
							return res;

						boolean localAliases = (leftOp instanceof Local || leftOp instanceof ArrayRef)
								&& !(leftOp.getType() instanceof PrimType);
						boolean fieldAliases = leftOp instanceof FieldRef
								&& !(((FieldRef) leftOp).getField().getType() instanceof PrimType);
						if ((localAliases || fieldAliases) && !(rightVal.getType() instanceof PrimType)) {
							boolean addLeftValue = false;
							boolean cutFirstFieldLeft = false;
							Type leftType = null;
							boolean createNewVal = false;

							if (rightVal instanceof StaticFieldRef) {
								if (manager.getConfig()
										.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
										&& ap.firstFieldMatches(((StaticFieldRef) rightVal).getField())) {
									addLeftValue = true;
								}
							} else if (rightVal instanceof InstanceFieldRef) {
								InstanceFieldRef instRef = (InstanceFieldRef) rightVal;

								if (instRef.getBase() == sourceBase && ap.isInstanceFieldRef()) {
									AccessPath mappedAp = Aliasing.getReferencedAPBase(ap,
											new SootField[] { instRef.getField() }, manager);
									if (mappedAp != null) {
										addLeftValue = true;
										cutFirstFieldLeft = true;
										if (!mappedAp.equals(ap)) {
											ap = mappedAp;
											source = source.deriveNewAbstraction(mappedAp, null);
										}
									}
								}
							} else if (rightVal == sourceBase) {
								addLeftValue = true;
								leftType = ap.getBaseType();
								// We did not keep the ArrayRef, so rightVal is already the array base
								if (rightOp instanceof ArrayRef) {
									leftType = ((ArrayType) leftType).getElementType();
								} else if (leftOp instanceof ArrayRef) {
									ArrayRef arrayRef = (ArrayRef) leftOp;
									leftType = TypeUtils.buildArrayOrAddDimension(leftType,
											arrayRef.getType().getArrayType());
								} else {
									if (!manager.getTypeUtils().checkCast(source.getAccessPath().getBaseType(),
											leftOp.getType()))
										return null;
								}

								// LengthExpr extends UnopExpr, not possible here
								if (rightVal instanceof CastExpr) {
									CastExpr ce = (CastExpr) rightOp;
									if (!manager.getHierarchy().canStoreType(leftType, ce.getCastType()))
										leftType = ce.getCastType();
								} else if (rightVal instanceof InstanceOfExpr) {
									// We could just produce a boolean, which won't be tracked anyways
									addLeftValue = false;
								}
							}

							if (addLeftValue) {
								AccessPath newAp;
								if (createNewVal)
									newAp = manager.getAccessPathFactory().createAccessPath(leftVal, true);
								else
									newAp = manager.getAccessPathFactory().copyWithNewValue(ap, leftOp, leftType,
											cutFirstFieldLeft);
								Abstraction newAbs = checkAbstraction(source.deriveNewAbstraction(newAp, assignStmt));
								if (newAbs != null && newAbs != source) {
									if (rightVal instanceof StaticFieldRef && manager.getConfig()
											.getStaticFieldTrackingMode() == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
										manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
									else {
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
			public FlowFunction<Abstraction> getCallFlowFunction(final Unit callSite, final SootMethod dest) {
				if (!dest.hasActiveBody()) {
					logger.debug("Call skipped because target has no body: {} -> {}", callSite, dest);
					return KillAll.v();
				}

				if (!(callSite instanceof Stmt))
					return KillAll.v();

				final Stmt callStmt = (Stmt) callSite;
				final InvokeExpr ie = callStmt.containsInvokeExpr() ? callStmt.getInvokeExpr() : null;

				final Local[] paramLocals = dest.getActiveBody().getParameterLocals().toArray(new Local[0]);
				final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();

				final boolean isSink = callStmt.hasTag(FlowDroidSinkStatement.TAG_NAME);
				final boolean isSource = callStmt.hasTag(FlowDroidSourceStatement.TAG_NAME);

				final ICallerCalleeArgumentMapper mapper = CallerCalleeManager.getMapper(manager, callStmt, dest);
				final boolean isReflectiveCallSite = mapper != null ? mapper.isReflectiveMapper() : false;

				return new SolverCallFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (source == getZeroValue())
							return null;

						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(callStmt, source, manager,
									TaintPropagationHandler.FlowFunctionType.CallFlowFunction);

						// TurnUnit is the sink. Below this stmt, the taint is not valid anymore
						// Therefore we turn around here.
						if (source.getTurnUnit() == callSite) {
							return notifyOutFlowHandlers(callSite, d1, source, null,
									TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
						}

						Set<Abstraction> res = computeTargetsInternal(d1, source);
						if (DEBUG_PRINT)
							System.out.println("Alias Call" + "\n" + "In: " + source.toString() + "\n" + "Stmt: "
									+ callStmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString())
									+ "\n" + "---------------------------------------");

						return notifyOutFlowHandlers(callStmt, d1, source, res,
								TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if (!manager.getConfig().getInspectSources() && isSource)
							return null;
						if (!manager.getConfig().getInspectSinks() && isSink)
							return null;
						if (manager.getConfig()
								.getStaticFieldTrackingMode() == InfoflowConfiguration.StaticFieldTrackingMode.None
								&& dest.isStaticInitializer())
							return null;
						if (isExcluded(dest))
							return null;
						if (taintWrapper != null && taintWrapper.isExclusive(callStmt, source))
							return null;

						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef()) {
							if (!interproceduralCFG().isStaticFieldRead(dest, source.getAccessPath().getFirstField()))
								return null;
						}

						HashSet<Abstraction> res = new HashSet<>();

						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef()) {
							Abstraction abs = checkAbstraction(
									source.deriveNewAbstraction(source.getAccessPath(), callStmt));
							if (abs != null)
								res.add(abs);
						}

						// map o to this
						if (!source.getAccessPath().isStaticFieldRef() && !dest.isStatic()) {
							Value callBase = mapper.getCallerValueOfCalleeParameter(ie,
									ICallerCalleeArgumentMapper.BASE_OBJECT);

							if (callBase != null) {
								Value sourceBase = source.getAccessPath().getPlainValue();
								if (callBase == sourceBase && manager.getTypeUtils()
										.hasCompatibleTypesForCall(source.getAccessPath(), dest.getDeclaringClass())) {
									if (isReflectiveCallSite || !hasAnotherReferenceOnBase(ie, sourceBase,
											mapper.getCallerIndexOfCalleeParameter(
													ICallerCalleeArgumentMapper.BASE_OBJECT))) {
										AccessPath ap = manager.getAccessPathFactory()
												.copyWithNewValue(source.getAccessPath(), thisLocal);
										Abstraction abs = checkAbstraction(
												source.deriveNewAbstraction(ap, (Stmt) callSite));
										if (abs != null)
											res.add(abs);
									}
								}
							}
						}

						// map arguments to parameter
						if (ie != null && dest.getParameterCount() > 0) {
							for (int i = 0; i < ie.getArgCount(); i++) {
								if (ie.getArg(i) != source.getAccessPath().getPlainValue())
									continue;

								int mappedIndex = mapper.getCalleeIndexOfCallerParameter(i);
								if (mappedIndex == ICallerCalleeArgumentMapper.UNKNOWN)
									continue;

								// taint all parameters if reflective call site
								if (mappedIndex == ICallerCalleeArgumentMapper.ALL_PARAMS) {
									for (Value param : paramLocals) {
										AccessPath ap = manager.getAccessPathFactory()
												.copyWithNewValue(source.getAccessPath(), param, null, false);
										Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, callStmt));
										if (abs != null)
											res.add(abs);
									}
									// taint just the tainted parameter
								} else {
									AccessPath ap = manager.getAccessPathFactory()
											.copyWithNewValue(source.getAccessPath(), paramLocals[i]);
									Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, callStmt));
									if (abs != null)
										res.add(abs);
								}
							}
						}

						for (Abstraction d3 : res)
							manager.getMainSolver().injectContext(solver, dest, d3, callSite, source, d1);

						return res;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getReturnFlowFunction(Unit callSite, SootMethod callee, Unit exitStmt,
					Unit returnSite) {
				if (callSite != null && !(callSite instanceof Stmt))
					return KillAll.v();

				final Value[] paramLocals = new Value[callee.getParameterCount()];
				for (int i = 0; i < callee.getParameterCount(); i++)
					paramLocals[i] = callee.getActiveBody().getParameterLocal(i);

				final Stmt callStmt = (Stmt) callSite;
				final InvokeExpr ie = (callStmt != null && callStmt.containsInvokeExpr()) ? callStmt.getInvokeExpr()
						: null;
				final ReturnStmt returnStmt = (exitStmt instanceof ReturnStmt) ? (ReturnStmt) exitStmt : null;

				final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
				final ICallerCalleeArgumentMapper mapper = CallerCalleeManager.getMapper(manager, callStmt, callee);
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
							taintPropagationHandler.notifyFlowIn(callStmt, source, manager,
									TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);

						// TurnUnit is the sink. Below this stmt, the taint is not valid anymore
						// Therefore we turn around here.
						if (source.getTurnUnit() == callSite) {
							return notifyOutFlowHandlers(callSite, calleeD1, source, null,
									TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
						}

						Set<Abstraction> res = computeTargetsInternal(source);
						if (DEBUG_PRINT)
							System.out.println("Alias Return" + "\n" + "In: " + source.toString() + "\n" + "Stmt: "
									+ callSite.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString())
									+ "\n" + "---------------------------------------");

						return notifyOutFlowHandlers(exitStmt, calleeD1, source, res,
								TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction source) {
						HashSet<Abstraction> res = new HashSet<>();

						// Static fields get propagated unchanged
						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef()) {
							res.add(source);
							return res;
						}

						// x = o.m()
						if (returnStmt != null && callStmt instanceof AssignStmt) {
							Value retLocal = returnStmt.getOp();
							DefinitionStmt defnStmt = (DefinitionStmt) callSite;
							Value leftOp = defnStmt.getLeftOp();

							if (retLocal == source.getAccessPath().getPlainValue() && !isExceptionHandler(returnSite)) {
								AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
										leftOp);
								Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, (Stmt) exitStmt));
								if (abs != null) {
									res.add(abs);
								}
							}
						}

						// o.m(a1, ..., an)
						// map o.f to this.f
						if (!callee.isStatic()) {
							Value sourceBase = source.getAccessPath().getPlainValue();
							if (thisLocal == sourceBase && manager.getTypeUtils()
									.hasCompatibleTypesForCall(source.getAccessPath(), callee.getDeclaringClass())) {
								Value callBase = mapper.getCallerValueOfCalleeParameter(ie,
										ICallerCalleeArgumentMapper.BASE_OBJECT);

								if (callBase != null) {
									if (isReflectiveCallSite || !hasAnotherReferenceOnBase(ie, sourceBase,
											mapper.getCallerIndexOfCalleeParameter(
													ICallerCalleeArgumentMapper.BASE_OBJECT))) {
										AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
												source.getAccessPath(), callBase,
												isReflectiveCallSite ? null : source.getAccessPath().getBaseType(),
												false);
										Abstraction abs = checkAbstraction(
												source.deriveNewAbstraction(ap, (Stmt) exitStmt));
										if (abs != null) {
											res.add(abs);
										}
									}
								}
							}
						}

						// map arguments to parameter
						if (ie != null) {
							for (int i = 0; i < callee.getParameterCount(); i++) {
								if (source.getAccessPath().getPlainValue() != paramLocals[i])
									continue;
								if (isPrimitiveOrStringBase(source))
									continue;

								Value originalCallArg = mapper.getCallerValueOfCalleeParameter(ie, i);
								if (originalCallArg == null)
									continue;
								if (callSite instanceof DefinitionStmt && !isExceptionHandler(returnSite)) {
									DefinitionStmt defnStmt = (DefinitionStmt) callSite;
									Value leftOp = defnStmt.getLeftOp();
									originalCallArg = defnStmt.getInvokeExpr().getArg(i);
									if (originalCallArg == leftOp)
										continue;
								}

								if (interproceduralCFG().methodWritesValue(callee, paramLocals[i]))
									continue;

								if (!AccessPath.canContainValue(originalCallArg))
									continue;
								if (!isReflectiveCallSite && !manager.getTypeUtils().checkCast(source.getAccessPath(),
										originalCallArg.getType()))
									continue;

								AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
										originalCallArg,
										isReflectiveCallSite ? null : source.getAccessPath().getBaseType(), false);
								Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, (Stmt) exitStmt));
								if (abs != null) {
									res.add(abs);
								}
							}
						}

						if (res.isEmpty()) {
							return null;
						} else {
							for (Abstraction abs : res) {
								if (abs != source) {
									abs.setCorrespondingCallSite((Stmt) callSite);
								}
							}
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

				final Stmt callStmt = (Stmt) callSite;
				final InvokeExpr invExpr = callStmt.getInvokeExpr();

				final Value[] callArgs = new Value[invExpr.getArgCount()];
				for (int i = 0; i < invExpr.getArgCount(); i++) {
					callArgs[i] = invExpr.getArg(i);
				}

				final SootMethod callee = invExpr.getMethod();

				final boolean isSource = manager.getSourceSinkManager() != null
						&& manager.getSourceSinkManager().getSourceInfo(callStmt, manager) != null;

				return new SolverCallToReturnFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (source == getZeroValue())
							return null;

						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(callSite, source, manager,
									TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);

						// TurnUnit is the sink. Below this stmt, the taint is not valid anymore
						// Therefore we turn around here.
						if (source.getTurnUnit() != null && (source.getTurnUnit() == callSite
								|| manager.getICFG().getCalleesOfCallAt(callSite).stream()
										.anyMatch(m -> manager.getICFG().getMethodOf(source.getTurnUnit()) == m))) {

							return notifyOutFlowHandlers(callSite, d1, source, null,
									TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
						}

						Set<Abstraction> res = computeTargetsInternal(d1, source);
						if (DEBUG_PRINT)
							System.out.println("Alias CallToReturn" + "\n" + "In: " + source.toString() + "\n"
									+ "Stmt: " + callStmt.toString() + "\n" + "Out: "
									+ (res == null ? "[]" : res.toString()) + "\n"
									+ "---------------------------------------");

						return notifyOutFlowHandlers(callSite, d1, source, res,
								TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if (taintWrapper != null) {
							if (taintWrapper.isExclusive(callStmt, source)) {
								handOver(d1, callSite, source);
							}

							Set<Abstraction> wrapperAliases = taintWrapper.getAliasesForMethod(callStmt, d1, source);
							if (wrapperAliases != null && !wrapperAliases.isEmpty()) {
								Set<Abstraction> passOnSet = new HashSet<>(wrapperAliases.size());
								for (Abstraction abs : wrapperAliases) {
									passOnSet.add(abs);
									if (abs != source)
										abs.setCorrespondingCallSite(callStmt);

									for (Unit u : manager.getICFG().getPredsOf(callSite))
										handOver(d1, u, abs);
								}
								return passOnSet;
							}
						}

						// If excluded or we do not anything about the callee,
						// we just pass the taint over the statement
						if (interproceduralCFG().getCalleesOfCallAt(callSite).isEmpty()) {
							return Collections.singleton(source);
						}

						if (isExcluded(callee)) {
							return Collections.singleton(source);
						}

						// If static field is used, we do not pass it over
						if (manager.getConfig()
								.getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
								&& source.getAccessPath().isStaticFieldRef() && interproceduralCFG()
										.isStaticFieldUsed(callee, source.getAccessPath().getFirstField())) {
							return null;
						}

						// See stringBuilderTest5. original needs to pass over arraycopy.
						if (!callee.isNative()) {
							// Do not pass tainted base over the statement
							if (callStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
								InstanceInvokeExpr inv = (InstanceInvokeExpr) callStmt.getInvokeExpr();
								if (inv.getBase() == source.getAccessPath().getPlainValue())
									return null;
							}

							// Do not pass over reference parameters
							// CallFlow passes this into the callee
							if (Arrays.stream(callArgs).anyMatch(arg -> !isPrimitiveOrStringBase(source)
									&& arg == source.getAccessPath().getPlainValue())) {
								// non standard source sink manager might need this
								if (isSource)
									handOver(d1, callSite, source);
								return null;
							}
						} else {
							for (Value arg : callArgs) {
								if (arg == source.getAccessPath().getPlainValue()) {
									// well, this is sorta a mix of infoflow and alias search
									// if we find an alias above, which is the an argument of arraycopy,
									// the native stmt does not create a new alias but we notice that we
									// missed this argument in the infoflow search.
									Abstraction newSource = source.deriveNewAbstractionWithTurnUnit(callSite);
									handOver(d1, callSite, newSource);
									return null;
								}
							}
						}

						return Collections.singleton(source);
					}
				};
			}

			private boolean isPrimitiveOrStringBase(Abstraction abs) {
				Type t = abs.getAccessPath().getBaseType();
				return t instanceof PrimType
						|| (TypeUtils.isStringType(t) && !abs.getAccessPath().getCanHaveImmutableAliases());
			}

			private void handOver(Abstraction d1, Unit unit, Abstraction in) {
				in = in.getActiveCopy();

				if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies()) {
					// We maybe turned around inside a conditional, so we reconstruct the condition
					// dominator. Also, we lost track of the dominators in the alias search. Thus,
					// we derive interprocedural wildcards.
					// See ImplicitTests#conditionalAliasingTest
					List<Unit> condUnits = manager.getOriginalICFG().getConditionalBranchesInterprocedural(unit);
					// No condition path -> no need to search for one
					for (Unit condUnit : condUnits) {
						Abstraction abs = in.deriveNewAbstractionWithDominator(condUnit);
						if (abs != null)
							manager.getMainSolver().processEdge(new PathEdge<>(d1, unit, abs));
					}
				} else {
					manager.getMainSolver().processEdge(new PathEdge<>(d1, unit, in));
				}
			}
		};
	}
}
