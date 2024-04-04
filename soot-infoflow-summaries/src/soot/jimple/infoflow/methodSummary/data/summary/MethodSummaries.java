package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import heros.solver.Pair;
import soot.Scene;
import soot.SootMethod;
import soot.util.ConcurrentHashMultiMap;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Data class encapsulating a set of method summaries
 * 
 * @author Steven Arzt
 */
public class MethodSummaries implements Iterable<MethodFlow> {

	public static final MethodSummaries EMPTY_SUMMARIES = new ImmutableMethodSummaries();

	private volatile MultiMap<String, MethodFlow> flows;
	private volatile MultiMap<String, MethodClear> clears;
	private volatile Map<Integer, GapDefinition> gaps;
	private volatile Set<String> excludedMethods;

	public MethodSummaries() {
		this(new ConcurrentHashMultiMap<String, MethodFlow>());
	}

	MethodSummaries(Set<MethodFlow> flows) {
		this(flowSetToFlowMap(flows), new ConcurrentHashMap<Integer, GapDefinition>());
	}

	MethodSummaries(MultiMap<String, MethodFlow> flows) {
		this(flows, null);
	}

	MethodSummaries(MultiMap<String, MethodFlow> flows, Map<Integer, GapDefinition> gaps) {
		this(flows, null, gaps);
	}

	MethodSummaries(MultiMap<String, MethodFlow> flows, MultiMap<String, MethodClear> clears,
			Map<Integer, GapDefinition> gaps) {
		this.flows = flows;
		this.clears = clears;
		this.gaps = gaps;
	}

	/**
	 * Converts a flat set of method flows into a map from method signature to set
	 * of flows inside the respective method
	 * 
	 * @param flows The flat set of method flows
	 * @return The signature-to-flow set map
	 */
	private static MultiMap<String, MethodFlow> flowSetToFlowMap(Set<MethodFlow> flows) {
		MultiMap<String, MethodFlow> flowSet = new HashMultiMap<>();
		if (flows != null && !flows.isEmpty()) {
			for (MethodFlow flow : flows)
				flowSet.put(flow.methodSig(), flow);
		}
		return flowSet;
	}

	/**
	 * Merges the given flows into the this method summary object
	 * 
	 * @param newFlows The new flows to be merged
	 */
	public void mergeFlows(Collection<MethodFlow> newFlows) {
		if (newFlows != null && !newFlows.isEmpty()) {
			ensureFlows();
			for (MethodFlow flow : newFlows)
				flows.put(flow.methodSig(), flow);
		}
	}

	/**
	 * Merges the given clears (kill flows) into the this method summary object
	 * 
	 * @param newFlows The new clears (kill flows) to be merged
	 */
	public void mergeClears(Collection<MethodClear> newClears) {
		if (newClears != null && !newClears.isEmpty()) {
			ensureClears();
			for (MethodClear clear : newClears)
				clears.put(clear.methodSig(), clear);
		}
	}

	/**
	 * Merges the given flows into the this method summary object
	 * 
	 * @param newSummaries The new summaries to be merged
	 */
	public void mergeSummaries(Collection<MethodSummaries> newSummaries) {
		if (newSummaries != null && !newSummaries.isEmpty()) {
			for (MethodSummaries summaries : newSummaries) {
				merge(summaries);
			}
		}
	}

	/**
	 * Merges the given flows into the this method summary object
	 * 
	 * @param newFlows The new flows to be merged
	 */
	public void merge(MultiMap<String, MethodFlow> newFlows) {
		if (newFlows != null && !newFlows.isEmpty())
			flows.putAll(newFlows);
	}

