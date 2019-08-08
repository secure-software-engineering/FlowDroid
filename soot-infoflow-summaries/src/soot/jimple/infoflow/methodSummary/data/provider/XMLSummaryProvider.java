package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodClear;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.xml.MetaDataReader;
import soot.jimple.infoflow.methodSummary.xml.SummaryReader;
import soot.util.MultiMap;

/**
 * This class loads method summary xml files.
 *
 */
public class XMLSummaryProvider implements IMethodSummaryProvider {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final static String FILE_META_DATA = "SummaryMetaData.xml";

	private final SummaryReader summaryReader;
	private MetaDataReader metaDataReader;

	private ClassSummaries summaries = new ClassSummaries();

	private Set<String> supportedClasses = new HashSet<String>();
	protected Set<String> loadableClasses = new HashSet<String>();
	private Set<File> files;
	private Set<Path> pathes;
	private FileSystem fileSystem;

	protected Set<String> subsigMethodsWithSummaries = new HashSet<String>();
	private boolean hasLoadingErrors;

	/**
	 * Loads a summary from a folder within the StubDroid jar file.
	 * 
	 * @param folderInJar The folder in the JAR file from which to load the summary
	 *                    files
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public XMLSummaryProvider(String folderInJar) throws URISyntaxException, IOException {
		this(folderInJar, XMLSummaryProvider.class);
	}

	/**
	 * Loads a file or all files in a dir (not recursively)
	 * 
	 * @param source The file to load, or the directory from which to load all
	 *               summary files it contains
	 */
	public XMLSummaryProvider(File source) {
		this(Collections.singletonList(source));
	}

	/**
	 * Loads the given summary files
	 * 
	 * @param files A list of summary files to load. If this list contains
	 *              directories, all summary files within those directories are
	 *              loaded
	 */
	public XMLSummaryProvider(List<File> files) {
		this.summaryReader = new SummaryReader();
		this.metaDataReader = new MetaDataReader();

		this.files = new HashSet<File>();
		for (File f : files) {
			// Check if the file exists
			if (!f.exists())
				throw new RuntimeException("Input file does not exist: " + f);

			// Distinguish between files and directories
			if (f.isFile())
				this.files.add(f);
			else if (f.isDirectory()) {
				File[] filesInDir = f.listFiles();
				if (filesInDir == null)
					throw new RuntimeException("Could not get files in directory " + f);
				this.files.addAll(Arrays.asList(filesInDir));
			} else
				throw new RuntimeException("Invalid input file: " + f);
		}

		init();
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
	public XMLSummaryProvider(String folderInJar, Class<?> parentClass) throws URISyntaxException, IOException {
		this.summaryReader = new SummaryReader();
		this.metaDataReader = new MetaDataReader();

		File f = new File(folderInJar);
		if (f.exists()) {
			load(f);
			return;
		}

		loadSummariesFromJAR(folderInJar, parentClass);
		init();
	}

	protected void loadSummariesFromJAR(String folderInJar, Class<?> parentClass)
			throws URISyntaxException, IOException {
		Path path = getPathInJar(folderInJar, parentClass);
		fileSystem = path.getFileSystem();
		try (Stream<Path> walk = Files.walk(path, 1)) {
			pathes = new HashSet<Path>();
			for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
				Path classp = it.next();
				pathes.add(classp);
				String name = getFileName(classp);
				if (name.endsWith(".xml")) {
					if (!name.equals(FILE_META_DATA))
						loadableClasses.add(fileToClass(name));
				}
			}
		}
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

	private String getFileName(Path path) {
		return path.getFileName().toString();
	}

	private void load(File source) {
		if (!source.exists())
			throw new RuntimeException("Source directory " + source + " does not exist");

		if (source.isFile())
			files = Collections.singleton(source);
		else if (source.isDirectory()) {
			File[] filesInDir = source.listFiles();
			if (filesInDir == null)
				throw new RuntimeException("Could not get files in directory " + source);
			files = new HashSet<File>(Arrays.asList(filesInDir));
		} else
			throw new RuntimeException("Invalid input file: " + source);

		init();
	}

	private void init() {
		if (files != null) {
			for (File f : files) {
				if (f.isFile() && f.getName().endsWith(".xml")) {
					loadableClasses.add(f.getName().replace(".xml", ""));
				}
			}
		}

		// Eagerly load the meta data
		loadMetaData();
	}

	protected SummaryReader getSummaryReader() {
		return summaryReader;
	}

	@Override
	public boolean supportsClass(String clazz) {
		if (supportedClasses.contains(clazz))
			return true;
		if (loadableClasses != null && loadableClasses.contains(clazz))
			return true;
		return false;
	}

	@Override
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
		if (loadableClasses != null) {
			for (String className : classes)
				if (loadableClasses.contains(className))
					loadClass(className);
		}
		return summaries.filterForMethod(classes, methodSignature);
	}

