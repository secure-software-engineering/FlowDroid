package soot.jimple.infoflow.methodSummary.ai;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import soot.SootMethod;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.AIConfiguration;
import soot.jimple.infoflow.ai.FlowDroidAIBase;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.Taint;

/**
 * Class to query an LLM when there is no Stubdroid summary available
 * 
 * @author Steven Arzt
 */
public class SummaryApplicationAI extends FlowDroidAIBase {

	private final static Logger logger = LoggerFactory.getLogger(SummaryApplicationAI.class);

	private static String SYSTEM_PROMPT = "You are a skilled program analysis researcher working on data flow analysis. "
			+ "You excel at reverse engineering Java programs and Android apps for finding data flows via taint tracking. "
			+ "You will be given a statement and a taint that is valid before that statement. The method will be given in "
			+ "Soot notation. You must list the taints that are valid directly after the given statement. Do not provide "
			+ "any additional information or explanation. You must only return the set of data flows. Do not make assumptions "
			+ "about aliases.";

	protected Map<SootMethod, TaintQueryCacheEntry> aiCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new instance of the {@link SummaryApplicationAI} class
	 * 
	 * @param config The AI integration configuration
	 */
	public SummaryApplicationAI(AIConfiguration config) {
		super(config, SYSTEM_PROMPT);
	}

	/**
	 * A taint notation that does not support fields, i.e., always taints the entire
	 * base object, parameter, or return value
	 * 
	 * @author Steven Arzt
	 */
	protected static class CachedBaseTaint {

		protected final BitSet taintFlags;

		/**
		 * Creates a new empty instance of the {@link CachedBaseTaint} class
		 */
		public CachedBaseTaint(Stmt callSite) {
			InvokeExpr iexpr = callSite.getInvokeExpr();
			this.taintFlags = new BitSet(iexpr.getArgCount() + 2);
		}

		/**
		 * Creates a new instance of the {@link CachedBaseTaint} class
		 * 
		 * @param callSite The call site where the taint is valid
		 * @param t        The taint
		 */
		public CachedBaseTaint(Stmt callSite, Taint t) {
			this(callSite);

			if (t.isField())
				taintFlags.set(0);
			if (t.isReturn())
				taintFlags.set(1);
			if (t.isAnyParameter())
				taintFlags.set(t.getParameterIndex() + 2);
		}

		/**
		 * Checks whether the base object is tainted
		 * 
		 * @return True if the base object is tainted, false otherwise
		 */
		public boolean isBaseObject() {
			return taintFlags.get(0);
		}

		/**
		 * Sets whether the base object shall be tainted
		 * 
		 * @param tainted True if the base object shall be tainted, false otherwise
		 */
		public void setBaseObjectTainted(boolean tainted) {
			taintFlags.set(0, tainted);
		}

		/**
		 * Checks whether the return value is tainted
		 * 
		 * @return True if the return value is tainted, false otherwise
		 */
		public boolean isReturnValue() {
			return taintFlags.get(1);
		}

		/**
		 * Sets whether the return value is tainted
		 * 
		 * @param tainted True if the return value is tainted, false otherwise
		 */
		public void setReturnValueTainted(boolean tainted) {
			taintFlags.set(1, tainted);
		}

		/**
		 * Checks whether any parameter is tainted
		 * 
		 * @return True if at last one parameter is tainted, false otherwise
		 */
		public boolean isAnyParameterTainted() {
			return taintFlags.nextSetBit(2) >= 0;
		}

		/**
		 * Checks whether the parameter with the given index is tainted
		 * 
		 * @param paramIdx The index of the parameter to check
		 * @return True if the parameter at the given index is tainted, false otherwise
		 */
		public boolean isParameterTainted(int paramIdx) {
			return taintFlags.get(paramIdx + 2);
		}

		/**
		 * Sets whether the parameter at the given index is tainted or not
		 * 
		 * @param paramIdx The parameter of which to change the taint state
		 * @param tainted  True if the parameter at the given index is tainted, false
		 *                 otherwise
		 */
		public void setParameterTainted(int paramIdx, boolean tainted) {
			taintFlags.set(paramIdx + 2, tainted);
		}

		/**
		 * Gets the number of parameters modeled in this taint
		 * 
		 * @return The number of parameters modeled in this taint
		 */
		public int getParameterCount() {
			return taintFlags.size() - 2;
		}

