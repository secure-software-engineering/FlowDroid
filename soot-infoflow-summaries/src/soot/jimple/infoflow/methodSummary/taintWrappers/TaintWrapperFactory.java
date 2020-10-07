package soot.jimple.infoflow.methodSummary.taintWrappers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider;

public class TaintWrapperFactory {

	public static final String DEFAULT_SUMMARY_DIR = "/summariesManual";

	public static SummaryTaintWrapper createTaintWrapper(Collection<String> files)
			throws FileNotFoundException, XMLStreamException {
		List<File> fs = new LinkedList<File>();
		for (String s : files)
			fs.add(new File(s));
		return new SummaryTaintWrapper(new LazySummaryProvider(fs));
	}

	public static SummaryTaintWrapper createTaintWrapperEager(Collection<String> files)
			throws FileNotFoundException, XMLStreamException {
		List<File> fs = new LinkedList<File>();
		for (String s : files)
			fs.add(new File(s));
		return new SummaryTaintWrapper(new EagerSummaryProvider(fs));
	}

	public static SummaryTaintWrapper createTaintWrapper(String f) throws FileNotFoundException, XMLStreamException {
		return createTaintWrapper(java.util.Collections.singletonList(f));
	}

	public static SummaryTaintWrapper createTaintWrapper() throws URISyntaxException, IOException {
		return new SummaryTaintWrapper(new LazySummaryProvider(DEFAULT_SUMMARY_DIR));
	}

	public static SummaryTaintWrapper createTaintWrapperEager() throws URISyntaxException, IOException {
		return new SummaryTaintWrapper(new EagerSummaryProvider(DEFAULT_SUMMARY_DIR));
	}

	public static SummaryTaintWrapper createTaintWrapperEager(String f)
			throws FileNotFoundException, XMLStreamException {
		return createTaintWrapperEager(java.util.Collections.singletonList(f));
	}

	public static SummaryTaintWrapper createTaintWrapper(File f) {
		return new SummaryTaintWrapper(new LazySummaryProvider(f));
	}

}
