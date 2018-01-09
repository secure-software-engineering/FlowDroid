package soot.jimple.infoflow.methodSummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.generator.IClassSummaryHandler;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;
import soot.jimple.infoflow.methodSummary.generator.SummaryGeneratorFactory;
import soot.jimple.infoflow.methodSummary.xml.XMLWriter;

class Main {
	final List<String> failedMethos = new LinkedList<>();
	
	public static void main(final String[] args) throws FileNotFoundException, XMLStreamException {
		// Check the parameters
		if (args.length < 3 || args[0].contains("--") || args[1].contains("--")
				|| args[2].contains("--")) {
			printUsage();	
			return;
		}
		
		boolean forceOverwrite = false;
		boolean loadFullJAR = false;
		Set<String> excludes = new HashSet<>();
		int repeatCount = 1;
		
		// Initialize the summary generator
		SummaryGenerator generator = new SummaryGeneratorFactory().initSummaryGenerator();
		
		// Collect the classes to be analyzed from our command line
		final int offset = 2;
		List<String> classesToAnalyze = new ArrayList<String>(args.length - offset);
		int i = offset;
		while (i < args.length) {
			if (args[i].startsWith("--")) {
				if (args[i].equalsIgnoreCase("--forceOverwrite"))
					forceOverwrite = true;
				else if (args[i].equalsIgnoreCase("--loadFullJar"))
					loadFullJAR = true;
				else if (args[i].equalsIgnoreCase("--exclude")) {
					excludes.add(args[i + 1]);
					i++;
				}
				else if (args[i].equalsIgnoreCase("--repeat")) {
					repeatCount = Integer.parseInt(args[i + 1]);
					i++;
				}
				else if (args[i].equalsIgnoreCase("--aliasFlowIns"))
					generator.getConfig().setFlowSensitiveAliasing(false);
				else if (args[i].equalsIgnoreCase("--novalidate"))
					generator.getConfig().setValidateResults(false);
				else {
					System.err.println("Invalid command line argument: " + args[i]);
					return;
				}
			}
			else
				classesToAnalyze.add(args[i]);
			i++;
		}
		
		// We need classes to analyze
		if (classesToAnalyze.isEmpty()) {
			printUsage();	
			return;
		}
		
		// Run it
		generator.getConfig().setLoadFullJAR(loadFullJAR);
		generator.getConfig().setExcludes(excludes);
		generator.getConfig().setRepeatCount(repeatCount);
		final boolean doForceOverwrite = forceOverwrite;
		ClassSummaries summaries = generator.createMethodSummaries(args[0],
				classesToAnalyze, new IClassSummaryHandler() {
			
			@Override
			public boolean onBeforeAnalyzeClass(String className) {
				// Are we forced to analyze all classes?
				if (doForceOverwrite)
					return true;
				
				// If we already have a summary file for this class, we skip over it
				String summaryFile = className + ".xml";
				return !new File(args[1], summaryFile).exists();
			}
			
			@Override
			public void onMethodFinished(String methodSignature, MethodSummaries summaries) {
				System.out.println("Method " + methodSignature + " done.");
			}
			
			@Override
			public void onClassFinished(String className, MethodSummaries summaries) {
				// Write out the class
				String summaryFile = className + ".xml";
				write(summaries, summaryFile, args[1]);
				System.out.println("Class " + className + " done.");
			}
			
		});
		
		System.out.println("Done.");
		if (summaries != null)
			if (!summaries.getDependencies().isEmpty()) {
				System.out.println("Dependencies:");
				for (String className : summaries.getDependencies())
					System.out.println("\t" + className);
			}
	}
	
	/**
	 * Prints information on how the summary generator can be used
	 */
	private static void printUsage() {
		System.out.println("FlowDroid Summary Generator (c) Secure Software Engineering Group @ EC SPRIDE");
		System.out.println();
		System.out.println("Incorrect arguments: [0] = JAR File, [1] = output folder "
				+ "[2] = <list of classes>, [3] = <optional arguments>");
		System.out.println();
		System.out.println("Supported optional arguments:");
		System.out.println("\t--forceOverwrite: Load all classes in the given JAR");
		System.out.println("\t--loadFullJar: Load all classes in the given JAR");
		System.out.println("\t--exclude: Exclude the given class or package");
		System.out.println("\t--repeat n: Repeat the analysis of each class n times");
	}
	
	/**
	 * Writes the given flows into an xml file
	 * @param flows The flows to write out
	 * @param fileName The name of the file to be written
	 * @param folder The folder in which to place the xml file
	 */
	private static void write(MethodSummaries flows, String fileName, String folder) {
		// Create the target folder if it does not exist
		File f = new File(folder);
		if(!f.exists())
			f.mkdir();
		
		// Dump the flows
		XMLWriter writer = new XMLWriter();

		try {
			writer.write(new File(f,fileName),flows);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