	/**
	 * Merges the given flows into the this method summary object
	 * 
	 * @param newFlows The new flows to be merged
	 * @return True if new data was added to this summary data object during the
	 *         merge, false otherwise
	 */
	public boolean merge(MethodSummaries newFlows) {
		if (newFlows == null || newFlows.isEmpty())
			return false;

		// If some of the gaps have the same IDs as the old ones, we need to
		// renumber the new ones or we'll overwrite data.
		Map<Integer, GapDefinition> renumberedGaps = null;
		if (newFlows.gaps != null) {
			renumberedGaps = new HashMap<>();
			int lastFreeGapId = 0;
			for (Integer newGapId : newFlows.gaps.keySet()) {
				GapDefinition newGap = newFlows.gaps.get(newGapId);

				// We might already have a gap with this id
				GapDefinition oldGap = gaps == null ? null : gaps.get(newGap.getID());
				if (oldGap == null)
					continue;

				// Same gap, same id
				if (oldGap == newGap)
					continue;

				// Find a new, free id
				while (gaps.containsKey(lastFreeGapId))
					lastFreeGapId++;
				GapDefinition renumberedGap = newGap.renumber(lastFreeGapId);
				renumberedGaps.put(newGapId, renumberedGap);

				// That id is used up as well now
				lastFreeGapId++;
			}
		}

		boolean newData = false;

		// Merge the flows. Keep in mind to exchange the gaps where necessary
		if (newFlows.flows != null && !newFlows.flows.isEmpty()) {
			for (String key : newFlows.flows.keySet()) {
				for (MethodFlow flow : newFlows.flows.get(key)) {
					MethodFlow replacedFlow = flow.replaceGaps(renumberedGaps);
					ensureFlows();
					if (flows.put(key, replacedFlow))
						newData = true;
				}
			}
		}

		// Merge the clears. Keep in mind to exchange the gaps where necessary
		if (newFlows.clears != null && !newFlows.clears.isEmpty()) {
			for (String key : newFlows.clears.keySet()) {
				for (MethodClear clear : newFlows.clears.get(key)) {
					MethodClear replacedFlow = clear.replaceGaps(renumberedGaps);
					ensureClears();
					if (clears.put(key, replacedFlow))
						newData = true;
				}
			}
		}

		// Copy over the gaps and replace those that occur in the replacement
		// map
		if (newFlows.gaps != null) {
			for (Integer newGapId : newFlows.gaps.keySet()) {
				GapDefinition replacedGap = renumberedGaps.get(newGapId);
				if (replacedGap == null)
					replacedGap = newFlows.gaps.get(newGapId);
				ensureGaps();
				gaps.put(replacedGap.getID(), replacedGap);
				newData = true;
			}
		}

		return newData;
	}

	/**
	 * Gets all flows for the method with the given signature
	 * 
	 * @param methodSig The signature of the method for which to retrieve the data
	 *                  flows
	 * @return The set of data flows for the method with the given signature
	 */
	public Set<MethodFlow> getFlowsForMethod(String methodSig) {
		return flows == null ? null : flows.get(methodSig);
	}

	/**
	 * Returns a filter this object that contains only flows for the given method
	 * signature
	 * 
	 * @param signature The method for which to filter the flows
	 * @return An object containing only flows for the given method
	 */
	public MethodSummaries filterForMethod(String signature) {
		MethodSummaries summaries = null;

		// Get the flows
		if (flows != null && !flows.isEmpty()) {
			Set<MethodFlow> sigFlows = flows.get(signature);
			if (sigFlows != null && !sigFlows.isEmpty()) {
				if (summaries == null)
					summaries = new MethodSummaries();
				summaries.mergeFlows(sigFlows);
			}
		}

		// Get the clears
		if (clears != null && !clears.isEmpty()) {
			Set<MethodClear> sigClears = clears.get(signature);
			if (sigClears != null && !sigClears.isEmpty()) {
				if (summaries == null)
					summaries = new MethodSummaries();
				summaries.mergeClears(sigClears);
			}
		}

		return summaries;
	}

	/**
	 * Adds a new flow for a method to this summary object
	 * 
	 * @param flow The flow to add
	 */
	public boolean addFlow(MethodFlow flow) {
		ensureFlows();
		return flows.put(flow.methodSig, flow);
	}

	/**
	 * Adds a new kill flow / taint clearing to this summary object
	 * 
	 * @param clear The taint clearing to add
	 */
	public boolean addClear(MethodClear clear) {
		ensureClears();
		return clears.put(clear.methodSig, clear);
	}

	/**
	 * Gets the gaps in this method summary. Gap definitions are mappings between
	 * unique IDs and definition objects
	 * 
	 * @return The gap mapping for this method summary
	 */
	public Map<Integer, GapDefinition> getGaps() {
		return this.gaps;
	}

	/**
	 * Gets the gap definition with the given id. If no such gap definition exists,
	 * null is returned
	 * 
	 * @param id The id for which to retrieve the gap definition
	 * @return The gap with the given id if it exists, otherwise null
	 */
	public GapDefinition getGap(int id) {
		return this.gaps == null ? null : this.gaps.get(id);
	}

