package soot.jimple.infoflow.results;

/**
 * Class that records performance data on a FlowDroid run
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowPerformanceData {

	private int callgraphConstructionSeconds = -1;
	private int taintPropagationSeconds = -1;
	private int pathReconstructionSeconds = -1;
	private int totalRuntimeSeconds = -1;
	private int maxMemoryConsumption = -1;

	private int sourceCount = -1;
	private int sinkCount = -1;

	public InfoflowPerformanceData() {
		//
	}

	/**
	 * Gets the number of seconds that the data flow analysis spent on callgraph
	 * constructions
	 * 
	 * @return The number of seconds that the data flow analysis spent on callgraph
	 *         constructions, or -1 if no such data has been recorded
	 */
	public int getCallgraphConstructionSeconds() {
		return callgraphConstructionSeconds;
	}

	/**
	 * Sets the number of seconds that the data flow analysis spent on callgraph
	 * constructions
	 * 
	 * @param callgraphSeconds The number of seconds that the data flow analysis
	 *                         spent on callgraph constructions, or -1 if no such
	 *                         data has been recorded
	 */
	public void setCallgraphConstructionSeconds(int callgraphSeconds) {
		this.callgraphConstructionSeconds = callgraphSeconds;
	}

	/**
	 * Gets the number of seconds spent on the taint propagation
	 * 
	 * @return The number of seconds spent on the taint propagation
	 */
	public int getTaintPropagationSeconds() {
		return taintPropagationSeconds;
	}

	/**
	 * Sets the number of seconds spent on the taint propagation
	 * 
	 * @param taintPropagationSeconds The number of seconds spent on the taint
	 *                                propagation
	 */
	public void setTaintPropagationSeconds(int taintPropagationSeconds) {
		this.taintPropagationSeconds = taintPropagationSeconds;
	}

	/**
	 * Gets the number of seconds spent on path reconstruction, i.e., finding the
	 * sources to which the sinks are connected in the taint graph
	 * 
	 * @return The number of seconds spent on path reconstruction.
	 */
	public int getPathReconstructionSeconds() {
		return pathReconstructionSeconds;
	}

	/**
	 * Sets the number of seconds spent on path reconstruction, i.e., finding the
	 * sources to which the sinks are connected in the taint graph
	 * 
	 * @param pathReconstructionSeconds The number of seconds spent on path
	 *                                  reconstruction.
	 */
	public void setPathReconstructionSeconds(int pathReconstructionSeconds) {
		this.pathReconstructionSeconds = pathReconstructionSeconds;
	}

	/**
	 * Gets the total number of seconds that the data flow analysis has spent to
	 * create the flow results
	 * 
	 * @return The total number of seconds that the data flow analysis has spent to
	 *         create the flow results, or -1 if no such data has been recorded
	 */
	public int getTotalRuntimeSeconds() {
		return totalRuntimeSeconds;
	}

	/**
	 * Sets the total number of seconds that the data flow analysis has spent to
	 * create the flow results
	 * 
	 * @param totalRuntimeSeconds The total number of seconds that the data flow
	 *                            analysis has spent to create the flow results, or
	 *                            -1 if no such data has been recorded
	 */
	public void setTotalRuntimeSeconds(int totalRuntimeSeconds) {
		this.totalRuntimeSeconds = totalRuntimeSeconds;
	}

	/**
	 * Gets the peak amount of memory that the data flow analysis has been using
	 * during its analysis
	 * 
	 * @return The peak amount of memory that the data flow analysis has been using
	 *         during its analysis, or -1 if no such data has been recorded
	 */
	public int getMaxMemoryConsumption() {
		return maxMemoryConsumption;
	}

	/**
	 * Sets the peak amount of memory that the data flow analysis has been using
	 * during its analysis
	 * 
	 * @param maxMemoryConsumption The peak amount of memory that the data flow
	 *                             analysis has been using during its analysis, or
	 *                             -1 if no such data has been recorded
	 */
	public void setMaxMemoryConsumption(int maxMemoryConsumption) {
		this.maxMemoryConsumption = maxMemoryConsumption;
	}

	/**
	 * Gets whether this data object is empty, i.e., does not contain any data
	 * 
	 * @return True if this data object is empty, i.e., does not contain any data,
	 *         false otherwise
	 */
	public boolean isEmpty() {
		return callgraphConstructionSeconds <= 0 && taintPropagationSeconds <= 0 && pathReconstructionSeconds <= 0
				&& totalRuntimeSeconds <= 0 && maxMemoryConsumption <= 0;
	}

	/**
	 * Adds the data of the given performance object to this one. The timings are
	 * simply added. For memory consumption, the maximum value is taken.
	 * 
	 * @param performanceData The performance data to add to this object
	 */
	public void add(InfoflowPerformanceData performanceData) {
		if (performanceData.callgraphConstructionSeconds > 0) {
			if (this.callgraphConstructionSeconds < 0)
				this.callgraphConstructionSeconds = performanceData.callgraphConstructionSeconds;
			else
				this.callgraphConstructionSeconds += performanceData.callgraphConstructionSeconds;
		}
		if (performanceData.taintPropagationSeconds > 0) {
			if (this.taintPropagationSeconds < 0)
				this.taintPropagationSeconds = performanceData.taintPropagationSeconds;
			else
				this.taintPropagationSeconds += performanceData.taintPropagationSeconds;
		}
		if (performanceData.totalRuntimeSeconds > 0) {
			if (this.totalRuntimeSeconds < 0)
				this.totalRuntimeSeconds = performanceData.totalRuntimeSeconds;
			else
				this.totalRuntimeSeconds += performanceData.totalRuntimeSeconds;
		}
		if (performanceData.maxMemoryConsumption > 0) {
			if (this.maxMemoryConsumption < 0)
				this.maxMemoryConsumption = performanceData.maxMemoryConsumption;
			else
				this.maxMemoryConsumption = Math.max(this.maxMemoryConsumption, performanceData.maxMemoryConsumption);
		}
		if (performanceData.sourceCount > 0) {
			if (this.sourceCount < 0)
				this.sourceCount = performanceData.sourceCount;
			else
				this.sourceCount += performanceData.sourceCount;
		}
		if (performanceData.sinkCount > 0) {
			if (this.sinkCount < 0)
				this.sinkCount = performanceData.sinkCount;
			else
				this.sinkCount += performanceData.sinkCount;
		}
	}

	/**
	 * Updates the maximum memory consumption with the given consumption value. The
	 * new value is the maximum between of old value and the new one.
	 * 
	 * @param usedMemory The maximum memory consumption of a computation step in
	 *                   megabytes
	 */
	public void updateMaxMemoryConsumption(int usedMemory) {
		int mem = this.maxMemoryConsumption;
		if (mem < 0)
			this.maxMemoryConsumption = usedMemory;
		else
			this.maxMemoryConsumption = Math.min(mem, usedMemory);
	}

	/**
	 * Adds the given number of seconds to the time spent on taint propagation
	 * 
	 * @param toaAdd The time to add in seconds
	 */
	public void addTaintPropagationSeconds(int toAdd) {
		int time = this.taintPropagationSeconds;
		if (time < 0)
			this.taintPropagationSeconds = toAdd;
		else
			this.taintPropagationSeconds = time + toAdd;
	}

	/**
	 * Sets the number of sources that were identified in the given input program
	 * 
	 * @param sourceCount The number of sources that were identified in the given
	 *                    input program
	 */
	public void setSourceCount(int sourceCount) {
		this.sourceCount = sourceCount;
	}

	/**
	 * Gets the number of sources that were identified in the given input program
	 * 
	 * @return The number of sources that were identified in the given input program
	 */
	public int getSourceCount() {
		return sourceCount;
	}

	/**
	 * Sets the number of sinks that were identified in the given input program
	 * 
	 * @param sourceCount The number of sinks that were identified in the given
	 *                    input program
	 */
	public void setSinkCount(int sinkCount) {
		this.sinkCount = sinkCount;
	}

	/**
	 * Gets the number of sinks that were identified in the given input program
	 * 
	 * @return The number of sinks that were identified in the given input program
	 */
	public int getSinkCount() {
		return sinkCount;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (callgraphConstructionSeconds > 0)
			sb.append(String.format("Callgraph Construction: %d seconds\n", callgraphConstructionSeconds));
		if (taintPropagationSeconds > 0)
			sb.append(String.format("Taint Propagation: %d seconds\n", taintPropagationSeconds));
		if (pathReconstructionSeconds > 0)
			sb.append(String.format("Path Reconstruction: %d seconds\n", pathReconstructionSeconds));
		if (totalRuntimeSeconds > 0)
			sb.append(String.format("Total Runtime: %d seconds\n", totalRuntimeSeconds));
		if (maxMemoryConsumption > 0)
			sb.append(String.format("Max Memory Consumption: %d MB\n", maxMemoryConsumption));

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + callgraphConstructionSeconds;
		result = prime * result + maxMemoryConsumption;
		result = prime * result + pathReconstructionSeconds;
		result = prime * result + sinkCount;
		result = prime * result + sourceCount;
		result = prime * result + taintPropagationSeconds;
		result = prime * result + totalRuntimeSeconds;
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
		InfoflowPerformanceData other = (InfoflowPerformanceData) obj;
		if (callgraphConstructionSeconds != other.callgraphConstructionSeconds)
			return false;
		if (maxMemoryConsumption != other.maxMemoryConsumption)
			return false;
		if (pathReconstructionSeconds != other.pathReconstructionSeconds)
			return false;
		if (sinkCount != other.sinkCount)
			return false;
		if (sourceCount != other.sourceCount)
			return false;
		if (taintPropagationSeconds != other.taintPropagationSeconds)
			return false;
		if (totalRuntimeSeconds != other.totalRuntimeSeconds)
			return false;
		return true;
	}

}
