package soot.jimple.infoflow.util;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.html.HtmlEscapers;

import soot.jimple.infoflow.data.Abstraction;
import soot.util.IdentityHashSet;

/**
 * Creates a Graphviz/dot graph of a {@link Abstraction} tree.
 * Contains the abstractions as well as its predecessors an neighbors
 * 
 * @author Marc Miltenberger
 */
public final class DebugAbstractionTree {

	private static final Logger logger = LoggerFactory.getLogger(DebugAbstractionTree.class);

	private DebugAbstractionTree() {
	}

	/**
	 * Creates a dot graph with indices as labels for the abstractions (otherwise, it would be way too large).
	 * The correspondence is printed to log.
	 * @param absStart start abstraction of the tree (root) 
	 * @param printNeighbors whether to print neighbors
	 * @return the dot graph
	 */
	public static String createDotGraph(Abstraction absStart, boolean printNeighbors) {
		IdentityHashMap<Abstraction, Integer> absToId = new IdentityHashMap<>();
		ArrayDeque<Abstraction> workQueue = new ArrayDeque<>();
		StringBuilder sbNodes = new StringBuilder();
		StringBuilder sbEdges = new StringBuilder();

		int idCounter = 0;

		workQueue.add(absStart);
		while (!workQueue.isEmpty()) {
			Abstraction p = workQueue.poll();
			Integer id = absToId.get(p);
			if (id == null) {
				idCounter++;
				absToId.put(p, idCounter);
				String absName = String.valueOf(idCounter); // escape(p.toString());
				logger.info(idCounter + ": " + p);
				StringBuilder neighbors = new StringBuilder();
				for (int i = 0; i < p.getNeighborCount(); i++) {
					neighbors.append(String.format("|<n%d> n%d", i, i));
				}
				if (p.getNeighborCount() > 0 && printNeighbors)
					workQueue.addAll(p.getNeighbors());
				if (p.getPredecessor() != null)
					workQueue.add(p.getPredecessor());
				else {
					if (p.getSourceContext() != null)
						absName += " [source]";
				}
				sbNodes.append(String.format("    abs%d[label=\"{%s|{<a> A%s}}\"];\n", idCounter, absName,
						neighbors.toString()));
			}
		}
		workQueue.add(absStart);
		Set<Abstraction> seen = new IdentityHashSet<>();
		while (!workQueue.isEmpty()) {
			Abstraction p = workQueue.poll();
			if (seen.add(p)) {
				Integer id = absToId.get(p);
				Abstraction pred = p.getPredecessor();
				if (pred != null) {
					int dest = absToId.get(pred);
					sbEdges.append(String.format("    abs%s:a -> abs%d;\n", id, dest));
					workQueue.add(pred);
				}
				if (p.getNeighborCount() > 0 && printNeighbors) {
					int i = 0;
					for (Abstraction n : p.getNeighbors()) {
						int dest = absToId.get(n);
						sbEdges.append(String.format("    abs%s:n%s -> abs%d;\n", id, i, dest));
						i++;
					}
					workQueue.addAll(p.getNeighbors());
				}
			}
		}

		return "digraph Debug {\n" + "    node [shape=record];\n" + sbNodes.toString() + sbEdges.toString() + "}";
	}

	private static String escape(String string) {
		return HtmlEscapers.htmlEscaper().escape(string);
	}

}
