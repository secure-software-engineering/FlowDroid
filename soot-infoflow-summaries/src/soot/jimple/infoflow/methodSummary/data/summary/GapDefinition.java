package soot.jimple.infoflow.methodSummary.data.summary;


/**
 * Definition of a gap in a method summary. A gap occurs if a data flow reaches
 * a callback method which is not known to the library generation code, but must
 * be analyzed when the summary is later used.
 * 
 * @author Steven Arzt
 *
 */
public class GapDefinition {
	
	private final int id;
	private String signature;
	
	/**
	 * Creates a new instance of the {@link GapDefinition} class
	 * @param id The unique ID of this gap definition
	 * @param signature The signature of the callee
	 */
	public GapDefinition(int id, String signature) {
		this.id = id;
		this.signature = signature;
	}
	
	/**
	 * Creates a new instance of the {@link GapDefinition} class
	 * @param id The unique ID of this gap definition
	 */
	public GapDefinition(int id) {
		this.id = id;
	}
	
	/**
	 * Gets the unique ID of this gap definition
	 * @return The unique ID of this gap definition
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Gets the signature of the callee of this gap
	 * @return The signature of the callee of this gap
	 */
	public String getSignature() {
		return this.signature;
	}
	
	/**
	 * Sets the signature of the callee of this gap
	 * @param signature The signature of the callee of this gap
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}
	
	@Override
	public GapDefinition clone() {
		return new GapDefinition(id, signature);
	}
	
	/**
	 * Generates a clone of this gap with a new ID
	 * @param newID The new gap ID
	 * @return The new gap with the changed ID
	 */
	public GapDefinition renumber(int newID) {
		return new GapDefinition(newID, signature);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result
				+ ((signature == null) ? 0 : signature.hashCode());
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
		GapDefinition other = (GapDefinition) obj;
		if (id != other.id)
			return false;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Gap " + id + " in " + signature;
	}
	
}
