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

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.xml.XMLReader;

/**
 * This class loads method summary xml files on demand.
 *
 */
public class LazySummaryProvider implements IMethodSummaryProvider {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private XMLReader reader;
	private ClassSummaries summaries = new ClassSummaries();
	private Set<String> supportedClasses = new HashSet<String>();
	private Set<String> loadableClasses = new HashSet<String>();
	private Set<File> files;
	private Set<Path> pathes;
	private FileSystem fileSystem;

	/**
	 * Loads a summary from a folder within the StubDroid jar file.
	 * 
	 * @param folderInJar
	 *            The folder in the JAR file from which to load the summary files
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public LazySummaryProvider(String folderInJar) throws URISyntaxException, IOException {
		File f = new File(folderInJar);
		if (f.exists()) {
			load(f);
			return;
		}

		URI uri = null;
		String jarRelativePath = folderInJar.startsWith("/") ? folderInJar : "/" + folderInJar;
		URL resourceURL = LazySummaryProvider.class.getResource(jarRelativePath);
		if (resourceURL == null) {
			logger.warn(String.format("Could not find folder %s in JAR, trying normal folder on disk...", folderInJar));
			String classLocation = LazySummaryProvider.class.getProtectionDomain().getCodeSource().getLocation()
					.getPath();
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
		// We load a file system to retrieve all available class summaries
		// Note that we cannot close the file system, since it may not be
		// possible to reopen it
		// using the same URL.
		// However, since we are loading from inside the JAR, we only create a
		// few file systems this way
		// File systems are reused automatically between different
		// LazySummaryProviders if their URL is the same.
		// Thus, in worst case, we open file systems for all different summary
		// folders in the JAR, which are being closed
		// when the JVM terminates.
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
		fileSystem = path.getFileSystem();
		Stream<Path> walk = Files.walk(path, 1);
		try {
			pathes = new HashSet<Path>();
			for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
				Path classp = it.next();
				pathes.add(classp);
				loadableClasses.add(fileToClass(getFileName(classp)));
			}
		} finally {
			walk.close();
		}
		init();

	}

	private String getFileName(Path path) {
		return path.getFileName().toString();
	}

	/**
	 * Loads a file or all files in a dir (not recursively)
	 * 
	 * @param source
	 */
	public LazySummaryProvider(File source) {
		load(source);
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

	public LazySummaryProvider(List<File> files) {
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

	private void init() {
		this.reader = new XMLReader();
		if (files != null) {
			for (File f : files) {
				if (f.isFile() && f.getName().endsWith(".xml")) {
					loadableClasses.add(f.getName().replace(".xml", ""));
				}
			}
		}
	}

	@Override
	public boolean supportsClass(String clazz) {
		if (supportedClasses.contains(clazz))
			return true;
		if (loadableClasses.contains(clazz))
			return true;
		return false;
	}

	@Override
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
		for (String className : classes)
			if (loadableClasses.contains(className))
				loadClass(className);
		return summaries.filterForMethod(classes, methodSignature);
	}

	@Override
	public MethodSummaries getMethodFlows(String className, String methodSignature) {
		if (loadableClasses.contains(className))
			loadClass(className);
		MethodSummaries classSummaries = summaries.getClassSummaries(className);
		return classSummaries == null ? null : classSummaries.filterForMethod(methodSignature);
	}

	private void loadClass(String clazz) {
		// Do not load classes more than once
		if (supportedClasses.contains(clazz))
			return;

		if (files != null) {
			for (File f : files) {
				if (fileToClass(f).equals(clazz)) {
					try {
						summaries.merge(clazz, reader.read(f));
						loadableClasses.remove(clazz);
						supportedClasses.add(clazz);
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (pathes != null) {
			for (Path path : pathes) {
				if (fileToClass(getFileName(path)).equals(clazz)) {
					try (InputStream inputStream = path.getFileSystem().provider().newInputStream(path)) {
						summaries.merge(clazz, reader.read(new InputStreamReader(inputStream)));
						loadableClasses.remove(clazz);
						supportedClasses.add(clazz);
						break;
					} catch (Exception e) {
						LoggerFactory.getLogger(getClass())
								.error(String.format("An error occurred while loading the summary of %s", clazz), e);
					}
				}
			}
		}
	}

	private String fileToClass(File f) {
		return fileToClass(f.getName());
	}

	private String fileToClass(String s) {
		return s.replace(".xml", "");
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
	public ClassSummaries getSummaries() {
		return summaries;
	}

	@Override
	public MethodSummaries getClassFlows(String className) {
		if (loadableClasses.contains(className))
			loadClass(className);
		return summaries.getClassSummaries(className);
	}

}
