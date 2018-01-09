package soot.jimple.infoflow.results.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a flow sink loaded from an external data storage. This
 * object thus cannot reference actual Soot objects
 * 
 * @author Steven Arzt
 *
 */
public class SerializedSourceInfo extends AbstractSerializedSourceSink {
	
	private List<SerializedPathElement> propagationPath = null;
	
	SerializedSourceInfo(SerializedAccessPath accessPath, String statement,
			String method) {
		this(accessPath, statement, method, null);
	}
	
	SerializedSourceInfo(SerializedAccessPath accessPath, String statement,
			String method, List<SerializedPathElement> propagationPath) {
		super(accessPath, statement, method);
		this.propagationPath = propagationPath;
	}
	
	/**
	 * Adds an element to the propagation path
	 * @param pathElement The path element to add
	 */
	void addPathElement(SerializedPathElement pathElement) {
		if (this.propagationPath == null)
			this.propagationPath = new ArrayList<>();
		this.propagationPath.add(pathElement);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((propagationPath == null) ? 0 : propagationPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SerializedSourceInfo other = (SerializedSourceInfo) obj;
		if (propagationPath == null) {
			if (other.propagationPath != null)
				return false;
		} else if (!propagationPath.equals(other.propagationPath))
			return false;
		return true;
	}
	
	/**
	 * Gets the propagation of this data flow
	 * @return The propagation path of this data flow if one has been loaded,
	 * otherwise null
	 */
	public List<SerializedPathElement> getPropagationPath() {
		return this.propagationPath;
	}

}