	/**
	 * Gets all gaps defined in this method summary
	 * 
	 * @return All gaps defined in this method summary
	 */
	public Collection<GapDefinition> getAllGaps() {
		return this.gaps == null ? null : this.gaps.values();
	}

	/**
	 * Gets all flows registered in this method summary as a mapping from method
	 * signature to flow set
	 * 
	 * @return The individual flows in this method summary
	 */
	public MultiMap<String, MethodFlow> getFlows() {
		return this.flows;
	}

	/**
	 * Gets all clears (kill taints) registered in this method summary as a mapping
	 * from method signature to flow set
	 * 
	 * @return The individual clears (kill taints)in this method summary
	 */
	public MultiMap<String, MethodClear> getClears() {
		return this.clears;
	}

	/**
	 * Gets a set containing all flows in this summary object regardless of the
	 * method they are in
	 * 
	 * @return A flat set of all flows contained in this summary object
	 */
	public Set<MethodFlow> getAllFlows() {
		return this.flows == null ? null : this.flows.values();
	}

	/**
	 * Gets a set containing all clears (kill taints) in this summary object
	 * regardless of the method they are in
	 * 
	 * @return A flat set of all clears (kill taints) contained in this summary
	 *         object
	 */
	public Set<MethodClear> getAllClears() {
		return this.clears == null ? null : this.clears.values();
	}

	@Override
	public Iterator<MethodFlow> iterator() {
		return new Iterator<MethodFlow>() {

			private Pair<String, MethodFlow> curPair = null;
			private Iterator<Pair<String, MethodFlow>> flowIt = flows.iterator();

			@Override
			public boolean hasNext() {
				return flowIt.hasNext();
			}

			@Override
			public MethodFlow next() {
				curPair = flowIt.next();
				return curPair.getO2();
			}

			@Override
			public void remove() {
				flows.remove(curPair.getO1(), curPair.getO2());
			}

		};
	}

	/**
	 * Retrieves the gap definition with the given ID if it exists, otherwise
	 * creates a new gap definition with this ID
	 * 
	 * @param gapID     The unique ID of the gap
	 * @param signature The signature of the callee
	 * @return The gap definition with the given ID
	 */
	public GapDefinition getOrCreateGap(int gapID, String signature) {
		ensureGaps();
		GapDefinition gd = this.gaps.get(gapID);
		if (gd == null) {
			gd = new GapDefinition(gapID, signature);
			this.gaps.put(gapID, gd);
		}

		// If the existing gap did not have a method signature so far, we
		// silently add it to make the definition complete
		if (gd.getSignature() == null || gd.getSignature().isEmpty())
			gd.setSignature(signature);
		else if (!gd.getSignature().equals(signature))
			throw new RuntimeException("Gap signature mismatch detected");

		return gd;
	}

	/**
	 * Creates a temporary, underspecified gap with the given ID. This method is
	 * intended for incrementally loading elements from XML.
	 * 
	 * @param gapID The unique ID of the gap
	 * @return The gap definition with the given ID
	 */
	public GapDefinition createTemporaryGap(int gapID) {
		if (this.gaps != null && this.gaps.containsKey(gapID))
			throw new RuntimeException("A gap with the ID " + gapID + " already exists");

		ensureGaps();
		GapDefinition gd = new GapDefinition(gapID);
		this.gaps.put(gapID, gd);
		return gd;
	}

	/**
	 * Removes the given gap definition from this method summary object
	 * 
	 * @param gap The gap definition to remove
	 * @return True if the gap was contained in this method summary object before,
	 *         otherwise false
	 */
	public boolean removeGap(GapDefinition gap) {
		if (this.gaps == null || this.gaps.isEmpty())
			return false;
		for (Entry<Integer, GapDefinition> entry : this.gaps.entrySet())
			if (entry.getValue() == gap) {
				boolean ok = this.gaps.remove(entry.getKey()) == gap;
				return ok;
			}
		return false;
	}

	/**
	 * Clears all flows from this method summary
	 */
	public void clear() {
		if (this.flows != null)
			this.flows.clear();
		if (this.clears != null)
			this.clears.clear();
		if (this.gaps != null)
			this.gaps.clear();
	}

	/**
	 * Gets the total number of flows in this summary object
	 * 
	 * @return The total number of flows in this summary object
	 */
	public int getFlowCount() {
		return this.flows == null || this.flows.isEmpty() ? 0 : this.flows.values().size();
	}

