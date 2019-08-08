package soot.jimple.infoflow.methodSummary.taintWrappers;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

public class ReportMissingSummaryWrapper extends SummaryTaintWrapper {

	public ReportMissingSummaryWrapper(IMethodSummaryProvider flows) {
		super(flows);
	}

	ConcurrentHashMap<SootClass, AtomicInteger> classSummariesMissing = new ConcurrentHashMap<>();

	@Override
	protected void reportMissingMethod(SootMethod method) {
		count(method.getDeclaringClass(), classSummariesMissing);
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
			classes.appendChild(clazz);
		}
		rootElement.appendChild(classes);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
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