	@Override
	public ClassMethodSummaries getMethodFlows(String className, String methodSignature) {
		if (loadableClasses != null && loadableClasses.contains(className))
			loadClass(className);
		ClassMethodSummaries classSummaries = summaries.getClassSummaries(className);
		return classSummaries == null ? null : classSummaries.filterForMethod(methodSignature);
	}

	protected void loadClass(String clazz) {
		// Do not load classes more than once
		if (supportedClasses.contains(clazz))
			return;

		if (files != null && !files.isEmpty()) {
			for (File f : files) {
				if (fileToClass(f).equals(clazz)) {
					try {
						ClassMethodSummaries classSummaries = new ClassMethodSummaries(clazz);
						summaryReader.read(f, classSummaries);
						addMethodSummaries(classSummaries);
						break;
					} catch (Exception e) {
						LoggerFactory.getLogger(getClass())
								.error(String.format("An error occurred while loading the summary of %s", clazz), e);
						hasLoadingErrors = true;
					}
				}
			}
		}
		if (pathes != null && !pathes.isEmpty()) {
			for (Path path : pathes) {
				if (fileToClass(getFileName(path)).equals(clazz)) {
					FileSystem fs = path.getFileSystem();
					if (fs != null && fs.isOpen()) {
						try (InputStream inputStream = fs.provider().newInputStream(path)) {
							ClassMethodSummaries classSummaries = new ClassMethodSummaries(clazz);
							summaryReader.read(new InputStreamReader(inputStream), classSummaries);
							addMethodSummaries(classSummaries);
							break;
						} catch (Exception e) {
							LoggerFactory.getLogger(getClass()).error(
									String.format("An error occurred while loading the summary of %s", clazz), e);
							hasLoadingErrors = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Merges new summaries into this provider
	 * 
	 * @param newSummaries The summaries to merge
	 */
	protected void addMethodSummaries(ClassMethodSummaries newSummaries) {
		addSubsigsForMethod(newSummaries.getMethodSummaries());
		summaries.merge(newSummaries);
		if (loadableClasses != null && !loadableClasses.isEmpty())
			loadableClasses.remove(newSummaries.getClassName());
		supportedClasses.add(newSummaries.getClassName());
	}

	protected void addSubsigsForMethod(MethodSummaries read) {
		final MultiMap<String, MethodFlow> flows = read.getFlows();
		final MultiMap<String, MethodClear> clears = read.getClears();
		if (flows != null)
			subsigMethodsWithSummaries.addAll(flows.keySet());
		if (clears != null)
			subsigMethodsWithSummaries.addAll(clears.keySet());
	}

	/**
	 * Loads the meta data for the summaries
	 */
	private void loadMetaData() {
		// Do we have a plain file we can load?
		if (files != null) {
			for (File f : files) {
				if (f.getName().equals(FILE_META_DATA)) {
					try {
						summaries.setMetaData(metaDataReader.read(f));
					} catch (Exception ex) {
						LoggerFactory.getLogger(getClass())
								.error("An error occurred while loading the meta data data of %s");
					}
				}
			}
		}

		// Do we have to load the meta data file from a JAR?
		if (pathes != null) {
			for (Path path : pathes) {
				if (getFileName(path).equals(FILE_META_DATA)) {
					try (InputStream inputStream = path.getFileSystem().provider().newInputStream(path)) {
						summaries.setMetaData(metaDataReader.read(new InputStreamReader(inputStream)));
					} catch (Exception e) {
						LoggerFactory.getLogger(getClass())
								.error("An error occurred while loading the meta data data of %s");
					}
				}
			}
		}
	}

	public boolean hasLoadingErrors() {
		return hasLoadingErrors;
	}

	private String fileToClass(File f) {
		return fileToClass(f.getName());
	}

	private String fileToClass(String s) {
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
		return this.supportedClasses;
	}

	@Override
	public Set<String> getLoadableClasses() {
		return this.loadableClasses;
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
		if (loadableClasses != null && loadableClasses.contains(className))
			loadClass(className);
		return summaries.getClassSummaries(className);
	}

	@Override
	public boolean mayHaveSummaryForMethod(String subsig) {
		if (loadableClasses != null && !loadableClasses.isEmpty()) {
			// we don't know, there are unloaded classes...
			return true;
		}

		return subsigMethodsWithSummaries.contains(subsig);
	}

}