		/**
		 * Creates a set of taints from this cached taint base
		 * 
		 * @return The set of taints that corresponds to this cache entry
		 */
		public Set<Taint> toTaintSet() {
			Set<Taint> res = new HashSet<>();
			if (isBaseObject())
				res.add(new Taint(SourceSinkType.Field, -1, null, true));
			if (isReturnValue())
				res.add(new Taint(SourceSinkType.Return, -1, null, true));
			if (isAnyParameterTainted()) {
				for (int i = 0; i < getParameterCount(); i++) {
					if (isParameterTainted(i))
						res.add(new Taint(SourceSinkType.Parameter, i, null, true));
				}
			}
			return res;
		}

		/**
		 * Integrates the response of the LLM into this cache object
		 * 
		 * @param text The response of the LLM
		 */
		public void integrateLlmResponse(String text) {
			// Sanity check
			if (text.isBlank()) {
				logger.warn("The LLM returned an empty response");
				return;
			}

			// Some LLMs return proper sets
			if (text.startsWith("{") && text.endsWith("}"))
				text = text.substring(1, text.length() - 1);
			if (text.startsWith("[") && text.endsWith("]"))
				text = text.substring(1, text.length() - 1);

			// Split the individual elements in the set
			for (String element : text.split(",")) {
				element = element.trim();
				if (element.equals("b"))
					setBaseObjectTainted(true);
				else if (element.equals("r"))
					setReturnValueTainted(true);
				else if (element.startsWith("p")) {
					Integer idx = Ints.tryParse(element.substring(1));
					if (idx == null)
						logger.warn("LLM returned invalid parameter index {}", element);
					else
						setParameterTainted(idx, true);
				}
			}
		}

		/**
		 * Checks whether this taint cache is empty, i.e., nothing is tainted
		 * 
		 * @return True if nothing is tainted in this cached object, false if there is
		 *         at least one taint
		 */
		public boolean isEmpty() {
			return taintFlags.isEmpty();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (isBaseObject())
				sb.append("base");
			if (isAnyParameterTainted()) {
				for (int i = 0; i < getParameterCount(); i++) {
					if (isParameterTainted(i)) {
						if (sb.length() > 0)
							sb.append(",");
						sb.append("p");
						sb.append(String.valueOf(i));
					}
				}
			}
			if (isReturnValue()) {
				if (sb.length() > 0)
					sb.append(",");
				sb.append("return");
			}
			return sb.toString();
		}

		@Override
		public int hashCode() {
			return Objects.hash(taintFlags);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CachedBaseTaint other = (CachedBaseTaint) obj;
			return Objects.equals(taintFlags, other.taintFlags);
		}

	}

	/**
	 * Entry for the taint query cache to avoid querying the LLM multiple times for
	 * the same method
	 * 
	 * @author Steven Arzt
	 */
	protected static class TaintQueryCacheEntry {

		protected Map<CachedBaseTaint, CachedBaseTaint> taintTransformations = new HashMap<>();

		/**
		 * Queries this cache entry to check if there is already an LLM response for the
		 * given access path
		 * 
		 * @param incoming The incoming taint
		 * @return The LLM's answer for this kind of incoming access path, or
		 *         <code>null</code> if the LLM hasn't been queried for this access path
		 */
		public CachedBaseTaint query(CachedBaseTaint incoming) {
			return taintTransformations.get(incoming);
		}

		/**
		 * Registers the given LLM response for the given incoming taint
		 * 
		 * @param cachedIncoming The incoming taint
		 * @param outgoingTaint  The LLM response
		 */
		public void update(CachedBaseTaint cachedIncoming, CachedBaseTaint outgoingTaint) {
			taintTransformations.put(cachedIncoming, outgoingTaint);
		}

	}

	/**
	 * Uses the LLM to propagate a taint across a call site
	 * 
	 * @param callSite The call site where the taint has arrived
	 * @param incoming The incoming taint
	 * @return The taints that are valid after the call site has been processed
	 */
	public Set<Taint> propagate(Stmt callSite, Taint incoming) {
		// Have we seen this method before?
		final SootMethod callee = callSite.getInvokeExpr().getMethod();
		CachedBaseTaint cachedIncoming = new CachedBaseTaint(callSite, incoming);
		TaintQueryCacheEntry cacheEntry = aiCache.computeIfAbsent(callee, c -> new TaintQueryCacheEntry());

		CachedBaseTaint outgoingTaint = cacheEntry.query(cachedIncoming);
		if (outgoingTaint == null) {
			// We need to query the LLM
			outgoingTaint = queryLlm(callSite, incoming);
			if (outgoingTaint == null)
				return null;
			cacheEntry.update(cachedIncoming, outgoingTaint);
		}

		// if nothing is tainted, we can abort early
		if (outgoingTaint.isEmpty())
			return null;

		// If the LLM replied with keeping the incoming taint, we also retain the
		// original access path with all fields
		if (outgoingTaint.equals(cachedIncoming))
			return Collections.singleton(incoming);

		return outgoingTaint.toTaintSet();
	}

