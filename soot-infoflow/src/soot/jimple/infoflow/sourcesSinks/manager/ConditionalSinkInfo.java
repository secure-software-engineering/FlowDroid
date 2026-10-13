package soot.jimple.infoflow.sourcesSinks.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.infoflow.sourcesSinks.definitions.IAdditionalFlowTriggerInformation;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

public final class ConditionalSinkInfo implements IAdditionalFlowTriggerInformation {
	private List<IAdditionalFlowTriggerInformation> triggerInformation = new ArrayList<>();

	public ConditionalSinkInfo(ISourceSinkDefinition def) {
		add(def);
	}

	public void add(ISourceSinkDefinition def) {
		if (def instanceof IAdditionalFlowTriggerInformation) {
			triggerInformation.add((IAdditionalFlowTriggerInformation) def);
		}
	}

	@Override
	public Set<Local> getTriggeredAdditionalFlows(Stmt stmt) {
		Set<Local> l = null;
		boolean createdSet = false;
		for (IAdditionalFlowTriggerInformation t : triggerInformation) {
			Set<Local> tr = t.getTriggeredAdditionalFlows(stmt);
			if (tr != null && !tr.isEmpty()) {
				if (l == null)
					l = tr;
				else {
					if (!createdSet) {
						l = new HashSet<>(l);
						createdSet = true;
					}
					l.addAll(tr);
				}
			}
		}
		return l;
	}

}