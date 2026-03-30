package soot.jimple.infoflow.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Can be used to express conditions on values that are used in statements on
 * the path. This class can be used to enforce that there is a specific method
 * invocation call where a specific constant value is used as a parameter.
 */
public class ValueOnPath {

	/**
	 * A value condition on a specific parameter
	 */
	public static class Parameter {

		private int paramIdx;
		private boolean regex, casesensitive;
		private String value;
		private Pattern matcher;

		/**
		 * Creates a new parameter
		 * 
		 * @param paramIndex    the 0-based parameter index
		 * @param regex         whether the parameter value is used as a regular
		 *                      expression
		 * @param casesensitive whether the check is case sensitive
		 */
		public Parameter(int paramIndex, boolean regex, boolean casesensitive) {
			this.paramIdx = paramIndex;
			this.regex = regex;
			this.casesensitive = casesensitive;
		}

		/**
		 * Returns the parameter index where the parameter condition applies
		 * 
		 * @return parameter index
		 */
		public int getParameterIndex() {
			return paramIdx;
		}

		/**
		 * Returns true if the parameter value is treated as a regular expression. If
		 * false, it is a regular string equals check
		 * 
		 * @return whether the value is treated as a regular expression
		 */
		public boolean isRegex() {
			return regex;
		}

		/**
		 * Returns true if the parameter value is checked case sensitive
		 * 
		 * @return whether the value is checked case sensitive
		 */
		public boolean isCaseSensitive() {
			return casesensitive;
		}

		@Override
		public int hashCode() {
			return Objects.hash(casesensitive, paramIdx, regex, value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parameter other = (Parameter) obj;
			return casesensitive == other.casesensitive && paramIdx == other.paramIdx && regex == other.regex
					&& Objects.equals(value, other.value);
		}

		public void setContentToMatch(String str) {
			this.value = str;
		}

		public String getContentToMatch() {
			return value;
		}

		public Pattern getRegexMatcher() {
			if (matcher == null)
				matcher = Pattern.compile(value, isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
			return matcher;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Value '" + value + "' on parameter " + paramIdx);
			if (regex)
				sb.append(", regex");
			if (casesensitive)
				sb.append(", case sensitive");
			return sb.toString();
		}

	}

	private String invocation;
	private Set<Parameter> parameters;

	/**
	 * Creates a new value on path condition
	 * 
	 * @param inv the invocation site where this condition applies
	 */
	public ValueOnPath(String inv) {
		this.invocation = inv;
	}

	/**
	 * Returns the soot method signature of the invocation site where this condition
	 * applies
	 * 
	 * @return the invocation site
	 */
	public String getInvocation() {
		return invocation;
	}

	/**
	 * Adds a parameter condition. Note that the parameters are combined using
	 * <i>AND</i>, i.e. all of them have to be true in order to fulfill this
	 * condition.
	 * 
	 * @param parameter the new parameters condition
	 * @return true if the parameter condition has been added successfully
	 */
	public boolean add(Parameter parameter) {
		if (parameters == null)
			parameters = new HashSet<>();
		return parameters.add(parameter);
	}

	/**
	 * Returns a set of parameter conditions. Note that the parameters are combined
	 * using <i>AND</i>, i.e. all of them have to be true in order to fulfill this
	 * condition.
	 * 
	 * @return the parameters
	 */
	public Set<Parameter> getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		return Objects.hash(invocation, parameters);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueOnPath other = (ValueOnPath) obj;
		return Objects.equals(invocation, other.invocation) && Objects.equals(parameters, other.parameters);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Value condition on invocation site " + getInvocation() + ": ");
		boolean first = true;
		for (Parameter p : getParameters()) {
			if (first)
				first = false;
			else
				sb.append(", ");
			sb.append(p.toString());
		}
		return super.toString();
	}

}
