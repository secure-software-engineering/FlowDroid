package soot.jimple.infoflow.river.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import heros.solver.Pair;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.NumericConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.data.ValueOnPath;
import soot.jimple.infoflow.data.ValueOnPath.Parameter;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.river.ConditionalSecondarySourceDefinition;
import soot.jimple.infoflow.river.TurnAroundSecondarySinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkCondition;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * A condition that checks additional data flow to see whether a source or sink
 * is valid or not based on classes and methods on the secondary data flow
 *
 * @author Steven Arzt
 *
 */
public class SignatureFlowCondition extends SourceSinkCondition {

	private final Set<String> classNamesOnPath;
	private final Set<String> signaturesOnPath;
	private final Set<String> excludedClassNames;

	private Set<SootMethod> methodsOnPath = null;
	private Set<SootClass> classesOnPath = null;
	private Set<SootClass> excludedClasses = null;
	private Set<ValueOnPath> valuesOnPath = null;

	/**
	 * Create a new additional flow condition
	 *
	 * @param classNamesOnPath   class names that have to be on the path
	 * @param signaturesOnPath   signatures that have to be on the path
	 * @param valuesOnPath       values that have to be on path
	 * @param excludedClassNames class names of primary sinks that should be
	 *                           filtered without context, e.g.
	 *                           ByteArrayOutputStream for OutputStream
	 */
	public SignatureFlowCondition(Set<String> classNamesOnPath, Set<String> signaturesOnPath,
			Set<ValueOnPath> valuesOnPath, Set<String> excludedClassNames) {
		this.classNamesOnPath = classNamesOnPath;
		this.signaturesOnPath = signaturesOnPath;
		this.valuesOnPath = valuesOnPath;
		this.excludedClassNames = excludedClassNames;
		if (valuesOnPath != null) {
			for (ValueOnPath v : valuesOnPath) {
				if (signaturesOnPath == null)
					signaturesOnPath = new HashSet<>();
				signaturesOnPath.add(v.getInvocation());
			}
		}
	}

	@Override
	public boolean evaluate(DataFlowResult result, InfoflowResults results, InfoflowManager manager) {
		// If we have nothing to check, we accept everything
		if (isEmpty())
			return true;

		MultiMap<ResultSinkInfo, ResultSourceInfo> additionalResults = results.getAdditionalResults();
		if (additionalResults == null || additionalResults.isEmpty())
			return false;

		// Get the connected flows
		final Stmt sinkStmt = result.getSink().getStmt();
		final Type baseType = result.getSource().getAccessPath().getBaseType();
		// Shortcut: There won't be a path if the class is excluded anyway
		ensureSootClassesOfExcludedClasses();
		if (this.classMatches(baseType.toString(), excludedClasses))
			return false;

		// Because we injected the taint in the SecondaryFlowGenerator with a
		// SecondarySinkDefinition,
		// if there is a flow containing the sink, it is always also in the MultiMap.

		return checkConditions(result, additionalResults, sinkStmt, manager);
	}

	private boolean signatureMatches(String sig, Set<SootMethod> methodsCheck) {
		SootMethod sm = Scene.v().grabMethod(sig);
		if (sm == null)
			return false;

		if (methodsCheck.contains(sm))
			return true;

		for (SootClass ifc : sm.getDeclaringClass().getInterfaces()) {
			SootMethod superMethod = ifc.getMethodUnsafe(sm.getSubSignature());
			if (superMethod != null && methodsCheck.contains(superMethod))
				return true;
		}

		SootClass superClass = sm.getDeclaringClass().getSuperclassUnsafe();
		while (superClass != null) {
			SootMethod superMethod = superClass.getMethodUnsafe(sm.getSubSignature());
			if (superMethod != null && methodsCheck.contains(superMethod))
				return true;
			superClass = superClass.getSuperclassUnsafe();
		}

		return false;
	}

	private boolean classMatches(String sig, Set<SootClass> classes) {
		SootClass sc = Scene.v().getSootClassUnsafe(sig);
		if (sc == null)
			return false;

		if (classes.contains(sc))
			return true;

		for (SootClass ifc : sc.getInterfaces())
			if (classes.contains(ifc))
				return true;

		SootClass superClass = sc.getSuperclassUnsafe();
		while (superClass != null) {
			if (classes.contains(superClass))
				return true;
			superClass = superClass.getSuperclassUnsafe();
		}

		return false;
	}

	@Override
	public Set<SootMethod> getReferencedMethods() {
		ensureSootMethodsOnPath();
		return methodsOnPath;
	}

	@Override
	public Set<SootClass> getReferencedClasses() {
		ensureSootClassesOnPath();
		return classesOnPath;
	}

	@Override
	public Set<SootClass> getExcludedClasses() {
		ensureSootClassesOfExcludedClasses();
		return excludedClasses;
	}

