package soot.jimple.infoflow.methodSummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.generator.IClassSummaryHandler;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;
import soot.jimple.infoflow.methodSummary.generator.SummaryGeneratorFactory;
import soot.jimple.infoflow.methodSummary.xml.SummaryWriter;

/**
 * Main class for the StubDroid summary generator
 * 
 * @author Steven Arzt
 *
 */
class Main {

	private final Options options = new Options();

	private static final String OPTION_FORCE_OVERWRITE = "fo";
	private static final String OPTION_LOAD_FULL_JAR = "lf";
	private static final String OPTION_SUMMARIZE_FULL_JAR = "sf";
	private static final String OPTION_EXCLUDE = "e";
	private static final String OPTION_REPEAT = "r";
	private static final String OPTION_FLOW_TIMEOUT = "ft";
	private static final String OPTION_CLASS_TIMEOUT = "ct";
	private static final String OPTION_ANALYZE_HASHCODE_EQUALS = "he";
	private static final String OPTION_ANDROID_PLATFORMS = "p";
	private static final String OPTION_IGNORE_DEFAULT_SUMMARIES = "is";
	private static final String OPTION_WRITE_JIMPLE_FILES = "wj";

	public static void main(final String[] args) throws FileNotFoundException, XMLStreamException {
		Main main = new Main();
		main.run(args);
	}

	public Main() {
		initializeCommandLineOptions();
	}

	/**
	 * Initializes the set of available command-line options
	 */
	private void initializeCommandLineOptions() {
		options.addOption("?", "help", false, "Print this help message");

		options.addOption(OPTION_FORCE_OVERWRITE, "forceoverwrite", false,
				"Silently overwrite summary files in output directory");
		options.addOption(OPTION_LOAD_FULL_JAR, "loadfulljar", false, "Loads all classes from the given JAR file");
		options.addOption(OPTION_SUMMARIZE_FULL_JAR, "summarizefulljar", false,
				"Summarizes all classes from the given JAR file");
		options.addOption(OPTION_EXCLUDE, "exclude", true, "Excludes the given class(es)");
		options.addOption(OPTION_REPEAT, "repeat", true,
				"Repeats the summary generation multiple times. Useful for performance measurements.");
		options.addOption(OPTION_FLOW_TIMEOUT, "flowtimeout", true,
				"Aborts the per-method data flow analysis after the given number of seconds");
		options.addOption(OPTION_CLASS_TIMEOUT, "classtimeout", true,
				"Aborts the summary generation for the current class after the given number of seconds");
		options.addOption(OPTION_ANALYZE_HASHCODE_EQUALS, "analyzehashcodeequals", false,
				"Also analyze hashCode() and equals() methods");
		options.addOption(OPTION_ANDROID_PLATFORMS, "platformsdir", true,
				"Path to the platforms directory from the Android SDK");
		options.addOption(OPTION_IGNORE_DEFAULT_SUMMARIES, "ignoresummaries", false,
				"Existing summaries from the default summary directory are ignored");
		options.addOption(OPTION_WRITE_JIMPLE_FILES, "writejimplefiles", false, "Write out the Jimple files");
	}

