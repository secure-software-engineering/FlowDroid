package soot.jimple.infoflow.results.util;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import soot.jimple.infoflow.results.xml.InfoflowResultsReader;
import soot.jimple.infoflow.results.xml.SerializedInfoflowResults;

/**
 * Utility class for comparing two data flow results
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowResultComparator {
	
	private static InfoflowResultComparator INSTANCE = null;
	
	public static InfoflowResultComparator v() {
		if (INSTANCE == null)
			INSTANCE = new InfoflowResultComparator();
		return INSTANCE;
	}
	
	/**
	 * Checks whether the data flow results in two given files are equal
	 * @param file1 The full path and file name of the fist file
	 * @param file2 The full path and file name of the second file
	 * @return True if the two files encode the same data flows, otherwise
	 * false
	 * @throws XMLStreamException Thrown if one of the XML files is
	 * syntactically invalid
	 * @throws IOException Thrown if one of the two files cannot be opened
	 */
	public boolean resultEquals(String file1, String file2)
			throws XMLStreamException, IOException {
		InfoflowResultsReader rdr = new InfoflowResultsReader();
		SerializedInfoflowResults results1 = rdr.readResults(file1);
		SerializedInfoflowResults results2 = rdr.readResults(file2);
		return results1.equals(results2);
	}
	
	public static void main(String[] args) throws XMLStreamException, IOException {
		if (args.length != 2) {
			System.err.println("Usage: InfoflowResultComparator <file1> <file2>");
			return;
		}
		
		if (InfoflowResultComparator.v().resultEquals(args[0], args[1]))
			System.out.println("Files are equal");
		else
			System.out.println("Files are NOT equal");
	}

}
