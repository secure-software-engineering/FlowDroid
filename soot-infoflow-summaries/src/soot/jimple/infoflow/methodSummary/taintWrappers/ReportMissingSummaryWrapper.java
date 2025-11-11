package soot.jimple.infoflow.methodSummary.taintWrappers;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;

/**
 * Reports missing summaries by writing the results to a XML file.
 */
public class ReportMissingSummaryWrapper extends SummaryTaintWrapper {

	public ReportMissingSummaryWrapper(IMethodSummaryProvider flows) {
		super(flows);
	}

	private ConcurrentHashMap<SootClass, AtomicInteger> classSummariesMissing = new ConcurrentHashMap<>();
	private ConcurrentHashMap<SootMethod, AtomicInteger> methodSummariesMissing = new ConcurrentHashMap<>();
	private boolean prettyPrint = false;
	private boolean showAppClasses = false;
	private boolean countMethods = false;

	@Override
	protected void reportMissingMethod(SootMethod method) {
		SootClass decl = method.getDeclaringClass();
		if (!showAppClasses && decl.isApplicationClass())
			return;
		count(decl, classSummariesMissing);
		if (countMethods)
			count(method, methodSummariesMissing);
	}

	private static <T> void count(T item, Map<T, AtomicInteger> map) {
		AtomicInteger ai = new AtomicInteger();
		{
			AtomicInteger old = map.putIfAbsent(item, ai);
			if (old != null)
				ai = old;
		}

		ai.incrementAndGet();
	}

	/**
	 * Sets the pretty printing flag. When enabled, pretty printing
	 * creates new lines for each XML node, and uses indentation for the XML tree
	 * @param prettyPrint whether pretty printing should be enabled
	 */
	public void setPrettyPrinting(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	/**
	 * Pretty printing creates new lines for each XML node, and uses indentation for the XML tree
	 * @return returns true if enabled, otherwise false
	 */
	public boolean isPrettyPrinting() {
		return prettyPrint;
	}

	/**
	 * When the given parameter is true, the class also reports application classes,
	 * i.e. classes that are part of the application being analyzed.
	 * @param showAppClasses whether app classes should be shown
	 */
	public void setShowApplicationClasses(boolean showAppClasses) {
		this.showAppClasses = showAppClasses;
	}

	/**
	 * Returns whether this class also reports application classes,
	 * i.e. classes that are part of the application being analyzed
	 * @return returns true if enabled, otherwise false
	 */
	public boolean isShowingApplicationClasses() {
		return showAppClasses;
	}

	/**
	 * If the given parameter is true, this class will report counts
	 * on a per class and per method basis.
	 * @param countMethods whether to count methods
	 */
	public void setCountMethods(boolean countMethods) {
		this.countMethods = countMethods;
	}

	/**
	 * Returns whether counting methods is enabled
	 * @return returns true if enabled, otherwise false
	 */
	public boolean isCountMethods() {
		return countMethods;
	}

	public void writeResults(File file) throws IOException, ParserConfigurationException, TransformerException {
		Map<SootClass, Integer> sortedClassSummariesMissing = sortMap(classSummariesMissing);
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("MissingSummaries");
		doc.appendChild(rootElement);

		Element classes = doc.createElement("Classes");
		for (Entry<SootClass, Integer> i : sortedClassSummariesMissing.entrySet()) {

			Element clazz = doc.createElement("Class");
			clazz.setAttribute("Name", i.getKey().getName());
			clazz.setAttribute("Count", String.valueOf(i.getValue()));
			if (countMethods) {
				SootClass c = i.getKey();
				Map<SootMethod, AtomicInteger> methods = new HashMap<>(c.getMethods().size());
				for (SootMethod m : c.getMethods()) {
					AtomicInteger v = methodSummariesMissing.get(m);
					if (v != null) {
						methods.put(m, v);
					}
				}
				sortMap(methods);
				for (Entry<SootMethod, AtomicInteger> m : methods.entrySet()) {
					Element method = doc.createElement("Method");
					method.setAttribute("Name", m.getKey().getSubSignature());
					method.setAttribute("Count", String.valueOf(m.getValue()));
					clazz.appendChild(method);
				}
			}
			classes.appendChild(clazz);
		}
		rootElement.appendChild(classes);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();

		if (prettyPrint) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		}
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);

		transformer.transform(source, result);

	}

	private static <T> Map<T, Integer> sortMap(final Map<T, AtomicInteger> input) {
		Map<T, Integer> res = new TreeMap<>(new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				return -Integer.compare(input.get(o1).get(), input.get(o2).get());
			}
		});
		for (Entry<T, AtomicInteger> i : input.entrySet()) {
			res.put(i.getKey(), i.getValue().get());
		}
		return res;
	}

}
