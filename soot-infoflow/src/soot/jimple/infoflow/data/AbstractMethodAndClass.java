package soot.jimple.infoflow.data;

import java.util.List;

/**
 * Abstract class for all MethodAndClass containers.
 * 
 * @author Steven Arzt
 * @author Niklas Vogel
 *
 */
public abstract class AbstractMethodAndClass {
	protected final String methodName;
	protected final String className;
	protected final String returnType;
	protected final List<String> parameters;

	private String subSignature = null;
	private String signature = null;

	public AbstractMethodAndClass(String methodName, String className, String returnType, List<String> parameters) {
		this.methodName = methodName;
		this.className = className;
		this.returnType = returnType;
		this.parameters = parameters;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public String getClassName() {
		return this.className;
	}

	public String getReturnType() {
		return this.returnType;
	}

	public List<String> getParameters() {
		return this.parameters;
	}

	/**
	 * Get subsignature of stored method and class
	 * 
	 * @return Subsignature
	 */
	public String getSubSignature() {
		if (subSignature != null)
			return subSignature;

		StringBuilder sb = new StringBuilder(
				10 + this.returnType.length() + this.methodName.length() + (this.parameters.size() * 30));
		if (!this.returnType.isEmpty()) {
			sb.append(this.returnType);
			sb.append(" ");
		}
		sb.append(this.methodName);
		sb.append("(");

		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(this.parameters.get(i).trim());
		}
		sb.append(")");
		this.subSignature = sb.toString();

		return this.subSignature;
	}

	/**
	 * Get the signature of the stored method and class
	 * 
	 * @return Signature
	 */
	public String getSignature() {
		if (signature != null)
			return signature;

		StringBuilder sb = new StringBuilder(10 + this.className.length() + this.returnType.length()
				+ this.methodName.length() + (this.parameters.size() * 30));
		sb.append("<");
		sb.append(this.className);
		sb.append(": ");
		if (!this.returnType.isEmpty()) {
			sb.append(this.returnType);
			sb.append(" ");
		}
		sb.append(this.methodName);
		sb.append("(");

		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(this.parameters.get(i).trim());
		}
		sb.append(")>");
		this.signature = sb.toString();

		return this.signature;
	}
}