	/**
	 * Return the signatures that should be on the path
	 *
	 * @return unmodifiable set of signatures
	 */
	public Set<String> getSignaturesOnPath() {
		return Collections.unmodifiableSet(signaturesOnPath);
	}

	/**
	 * Return the class names that should be on the path
	 *
	 * @return unmodifiable set of class names
	 */
	public Set<String> getClassNamesOnPath() {
		return Collections.unmodifiableSet(classNamesOnPath);
	}

	/**
	 * Return the excluded class names
	 *
	 * @return unmodifiable set of excluded class names
	 */
	public Set<String> getExcludedClassNames() {
		return Collections.unmodifiableSet(excludedClassNames);
	}

	/**
	 * Ensures that the set of Soot methods on the data flow path has been
	 * initialized
	 */
	private void ensureSootMethodsOnPath() {
		if (methodsOnPath == null) {
			methodsOnPath = new HashSet<>();
			if (signaturesOnPath != null && !signaturesOnPath.isEmpty()) {
				for (String sig : signaturesOnPath) {
					SootMethod sm = Scene.v().grabMethod(sig);
					if (sm != null)
						methodsOnPath.add(sm);
				}
			}
		}
	}

	/**
	 * Ensures that the set of Soot classeson the data flow path has been
	 * initialized
	 */
	private void ensureSootClassesOnPath() {
		if (classesOnPath == null)
			classesOnPath = resolveSootClassesToSet(classNamesOnPath);
	}

	/**
	 * Ensures that the set of excluded classes for the condition
	 */
	private void ensureSootClassesOfExcludedClasses() {
		if (excludedClasses == null)
			excludedClasses = resolveSootClassesToSet(excludedClassNames);
	}

	private Set<SootClass> resolveSootClassesToSet(Set<String> classNameSet) {
		Set<SootClass> resolved = new HashSet<>();
		if (classNameSet != null && !classNameSet.isEmpty()) {
			for (String className : classNameSet) {
				SootClass sc = Scene.v().getSootClassUnsafe(className);
				if (sc != null)
					resolved.add(sc);
			}
		}
		return resolved;
	}

	/**
	 * Checks the conditions
	 * 
	 * @param result
	 *
	 * @param additionalResults MultiMap containing the additional results
	 * @param primarySinkStmt   Sink of interest
	 * @param manager
	 * @return true if the conditions matched
	 */
	protected boolean checkConditions(DataFlowResult result,
			MultiMap<ResultSinkInfo, ResultSourceInfo> additionalResults, Stmt primarySinkStmt,
			InfoflowManager manager) {
		Set<String> sigSet = new HashSet<>();
		Set<String> classSet = new HashSet<>();
		MultiMap<ValueOnPath, Stmt> valueStmtMap = new HashMultiMap<>();

		List<Pair<ResultSourceInfo, ResultSinkInfo>> turnAroundFlows = new ArrayList<>();
		for (ResultSinkInfo secondarySinkInfo : additionalResults.keySet()) {
			for (ResultSourceInfo secondarySourceInfo : additionalResults.get(secondarySinkInfo)) {
				if (secondarySourceInfo.getDefinition() instanceof TurnAroundSecondarySinkDefinition) {
					turnAroundFlows.add(new Pair<>(secondarySourceInfo, secondarySinkInfo));
				}

			}
		}

		for (ResultSinkInfo secondarySinkInfo : additionalResults.keySet()) {
			for (ResultSourceInfo secondarySourceInfo : additionalResults.get(secondarySinkInfo)) {
				if (!(secondarySourceInfo.getDefinition() instanceof ConditionalSecondarySourceDefinition))
					continue;
				// Match secondary source with primary sink of interest
				boolean matchesStmt = secondarySourceInfo.getStmt() == primarySinkStmt;
				boolean hasTurnAround = false;
				Stmt valueStmt = secondarySinkInfo.getStmt();
				if (!matchesStmt) {
					hasTurnAround = secondarySinkInfo.getDefinition() instanceof TurnAroundSecondarySinkDefinition;
					if (hasTurnAround) {
						valueStmt = null;
						Stmt stmt = secondarySinkInfo.getStmt();
						for (Pair<ResultSourceInfo, ResultSinkInfo> ta : turnAroundFlows) {
							ResultSourceInfo src = ta.getO1();
							if (src.getStmt() == stmt
									&& src.getAccessPath().equals(secondarySinkInfo.getAccessPath())) {
								matchesStmt = true;
								valueStmt = ta.getO2().getStmt();
								if (valueStmt.containsInvokeExpr()) {
									SootMethod callee = valueStmt.getInvokeExpr().getMethod();
									sigSet.add(callee.getSignature());
									classSet.add(callee.getDeclaringClass().getName());
								}
								break;
							}
						}
					}
				}
				if (matchesStmt) {
					if (secondarySourceInfo.getPath() == null) {
						// Fall back if path reconstruction is not enabled
						SootMethod callee = secondarySinkInfo.getStmt().getInvokeExpr().getMethod();
						sigSet.add(callee.getSignature());
						classSet.add(callee.getDeclaringClass().getName());
						if (valueStmt != null) {
							mapValueOnPath(valueStmtMap, valueStmt);
						}
					} else {
						Stmt[] path = secondarySourceInfo.getPath();
						for (Stmt stmt : path) {
							if (stmt.containsInvokeExpr()) {
								// Register all calls on the path
								SootMethod callee = stmt.getInvokeExpr().getMethod();
								sigSet.add(callee.getSignature());
								classSet.add(callee.getDeclaringClass().getName());
								mapValueOnPath(valueStmtMap, stmt);
							}
						}
					}
				}
			}
		}
		boolean sigMatch = signaturesOnPath == null || signaturesOnPath.isEmpty()
				|| sigSet.stream().anyMatch(c -> this.signatureMatches(c, methodsOnPath));
		boolean classMatch = classesOnPath == null || classesOnPath.isEmpty()
				|| classSet.stream().anyMatch(c -> this.classMatches(c, classesOnPath));
		boolean valuesMatch = valuesOnPath == null || valuesOnPath.isEmpty()
				|| valuesOnPath.stream().anyMatch(c -> this.valuesMatches(c, valueStmtMap.get(c)));
		return sigMatch && classMatch && valuesMatch;
	}

