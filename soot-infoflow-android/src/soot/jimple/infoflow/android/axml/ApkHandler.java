package soot.jimple.infoflow.android.axml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/**
 * Provides access to the files within an APK and can add and replace files.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class ApkHandler implements AutoCloseable {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The handled APK file.
	 */
	protected File apk;

	/**
	 * Pointer to the ZipFile. If an InputStream for a file within the ZipFile is
	 * returned by {@link ApkHandler#getInputStream(String)} the ZipFile object has
	 * to remain available in order to read the InputStream.
	 */
	protected ZipFile zip;

	/**
	 * @param path the APK's path
	 * @throws ZipException occurs if the APK is no a valid zip file.
	 * @throws IOException  if an I/O error occurs.
	 * @see ApkHandler#ApkHandler(File)
	 */
	public ApkHandler(String path) throws ZipException, IOException {
		this(new File(path));
	}

	/**
	 * Creates a new {@link ApkHandler} which handles the given APK file.
	 * 
	 * @param apk the APK's path
	 * @throws ZipException occurs if the APK is no a valid zip file.
	 * @throws IOException  if an I/O error occurs.
	 */
	public ApkHandler(File apk) throws ZipException, IOException {
		this.apk = apk;
	}

	/**
	 * Returns the absolute path of the APK which is held by the {@link ApkHandler}.
	 * 
	 * @see File#getAbsolutePath()
	 */
	public String getAbsolutePath() {
		return this.apk.getAbsolutePath();
	}

	/**
	 * Returns the path of the APK which is held by the {@link ApkHandler}.
	 * 
	 * @see File#getPath()
	 */
	public String getPath() {
		return this.apk.getPath();
	}

	/**
	 * Returns the filename of the APK which is held by the {@link ApkHandler}.
	 * 
	 * @see File#getName()
	 */
	public String getFilename() {
		return this.apk.getName();
	}

	/**
	 * Returns an {@link InputStream} for a file within the APK.<br />
	 * The given filename has to be the relative path within the APK, e.g.
	 * <code>res/menu/main.xml</code>
	 * 
	 * @param filename the file's path
	 * @return {@link InputStream} for the searched file, if not found null
	 * @throws IOException if an I/O error occurs.
	 */
	public InputStream getInputStream(String filename) throws IOException {
		InputStream is = null;

		// check if zip file is already opened
		if (this.zip == null)
			this.zip = new ZipFile(this.apk);

		// search for file with given filename
		Enumeration<?> entries = this.zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			String entryName = entry.getName();
			if (entryName.equals(filename)) {
				is = this.zip.getInputStream(entry);
				break;
			}
		}

		return is;
	}

	/**
	 * @param files array with File objects to be added to the APK.
	 * @throws IOException if an I/O error occurs.
	 * @see {@link ApkHandler#addFilesToApk(List, Map)}
	 */
	public void addFilesToApk(List<File> files) throws IOException {
		this.addFilesToApk(files, new HashMap<String, String>());
	}

	/**
	 * Adds the files to the APK which is handled by this {@link ApkHandler}.
	 * 
	 * @param files Array with File objects to be added to the APK.
	 * @param paths Map containing paths where to put the files. The Map's keys are
	 *              the file's paths: <code>paths.get(file.getPath())</code>
	 * @throws IOException if an I/O error occurs.
	 */
	public void addFilesToApk(List<File> files, Map<String, String> paths) throws IOException {
		// close zip file to rename apk
		if (this.zip != null) {
			this.zip.close();
			this.zip = null;
		}

		// add missing paths to directories parameter
		for (File file : files) {
			if (!paths.containsKey(file.getPath()))
				paths.put(file.getPath(), file.getName());
		}

		// get a temp file
		File tempFile = File.createTempFile(this.apk.getName(), null);

		// delete it, otherwise we cannot rename the existing zip to it
		tempFile.delete();

		boolean renameOk = this.apk.renameTo(tempFile);
		if (!renameOk) {
			try {
				Files.move(this.apk, tempFile);
			} catch (IOException ex) {
				throw new IOException(
						"could not rename the file " + this.apk.getAbsolutePath() + " to " + tempFile.getAbsolutePath(),
						ex);
			}
		}

		byte[] buf = new byte[1024];
		try (ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(this.apk))) {
			ZipEntry entry;

			nextEntry: while ((entry = zin.getNextEntry()) != null) {
				// skip replaced entries
				for (String path : paths.values())
					if (entry.getName().equals(path))
						continue nextEntry;

				// Since we are modifying an APK file, the old signature becomes
				// invalid and we thus need to remove it.
				if (entry.getName().startsWith("META-INF/")
						&& (entry.getName().endsWith(".RSA") || entry.getName().endsWith(".SF")))
					continue;

				// if not replaced add the zip entry to the output stream
				ZipEntry ze = new ZipEntry(entry.getName());
				// Only compress those files that were compressed in the
				// original APK
				ze.setMethod(entry.getMethod());
				// We need to copy over those flags for the STORE method
				if (entry.getTime() != -1)
					ze.setTime(entry.getTime());
				if (entry.getSize() != -1)
					ze.setSize(entry.getSize());
				if (entry.getCrc() != -1)
					ze.setCrc(entry.getCrc());
				// Add the entry header to the ZIP file
				out.putNextEntry(ze);

				// transfer bytes from the zip file to the output file
				int len;
				while ((len = zin.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				// close entries
				zin.closeEntry();
				out.closeEntry();
			}

			// add files
			for (File file : files) {
				try (InputStream in = new FileInputStream(file)) {
					out.putNextEntry(new ZipEntry(paths.get(file.getPath())));
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					out.closeEntry();
				}
			}
		} finally {
			// Delete the tmeporary file
			if (tempFile != null && tempFile.exists())
				tempFile.delete();
		}
	}

	/**
	 * Closes this apk file
	 */
	public void close() {
		if (this.zip != null) {
			try {
				this.zip.close();
			} catch (IOException e) {
				logger.error("Could not close apk file", e);
			}
			this.zip = null;
		}
	}

}
