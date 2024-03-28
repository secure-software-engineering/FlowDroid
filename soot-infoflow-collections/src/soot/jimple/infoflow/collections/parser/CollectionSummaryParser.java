package soot.jimple.infoflow.collections.parser;

import soot.jimple.infoflow.methodSummary.data.provider.XMLSummaryProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CollectionSummaryParser extends XMLSummaryProvider {

    /**
     * Loads a summary from a folder within the StubDroid jar file.
     *
     * @param folderInJar The folder in the JAR file from which to load the summary
     *                    files
     * @throws URISyntaxException
     * @throws IOException
     */
    public CollectionSummaryParser(String folderInJar) throws URISyntaxException, IOException {
        this(folderInJar, CollectionSummaryParser.class);
    }

    /**
     * Loads a summary from a folder within the StubDroid jar file.
     *
     * @param folderInJar The folder in the JAR file from which to load the summary
     *                    files
     * @param parentClass The class in whose jar to look for the summary files
     * @throws URISyntaxException
     * @throws IOException
     */
    public CollectionSummaryParser(String folderInJar, Class<?> parentClass) throws URISyntaxException, IOException {
        super(new StubDroidParser());
        loadSummariesFromJAR(folderInJar, parentClass, p -> loadClass(p));
    }

    /**
     * Loads a file or all files in a dir (not recursively)
     *
     * @param source The single file or directory to load
     */
    public CollectionSummaryParser(File source) {
        this(Collections.singletonList(source));
    }

    /**
     * Loads the summaries from all of the given files
     *
     * @param files The files to load
     */
    public CollectionSummaryParser(List<File> files) {
        super(new StubDroidParser());
        loadSummariesFromFiles(files, f -> loadClass(f));
    }

    public void loadAdditionalSummaries(String folderInJar) throws URISyntaxException, IOException {
        loadSummariesFromJAR(folderInJar, CollectionSummaryParser.class, p -> {
            if (!loadedClasses.contains(fileToClass(p.getFileName().toString())))
                loadClass(p);
        });
    }

    @Override
    public Set<String> getAllClassesWithSummaries() {
        return loadedClasses;
    }

}
