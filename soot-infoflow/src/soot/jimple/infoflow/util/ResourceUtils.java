package soot.jimple.infoflow.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceUtils {

	/**
	 * Returns an input stream of a specified resource files, which is contained
	 * within the JAR. This method does throw an exception in case the resource file
	 * was not found. The caller responsible for closing the stream.
	 * 
	 * @param class
	 *            the class file to use as a base
	 * @param filename
	 *            the resource file name
	 * @return the input stream
	 * @throws IOException
	 */
	public static InputStream getResourceStream(Class<?> clazz, String filename) throws IOException {
		File f = new File(filename);
		if (f.exists())
			return new FileInputStream(f);

		if (!filename.startsWith("/"))
			filename = "/" + filename;

		InputStream inp = clazz.getResourceAsStream(filename);
		if (inp == null)
			throw new IOException(String.format("Resource %s was not found", filename));
		return inp;
	}

	/**
	 * Returns an input stream of a specified resource files, which is contained
	 * within the JAR. This method does throw an exception in case the resource file
	 * was not found. The caller responsible for closing the stream.
	 * 
	 * @param filename
	 *            the resource file name
	 * @return the input stream
	 * @throws IOException
	 */
	public static InputStream getResourceStream(String filename) throws IOException {
		return getResourceStream(ResourceUtils.class, filename);
	}
}
