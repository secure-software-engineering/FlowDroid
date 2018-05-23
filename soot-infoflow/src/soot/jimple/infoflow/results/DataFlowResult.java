package soot.jimple.infoflow.results;

/**
 * Data class for having the source and sink of a single flow together in one
 * place
 * 
 * @author Steven Arzt
 *
 */
public class DataFlowResult {

	private final ResultSourceInfo source;
	private final ResultSinkInfo sink;

	public DataFlowResult(ResultSourceInfo source, ResultSinkInfo sink) {
		this.source = source;
		this.sink = sink;
	}

	public ResultSourceInfo getSource() {
		return source;
	}

	public ResultSinkInfo getSink() {
		return sink;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(source.toString());
		sb.append(" -> ");
		sb.append(sink.toString());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sink == null) ? 0 : sink.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		DataFlowResult other = (DataFlowResult) obj;
		if (sink == null) {
			if (other.sink != null)
				return false;
		} else if (!sink.equals(other.sink))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

}