	public void run(final String[] args) throws FileNotFoundException, XMLStreamException {
		// Parse the command-line parameters
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			final boolean forceOverwrite = cmd.hasOption(OPTION_FORCE_OVERWRITE);
			boolean loadFullJAR = cmd.hasOption(OPTION_LOAD_FULL_JAR);
			boolean summarizeFullJAR = cmd.hasOption(OPTION_SUMMARIZE_FULL_JAR);

			// We need proper parameters
			String[] extraArgs = cmd.getArgs();
			if (extraArgs.length < 2 || (extraArgs.length < 3 && !summarizeFullJAR)) {
				printHelpMessage();
				return;
			}

			// Parse the other command-line arguments
			Set<String> excludes = parseExcludes(cmd);

			// Initialize the summary generator
			SummaryGenerator generator = new SummaryGeneratorFactory().initSummaryGenerator();

			// Parse the mandatory arguments
			String toAnalyze = extraArgs[0];
			File outputFolder = new File(extraArgs[1]);

			// Check if the given files to analyze do exist
			List<String> filesToAnalyze = Arrays.asList(toAnalyze.split(File.pathSeparator));
			for (String fileStr : filesToAnalyze) {
				File file = new File(fileStr);
				if (!file.exists()) {
					System.err.println("File not found: " + file);
					System.exit(1);
					return;
				}
				if (file.isDirectory()) {
					File[] files = file.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(".jar");
						}
					});
					for (int c = 0; c < files.length; c++) {
						File f = files[c];
						toAnalyze += File.pathSeparator + f.getPath();
					}
				}
			}

			// Collect the classes to be analyzed from our command line
			List<String> classesToAnalyze = new ArrayList<>();
			if (!summarizeFullJAR) {
				for (int i = 2; i < extraArgs.length; i++) {
					if (extraArgs[i].startsWith("-")) {
						printHelpMessage();
						return;
					}
					classesToAnalyze.add(extraArgs[i]);
				}

				// We need classes to analyze
				if (classesToAnalyze.isEmpty()) {
					printHelpMessage();
					return;
				}
			}

			generator.getConfig().setLoadFullJAR(loadFullJAR);
			generator.getConfig().setSummarizeFullJAR(summarizeFullJAR);
			generator.getConfig().setExcludes(excludes);

			// Set optional settings
			configureOptionalSettings(cmd, generator);

			// Configure the output directory
			generator.getConfig().addAdditionalSummaryDirectory(outputFolder.getAbsolutePath());

			// Run it
			createSummaries(generator, classesToAnalyze, forceOverwrite, toAnalyze, outputFolder);

			System.out.println("Done.");
		} catch (ParseException e) {
			printHelpMessage();
			return;
		}
	}

	/**
	 * Configures optional settings for the summary generation that might have been
	 * set on the command
	 * 
	 * @param cmd       The command-line options
	 * @param generator The summary generator
	 */
	protected void configureOptionalSettings(CommandLine cmd, SummaryGenerator generator) {
		{
			int repeatCount = Integer.parseInt(cmd.getOptionValue(OPTION_REPEAT, "-1"));
			if (repeatCount > 0)
				generator.getConfig().setRepeatCount(repeatCount);
		}
		{
			long flowTimeout = Long.parseLong(cmd.getOptionValue(OPTION_FLOW_TIMEOUT, "-1"));
			if (flowTimeout > 0)
				generator.getConfig().setDataFlowTimeout(flowTimeout);
		}
		{
			long classTimeout = Long.parseLong(cmd.getOptionValue(OPTION_CLASS_TIMEOUT, "-1"));
			if (classTimeout > 0)
				generator.getConfig().setClassSummaryTimeout(classTimeout);
		}
		{
			boolean analyzeHashCodeEquals = cmd.hasOption(OPTION_ANALYZE_HASHCODE_EQUALS);
			if (analyzeHashCodeEquals)
				generator.getConfig().setSummarizeHashCodeEquals(analyzeHashCodeEquals);
		}
		{
			String platformsDir = cmd.getOptionValue(OPTION_ANDROID_PLATFORMS);
			generator.getConfig().setAndroidPlatformDir(platformsDir);
		}
		{
			boolean ignoreDefaultSummaries = cmd.hasOption(OPTION_IGNORE_DEFAULT_SUMMARIES);
			if (ignoreDefaultSummaries)
				generator.getConfig().setUseDefaultSummaries(false);
		}
		{
			boolean writeJimpleFiles = cmd.hasOption(OPTION_WRITE_JIMPLE_FILES);
			if (writeJimpleFiles)
				generator.getConfig().setWriteOutputFiles(true);
		}
	}

	/**
	 * Parses the command line and returns a set of all classes that shall be
	 * excluded from summary generation
	 * 
	 * @param cmd The command line to parse
	 * @return The set that contains all excluded classes
	 */
	private Set<String> parseExcludes(CommandLine cmd) {
		String[] excludes = cmd.getOptionValues(OPTION_EXCLUDE);
		if (excludes == null || excludes.length == 0)
			return Collections.emptySet();
		HashSet<String> excludeSet = new HashSet<>(excludes.length);
		for (String exclude : excludes)
			excludeSet.add(exclude);
		return excludeSet;
	}

	/**
	 * Displays the instructions on how to use StubDroid on the command line
	 */
	private void printHelpMessage() {
		System.out.println("FlowDroid Summary Generator (c) Secure Software Engineering Group");
		System.out.println();

		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("soot-infoflow-summaries <JAR File> <Output Directory> <Classes...> [OPTIONS]", options);
	}

	private static void createSummaries(SummaryGenerator generator, List<String> classesToAnalyze,
			final boolean doForceOverwrite, String toAnalyze, File outputFolder) {
		ClassSummaries summaries = generator.createMethodSummaries(toAnalyze, classesToAnalyze,
				new IClassSummaryHandler() {

					@Override
					public boolean onBeforeAnalyzeClass(String className) {
						// Are we forced to analyze all classes?
						if (doForceOverwrite)
							return true;

						// If we already have a summary file for this class, we skip over it
						String summaryFile = className + ".xml";
						return !new File(outputFolder, summaryFile).exists();
					}

					@Override
					public void onMethodFinished(String methodSignature, MethodSummaries summaries) {
						System.out.println("Method " + methodSignature + " done.");
					}

					@Override
					public void onClassFinished(ClassMethodSummaries summaries) {
						// Write out the class
						final String className = summaries.getClassName();
						String summaryFile = className + ".xml";
						write(summaries, summaryFile, outputFolder.getPath());
						System.out.println("Class " + className + " done.");
					}

				});
		if (summaries != null) {
			if (!summaries.getDependencies().isEmpty()) {
				System.out.println("Dependencies:");
				for (String className : summaries.getDependencies())
					System.out.println("\t" + className);
			}
		}
	}

	/**
	 * Writes the given flows into an xml file
	 * 
	 * @param flows    The flows to write out
	 * @param fileName The name of the file to be written
	 * @param folder   The folder in which to place the xml file
	 */
	private static void write(ClassMethodSummaries flows, String fileName, String folder) {
		// Create the target folder if it does not exist
		File f = new File(folder);
		if (!f.exists())
			f.mkdir();

		// Dump the flows
		SummaryWriter writer = new SummaryWriter();

		try {
			writer.write(new File(f, fileName), flows);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
