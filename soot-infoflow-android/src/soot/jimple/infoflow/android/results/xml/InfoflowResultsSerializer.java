package soot.jimple.infoflow.android.results.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.CategoryDefinition;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;

/**
 * Android-specific variant of the results serializer
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowResultsSerializer extends soot.jimple.infoflow.results.xml.InfoflowResultsSerializer {

	/**
	 * Creates a new instance of the InfoflowResultsSerializer class
	 * 
	 * @param config The configuration of the data flow
	 */
	public InfoflowResultsSerializer(InfoflowConfiguration config) {
		super(config);
	}

	/**
	 * Creates a new instance of the InfoflowResultsSerializer class
	 * 
	 * @param cfg    The control flow graph to be used for obtaining additional
	 *               information such as the methods containing source or sink
	 *               statements
	 * @param config The configuration of the data flow
	 */
	public InfoflowResultsSerializer(IInfoflowCFG cfg, InfoflowConfiguration config) {
		super(cfg, config);
	}

	@Override
	protected void writeAdditionalSourceInfo(ResultSourceInfo source, XMLStreamWriter writer)
			throws XMLStreamException {
		super.writeAdditionalSourceInfo(source, writer);

		// Write out the category to which this source belongs
		if (source.getDefinition() != null && source.getDefinition() instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) source.getDefinition();
			if (mssd.getMethod() instanceof AndroidMethod) {
				if (mssd.getCategory() != null) {
					writer.writeAttribute(XmlConstants.Attributes.systemCategory, mssd.getCategory().toString());
					if (mssd.getCategory() instanceof CategoryDefinition) {
						CategoryDefinition catDef = (CategoryDefinition) mssd.getCategory();
						String customCat = catDef.getCustomCategory();
						if (customCat != null && !customCat.isEmpty())
							writer.writeAttribute(XmlConstants.Attributes.userCategory, customCat);
					}
				}
			}
		}
	}

	@Override
	protected void writeAdditionalSinkInfo(ResultSinkInfo sink, XMLStreamWriter writer) throws XMLStreamException {
		super.writeAdditionalSinkInfo(sink, writer);

		// Write out the category to which this sink belongs
		if (sink.getDefinition() != null && sink.getDefinition() instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) sink.getDefinition();
			if (mssd.getMethod() instanceof AndroidMethod) {
				if (mssd.getCategory() != null) {
					writer.writeAttribute(XmlConstants.Attributes.systemCategory, mssd.getCategory().toString());
					if (mssd.getCategory() instanceof CategoryDefinition) {
						CategoryDefinition catDef = (CategoryDefinition) mssd.getCategory();
						String customCat = catDef.getCustomCategory();
						if (customCat != null && !customCat.isEmpty())
							writer.writeAttribute(XmlConstants.Attributes.userCategory, customCat);
					}
				}
			}
		}
	}

}
