package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import soot.jimple.infoflow.methodSummary.xml.SummaryReader;

/**
 * This class loads all method summary xml files.
 *
 */
public class EagerSummaryProvider extends XMLSummaryProvider {

	/**
	 * Loads a summary from a folder within the StubDroid jar file.
	 * 
	 * @param folderInJar The folder in the JAR file from which to load the summary
	 *                    files
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public EagerSummaryProvider(String folderInJar) throws URISyntaxException, IOException {
		this(folderInJar, EagerSummaryProvider.class);
	}

	/**
	 * Loads a summary from a folder within the StubDroid jar file.
	 * 
	 * @param folderInJar The folder in the JAR file from which to load the summary
	 *                    files
	 * @param parentClass The class in whose jar to look for the summary files
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public EagerSummaryProvider(String folderInJar, Class<?> parentClass) throws URISyntaxException, IOException {
		loadSummariesFromJAR(folderInJar, parentClass, p -> loadClass(p));
	}

	public EagerSummaryProvider(String folderInJar, Class<?> parentClass, SummaryReader reader)
			throws URISyntaxException, IOException {
		super(reader);
		loadSummariesFromJAR(folderInJar, parentClass, p -> loadClass(p));
	}

	/**
	 * Loads a file or all files in a dir (not recursively)
	 * 
	 * @param source The single file or directory to load
	 */
	public EagerSummaryProvider(File source) {
		this(Collections.singletonList(source));
	}

	/**
	 * Loads the summaries from all of the given files
	 * 
	 * @param files The files to load
	 */
	public EagerSummaryProvider(Collection<File> files) {
		loadSummariesFromFiles(files, f -> loadClass(f));
	}

	@Override
	public boolean mayHaveSummaryForMethod(String subsig) {
		return subsigMethodsWithSummaries.contains(subsig);
	}

	@Override
	public Set<String> getAllClassesWithSummaries() {
		return loadedClasses;
	}

}
