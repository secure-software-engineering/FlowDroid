package soot.jimple.infoflow.methodSummary.data.provider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ConcurrentClassSummaries;

/**
 * Concurrent version of the {@link EagerSummaryProvider} class
 * 
 * @author Steven Arzt
 *
 */
public class ConcurrentEagerSummaryProvider extends EagerSummaryProvider {

	public ConcurrentEagerSummaryProvider(File source) {
		super(source);
	}

	public ConcurrentEagerSummaryProvider(List<File> files) {
		super(files);
	}

	public ConcurrentEagerSummaryProvider(String folderInJar, Class<?> parentClass)
			throws URISyntaxException, IOException {
		super(folderInJar, parentClass);
	}

	public ConcurrentEagerSummaryProvider(String folderInJar) throws URISyntaxException, IOException {
		super(folderInJar);
	}

	@Override
	protected ClassSummaries createClassSummaries() {
		return new ConcurrentClassSummaries();
	}

}