	/**
	 * Validates this method summary object
	 */
	public void validate() {
		validateGaps();
		validateFlows();
	}

	/**
	 * Checks whether the gaps in this method summary are valid
	 */
	private void validateGaps() {
		if (this.gaps == null || this.gaps.isEmpty())
			return;

		// For method that has a flow into a gap, we must also have one flow to
		// the base object of that gap
		for (String methodName : getFlows().keySet()) {
			Set<GapDefinition> gapsWithFlows = new HashSet<GapDefinition>();
			Set<GapDefinition> gapsWithBases = new HashSet<GapDefinition>();

			for (MethodFlow flow : getFlows().get(methodName))
				if (!flow.isCustom()) {
					// For the source, record all flows to gaps and all flows to
					// bases
					if (flow.source().getGap() != null) {
						if (flow.source().getType() == SourceSinkType.GapBaseObject)
							gapsWithBases.add(flow.source().getGap());
						else
							gapsWithFlows.add(flow.source().getGap());
					}

					// For the sink, record all flows to gaps and all flows to
					// bases
					if (flow.sink().getGap() != null) {
						if (flow.sink().getType() == SourceSinkType.GapBaseObject)
							gapsWithBases.add(flow.sink().getGap());
						else
							gapsWithFlows.add(flow.sink().getGap());
					}
				}

			// Check whether we have some flow for which we don't have a base
			for (GapDefinition gd : gapsWithFlows) {
				// We don't need a base for a static method
				SootMethod sm = Scene.v().grabMethod(gd.getSignature());
				if (sm != null && sm.isStatic())
					continue;

				if (!gapsWithBases.contains(gd))
					throw new RuntimeException("Flow to/from a gap without a base detected " + " for method "
							+ methodName + ". Gap target is " + gd.getSignature());
			}
		}

		// No gap without a method signature may exist
		for (GapDefinition gap : this.getAllGaps())
			if (gap.getSignature() == null || gap.getSignature().isEmpty())
				throw new RuntimeException("Gap without signature detected");

		// No two gaps may have the same id
		for (Integer gapId : gaps.keySet()) {
			GapDefinition gd1 = gaps.get(gapId);
			for (GapDefinition gd2 : gaps.values())
				if (gd1 != gd2 && gd1.getID() == gd2.getID())
					throw new RuntimeException("Duplicate gap id");
		}
	}

	/**
	 * Validates all flows inside this method summary object
	 */
	private void validateFlows() {
		if (this.flows == null || this.flows.isEmpty())
			return;

		for (String methodName : getFlows().keySet())
			for (MethodFlow flow : getFlows().get(methodName)) {
				flow.validate();
			}
	}

	/**
	 * Gets all flows into the given gap
	 * 
	 * @param gd The gap for which to get the incoming flows
	 * @return The set of flows into the given gap
	 */
	public Set<MethodFlow> getInFlowsForGap(GapDefinition gd) {
		Set<MethodFlow> res = new HashSet<>();
		for (String methodName : getFlows().keySet())
			for (MethodFlow flow : getFlows().get(methodName)) {
				if (flow.sink().getGap() == gd)
					res.add(flow);
			}
		return res;
	}

	/**
	 * Gets all flows out of the given gap
	 * 
	 * @param gd The gap for which to get the outgoing flows
	 * @return The set of flows out of the given gap
	 */
	public Set<MethodFlow> getOutFlowsForGap(GapDefinition gd) {
		Set<MethodFlow> res = new HashSet<>();
		for (String methodName : getFlows().keySet())
			for (MethodFlow flow : getFlows().get(methodName)) {
				if (flow.source().getGap() == gd)
					res.add(flow);
				else if (flow.isAlias()) {
					MethodFlow reverseFlow = flow.reverse();
					if (reverseFlow.source().getGap() == gd)
						res.add(reverseFlow);
				}
			}
		return res;
	}

	/**
	 * Removes the given flow summary from this set
	 * 
	 * @param toRemove The flow summary to remove
	 */
	public void remove(MethodFlow toRemove) {
		Set<MethodFlow> flowsForMethod = flows.get(toRemove.methodSig());
		if (flowsForMethod != null) {
			flowsForMethod.remove(toRemove);
			if (flowsForMethod.isEmpty())
				flows.remove(toRemove.methodSig());
		}
	}

