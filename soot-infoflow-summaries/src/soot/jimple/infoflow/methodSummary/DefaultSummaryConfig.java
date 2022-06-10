package soot.jimple.infoflow.methodSummary;

import java.util.LinkedList;
import java.util.List;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.methodSummary.generator.SummaryGeneratorConfiguration;
import soot.options.Options;

public class DefaultSummaryConfig implements IInfoflowConfig {

	@Override
	public void setSootOptions(Options options, InfoflowConfiguration config) {
		final SummaryGeneratorConfiguration summaryConfig = (SummaryGeneratorConfiguration) config;

		// explicitly include packages for shorter runtime:
		List<String> includeList = new LinkedList<String>();
		includeList.add("java.lang.*");
		includeList.add("java.util.*");
		includeList.add("android.app.*");
		includeList.add("java.nio.charset.*");
		includeList.add("sun.util.*");
		includeList.add("sun.nio.cs.*");
		// includeList.add("java.io.");
		// includeList.add("java.security.");
		// includeList.add("sun.misc.");
		includeList.add("java.net.*");
		// includeList.add("javax.servlet.");
		// includeList.add("javax.crypto.");

		includeList.add("android.*");
		includeList.add("android.content.*");
		// includeList.add("org.apache.http.");
		includeList.add("soot.*");
		// includeList.add("com.example.");
		// includeList.add("com.jakobkontor.");
		// includeList.add("libcore.icu.");
		// includeList.add("securibench.");
		options.set_no_bodies_for_excluded(true);
		options.set_allow_phantom_refs(true);
		options.set_include(includeList);
		if (summaryConfig.getWriteOutputFiles())
			options.set_output_format(Options.output_format_jimple);
		else
			options.set_output_format(Options.output_format_none);

		options.setPhaseOption("jb", "use-original-names:true");
		options.set_ignore_classpath_errors(true);

		// We can also create summaries for classes from APK files
		options.set_process_multiple_dex(true);
		if (summaryConfig.getAndroidPlatformDir() != null)
			options.set_android_jars(summaryConfig.getAndroidPlatformDir());
	}

}
