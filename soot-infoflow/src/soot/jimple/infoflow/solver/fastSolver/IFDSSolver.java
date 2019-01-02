/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * Copyright (c) 2013 Tata Consultancy Services & Ecole Polytechnique de Montreal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 *     Marc-Andre Laverdiere-Papineau - Fixed race condition
 *     Steven Arzt - Created FastSolver implementation
 ******************************************************************************/
package soot.jimple.infoflow.solver.fastSolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;

import heros.DontSynchronize;
import heros.FlowFunction;
import heros.FlowFunctionCache;
import heros.FlowFunctions;
import heros.IFDSTabulationProblem;
import heros.SynchronizedBy;
import heros.ZeroedFlowFunctions;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.solver.PredecessorShorteningMode;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.executors.SetPoolExecutor;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * A solver for an {@link IFDSTabulationProblem}. This solver is not based on
 * the IDESolver implementation in Heros for performance reasons.
 * 
 * @param <N> The type of nodes in the interprocedural control-flow graph.
 *        Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation
 *        problem.
 * @param <I> The type of inter-procedural control-flow graph being used.
 * @see IFDSTabulationProblem
 */
public class IFDSSolver<N, D extends FastSolverLinkedNode<D, N>, I extends BiDiInterproceduralCFG<N, SootMethod>>
		implements IMemoryBoundedSolver {

	public static CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER = CacheBuilder.newBuilder()
			.concurrencyLevel(Runtime.getRuntime().availableProcessors()).initialCapacity(10000).softValues();

	protected static final Logger logger = LoggerFactory.getLogger(IFDSSolver.class);

	// enable with -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
	public static final boolean DEBUG = logger.isDebugEnabled();

	protected InterruptableExecutor executor;

	@DontSynchronize("only used by single thread")
	protected int numThreads;

	@SynchronizedBy("thread safe data structure, consistent locking when used")
	protected MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctions = new MyConcurrentHashMap<PathEdge<N, D>, D>();

	@SynchronizedBy("thread safe data structure, only modified internally")
	protected final I icfg;

	// stores summaries that were queried before they were computed
	// see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on 'incoming'")
	protected final MyConcurrentHashMap<Pair<SootMethod, D>, Set<Pair<N, D>>> endSummary = new MyConcurrentHashMap<Pair<SootMethod, D>, Set<Pair<N, D>>>();

	// edges going along calls
	// see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on field")
	protected final MyConcurrentHashMap<Pair<SootMethod, D>, MyConcurrentHashMap<N, Map<D, D>>> incoming = new MyConcurrentHashMap<Pair<SootMethod, D>, MyConcurrentHashMap<N, Map<D, D>>>();

	@DontSynchronize("stateless")
	protected final FlowFunctions<N, D, SootMethod> flowFunctions;

	@DontSynchronize("only used by single thread")
	protected final Map<N, Set<D>> initialSeeds;

	@DontSynchronize("benign races")
	public long propagationCount;

	@DontSynchronize("stateless")
	protected final D zeroValue;

	@DontSynchronize("readOnly")
	protected final FlowFunctionCache<N, D, SootMethod> ffCache;

	@DontSynchronize("readOnly")
	protected final boolean followReturnsPastSeeds;

	@DontSynchronize("readOnly")
	protected PredecessorShorteningMode shorteningMode = PredecessorShorteningMode.NeverShorten;

	@DontSynchronize("readOnly")
	private int maxJoinPointAbstractions = -1;

	@DontSynchronize("readOnly")
	protected IMemoryManager<D, N> memoryManager = null;

	protected boolean solverId;

	private Set<IMemoryBoundedSolverStatusNotification> notificationListeners = new HashSet<>();
	private ISolverTerminationReason killFlag = null;

	private int maxCalleesPerCallSite = 75;
	private int maxAbstractionPathLength = 100;

	/**
	 * Creates a solver for the given problem, which caches flow functions and edge
	 * functions. The solver must then be started by calling {@link #solve()}.
	 */
	public IFDSSolver(IFDSTabulationProblem<N, D, SootMethod, I> tabulationProblem) {
		this(tabulationProblem, DEFAULT_CACHE_BUILDER);
	}

	/**
	 * Creates a solver for the given problem, constructing caches with the given
	 * {@link CacheBuilder}. The solver must then be started by calling
	 * {@link #solve()}.
	 * 
	 * @param tabulationProblem        The tabulation problem to solve
	 * @param flowFunctionCacheBuilder A valid {@link CacheBuilder} or
	 *                                 <code>null</code> if no caching is to be used
	 *                                 for flow functions.
	 */
	public IFDSSolver(IFDSTabulationProblem<N, D, SootMethod, I> tabulationProblem,
			@SuppressWarnings("rawtypes") CacheBuilder flowFunctionCacheBuilder) {
		if (logger.isDebugEnabled())
			flowFunctionCacheBuilder = flowFunctionCacheBuilder.recordStats();
		this.zeroValue = tabulationProblem.zeroValue();
		this.icfg = tabulationProblem.interproceduralCFG();
		FlowFunctions<N, D, SootMethod> flowFunctions = tabulationProblem.autoAddZero()
				? new ZeroedFlowFunctions<N, D, SootMethod>(tabulationProblem.flowFunctions(), zeroValue)
				: tabulationProblem.flowFunctions();
		if (flowFunctionCacheBuilder != null) {
			ffCache = new FlowFunctionCache<N, D, SootMethod>(flowFunctions, flowFunctionCacheBuilder);
			flowFunctions = ffCache;
		} else {
			ffCache = null;
		}
		this.flowFunctions = flowFunctions;
		this.initialSeeds = tabulationProblem.initialSeeds();
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.numThreads = Math.max(1, tabulationProblem.numThreads());
		this.executor = getExecutor();
	}

	public void setSolverId(boolean solverId) {
		this.solverId = solverId;
	}

	/**
	 * Runs the solver on the configured problem. This can take some time.
	 */
	public void solve() {
		reset();

		// Notify the listeners that the solver has been started
		for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
			listener.notifySolverStarted(this);

		submitInitialSeeds();
		awaitCompletionComputeValuesAndShutdown();

		// Notify the listeners that the solver has been terminated
		for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
			listener.notifySolverTerminated(this);
	}

	/**
	 * Schedules the processing of initial seeds, initiating the analysis. Clients
	 * should only call this methods if performing synchronization on their own.
	 * Normally, {@link #solve()} should be called instead.
	 */
	protected void submitInitialSeeds() {
		for (Entry<N, Set<D>> seed : initialSeeds.entrySet()) {
			N startPoint = seed.getKey();
			for (D val : seed.getValue())
				propagate(zeroValue, startPoint, val, null, false);
			addFunction(new PathEdge<N, D>(zeroValue, startPoint, zeroValue));
		}
	}

	/**
	 * Awaits the completion of the exploded super graph. When complete, computes
	 * result values, shuts down the executor and returns.
	 */
	protected void awaitCompletionComputeValuesAndShutdown() {
		{
			// run executor and await termination of tasks
			runExecutorAndAwaitCompletion();
		}
		if (logger.isDebugEnabled())
			printStats();

		// ask executor to shut down;
		// this will cause new submissions to the executor to be rejected,
		// but at this point all tasks should have completed anyway
		executor.shutdown();

		// Wait for the executor to be really gone
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// silently ignore the exception, it's not an issue if the
				// thread gets aborted
			}
		}
	}

	/**
	 * Runs execution, re-throwing exceptions that might be thrown during its
	 * execution.
	 */
	private void runExecutorAndAwaitCompletion() {
		try {
			executor.awaitCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Throwable exception = executor.getException();
		if (exception != null) {
			throw new RuntimeException("There were exceptions during IFDS analysis. Exiting.", exception);
		}
	}

	/**
	 * Dispatch the processing of a given edge. It may be executed in a different
	 * thread.
	 * 
	 * @param edge the edge to process
	 */
	protected void scheduleEdgeProcessing(PathEdge<N, D> edge) {
		// If the executor has been killed, there is little point
		// in submitting new tasks
		if (killFlag != null || executor.isTerminating() || executor.isTerminated())
			return;

		executor.execute(new PathEdgeProcessingTask(edge, solverId));
		propagationCount++;
	}

	/**
	 * Lines 13-20 of the algorithm; processing a call site in the caller's context.
	 * 
	 * For each possible callee, registers incoming call edges. Also propagates
	 * call-to-return flows and summarized callee flows within the caller.
	 * 
	 * @param edge an edge whose target node resembles a method call
	 */
	private void processCall(PathEdge<N, D> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); // a call node; line 14...

		final D d2 = edge.factAtTarget();
		assert d2 != null;
		Collection<N> returnSiteNs = icfg.getReturnSitesOfCallAt(n);

		// for each possible callee
		Collection<SootMethod> callees = icfg.getCalleesOfCallAt(n);
		if (maxCalleesPerCallSite < 0 || callees.size() <= maxCalleesPerCallSite) {
			callees.stream().filter(m -> m.isConcrete()).forEach(new Consumer<SootMethod>() {

				@Override
				public void accept(SootMethod sCalledProcN) {
					// Early termination check
					if (killFlag != null)
						return;

					// compute the call-flow function
					FlowFunction<D> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
					Set<D> res = computeCallFlowFunction(function, d1, d2);

					if (res != null && !res.isEmpty()) {
						Collection<N> startPointsOf = icfg.getStartPointsOf(sCalledProcN);
						// for each result node of the call-flow function
						for (D d3 : res) {
							if (memoryManager != null)
								d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
							if (d3 == null)
								continue;

							// for each callee's start point(s)
							for (N sP : startPointsOf) {
								// create initial self-loop
								propagate(d3, sP, d3, n, false); // line 15
							}

							// register the fact that <sp,d3> has an incoming edge from
							// <n,d2>
							// line 15.1 of Naeem/Lhotak/Rodriguez
							if (!addIncoming(sCalledProcN, d3, n, d1, d2))
								continue;

							applyEndSummaryOnCall(d1, n, d2, returnSiteNs, sCalledProcN, d3);
						}
					}
				}

			});
		}

		// line 17-19 of Naeem/Lhotak/Rodriguez
		// process intra-procedural flows along call-to-return flow functions
		for (N returnSiteN : returnSiteNs) {
			FlowFunction<D> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n, returnSiteN);
			Set<D> res = computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2);
			if (res != null && !res.isEmpty()) {
				for (D d3 : res) {
					if (memoryManager != null)
						d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
					if (d3 != null)
						propagate(d1, returnSiteN, d3, n, false);
				}
			}
		}
	}

	protected void applyEndSummaryOnCall(final D d1, final N n, final D d2, Collection<N> returnSiteNs,
			SootMethod sCalledProcN, D d3) {
		// line 15.2
		Set<Pair<N, D>> endSumm = endSummary(sCalledProcN, d3);

		// still line 15.2 of Naeem/Lhotak/Rodriguez
		// for each already-queried exit value <eP,d4> reachable
		// from <sP,d3>, create new caller-side jump functions to
		// the return sites because we have observed a potentially
		// new incoming edge into <sP,d3>
		if (endSumm != null && !endSumm.isEmpty()) {
			for (Pair<N, D> entry : endSumm) {
				N eP = entry.getO1();
				D d4 = entry.getO2();
				// for each return site
				for (N retSiteN : returnSiteNs) {
					// compute return-flow function
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, eP, retSiteN);
					Set<D> retFlowRes = computeReturnFlowFunction(retFunction, d3, d4, n, Collections.singleton(d1));
					if (retFlowRes != null && !retFlowRes.isEmpty()) {
						// for each target value of the function
						for (D d5 : retFlowRes) {
							if (memoryManager != null)
								d5 = memoryManager.handleGeneratedMemoryObject(d4, d5);

							// If we have not changed anything in
							// the callee, we do not need the facts from
							// there. Even if we change something:
							// If we don't need the concrete path,
							// we can skip the callee in the predecessor
							// chain
							D d5p = d5;
							switch (shorteningMode) {
							case AlwaysShorten:
								if (d5p != d2) {
									d5p = d5p.clone();
									d5p.setPredecessor(d2);
								}
								break;
							case ShortenIfEqual:
								if (d5.equals(d2))
									d5p = d2;
								break;
							}
							propagate(d1, retSiteN, d5p, n, false);
						}
					}
				}
			}
		}
	}

	/**
	 * Computes the call flow function for the given call-site abstraction
	 * 
	 * @param callFlowFunction The call flow function to compute
	 * @param d1               The abstraction at the current method's start node.
	 * @param d2               The abstraction at the call site
	 * @return The set of caller-side abstractions at the callee's start node
	 */
	protected Set<D> computeCallFlowFunction(FlowFunction<D> callFlowFunction, D d1, D d2) {
		return callFlowFunction.computeTargets(d2);
	}

	/**
	 * Computes the call-to-return flow function for the given call-site abstraction
	 * 
	 * @param callToReturnFlowFunction The call-to-return flow function to compute
	 * @param d1                       The abstraction at the current method's start
	 *                                 node.
	 * @param d2                       The abstraction at the call site
	 * @return The set of caller-side abstractions at the return site
	 */
	protected Set<D> computeCallToReturnFlowFunction(FlowFunction<D> callToReturnFlowFunction, D d1, D d2) {
		return callToReturnFlowFunction.computeTargets(d2);
	}

	/**
	 * Lines 21-32 of the algorithm.
	 * 
	 * Stores callee-side summaries. Also, at the side of the caller, propagates
	 * intra-procedural flows to return sites using those newly computed summaries.
	 * 
	 * @param edge an edge whose target node resembles a method exits
	 */
	protected void processExit(PathEdge<N, D> edge) {
		final N n = edge.getTarget(); // an exit node; line 21...
		SootMethod methodThatNeedsSummary = icfg.getMethodOf(n);

		final D d1 = edge.factAtSource();
		final D d2 = edge.factAtTarget();

		// for each of the method's start points, determine incoming calls

		// line 21.1 of Naeem/Lhotak/Rodriguez
		// register end-summary
		if (!addEndSummary(methodThatNeedsSummary, d1, n, d2))
			return;
		Map<N, Map<D, D>> inc = incoming(d1, methodThatNeedsSummary);

		// for each incoming call edge already processed
		// (see processCall(..))
		if (inc != null && !inc.isEmpty())
			for (Entry<N, Map<D, D>> entry : inc.entrySet()) {
				// Early termination check
				if (killFlag != null)
					return;

				// line 22
				N c = entry.getKey();
				Set<D> callerSideDs = entry.getValue().keySet();
				// for each return site
				for (N retSiteC : icfg.getReturnSitesOfCallAt(c)) {
					// compute return-flow function
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
							retSiteC);
					Set<D> targets = computeReturnFlowFunction(retFunction, d1, d2, c, callerSideDs);
					// for each incoming-call value
					if (targets != null && !targets.isEmpty()) {
						for (Entry<D, D> d1d2entry : entry.getValue().entrySet()) {
							final D d4 = d1d2entry.getKey();
							final D predVal = d1d2entry.getValue();

							for (D d5 : targets) {
								if (memoryManager != null)
									d5 = memoryManager.handleGeneratedMemoryObject(d2, d5);
								if (d5 == null)
									continue;

								// If we have not changed anything in the callee, we do not need the facts from
								// there. Even if we change something: If we don't need the concrete path, we
								// can skip the callee in the predecessor chain
								D d5p = d5;
								switch (shorteningMode) {
								case AlwaysShorten:
									if (d5p != predVal) {
										d5p = d5p.clone();
										d5p.setPredecessor(predVal);
									}
									break;
								case ShortenIfEqual:
									if (d5.equals(predVal))
										d5p = predVal;
									break;
								}
								propagate(d4, retSiteC, d5p, c, false);
							}
						}
					}
				}
			}

		// handling for unbalanced problems where we return out of a method with
		// a fact for which we have no incoming flow
		// note: we propagate that way only values that originate from ZERO, as
		// conditionally generated values should only be propagated into callers that
		// have an incoming edge for this condition
		if (followReturnsPastSeeds && d1 == zeroValue && (inc == null || inc.isEmpty())) {
			Collection<N> callers = icfg.getCallersOf(methodThatNeedsSummary);
			for (N c : callers) {
				for (N retSiteC : icfg.getReturnSitesOfCallAt(c)) {
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
							retSiteC);
					Set<D> targets = computeReturnFlowFunction(retFunction, d1, d2, c,
							Collections.singleton(zeroValue));
					if (targets != null && !targets.isEmpty()) {
						for (D d5 : targets) {
							if (memoryManager != null)
								d5 = memoryManager.handleGeneratedMemoryObject(d2, d5);
							if (d5 != null)
								propagate(zeroValue, retSiteC, d5, c, true);
						}
					}
				}
			}
			// in cases where there are no callers, the return statement would
			// normally not be processed at all; this might be undesirable if the flow
			// function has a side effect such as registering a taint; instead we thus call
			// the return flow function will a null caller
			if (callers.isEmpty()) {
				FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(null, methodThatNeedsSummary, n,
						null);
				retFunction.computeTargets(d2);
			}
		}
	}

	/**
	 * Computes the return flow function for the given set of caller-side
	 * abstractions.
	 * 
	 * @param retFunction  The return flow function to compute
	 * @param d1           The abstraction at the beginning of the callee
	 * @param d2           The abstraction at the exit node in the callee
	 * @param callSite     The call site
	 * @param callerSideDs The abstractions at the call site
	 * @return The set of caller-side abstractions at the return site
	 */
	protected Set<D> computeReturnFlowFunction(FlowFunction<D> retFunction, D d1, D d2, N callSite,
			Collection<D> callerSideDs) {
		return retFunction.computeTargets(d2);
	}

	/**
	 * Lines 33-37 of the algorithm. Simply propagate normal, intra-procedural
	 * flows.
	 * 
	 * @param edge
	 */
	private void processNormalFlow(PathEdge<N, D> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget();
		final D d2 = edge.factAtTarget();

		for (N m : icfg.getSuccsOf(n)) {
			// Early termination check
			if (killFlag != null)
				return;

			// Compute the flow function
			FlowFunction<D> flowFunction = flowFunctions.getNormalFlowFunction(n, m);
			Set<D> res = computeNormalFlowFunction(flowFunction, d1, d2);
			if (res != null && !res.isEmpty()) {
				for (D d3 : res) {
					if (memoryManager != null && d2 != d3)
						d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
					if (d3 != null)
						propagate(d1, m, d3, null, false);
				}
			}
		}
	}

	/**
	 * Computes the normal flow function for the given set of start and end
	 * abstractions.
	 * 
	 * @param flowFunction The normal flow function to compute
	 * @param d1           The abstraction at the method's start node
	 * @param d2           The abstraction at the current node
	 * @return The set of abstractions at the successor node
	 */
	protected Set<D> computeNormalFlowFunction(FlowFunction<D> flowFunction, D d1, D d2) {
		return flowFunction.computeTargets(d2);
	}

	/**
	 * Propagates the flow further down the exploded super graph.
	 * 
	 * @param sourceVal          the source value of the propagated summary edge
	 * @param target             the target statement
	 * @param targetVal          the target value at the target statement
	 * @param relatedCallSite    for call and return flows the related call
	 *                           statement, <code>null</code> otherwise (this value
	 *                           is not used within this implementation but may be
	 *                           useful for subclasses of {@link IFDSSolver})
	 * @param isUnbalancedReturn <code>true</code> if this edge is propagating an
	 *                           unbalanced return (this value is not used within
	 *                           this implementation but may be useful for
	 *                           subclasses of {@link IFDSSolver})
	 */
	protected void propagate(D sourceVal, N target, D targetVal,
			/* deliberately exposed to clients */ N relatedCallSite,
			/* deliberately exposed to clients */ boolean isUnbalancedReturn) {
		// Let the memory manager run
		if (memoryManager != null) {
			sourceVal = memoryManager.handleMemoryObject(sourceVal);
			targetVal = memoryManager.handleMemoryObject(targetVal);
			if (targetVal == null)
				return;
		}

		// Check the path length
		if (maxAbstractionPathLength >= 0 && targetVal.getPathLength() > maxAbstractionPathLength)
			return;

		final PathEdge<N, D> edge = new PathEdge<N, D>(sourceVal, target, targetVal);
		final D existingVal = addFunction(edge);
		if (existingVal != null) {
			if (existingVal != targetVal) {
				// Check whether we need to retain this abstraction
				boolean isEssential;
				if (memoryManager == null)
					isEssential = relatedCallSite != null && icfg.isCallStmt(relatedCallSite);
				else
					isEssential = memoryManager.isEssentialJoinPoint(targetVal, relatedCallSite);

				if (maxJoinPointAbstractions < 0 || existingVal.getNeighborCount() < maxJoinPointAbstractions
						|| isEssential) {
					existingVal.addNeighbor(targetVal);
				}
			}
		} else {
			// If this is an inactive abstraction and we have already processed
			// its active counterpart, we can skip this one
			D activeVal = targetVal.getActiveCopy();
			if (activeVal != targetVal) {
				PathEdge<N, D> activeEdge = new PathEdge<>(sourceVal, target, activeVal);
				if (jumpFunctions.containsKey(activeEdge))
					return;
			}
			scheduleEdgeProcessing(edge);
		}
	}

	/**
	 * Records a jump function. The source statement is implicit.
	 * 
	 * @see PathEdge
	 */
	public D addFunction(PathEdge<N, D> edge) {
		return jumpFunctions.putIfAbsent(edge, edge.factAtTarget());
	}

	protected Set<Pair<N, D>> endSummary(SootMethod m, D d3) {
		Set<Pair<N, D>> map = endSummary.get(new Pair<SootMethod, D>(m, d3));
		return map;
	}

	private boolean addEndSummary(SootMethod m, D d1, N eP, D d2) {
		if (d1 == zeroValue)
			return true;

		Set<Pair<N, D>> summaries = endSummary.putIfAbsentElseGet(new Pair<SootMethod, D>(m, d1),
				new ConcurrentHashSet<Pair<N, D>>());
		return summaries.add(new Pair<N, D>(eP, d2));
	}

	protected Map<N, Map<D, D>> incoming(D d1, SootMethod m) {
		Map<N, Map<D, D>> map = incoming.get(new Pair<SootMethod, D>(m, d1));
		return map;
	}

	protected boolean addIncoming(SootMethod m, D d3, N n, D d1, D d2) {
		MyConcurrentHashMap<N, Map<D, D>> summaries = incoming.putIfAbsentElseGet(new Pair<SootMethod, D>(m, d3),
				new MyConcurrentHashMap<N, Map<D, D>>());
		Map<D, D> set = summaries.putIfAbsentElseGet(n, new ConcurrentHashMap<D, D>());
		return set.put(d1, d2) == null;
	}

	/**
	 * Factory method for this solver's thread-pool executor.
	 */
	protected InterruptableExecutor getExecutor() {
		SetPoolExecutor executor = new SetPoolExecutor(1, this.numThreads, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		executor.setThreadFactory(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thrIFDS = new Thread(r);
				thrIFDS.setDaemon(true);
				thrIFDS.setName("IFDS Solver");
				return thrIFDS;
			}
		});
		return executor;
	}

	/**
	 * Returns a String used to identify the output of this solver in debug mode.
	 * Subclasses can overwrite this string to distinguish the output from different
	 * solvers.
	 */
	protected String getDebugName() {
		return "FAST IFDS SOLVER";
	}

	public void printStats() {
		if (logger.isDebugEnabled()) {
			if (ffCache != null)
				ffCache.printStats();
		} else {
			logger.info("No statistics were collected, as DEBUG is disabled.");
		}
	}

	private class PathEdgeProcessingTask implements Runnable {

		private final PathEdge<N, D> edge;
		private final boolean solverId;

		public PathEdgeProcessingTask(PathEdge<N, D> edge, boolean solverId) {
			this.edge = edge;
			this.solverId = solverId;
		}

		public void run() {
			if (icfg.isCallStmt(edge.getTarget())) {
				processCall(edge);
			} else {
				// note that some statements, such as "throw" may be
				// both an exit statement and a "normal" statement
				if (icfg.isExitStmt(edge.getTarget()))
					processExit(edge);
				if (!icfg.getSuccsOf(edge.getTarget()).isEmpty())
					processNormalFlow(edge);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((edge == null) ? 0 : edge.hashCode());
			result = prime * result + (solverId ? 1231 : 1237);
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
			PathEdgeProcessingTask other = (PathEdgeProcessingTask) obj;
			if (edge == null) {
				if (other.edge != null)
					return false;
			} else if (!edge.equals(other.edge))
				return false;
			if (solverId != other.solverId)
				return false;
			return true;
		}

	}

	/**
	 * Sets whether abstractions on method returns shall be connected to the
	 * respective call abstractions to shortcut paths.
	 * 
	 * @param mode The strategy to use for shortening predecessor paths
	 */
	public void setPredecessorShorteningMode(PredecessorShorteningMode mode) {
		// this.shorteningMode = mode;
	}

	/**
	 * Sets the maximum number of abstractions that shall be recorded per join
	 * point. In other words, enabling this option disables the recording of
	 * neighbors beyond the given count.
	 * 
	 * @param maxJoinPointAbstractions The maximum number of abstractions per join
	 *                                 point, or -1 to record an arbitrary number of
	 *                                 join point abstractions
	 */
	public void setMaxJoinPointAbstractions(int maxJoinPointAbstractions) {
		this.maxJoinPointAbstractions = maxJoinPointAbstractions;
	}

	/**
	 * Sets the memory manager that shall be used to manage the abstractions
	 * 
	 * @param memoryManager The memory manager that shall be used to manage the
	 *                      abstractions
	 */
	public void setMemoryManager(IMemoryManager<D, N> memoryManager) {
		this.memoryManager = memoryManager;
	}

	/**
	 * Gets the memory manager used by this solver to reduce memory consumption
	 * 
	 * @return The memory manager registered with this solver
	 */
	public IMemoryManager<D, N> getMemoryManager() {
		return this.memoryManager;
	}

	@Override
	public void forceTerminate(ISolverTerminationReason reason) {
		this.killFlag = reason;
		this.executor.interrupt();
		this.executor.shutdown();
	}

	@Override
	public boolean isTerminated() {
		return killFlag != null || this.executor.isFinished();
	}

	@Override
	public boolean isKilled() {
		return killFlag != null;
	}

	@Override
	public void reset() {
		this.killFlag = null;
	}

	@Override
	public void addStatusListener(IMemoryBoundedSolverStatusNotification listener) {
		this.notificationListeners.add(listener);
	}

	@Override
	public ISolverTerminationReason getTerminationReason() {
		return killFlag;
	}

	public void setMaxCalleesPerCallSite(int maxCalleesPerCallSite) {
		this.maxCalleesPerCallSite = maxCalleesPerCallSite;
	}

	public void setMaxAbstractionPathLength(int maxAbstractionPathLength) {
		this.maxAbstractionPathLength = maxAbstractionPathLength;
	}

}
