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
package soot.jimple.infoflow.problems;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.KillAll;
import soot.BooleanType;
import soot.Local;
import soot.NullType;
import soot.PrimType;
import soot.RefType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
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
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.handlers.TaintPropagationHandler.FlowFunctionType;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;

public class InfoflowProblem extends AbstractInfoflowProblem {

	private final PropagationRuleManager propagationRules;

	protected final TaintPropagationResults results;

	public InfoflowProblem(InfoflowManager manager, Abstraction zeroValue,
			IPropagationRuleManagerFactory ruleManagerFactory) {
		super(manager);

		this.zeroValue = zeroValue == null ? createZeroValue() : zeroValue;
		this.results = new TaintPropagationResults(manager);
		this.propagationRules = ruleManagerFactory.createRuleManager(manager, this.zeroValue, results);
	}

	@Override
	public FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Abstraction, SootMethod>() {

			/**
			 * Abstract base class for all normal flow functions. This is to share code that
			 * e.g. notifies the taint handlers between the various functions.
			 * 
			 * @author Steven Arzt
			 */
			abstract class NotifyingNormalFlowFunction extends SolverNormalFlowFunction {

				protected final Stmt stmt;

				public NotifyingNormalFlowFunction(Stmt stmt) {
					this.stmt = stmt;
				}

				@Override
				public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
					// Notify the handler if we have one
					if (taintPropagationHandler != null)
						taintPropagationHandler.notifyFlowIn(stmt, source, manager,
								FlowFunctionType.NormalFlowFunction);

					// Compute the new abstractions
					Set<Abstraction> res = computeTargetsInternal(d1, source);
					return notifyOutFlowHandlers(stmt, d1, source, res, FlowFunctionType.NormalFlowFunction);
				}

				public abstract Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source);

			}

			/**
			 * Taints the left side of the given assignment
			 * 
			 * @param assignStmt  The source statement from which the taint originated
			 * @param targetValue The target value that shall now be tainted
			 * @param source      The incoming taint abstraction from the source
			 * @param taintSet    The taint set to which to add all newly produced taints
			 */
			private void addTaintViaStmt(final Abstraction d1, final AssignStmt assignStmt, Abstraction source,
					Set<Abstraction> taintSet, boolean cutFirstField, SootMethod method, Type targetType) {
				final Value leftValue = assignStmt.getLeftOp();
				final Value rightValue = assignStmt.getRightOp();

				// Do not taint static fields unless the option is enabled
				if (leftValue instanceof StaticFieldRef
						&& manager.getConfig().getStaticFieldTrackingMode() == StaticFieldTrackingMode.None)
					return;

				Abstraction newAbs = null;
				if (!source.getAccessPath().isEmpty()) {
					// Special handling for array construction
					if (leftValue instanceof ArrayRef && targetType != null) {
						ArrayRef arrayRef = (ArrayRef) leftValue;
						targetType = TypeUtils.buildArrayOrAddDimension(targetType, arrayRef.getType().getArrayType());
					}

					// If this is an unrealizable typecast, drop the abstraction
					if (rightValue instanceof CastExpr) {
						// For casts, we must update our typing information
						CastExpr cast = (CastExpr) assignStmt.getRightOp();
						targetType = cast.getType();
					}
					// Special type handling for certain operations
					else if (rightValue instanceof InstanceOfExpr)
						newAbs = source.deriveNewAbstraction(manager.getAccessPathFactory().createAccessPath(leftValue,
								BooleanType.v(), true, ArrayTaintType.ContentsAndLength), assignStmt);
				} else
					// For implicit taints, we have no type information
					assert targetType == null;

				// Do we taint the contents of an array? If we do not
				// differentiate, we do not set any special type.
				ArrayTaintType arrayTaintType = source.getAccessPath().getArrayTaintType();
				if (leftValue instanceof ArrayRef && manager.getConfig().getEnableArraySizeTainting())
					arrayTaintType = ArrayTaintType.Contents;

				// also taint the target of the assignment
				if (newAbs == null)
					if (source.getAccessPath().isEmpty())
						newAbs = source.deriveNewAbstraction(
								manager.getAccessPathFactory().createAccessPath(leftValue, true), assignStmt, true);
					else {
						AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
								leftValue, targetType, cutFirstField, true, arrayTaintType);
						newAbs = source.deriveNewAbstraction(ap, assignStmt);
					}

				if (newAbs != null) {
					// Do we treat this taint specially, i.e., outside of IFDS?
					if (leftValue instanceof StaticFieldRef && manager.getConfig()
							.getStaticFieldTrackingMode() == StaticFieldTrackingMode.ContextFlowInsensitive) {
						// We need to add the taint to our global taint state
						manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
					} else {
						// Perform a normal IFDS-style propagation for the new taint
						taintSet.add(newAbs);
						final Aliasing aliasing = manager.getAliasing();
						if (aliasing != null && Aliasing.canHaveAliases(assignStmt, leftValue, newAbs)) {
							aliasing.computeAliases(d1, assignStmt, leftValue, taintSet, method, newAbs);
						}
					}
				}
			}

			/**
			 * Checks whether the given call has at least one valid target, i.e. a callee
			 * with a body.
			 * 
			 * @param call The call site to check
			 * @return True if there is at least one callee implementation for the given
			 *         call, otherwise false
			 */
			private boolean hasValidCallees(Unit call) {
				Collection<SootMethod> callees = interproceduralCFG().getCalleesOfCallAt(call);
				for (SootMethod callee : callees)
					if (callee.isConcrete())
						return true;
				return false;
			}

			private Set<Abstraction> createNewTaintOnAssignment(final AssignStmt assignStmt, final Value[] rightVals,
					Abstraction d1, final Abstraction newSource) {
				final Value leftValue = assignStmt.getLeftOp();
				final Value rightValue = assignStmt.getRightOp();
				boolean addLeftValue = false;

				// i = lengthof a. This is handled by a rule.
				if (rightValue instanceof LengthExpr) {
					return Collections.singleton(newSource);
				}

				// If we have an implicit flow, but the assigned
				// local is never read outside the condition, we do
				// not need to taint it.
				boolean implicitTaint = newSource.getTopPostdominator() != null
						&& newSource.getTopPostdominator().getUnit() != null;
				implicitTaint |= newSource.getAccessPath().isEmpty();

				// If we have a non-empty postdominator stack, we taint
				// every assignment target
				if (implicitTaint) {
					// We can skip over all local assignments inside
					// conditionally-called functions since they are not visible
					// in the caller anyway
					if ((d1 == null || d1.getAccessPath().isEmpty()) && !(leftValue instanceof FieldRef))
						return Collections.singleton(newSource);

					if (newSource.getAccessPath().isEmpty())
						addLeftValue = true;
				}

				// If we have a = x with the taint "x" being inactive,
				// we must not taint the left side. We can only taint
				// the left side if the tainted value is some "x.y".
				boolean aliasOverwritten = !addLeftValue && !newSource.isAbstractionActive()
						&& Aliasing.baseMatchesStrict(rightValue, newSource) && rightValue.getType() instanceof RefType
						&& !newSource.dependsOnCutAP();

				// If we can't reason about aliases, there's little we can do here
				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return null;

				boolean cutFirstField = false;
				AccessPath mappedAP = newSource.getAccessPath();
				Type targetType = null;
				if (!addLeftValue && !aliasOverwritten) {
					for (Value rightVal : rightVals) {
						if (rightVal instanceof FieldRef) {
							// Get the field reference
							FieldRef rightRef = (FieldRef) rightVal;

							// If the right side references a NULL field, we
							// kill the taint
							if (rightRef instanceof InstanceFieldRef
									&& ((InstanceFieldRef) rightRef).getBase().getType() instanceof NullType)
								return null;

							// Check for aliasing
							mappedAP = aliasing.mayAlias(newSource.getAccessPath(), rightRef);

							// check if static variable is tainted (same name,
							// same class)
							// y = X.f && X.f tainted --> y, X.f tainted
							if (rightVal instanceof StaticFieldRef) {
								if (manager.getConfig().getStaticFieldTrackingMode() != StaticFieldTrackingMode.None
										&& mappedAP != null) {
									addLeftValue = true;
									cutFirstField = true;
								}
							}
							// check for field references
							// y = x.f && x tainted --> y, x tainted
							// y = x.f && x.f tainted --> y, x tainted
							else if (rightVal instanceof InstanceFieldRef) {
								Local rightBase = (Local) ((InstanceFieldRef) rightRef).getBase();
								Local sourceBase = newSource.getAccessPath().getPlainValue();
								final SootField rightField = rightRef.getField();

								// We need to compare the access path on the
								// right side
								// with the start of the given one
								if (mappedAP != null) {
									addLeftValue = true;
									cutFirstField = (mappedAP.getFieldCount() > 0
											&& mappedAP.getFirstField() == rightField);
								} else if (aliasing.mayAlias(rightBase, sourceBase)
										&& newSource.getAccessPath().getFieldCount() == 0
										&& newSource.getAccessPath().getTaintSubFields()) {
									addLeftValue = true;
									targetType = rightField.getType();
									if (mappedAP == null)
										mappedAP = manager.getAccessPathFactory().createAccessPath(rightBase, true);
								}
							}
						}
						// indirect taint propagation:
						// if rightvalue is local and source is instancefield of
						// this local:
						// y = x && x.f tainted --> y.f, x.f tainted
						// y.g = x && x.f tainted --> y.g.f, x.f tainted
						else if (rightVal instanceof Local && newSource.getAccessPath().isInstanceFieldRef()) {
							Local base = newSource.getAccessPath().getPlainValue();
							if (aliasing.mayAlias(rightVal, base)) {
								addLeftValue = true;
								targetType = newSource.getAccessPath().getBaseType();
							}
						}
						// generic case, is true for Locals, ArrayRefs that are
						// equal etc..
						// y = x && x tainted --> y, x tainted
						else if (aliasing.mayAlias(rightVal, newSource.getAccessPath().getPlainValue())) {
							if (!(assignStmt.getRightOp() instanceof NewArrayExpr)) {
								if (manager.getConfig().getEnableArraySizeTainting()
										|| !(rightValue instanceof NewArrayExpr)) {
									addLeftValue = true;
									targetType = newSource.getAccessPath().getBaseType();
								}
							}
						}

						// One reason to taint the left side is enough
						if (addLeftValue)
							break;
					}
				}

				// If we have nothing to add, we quit
				if (!addLeftValue)
					return null;

				// Do not propagate non-active primitive taints
				if (!newSource.isAbstractionActive() && (assignStmt.getLeftOp().getType() instanceof PrimType
						|| (TypeUtils.isStringType(assignStmt.getLeftOp().getType())
								&& !newSource.getAccessPath().getCanHaveImmutableAliases())))
					return Collections.singleton(newSource);

				Set<Abstraction> res = new HashSet<Abstraction>();
				Abstraction targetAB = mappedAP.equals(newSource.getAccessPath()) ? newSource
						: newSource.deriveNewAbstraction(mappedAP, null);
				addTaintViaStmt(d1, assignStmt, targetAB, res, cutFirstField,
						interproceduralCFG().getMethodOf(assignStmt), targetType);
				res.add(newSource);
				return res;
			}

			@Override
			public FlowFunction<Abstraction> getNormalFlowFunction(final Unit src, final Unit dest) {
				// Get the call site
				if (!(src instanceof Stmt))
					return KillAll.v();

				return new NotifyingNormalFlowFunction((Stmt) src) {

					@Override
					public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						// Check whether we must activate a taint
						final Abstraction newSource;
						if (!source.isAbstractionActive() && src == source.getActivationUnit())
							newSource = source.getActiveCopy();
						else
							newSource = source;

						// Apply the propagation rules
						ByReferenceBoolean killSource = new ByReferenceBoolean();
						ByReferenceBoolean killAll = new ByReferenceBoolean();
						Set<Abstraction> res = propagationRules.applyNormalFlowFunction(d1, newSource, stmt,
								(Stmt) dest, killSource, killAll);
						if (killAll.value)
							return Collections.<Abstraction>emptySet();

						// Propagate over an assignment
						if (src instanceof AssignStmt) {
							final AssignStmt assignStmt = (AssignStmt) src;
							final Value right = assignStmt.getRightOp();
							final Value[] rightVals = BaseSelector.selectBaseList(right, true);

							// Create the new taints that may be created by this
							// assignment
							Set<Abstraction> resAssign = createNewTaintOnAssignment(assignStmt, rightVals, d1,
									newSource);
							if (resAssign != null && !resAssign.isEmpty()) {
								if (res != null) {
									res.addAll(resAssign);
									return res;
								} else
									res = resAssign;
							}
						}

						// Return what we have so far
						return res == null || res.isEmpty() ? Collections.<Abstraction>emptySet() : res;
					}

				};
			}

			@Override
			public FlowFunction<Abstraction> getCallFlowFunction(final Unit src, final SootMethod dest) {
				if (!dest.isConcrete()) {
					logger.debug("Call skipped because target has no body: {} -> {}", src, dest);
					return KillAll.v();
				}

				final Stmt stmt = (Stmt) src;
				final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr()) ? stmt.getInvokeExpr() : null;

				final Local[] paramLocals = dest.getActiveBody().getParameterLocals().toArray(new Local[0]);

				// This is not cached by Soot, so accesses are more expensive
				// than one might think
				final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();

				// If we can't reason about aliases, there's little we can do here
				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return null;

				return new SolverCallFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						Set<Abstraction> res = computeTargetsInternal(d1, source);
						if (res != null && !res.isEmpty()) {
							for (Abstraction abs : res)
								aliasing.getAliasingStrategy().injectCallingContext(abs, solver, dest, src, source, d1);
						}
						return notifyOutFlowHandlers(stmt, d1, source, res, FlowFunctionType.CallFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
							return null;
						if (source == getZeroValue())
							return null;

						// Do not propagate into Soot library classes if that
						// optimization is enabled
						if (isExcluded(dest))
							return null;

						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(stmt, source, manager,
									FlowFunctionType.CallFlowFunction);

						// We might need to activate the abstraction
						if (!source.isAbstractionActive() && source.getActivationUnit() == src)
							source = source.getActiveCopy();

						ByReferenceBoolean killAll = new ByReferenceBoolean();
						Set<Abstraction> res = propagationRules.applyCallFlowFunction(d1, source, stmt, dest, killAll);
						if (killAll.value)
							return null;

						// Map the source access path into the callee
						Set<AccessPath> resMapping = mapAccessPathToCallee(dest, ie, paramLocals, thisLocal,
								source.getAccessPath());
						if (resMapping == null)
							return res;

						// Translate the access paths into abstractions
						Set<Abstraction> resAbs = new HashSet<Abstraction>(resMapping.size());
						if (res != null && !res.isEmpty())
							resAbs.addAll(res);
						for (AccessPath ap : resMapping) {
							if (ap != null) {
								// If the variable is never read in the callee,
								// there is no need to propagate it through
								if (aliasing.getAliasingStrategy().isLazyAnalysis() || source.isImplicit()
										|| interproceduralCFG().methodReadsValue(dest, ap.getPlainValue())) {
									Abstraction newAbs = source.deriveNewAbstraction(ap, stmt);
									if (newAbs != null)
										resAbs.add(newAbs);
								}
							}
						}

						return resAbs;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getReturnFlowFunction(final Unit callSite, final SootMethod callee,
					final Unit exitStmt, final Unit retSite) {
				// Get the call site
				if (callSite != null && !(callSite instanceof Stmt))
					return KillAll.v();
				final Stmt iCallStmt = (Stmt) callSite;
				final boolean isReflectiveCallSite = callSite != null
						&& interproceduralCFG().isReflectiveCallSite(callSite);

				final ReturnStmt returnStmt = (exitStmt instanceof ReturnStmt) ? (ReturnStmt) exitStmt : null;

				final Local[] paramLocals = callee.getActiveBody().getParameterLocals().toArray(new Local[0]);

				// If we can't reason about aliases, there's little we can do here
				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return null;

				// This is not cached by Soot, so accesses are more expensive
				// than one might think
				final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();

				return new SolverReturnFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction source, Abstraction d1,
							Collection<Abstraction> callerD1s) {
						Set<Abstraction> res = computeTargetsInternal(source, callerD1s);
						return notifyOutFlowHandlers(exitStmt, d1, source, res, FlowFunctionType.ReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction source,
							Collection<Abstraction> callerD1s) {
						if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
							return null;
						if (source == getZeroValue())
							return null;

						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(exitStmt, source, manager,
									FlowFunctionType.ReturnFlowFunction);
						boolean callerD1sConditional = false;
						for (Abstraction d1 : callerD1s) {
							if (d1.getAccessPath().isEmpty()) {
								callerD1sConditional = true;
								break;
							}
						}

						// Activate taint if necessary
						Abstraction newSource = source;
						if (!source.isAbstractionActive())
							if (callSite != null)
								if (callSite == source.getActivationUnit()
										|| isCallSiteActivatingTaint(callSite, source.getActivationUnit()))
									newSource = source.getActiveCopy();

						// if abstraction is not active and activeStmt was in
						// this method, it will not get activated = it can be
						// removed:
						if (!newSource.isAbstractionActive() && newSource.getActivationUnit() != null)
							if (interproceduralCFG().getMethodOf(newSource.getActivationUnit()) == callee)
								return null;

						ByReferenceBoolean killAll = new ByReferenceBoolean();
						Set<Abstraction> res = propagationRules.applyReturnFlowFunction(callerD1s, newSource,
								(Stmt) exitStmt, (Stmt) retSite, (Stmt) callSite, killAll);
						if (killAll.value)
							return null;
						if (res == null)
							res = new HashSet<>();

						// If we have no caller, we have nowhere to propagate.
						// This can happen when leaving the main method.
						if (callSite == null)
							return null;

						// Do we need to retain all the taints?
						if (aliasing.getAliasingStrategy().isLazyAnalysis()
								&& Aliasing.canHaveAliases(newSource.getAccessPath()))
							res.add(newSource);

						// Static fields are handled in a rule
						if (!newSource.getAccessPath().isStaticFieldRef() && !callee.isStaticInitializer()) {
							// if we have a returnStmt we have to look at the
							// returned value:
							if (returnStmt != null && callSite instanceof DefinitionStmt) {
								Value retLocal = returnStmt.getOp();
								DefinitionStmt defnStmt = (DefinitionStmt) callSite;
								Value leftOp = defnStmt.getLeftOp();

								if (aliasing.mayAlias(retLocal, newSource.getAccessPath().getPlainValue())
										&& !isExceptionHandler(retSite)) {
									AccessPath ap = manager.getAccessPathFactory()
											.copyWithNewValue(newSource.getAccessPath(), leftOp);
									Abstraction abs = newSource.deriveNewAbstraction(ap, (Stmt) exitStmt);
									if (abs != null) {
										res.add(abs);

										// Aliases of implicitly tainted variables must be mapped back into the caller's
										// context on return when we leave the last implicitly-called method
										if (aliasing.getAliasingStrategy().requiresAnalysisOnReturn())
											for (Abstraction d1 : callerD1s)
												aliasing.computeAliases(d1, iCallStmt, leftOp, res,
														interproceduralCFG().getMethodOf(callSite), abs);
									}
								}
							}

							// Check parameters
							Value sourceBase = newSource.getAccessPath().getPlainValue();
							boolean parameterAliases = false;
							{
								Value originalCallArg = null;
								for (int i = 0; i < callee.getParameterCount(); i++) {
									// If this parameter is overwritten, we
									// cannot propagate the "old" taint over.
									// Return value propagation must always
									// happen explicitly.
									if (callSite instanceof DefinitionStmt && !isExceptionHandler(retSite)) {
										DefinitionStmt defnStmt = (DefinitionStmt) callSite;
										Value leftOp = defnStmt.getLeftOp();
										originalCallArg = defnStmt.getInvokeExpr().getArg(i);
										if (originalCallArg == leftOp)
											continue;
									}

									// Propagate over the parameter taint
									if (aliasing.mayAlias(paramLocals[i], sourceBase)) {
										parameterAliases = true;
										originalCallArg = iCallStmt.getInvokeExpr()
												.getArg(isReflectiveCallSite ? 1 : i);

										// If this is a constant parameter, we
										// can
										// safely ignore it
										if (!AccessPath.canContainValue(originalCallArg))
											continue;
										if (!isReflectiveCallSite && !manager.getTypeUtils()
												.checkCast(source.getAccessPath(), originalCallArg.getType()))
											continue;

										// Primitive types and strings cannot
										// have aliases and thus never need to
										// be propagated back
										if (source.getAccessPath().getBaseType() instanceof PrimType)
											continue;
										if (TypeUtils.isStringType(source.getAccessPath().getBaseType())
												&& !source.getAccessPath().getCanHaveImmutableAliases())
											continue;

										// If only the object itself, but no
										// field is tainted, we can safely
										// ignore it
										if (!source.getAccessPath().getTaintSubFields())
											continue;

										// If the variable was overwritten
										// somewehere in the callee, we assume
										// it to overwritten on all paths (yeah,
										// I know ...) Otherwise, we need SSA
										// or lots of bookkeeping to avoid FPs
										// (BytecodeTests.flowSensitivityTest1).
										if (interproceduralCFG().methodWritesValue(callee, paramLocals[i]))
											continue;

										AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
												newSource.getAccessPath(), originalCallArg,
												isReflectiveCallSite ? null : newSource.getAccessPath().getBaseType(),
												false);
										Abstraction abs = newSource.deriveNewAbstraction(ap, (Stmt) exitStmt);

										if (abs != null) {
											res.add(abs);
										}
									}
								}
							}

							// If this parameter is overwritten, we
							// cannot propagate the "old" taint over. Return
							// value propagation must always happen explicitly.
							boolean thisAliases = false;
							if (callSite instanceof DefinitionStmt && !isExceptionHandler(retSite)) {
								DefinitionStmt defnStmt = (DefinitionStmt) callSite;
								Value leftOp = defnStmt.getLeftOp();
								if (thisLocal == leftOp)
									thisAliases = true;
							}

							// check if it is not one of the params
							// (then we have already fixed it)
							if (!parameterAliases && !thisAliases && source.getAccessPath().getTaintSubFields()
									&& iCallStmt.getInvokeExpr() instanceof InstanceInvokeExpr
									&& aliasing.mayAlias(thisLocal, sourceBase)) {
								// Type check
								if (manager.getTypeUtils().checkCast(source.getAccessPath(), thisLocal.getType())) {
									InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) iCallStmt.getInvokeExpr();

									// Get the caller-side base local
									// and create a new access path for it
									Value callerBaseLocal = interproceduralCFG().isReflectiveCallSite(iIExpr)
											? iIExpr.getArg(0)
											: iIExpr.getBase();
									AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
											newSource.getAccessPath(), callerBaseLocal,
											isReflectiveCallSite ? null : newSource.getAccessPath().getBaseType(),
											false);
									Abstraction abs = newSource.deriveNewAbstraction(ap, (Stmt) exitStmt);
									if (abs != null) {
										res.add(abs);
									}
								}
							}
						}

						for (Abstraction abs : res) {
							// Aliases of implicitly tainted variables must be
							// mapped back into the caller's context on return
							// when we leave the last implicitly-called method
							if ((abs.isImplicit() && !callerD1sConditional)
									|| aliasing.getAliasingStrategy().requiresAnalysisOnReturn()) {
								for (Abstraction d1 : callerD1s) {
									aliasing.computeAliases(d1, iCallStmt, null, res,
											interproceduralCFG().getMethodOf(callSite), abs);
								}
							}

							// Set the corresponding call site
							if (abs != newSource) {
								abs.setCorrespondingCallSite(iCallStmt);
							}
						}
						return res;
					}

				};
			}

			@Override
			public FlowFunction<Abstraction> getCallToReturnFlowFunction(final Unit call, final Unit returnSite) {
				// special treatment for native methods:
				if (!(call instanceof Stmt))
					return KillAll.v();

				final Stmt iCallStmt = (Stmt) call;
				final InvokeExpr invExpr = iCallStmt.getInvokeExpr();

				// If we can't reason about aliases, there's little we can do here
				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return null;

				final Value[] callArgs = new Value[invExpr.getArgCount()];
				for (int i = 0; i < invExpr.getArgCount(); i++)
					callArgs[i] = invExpr.getArg(i);

				final boolean isSink = (manager.getSourceSinkManager() != null)
						? manager.getSourceSinkManager().getSinkInfo(iCallStmt, manager, null) != null
						: false;
				final boolean isSource = (manager.getSourceSinkManager() != null)
						? manager.getSourceSinkManager().getSourceInfo(iCallStmt, manager) != null
						: false;

				final SootMethod callee = invExpr.getMethod();
				final boolean hasValidCallees = hasValidCallees(call);

				return new SolverCallToReturnFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						Set<Abstraction> res = computeTargetsInternal(d1, source);
						return notifyOutFlowHandlers(call, d1, source, res, FlowFunctionType.CallToReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
							return null;

						// Notify the handler if we have one
						if (taintPropagationHandler != null)
							taintPropagationHandler.notifyFlowIn(call, source, manager,
									FlowFunctionType.CallToReturnFlowFunction);

						// check inactive elements:
						final Abstraction newSource;
						if (!source.isAbstractionActive() && (call == source.getActivationUnit()
								|| isCallSiteActivatingTaint(call, source.getActivationUnit())))
							newSource = source.getActiveCopy();
						else
							newSource = source;

						ByReferenceBoolean killSource = new ByReferenceBoolean();
						ByReferenceBoolean killAll = new ByReferenceBoolean();
						Set<Abstraction> res = propagationRules.applyCallToReturnFlowFunction(d1, newSource, iCallStmt,
								killSource, killAll, true);
						if (killAll.value)
							return null;
						boolean passOn = !killSource.value;

						// Do not propagate zero abstractions
						if (source == getZeroValue())
							return res == null || res.isEmpty() ? Collections.<Abstraction>emptySet() : res;

						// Initialize the result set
						if (res == null)
							res = new HashSet<>();

						if (newSource.getTopPostdominator() != null
								&& newSource.getTopPostdominator().getUnit() == null)
							return Collections.singleton(newSource);

						// Static taints must always go through the callee
						if (newSource.getAccessPath().isStaticFieldRef())
							passOn = false;

						// we only can remove the taint if we step into the
						// call/return edges
						// otherwise we will loose taint - see
						// ArrayTests/arrayCopyTest
						if (passOn && invExpr instanceof InstanceInvokeExpr
								&& (manager.getConfig().getInspectSources() || !isSource)
								&& (manager.getConfig().getInspectSinks() || !isSink)
								&& newSource.getAccessPath().isInstanceFieldRef() && (hasValidCallees
										|| (taintWrapper != null && taintWrapper.isExclusive(iCallStmt, newSource)))) {
							// If one of the callers does not read the value, we
							// must pass it on in any case
							boolean allCalleesRead = true;
							outer: for (SootMethod callee : interproceduralCFG().getCalleesOfCallAt(call)) {
								if (callee.isConcrete() && callee.hasActiveBody()) {
									Set<AccessPath> calleeAPs = mapAccessPathToCallee(callee, invExpr, null, null,
											source.getAccessPath());
									if (calleeAPs != null) {
										for (AccessPath ap : calleeAPs) {
											if (ap != null) {
												if (!interproceduralCFG().methodReadsValue(callee,
														ap.getPlainValue())) {
													allCalleesRead = false;
													break outer;
												}
											}
										}
									}
								}

								// Additional check: If all callees are library
								// classes, we pass it on as well
								if (isExcluded(callee)) {
									allCalleesRead = false;
									break;
								}
							}

							if (allCalleesRead) {
								if (aliasing.mayAlias(((InstanceInvokeExpr) invExpr).getBase(),
										newSource.getAccessPath().getPlainValue())) {
									passOn = false;
								}
								if (passOn)
									for (int i = 0; i < callArgs.length; i++)
										if (aliasing.mayAlias(callArgs[i], newSource.getAccessPath().getPlainValue())) {
											passOn = false;
											break;
										}
								// static variables are always propagated if
								// they are not overwritten. So if we have at
								// least one call/return edge pair,
								// we can be sure that the value does not get
								// "lost" if we do not pass it on:
								if (newSource.getAccessPath().isStaticFieldRef())
									passOn = false;
							}
						}

						// If the callee does not read the given value, we also
						// need to pass it on since we do not propagate it into
						// the callee.
						if (source.getAccessPath().isStaticFieldRef()) {
							if (!interproceduralCFG().isStaticFieldUsed(callee, source.getAccessPath().getFirstField()))
								passOn = true;
						}

						// Implicit taints are always passed over conditionally
						// called methods
						passOn |= source.getTopPostdominator() != null || source.getAccessPath().isEmpty();
						if (passOn) {
							if (newSource != getZeroValue())
								res.add(newSource);
						}

						if (callee.isNative())
							for (Value callVal : callArgs)
								if (callVal == newSource.getAccessPath().getPlainValue()) {
									// java uses call by value, but fields of
									// complex objects can be changed (and
									// tainted), so use this conservative
									// approach:
									Set<Abstraction> nativeAbs = ncHandler.getTaintedValues(iCallStmt, newSource,
											callArgs);
									if (nativeAbs != null) {
										res.addAll(nativeAbs);

										// Compute the aliases
										for (Abstraction abs : nativeAbs)
											if (abs.getAccessPath().isStaticFieldRef() || Aliasing.canHaveAliases(
													iCallStmt, abs.getAccessPath().getPlainValue(), abs))
												aliasing.computeAliases(d1, iCallStmt,
														abs.getAccessPath().getPlainValue(), res,
														interproceduralCFG().getMethodOf(call), abs);
									}

									// We only call the native code handler once
									// per statement
									break;
								}

						for (Abstraction abs : res)
							if (abs != newSource)
								abs.setCorrespondingCallSite(iCallStmt);

						return res;
					}
				};
			}

			/**
			 * Maps the given access path into the scope of the callee
			 * 
			 * @param callee      The method that is being called
			 * @param ie          The invocation expression for the call
			 * @param paramLocals The list of parameter locals in the callee
			 * @param thisLocal   The "this" local in the callee
			 * @param ap          The caller-side access path to map
			 * @return The set of callee-side access paths corresponding to the given
			 *         caller-side access path
			 */
			private Set<AccessPath> mapAccessPathToCallee(final SootMethod callee, final InvokeExpr ie,
					Value[] paramLocals, Local thisLocal, AccessPath ap) {
				// We do not transfer empty access paths
				if (ap.isEmpty())
					return null;

				// Android executor methods are handled specially.
				// getSubSignature()
				// is slow, so we try to avoid it whenever we can
				final boolean isExecutorExecute = interproceduralCFG().isExecutorExecute(ie, callee);

				Set<AccessPath> res = null;

				// If we can't reason about aliases, there's little we can do here
				final Aliasing aliasing = manager.getAliasing();
				if (aliasing == null)
					return null;

				// If we are performing lazy aliasing, we need to retain all
				// taints
				if (aliasing.getAliasingStrategy().isLazyAnalysis() && Aliasing.canHaveAliases(ap)) {
					res = new HashSet<>();
					res.add(ap);
				}

				// Is this a virtual method call?
				Value baseLocal = null;
				if (!isExecutorExecute && !ap.isStaticFieldRef() && !callee.isStatic()) {
					if (interproceduralCFG().isReflectiveCallSite(ie)) {
						// Method.invoke(target, arg0, ..., argn)
						baseLocal = ie.getArg(0);
					} else {
						assert ie instanceof InstanceInvokeExpr;
						InstanceInvokeExpr vie = (InstanceInvokeExpr) ie;
						baseLocal = vie.getBase();
					}
				}

				// If we have a base local to map, we need to find the
				// corresponding "this" local
				if (baseLocal != null) {
					if (aliasing.mayAlias(baseLocal, ap.getPlainValue()))
						if (manager.getTypeUtils().hasCompatibleTypesForCall(ap, callee.getDeclaringClass())) {
							if (res == null)
								res = new HashSet<AccessPath>();

							// Get the "this" local if we don't have it yet
							if (thisLocal == null)
								thisLocal = callee.getActiveBody().getThisLocal();

							res.add(manager.getAccessPathFactory().copyWithNewValue(ap, thisLocal));
						}
				}

				// special treatment for clinit methods - no param mapping
				// possible
				if (isExecutorExecute) {
					if (aliasing.mayAlias(ie.getArg(0), ap.getPlainValue())) {
						if (res == null)
							res = new HashSet<AccessPath>();
						res.add(manager.getAccessPathFactory().copyWithNewValue(ap,
								callee.getActiveBody().getThisLocal()));
					}
				} else if (callee.getParameterCount() > 0) {
					boolean isReflectiveCallSite = interproceduralCFG().isReflectiveCallSite(ie);

					// check if param is tainted:
					for (int i = isReflectiveCallSite ? 1 : 0; i < ie.getArgCount(); i++) {
						if (aliasing.mayAlias(ie.getArg(i), ap.getPlainValue())) {
							if (res == null)
								res = new HashSet<AccessPath>();

							// Get the parameter locals if we don't have them
							// yet
							if (paramLocals == null)
								paramLocals = callee.getActiveBody().getParameterLocals()
										.toArray(new Local[callee.getParameterCount()]);

							if (isReflectiveCallSite) {
								// Taint all parameters in the callee if the
								// argument array of a
								// reflective method call is tainted
								for (int j = 0; j < paramLocals.length; j++) {
									AccessPath newAP = manager.getAccessPathFactory().copyWithNewValue(ap,
											paramLocals[j], null, false);
									if (newAP != null)
										res.add(newAP);
								}
							} else {
								// Taint the corresponding parameter local in
								// the callee
								AccessPath newAP = manager.getAccessPathFactory().copyWithNewValue(ap, paramLocals[i]);
								if (newAP != null)
									res.add(newAP);
							}
						}
					}
				}
				return res;
			}
		};
	}

	@Override
	public boolean autoAddZero() {
		return false;
	}

	/**
	 * Gets the results of the data flow analysis
	 */
	public TaintPropagationResults getResults() {
		return this.results;
	}

	/**
	 * Gets the rules that FlowDroid uses internally to conduct specific analysis
	 * tasks such as handling sources or sinks
	 * 
	 * @return The propagation rule manager
	 */
	public PropagationRuleManager getPropagationRules() {
		return propagationRules;
	}

}