	/**
	 * Removes all of the given flows from this flow set
	 * 
	 * @param toRemove The collection of flows to remove
	 */
	public void removeAll(Collection<MethodFlow> toRemove) {
		for (Iterator<MethodFlow> flowIt = this.iterator(); flowIt.hasNext();) {
			MethodFlow flow = flowIt.next();
			if (toRemove.contains(flow))
				flowIt.remove();
		}
	}

	/**
	 * Gets whether this method summary object is empty, i.e., does not contain any
	 * flows
	 * 
	 * @return True if this method summary object is empty, otherwise false
	 */
	public boolean isEmpty() {
		return (this.flows == null || this.flows.isEmpty()) && (this.clears == null || this.clears.isEmpty());
	}

	/**
	 * Ensures that the gap collection exists
	 */
	private void ensureGaps() {
		if (this.gaps == null) {
			synchronized (this) {
				if (this.gaps == null)
					this.gaps = new ConcurrentHashMap<>();

			}
		}
	}

	/**
	 * Ensures that the flow collection exists
	 */
	private void ensureFlows() {
		if (flows == null) {
			synchronized (this) {
				if (flows == null)
					flows = new ConcurrentHashMultiMap<>();
			}
		}
	}

	/**
	 * Ensures that the clear collection exists
	 */
	private void ensureClears() {
		if (clears == null) {
			synchronized (this) {
				if (clears == null)
					clears = new ConcurrentHashMultiMap<>();
			}
		}
	}

	/**
	 * Gets whether there are any flows inside this summary object
	 * 
	 * @return True if there are any flows inside this summary object, otherwise
	 *         false
	 */
	public boolean hasFlows() {
		return this.flows != null && !this.flows.isEmpty();
	}

	/**
	 * Gets whether there are any gaps inside this summary object
	 * 
	 * @return True if there are any gaps inside this summary object, otherwise
	 *         false
	 */
	public boolean hasGaps() {
		return this.gaps != null && !this.gaps.isEmpty();
	}

	/**
	 * Gets whether there are any clears (kill taints) inside this summary object
	 * 
	 * @return True if there are any clears (kill taints) inside this summary
	 *         object, otherwise false
	 */
	public boolean hasClears() {
		return this.clears != null && !this.clears.isEmpty();
	}

	/**
	 * Reverses all data flows in this data object
	 * 
	 * @return A new data object with all flows reversed
	 */
	public MethodSummaries reverse() {
		MultiMap<String, MethodFlow> reversedFlows = new HashMultiMap<>(flows.size());
		for (String className : flows.keySet()) {
			for (MethodFlow flow : flows.get(className))
				reversedFlows.put(className, flow.reverse());
		}
		return new MethodSummaries(reversedFlows, clears, gaps);
	}

	/**
	 * Adds a method to exclude in the taint wrapper
	 * 
	 * @param methodSignature The subsignature of the method to ignore
	 */
	public void addExcludedMethod(String methodSignature) {
		if (excludedMethods == null)
			excludedMethods = new HashSet<>();
		excludedMethods.add(methodSignature);
	}

	/**
	 * Gets whether the method with the given subsignature has been excluded from
	 * the data flow analysis
	 * 
	 * @param subsignature The subsignature
	 * @return True if the method with the given subsignature has been excluded,
	 *         false otherwise
	 */
	public boolean isExcluded(String subsignature) {
		return excludedMethods != null && excludedMethods.contains(subsignature);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clears == null) ? 0 : clears.hashCode());
		result = prime * result + ((excludedMethods == null) ? 0 : excludedMethods.hashCode());
		result = prime * result + ((flows == null) ? 0 : flows.hashCode());
		result = prime * result + ((gaps == null) ? 0 : gaps.hashCode());
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
		MethodSummaries other = (MethodSummaries) obj;
		if (clears == null) {
			if (other.clears != null)
				return false;
		} else if (!clears.equals(other.clears))
			return false;
		if (excludedMethods == null) {
			if (other.excludedMethods != null)
				return false;
		} else if (!excludedMethods.equals(other.excludedMethods))
			return false;
		if (flows == null) {
			if (other.flows != null)
				return false;
		} else if (!flows.equals(other.flows))
			return false;
		if (gaps == null) {
			if (other.gaps != null)
				return false;
		} else if (!gaps.equals(other.gaps))
			return false;
		return true;
	}

}
