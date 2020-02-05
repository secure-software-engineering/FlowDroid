package soot.jimple.infoflow.rifl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.xml.sax.SAXException;

import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.rifl.RIFLDocument.Assignable;
import soot.jimple.infoflow.rifl.RIFLDocument.Category;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaFieldSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaMethodSourceSinkSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaParameterSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaReturnValueSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkType;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;

/**
 * Source/sink definition provider class for RIFL
 *
 * @author Steven Arzt
 */
public class RIFLSourceSinkDefinitionProvider implements ISourceSinkDefinitionProvider {

	private final Set<ISourceSinkDefinition> sources = new HashSet<>();
	private final Set<ISourceSinkDefinition> sinks = new HashSet<>();
	private Set<ISourceSinkDefinition> allMethods = null;
	private String lastCategory = null;

	/**
	 * Creates a new instance of the RIFLSourceSinkDefinitionProvider class
	 *
	 * @param file The file from which to read the RIFL specification
	 * @throws IOException  Thrown if the given RIFL file cannot be read
	 * @throws SAXException Thrown in the case of an XML error while parsing the
	 *                      RIFL file
	 */
	public RIFLSourceSinkDefinitionProvider(String file) throws SAXException, IOException {
		RIFLParser parser = new RIFLParser();
		RIFLDocument doc = parser.parseRIFL(new File(file));

		// Collect the sources and sinks
		for (Assignable assign : doc.getInterfaceSpec().getSourcesSinks()) {
			parseRawDefinition(assign.getElement());
		}
	}

	/**
	 * Parses a definition depending on its type (source, sink, category)
	 *
	 * @param element The source/sink specification to parse
	 */
	private void parseRawDefinition(SourceSinkSpec element) {
		if (element.getType() == SourceSinkType.Source) {
			ISourceSinkDefinition sourceSinkDefinition = parseDefinition(element, SourceSinkType.Source);
			final String permanentCategory = lastCategory;
			sourceSinkDefinition.setCategory(new ISourceSinkCategory() {
				@Override
				public String getHumanReadableDescription() {
					return permanentCategory;
				}

				@Override
				public String getID() {
					return permanentCategory;
				}
			});
			sources.add(sourceSinkDefinition);

		} else if (element.getType() == SourceSinkType.Sink) {
			ISourceSinkDefinition sourceSinkDefinition = parseDefinition(element, SourceSinkType.Sink);
			final String permanentCategory = lastCategory;
			sourceSinkDefinition.setCategory(new ISourceSinkCategory() {
				@Override
				public String getHumanReadableDescription() {
					return permanentCategory;
				}

				@Override
				public String getID() {
					return permanentCategory.toUpperCase(Locale.US);
				}
			});
			sinks.add(sourceSinkDefinition);
		} else if (element.getType() == SourceSinkType.Category) {
			Category cat = (Category) element;
			lastCategory = cat.getName();
			String[] s = lastCategory.split("_");
			lastCategory = "";
			for (int i = 0; i < s.length - 1; ++i) {
				if (i != 0)
					lastCategory = lastCategory + " ";
				lastCategory += s[i];
			}
			for (SourceSinkSpec spec : cat.getElements())
				parseRawDefinition(spec);
		} else
			throw new RuntimeException("Invalid element type");
	}

	/**
	 * Parses the contents of a source/sink specification element
	 *
	 * @param element        The element to parse
	 * @param sourceSinkType Specifies whether the current element is a source or a
	 *                       sink
	 * @return The source/sink definition that corresponds to the given RIFL
	 *         specification element
	 */
	private ISourceSinkDefinition parseDefinition(SourceSinkSpec element, SourceSinkType sourceSinkType) {
		if (element instanceof JavaMethodSourceSinkSpec) {
			JavaMethodSourceSinkSpec javaElement = (JavaMethodSourceSinkSpec) element;

			// Get the method signature in Soot format
			String methodName = SootMethodRepresentationParser.v()
					.getMethodNameFromSubSignature(javaElement.getHalfSignature());
			String[] parameters = (SootMethodRepresentationParser.v()
					.getParameterTypesFromSubSignature(javaElement.getHalfSignature()));

			// Build the parameter list
			List<String> parameterTypes = new ArrayList<>();
			if (parameters != null) {// handle empty parameter case

				for (String p : parameters)
					parameterTypes.add(p);
			}
			if (element instanceof JavaParameterSpec) {// sink
				JavaParameterSpec paramSpec = (JavaParameterSpec) element;

				Set<AccessPathTuple> returnValue = new HashSet<>();// dummy

				SootMethodAndClass am = new SootMethodAndClass(methodName, javaElement.getClassName(), "",
						parameterTypes);
				return new MethodSourceSinkDefinition(am, null, null, returnValue, CallType.MethodCall);
			} else if (element instanceof JavaReturnValueSpec) {// source
				AccessPathTuple apt = AccessPathTuple.fromPathElements((String[]) null, null,
						sourceSinkType == SourceSinkType.Source, sourceSinkType == SourceSinkType.Sink);

				SootMethodAndClass am = new SootMethodAndClass(methodName, javaElement.getClassName(), "",
						parameterTypes);
				return new MethodSourceSinkDefinition(am, null, null, null, CallType.MethodCall);
			}
		} else if (element instanceof JavaFieldSpec) {
			// JavaFieldSpec javaElement = (JavaFieldSpec) element;
			/* TODO: Does not really fit into the architecture */
		}
		throw new RuntimeException("Invalid source/sink specification element");
	}

	@Override
	public Set<ISourceSinkDefinition> getSources() {
		return sources;
	}

	@Override
	public Set<ISourceSinkDefinition> getSinks() {
		return sinks;
	}

	@Override
	public Set<ISourceSinkDefinition> getAllMethods() {
		if (allMethods == null) {
			allMethods = new HashSet<>(sources.size() + sinks.size());
			allMethods.addAll(sources);
			allMethods.addAll(sinks);
		}
		return allMethods;
	}

}
