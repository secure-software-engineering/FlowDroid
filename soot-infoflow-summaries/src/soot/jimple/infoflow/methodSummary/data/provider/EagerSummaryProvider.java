package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

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
		super(folderInJar);
		load();
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
		super(folderInJar, parentClass);
		load();
	}

	/**
	 * Loads a file or all files in a dir (not recursively)
	 * 
	 * @param source The single file or directory to load
	 */
	public EagerSummaryProvider(File source) {
		super(source);
		load();
	}

	/**
	 * Loads the summaries from all of the given files
	 * 
	 * @param files The files to load
	 */
	public EagerSummaryProvider(List<File> files) {
		super(files);
		load();
	}

	/**
	 * Loads all summaries available in the target directory
	 */
	protected void load() {
		for (Object s : loadableClasses.toArray())
			loadClass(s.toString());
		loadableClasses = null;
	}

	@Override
	public boolean mayHaveSummaryForMethod(String subsig) {
		return subsigMethodsWithSummaries.contains(subsig);
	}

}