	/**
	 * Queries the LLM for how a given callee affects a given incoming taint
	 * 
	 * @param stmt     The call site over which the taint shall be propagated
	 * @param incoming The incoming taint
	 * @return The cache entry with the outgoing taints
	 */
	protected CachedBaseTaint queryLlm(Stmt stmt, Taint incoming) {
		// Do we have a taint that corresponds to the statement at hand?
		String normalizedLocal = taintToNormalizedLocal(incoming);
		if (normalizedLocal == null)
			return null;

		// Build the prompt
		final SootMethod callee = stmt.getInvokeExpr().getMethod();
		String normalizedSig = normalizeCallToMethod(callee);
		StringBuilder sb = new StringBuilder(normalizedSig.length() * 2);
		sb.append("/STATEMENT/ ");
		sb.append(normalizedSig);
		sb.append("\r\n");
		sb.append("/TAINT/ ");
		sb.append(normalizedLocal);
		String prompt = sb.toString();

		// Build the message to send to the LLM
		Msg msg = Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text(prompt).build()).build();
		Msg response = callWithAgent(msg);
		if (response != null && response.getGenerateReason() == GenerateReason.MODEL_STOP) {
			CachedBaseTaint cache = new CachedBaseTaint(stmt);
			for (ContentBlock block : response.getContent()) {
				if (block instanceof TextBlock) {
					TextBlock tb = (TextBlock) block;
					cache.integrateLlmResponse(tb.getText());
				} else
					logger.warn("Expected a text response from the LLM but got {}", block.getClass().getName());
			}
			return cache;
		}
		return null;
	}

	/**
	 * Generates the normalized local name for the base of the given access path
	 * 
	 * @param stmt     The statement to which the access path is applied
	 * @param incoming The incoming access path
	 * @return The normalized local name of the base object in the given access path
	 */
	protected String accessPathToNormalizedLocal(Stmt stmt, AccessPath incoming) {
		if (incoming.isInstanceRef()) {
			InvokeExpr iexpr = stmt.getInvokeExpr();
			Value pv = incoming.getPlainValue();
			if (iexpr instanceof InstanceInvokeExpr && ((InstanceInvokeExpr) iexpr).getBase() == pv)
				return "b";
			else if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() == pv)
				return "r";
			else if (iexpr.getArgCount() > 0) {
				for (int i = 0; i < iexpr.getArgCount(); i++) {
					if (iexpr.getArg(i) == pv)
						return "p" + i;
				}
			}
		}
		return null;
	}

	/**
	 * Generates the normalized local name for the base of the given taint
	 * 
	 * @param incoming The incoming taint
	 * @return The normalized local name of the base object in the given access path
	 */
	protected String taintToNormalizedLocal(Taint incoming) {
		if (incoming.isField())
			return "b";
		else if (incoming.isReturn())
			return "r";
		else if (incoming.isAnyParameter()) {
			return "p" + incoming.getParameterIndex();
		}
		return null;
	}

	/**
	 * Gets a normalized version of a call to the given method that does not drag in
	 * any spurious context from real call sites
	 * 
	 * @param callee The target method to which to create a normalized call
	 * @return A normalized call to the given method
	 */
	protected String normalizeCallToMethod(SootMethod callee) {
		final String sig = callee.getSignature();
		StringBuilder sb = new StringBuilder(sig.length() * 2);
		if (callee.getReturnType() != VoidType.v())
			sb.append("r = ");
		if (callee.isStatic())
			sb.append("staticinvoke ");
		else
			sb.append("virtualinvoke b.");
		sb.append(sig);
		sb.append("(");
		final int paramCount = callee.getParameterCount();
		for (int i = 0; i < paramCount; i++) {
			sb.append("p");
			sb.append(Integer.toString(i));
			if (i < paramCount - 1)
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}

}