	private void mapValueOnPath(MultiMap<ValueOnPath, Stmt> results, Stmt stmt) {
		if (valuesOnPath != null) {
			for (ValueOnPath v : valuesOnPath) {
				SootMethod m = Scene.v().grabMethod(v.getInvocation());

				if (m != null && signatureMatches(stmt.getInvokeExpr().getMethod().getSignature(),
						Collections.singleton(m))) {
					results.put(v, stmt);
				}
			}
		}
	}

	private boolean valuesMatches(ValueOnPath c, Set<Stmt> stmts) {
		nextStmt: for (Stmt s : stmts) {
			InvokeExpr inv = s.getInvokeExpr();
			// we use AND on the parameters
			for (Parameter p : c.getParameters()) {
				int idx = p.getParameterIndex();
				if (idx < 0 || idx >= inv.getArgCount()) {
					continue nextStmt;
				}
				Value v = inv.getArg(idx);
				if (v instanceof Constant) {
					String cmp;
					if (v instanceof StringConstant)
						cmp = ((StringConstant) v).value;
					else if (v instanceof NumericConstant)
						cmp = String.valueOf(((NumericConstant) v).getNumericValue());
					else if (v instanceof ClassConstant)
						cmp = ((ClassConstant) v).getValue();
					else
						continue nextStmt;

					String vopContent = p.getContentToMatch();

					boolean matched = false;
					if (p.isRegex()) {
						matched = p.getRegexMatcher().matcher(cmp).matches();
					} else {
						if (!p.isCaseSensitive()) {
							matched = cmp.equalsIgnoreCase(vopContent);
						} else {
							matched = cmp.equals(vopContent);
						}
					}
					if (!matched)
						continue nextStmt;
				}
			}
			// all matched
			return true;
		}
		return false;
	}

	/**
	 * Gets whether this flow condition is empty, i.e., has nothing to check for
	 *
	 * @return True if this flow condition is empty, false otherwise
	 */
	public boolean isEmpty() {
		return (classNamesOnPath == null || classNamesOnPath.isEmpty())
				&& (signaturesOnPath == null || signaturesOnPath.isEmpty());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classNamesOnPath == null) ? 0 : classNamesOnPath.hashCode());
		result = prime * result + ((signaturesOnPath == null) ? 0 : signaturesOnPath.hashCode());
		result = prime * result + ((excludedClassNames == null) ? 0 : excludedClassNames.hashCode());
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
		SignatureFlowCondition other = (SignatureFlowCondition) obj;
		if (classNamesOnPath == null) {
			if (other.classNamesOnPath != null)
				return false;
		} else if (!classNamesOnPath.equals(other.classNamesOnPath))
			return false;
		if (signaturesOnPath == null) {
			if (other.signaturesOnPath != null)
				return false;
		} else if (!signaturesOnPath.equals(other.signaturesOnPath))
			return false;
		if (excludedClassNames == null) {
			if (other.excludedClassNames != null)
				return false;
		} else if (!excludedClassNames.equals(other.excludedClassNames))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AdditionalFlowCondition: " + "classNamesOnPath=" + classNamesOnPath + ", signaturesOnPath="
				+ signaturesOnPath + ", excludedClasses=" + excludedClassNames;
	}

	@Override
	public Set<SootMethodAndClass> getReferencedMethodDefs() {
		final SootMethodRepresentationParser rep = SootMethodRepresentationParser.v();
		return signaturesOnPath.stream().map(s -> rep.parseSootMethodString(s)).collect(Collectors.toSet());
	}
}
