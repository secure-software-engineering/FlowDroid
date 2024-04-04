package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ConcurrentClassSummaries;

/**
 * Concurrent version of the {@link LazySummaryProvider} class
 * 
 * @author Steven Arzt
 *
 */
public class ConcurrentLazySummaryProvider extends LazySummaryProvider {

	public ConcurrentLazySummaryProvider(File source) {
		super(source);
	}

	public ConcurrentLazySummaryProvider(List<File> files) {
		super(files);
	}

	public ConcurrentLazySummaryProvider(String folderInJar, Class<?> parentClass)
			throws URISyntaxException, IOException {
		super(folderInJar, parentClass);
	}

	public ConcurrentLazySummaryProvider(String folderInJar) throws URISyntaxException, IOException {
		super(folderInJar);
	}

	@Override
	protected ClassSummaries createClassSummaries() {
		return new ConcurrentClassSummaries();
	}

}
