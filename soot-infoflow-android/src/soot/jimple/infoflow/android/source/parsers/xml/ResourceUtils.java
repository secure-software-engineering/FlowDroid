package soot.jimple.infoflow.android.source.parsers.xml;

import java.io.IOException;
import java.io.InputStream;

public class ResourceUtils {

	/**
	 * Returns an input stream of a specified resource files, which is contained within the
	 * JAR. This method does throw an exception in case the resource file was not found.
	 * The caller responsible for closing the stream.  
	 * @param filename the resource file name
	 * @return the input stream
	 * @throws IOException
	 */
	public static InputStream getResourceStream(String filename) throws IOException {
		return soot.jimple.infoflow.util.ResourceUtils.getResourceStream(ResourceUtils.class, filename);
	}

}
