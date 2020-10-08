package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;

/**
 * This class loads method summary xml files on demand.
 *
 */
public class LazySummaryProvider extends XMLSummaryProvider {

	protected Set<File> files = new HashSet<>();
	protected Set<Path> pathes = new HashSet<>();
	protected Set<String> loadableClasses = new HashSet<String>();

	/**
	 * Loads a summary from a folder within the StubDroid jar file.
	 * 
	 * @param folderInJar The folder in the JAR file from which to load the summary
	 *                    files
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public LazySummaryProvider(String folderInJar) throws URISyntaxException, IOException {
		this(folderInJar, LazySummaryProvider.class);
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
	public LazySummaryProvider(String folderInJar, Class<?> parentClass) throws URISyntaxException, IOException {
		loadSummariesFromJAR(folderInJar, parentClass, p -> {
			this.pathes.add(p);
			loadableClasses.add(fileToClass(getFileName(p)));
		});
	}

	/**
	 * Loads a file or all files in a dir (not recursively)
	 * 
	 * @param source The single file or directory to load
	 */
	public LazySummaryProvider(File source) {
		this(Collections.singletonList(source));
	}

	/**
	 * Loads the summaries from all of the given files
	 * 
	 * @param files The files to load
	 */
	public LazySummaryProvider(List<File> files) {
		loadSummariesFromFiles(files, f -> {
			this.files.add(f);
			loadableClasses.add(fileToClass(f));
		});
	}

	protected void loadClass(String clazz) {
		// Do not load classes more than once
		if (loadedClasses.contains(clazz))
			return;

		if (files != null && !files.isEmpty()) {
			for (File f : files) {
				if (fileToClass(f).equals(clazz)) {
					loadClass(clazz, f);
					break;
				}
			}
		}
		if (pathes != null && !pathes.isEmpty()) {
			for (Path path : pathes) {
				if (fileToClass(getFileName(path)).equals(clazz)) {
					loadClass(clazz, path);
					break;
				}
			}
		}
	}

	@Override
	public boolean supportsClass(String clazz) {
		if (loadableClasses != null && loadableClasses.contains(clazz))
			return true;
		return super.supportsClass(clazz);
	}

	@Override
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
		// We may need to lazily load the summaries
		if (loadableClasses != null) {
			for (String className : classes)
				if (loadableClasses.contains(className))
					loadClass(className);
		}
		return super.getMethodFlows(classes, methodSignature);
	}

	@Override
	protected ClassMethodSummaries getClassSummaries(String className) {
		// We may need to lazily load the summaries
		if (loadableClasses != null && loadableClasses.contains(className))
			loadClass(className);
		return super.getClassSummaries(className);
	}

	@Override
	protected void addMethodSummaries(ClassMethodSummaries newSummaries) {
		super.addMethodSummaries(newSummaries);

		// Since we now have summaries for the classes, we prevent further loadings for
		// this class
		if (loadableClasses != null && !loadableClasses.isEmpty())
			loadableClasses.remove(newSummaries.getClassName());
	}

	/**
	 * Gets the names of all classes for which summaries have not yet been loaded,
	 * but are available on some external storage
	 * 
	 * @return The set of classes for which summaries can be loaded
	 */
	public Set<String> getLoadableClasses() {
		return this.loadableClasses;
	}

	@Override
	public ClassMethodSummaries getClassFlows(String className) {
		if (loadableClasses != null && loadableClasses.contains(className))
			loadClass(className);
		return super.getClassFlows(className);
	}

	@Override
	public boolean mayHaveSummaryForMethod(String subsig) {
		if (loadableClasses != null && !loadableClasses.isEmpty()) {
			// we don't know, there are unloaded classes...
			return true;
		}
		return super.mayHaveSummaryForMethod(subsig);
	}

	@Override
	public Set<String> getAllClassesWithSummaries() {
		Set<String> classes = new HashSet<>(loadedClasses.size() + loadableClasses.size());
		classes.addAll(loadedClasses);
		classes.addAll(loadableClasses);
		return classes;
	}

}
