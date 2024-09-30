package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodClear;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SummaryMetaData;
import soot.jimple.infoflow.methodSummary.xml.MetaDataReader;
import soot.jimple.infoflow.methodSummary.xml.SummaryReader;
import soot.util.MultiMap;

/**
 * This class loads method summary xml files.
 *
 */
public abstract class XMLSummaryProvider extends AbstractMethodSummaryProvider {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected final static String FILE_META_DATA = "SummaryMetaData.xml";

	protected final SummaryReader summaryReader;
	protected MetaDataReader metaDataReader = new MetaDataReader();

	protected ClassSummaries summaries = createClassSummaries();

	protected Set<String> loadedClasses = new ConcurrentHashSet<>();

	protected Set<String> subsigMethodsWithSummaries = new ConcurrentHashSet<String>();
	protected boolean hasLoadingErrors;

	protected XMLSummaryProvider() {
		this(new SummaryReader());
	}

	protected XMLSummaryProvider(SummaryReader reader) {
		summaryReader = reader;
	}

	/**
	 * Creates a new instance of the {@link ClassSummaries} class
	 * 
	 * @return The new {@link ClassSummaries}
	 */
	protected ClassSummaries createClassSummaries() {
		return new ClassSummaries();
	}

	/**
	 * Loads the given summary files
	 * 
	 * @param files A list of summary files to load. If this list contains
	 *              directories, all summary files within those directories are
	 *              loaded
	 */
	protected void loadSummariesFromFiles(Collection<File> files, Consumer<File> summaryLoader) {
		SummaryMetaData metadata = null;
		for (File f : files) {
			// Check if the file exists
			if (!f.exists())
				throw new RuntimeException("Input file does not exist: " + f.getAbsolutePath());

			// Distinguish between files and directories
			if (f.isFile())
				summaryLoader.accept(f);
			else if (f.isDirectory()) {
				File[] filesInDir = f.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".xml");
					}

				});
				if (filesInDir == null)
					throw new RuntimeException("Could not get files in directory " + f);
				for (File f2 : filesInDir) {
					if (f2.getName().equals(FILE_META_DATA)) {
						metadata = loadMetaDataFile(f2);
						summaries.setMetaData(metadata);
					} else
						summaryLoader.accept(f2);
				}
			} else
				throw new RuntimeException("Invalid input file: " + f);
		}

		// Merge the hierarchy from the meta data into the summaries
		if (metadata != null) {
			metadata.mergeHierarchyData(summaries);
			loadedClasses.addAll(metadata.getClassesWithHierarchyInfo());
		}
	}

	protected void loadSummariesFromJAR(String folderInJar, Class<?> parentClass, Consumer<Path> summaryLoader)
			throws URISyntaxException, IOException {
		SummaryMetaData metadata = null;
		Path path = getPathInJar(folderInJar, parentClass);
		try (Stream<Path> walk = Files.walk(path, 1)) {
			for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
				Path classp = it.next();
				String name = getFileName(classp);
				if (name.endsWith(".xml")) {
					if (name.equals(FILE_META_DATA)) {
						metadata = loadMetaDataFile(classp);
						summaries.setMetaData(metadata);
					} else
						summaryLoader.accept(classp);
				}
			}
		}

		// Merge the hierarchy from the meta data into the summaries
		if (metadata != null)
			metadata.mergeHierarchyData(summaries);
	}

	protected SummaryMetaData loadMetaDataFile(File f) {
		try (Reader rdr = new InputStreamReader(new FileInputStream(f))) {
			return metaDataReader.read(rdr);
		} catch (Exception e) {
			logger.error(String.format("An error occurred while loading the meta data of %s", f.getAbsolutePath()));
		}
		return null;
	}

	protected SummaryMetaData loadMetaDataFile(Path path) {
		if (!hasLoadingErrors) {
			try (Reader rdr = new InputStreamReader(path.getFileSystem().provider().newInputStream(path))) {
				return metaDataReader.read(rdr);
			} catch (ClosedChannelException e) {
				logger.error("Channel closed for path loading, ending summary loading.");
				hasLoadingErrors = true;
			} catch (Exception e) {
				logger.error(String.format("An error occurred while loading the meta data of %s", path.toString()));
			}
		}
		return null;
	}

	protected Path getPathInJar(String folderInJar, Class<?> parentClass) throws URISyntaxException, IOException {
		URI uri = null;
		String jarRelativePath = folderInJar.startsWith("/") ? folderInJar : "/" + folderInJar;
		URL resourceURL = parentClass.getResource(jarRelativePath);
		if (resourceURL == null) {
			logger.warn(String.format("Could not find folder %s in JAR, trying normal folder on disk...", folderInJar));
			String classLocation = parentClass.getProtectionDomain().getCodeSource().getLocation().getPath();
			File classFile = new File(new URI("file:///" + classLocation));
			if (classFile.getCanonicalPath().endsWith("build" + File.separator + "classes"))
				classFile = classFile.getParentFile().getParentFile();
			classFile = new File(classFile, folderInJar);
			if (classFile.exists())
				uri = classFile.toURI();
		} else
			uri = resourceURL.toURI();

		// Do we have the summaries
		if (uri == null)
			throw new RuntimeException(String.format("Could not find summaries in folder %s", folderInJar));

		Path path;
		// We load a file system to retrieve all available class summaries Note that we
		// cannot close the file system, since it may not be possible to reopen it using
		// the same URL. However, since we are loading from inside the JAR, we only
		// create a few file systems this way File systems are reused automatically
		// between different LazySummaryProviders if their URL is the same. Thus, in
		// worst case, we open file systems for all different summary folders in the
		// JAR, which are being closed when the JVM terminates.
		FileSystem fileSystem = null;
		if (uri.getScheme().equals("jar")) {
			try {
				fileSystem = FileSystems.getFileSystem(uri);
			} catch (FileSystemNotFoundException e) {
				fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
			}
			path = fileSystem.getPath(folderInJar);
		} else {
			path = Paths.get(uri);
		}
		return path;
	}

	protected String getFileName(Path path) {
		return path.getFileName().toString();
	}

	protected SummaryReader getSummaryReader() {
		return summaryReader;
	}

	@Override
	public boolean supportsClass(String clazz) {
		return loadedClasses.contains(clazz);
	}

	@Override
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
		return summaries.filterForMethod(classes, methodSignature);
	}

	@Override
	public ClassMethodSummaries getMethodFlows(String className, String methodSignature) {
		ClassMethodSummaries classSummaries = getClassSummaries(className);
		return classSummaries == null ? null : classSummaries.filterForMethod(methodSignature);
	}

	/**
	 * Gets all summaries for the given class
	 * 
	 * @param className The name of the class for which to get the summaries
	 * @return The data flow summaries for the given class
	 */
	protected ClassMethodSummaries getClassSummaries(String className) {
		ClassMethodSummaries classSummaries = summaries.getClassSummaries(className);
		return classSummaries;
	}

	/**
	 * Loads a summary from the given path
	 * 
	 * @param path The path from which to load the summary
	 */
	protected void loadClass(Path path) {
		loadClass(fileToClass(getFileName(path)), path);
	}

	/**
	 * Loads a summary for the given class from the given path
	 * 
	 * @param clazz The class for which to load the summary
	 * @param path  The path from which to load the summary
	 */
	protected void loadClass(String clazz, Path path) {
		try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ);
				Reader rdr = new InputStreamReader(inputStream)) {
			ClassMethodSummaries classSummaries = new ClassMethodSummaries(clazz);
			summaryReader.read(rdr, classSummaries);
			addMethodSummaries(classSummaries);
			onClassSummariesLoaded(clazz);
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass())
					.error(String.format("An error occurred while loading the summary of %s", clazz), e);
			hasLoadingErrors = true;
		}
	}

	/**
	 * Loads a summary from the given file
	 * 
	 * @param f The file from which to load the summary
	 */
	protected void loadClass(File f) {
		loadClass(fileToClass(f), f);
	}

	/**
	 * Loads a summary for the given class from the given file
	 * 
	 * @param clazz The class for which to load the summary
	 * @param f     The file from which to load the summary
	 */
	protected void loadClass(String clazz, File f) {
		try {
			ClassMethodSummaries classSummaries = new ClassMethodSummaries(clazz);
			summaryReader.read(f, classSummaries);
			addMethodSummaries(classSummaries);
			onClassSummariesLoaded(clazz);
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass())
					.error(String.format("An error occurred while loading the summary of %s", clazz), e);
			hasLoadingErrors = true;
		}
	}

	/**
	 * Callback method that is called when the summaries for the given have been
	 * loaded
	 * 
	 * @param clazz The class for which summaries have been loaded
	 */
	protected void onClassSummariesLoaded(String clazz) {
		//
	}

	/**
	 * Merges new summaries into this provider
	 * 
	 * @param newSummaries The summaries to merge
	 */
	protected void addMethodSummaries(ClassMethodSummaries newSummaries) {
		addSubsigsForMethod(newSummaries.getMethodSummaries());
		summaries.merge(newSummaries);
		loadedClasses.add(newSummaries.getClassName());
	}

	protected void addSubsigsForMethod(MethodSummaries read) {
		final MultiMap<String, MethodFlow> flows = read.getFlows();
		final MultiMap<String, MethodClear> clears = read.getClears();
		if (flows != null)
			subsigMethodsWithSummaries.addAll(flows.keySet());
		if (clears != null)
			subsigMethodsWithSummaries.addAll(clears.keySet());
	}

	public boolean hasLoadingErrors() {
		return hasLoadingErrors;
	}

	protected String fileToClass(File f) {
		return fileToClass(f.getName());
	}

	protected String fileToClass(String s) {
		int idx = s.lastIndexOf(".");
		if (idx >= 0) {
			String ext = s.substring(idx);
			if (ext.equals(".xml"))
				s = s.substring(0, idx);
		}
		return s;
	}

	@Override
	public Set<String> getSupportedClasses() {
		return this.loadedClasses;
	}

	/**
	 * Gets all method flow summaries that have been loaded so far
	 * 
	 * @return All summaries that have been loaded so far
	 */
	@Override
	public ClassSummaries getSummaries() {
		return summaries;
	}

	@Override
	public ClassMethodSummaries getClassFlows(String className) {
		return summaries.getClassSummaries(className);
	}

	@Override
	public boolean mayHaveSummaryForMethod(String subsig) {
		return subsigMethodsWithSummaries.contains(subsig);
	}

	@Override
	public boolean isMethodExcluded(String className, String subSignature) {
		ClassMethodSummaries summaries = getClassSummaries(className);
		return summaries != null && summaries.getMethodSummaries().isExcluded(subSignature);
	}

}
