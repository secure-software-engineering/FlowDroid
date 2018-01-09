package soot.jimple.infoflow.results.xml;

import java.util.Set;

import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Data flow results that we loaded from an external storage and thus cannot
 * reference actual Soot objects
 * 
 * @author Steven Arzt
 *
 */
public class SerializedInfoflowResults {
	
	private int fileFormatVersion = -1;
	private final MultiMap<SerializedSinkInfo, SerializedSourceInfo> results = new HashMultiMap<>();
	
	/**
	 * Creates a new instance of the SerializedInfoflowResults class
	 */
	SerializedInfoflowResults() {
		
	}
	
	/**
	 * Sets the format version of the XML file from which this data was read
	 * @param version The format version of the XML file from which this data
	 * was read
	 */
	void setFileFormatVersion(int version) {
		this.fileFormatVersion = version;
	}
	
	/**
	 * Adds a result entry to this data object
	 * @param source The source from which the data flow originated
	 * @param sink The sink at which the data flow arrived
	 */
	void addResult(SerializedSourceInfo source, SerializedSinkInfo sink) {
		this.results.put(sink, source);
	}
	
	/**
	 * Gets the number of data flow results in this object
	 * @return The number of data flow results in this object
	 */
	public int getResultCount() {
		int cnt = 0;
		for (SerializedSinkInfo sink : results.keySet()) {
			Set<SerializedSourceInfo> sources = results.get(sink);
			cnt += sources == null ? 0 : sources.size();
		}
		return cnt;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fileFormatVersion;
		result = prime * result + ((results == null) ? 0 : results.hashCode());
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
		SerializedInfoflowResults other = (SerializedInfoflowResults) obj;
		if (fileFormatVersion != other.fileFormatVersion)
			return false;
		if (results == null) {
			if (other.results != null)
				return false;
		} else if (!results.equals(other.results))
			return false;
		return true;
	}
	
	/**
	 * Gets whether this data object is empty, i.e., does not contain any data
	 * flows
	 * @return True if this data object is empty, otherwise false
	 */
	public boolean isEmpty() {
		return this.results.isEmpty();
	}
	
	/**
	 * Gets the format version of the XML file from which this data was read
	 * @return the format version of the XML file from which this data was read
	 */
	public int getFileFormatVersion() {
		return this.fileFormatVersion;
	}
	
	/**
	 * Gets the results as a map. Every sink is connected to a number of sources.
	 * @return The mapping between sinks and sources that denotes the discovered
	 * data flows
	 */
	public MultiMap<SerializedSinkInfo, SerializedSourceInfo> getResults() {
		return this.results;
	}

}
