package soot.jimple.infoflow.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class ValueOnPath {

	public static class Parameter {

		private int paramIdx;
		private boolean regex, casesensitive;
		private String value;
		private Pattern matcher;

		public Parameter(int paramIndex, boolean regex, boolean casesensitive) {
			this.paramIdx = paramIndex;
			this.regex = regex;
			this.casesensitive = casesensitive;
		}

		public int getParameterIndex() {
			return paramIdx;
		}

		public boolean isRegex() {
			return regex;
		}

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

	}

	private String invocation;
	private Set<Parameter> parameters;

	public ValueOnPath(String inv) {
		this.invocation = inv;
	}

	public String getInvocation() {
		return invocation;
	}

	public void add(Parameter parameter) {
		if (parameters == null)
			parameters = new HashSet<>();
		parameters.add(parameter);
	}

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

}
