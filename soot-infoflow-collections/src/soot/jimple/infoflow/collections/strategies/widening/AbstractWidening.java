package soot.jimple.infoflow.collections.strategies.widening;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.PositionBasedContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContainerContext;

public abstract class AbstractWidening implements WideningStrategy<Unit, Abstraction> {
    protected final InfoflowManager manager;

    protected AbstractWidening(InfoflowManager manager) {
        this.manager = manager;
    }

    protected boolean isShift(Abstraction d2, Abstraction d3) {
        if (!d2.equalsWithoutContext(d3))
            return false;

        int n = d2.getAccessPath().getFragmentCount();
        AccessPathFragment[] d2f = d3.getAccessPath().getFragments();
        AccessPathFragment[] d3f = d3.getAccessPath().getFragments();
        for (int i = 0; i < n; i++) {
            ContainerContext[] d2c = d2f[i].getContext();
            ContainerContext[] d3c = d3f[i].getContext();
            if (d2c == null || d3c == null)
                continue;

            for (int j = 0; j < d3c.length; j++) {
                if (d3c[j] instanceof PositionBasedContext<?>
                        && d2c[j] instanceof PositionBasedContext<?>
                        && d3c[j].equals(d2c[j]))
                    return true;
            }
        }

        return false;
    }

    @Override
    public Abstraction forceWiden(Abstraction abs, Unit unit) {
        AccessPathFragment[] oldFragments = abs.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
        fragments[0] = oldFragments[0].copyWithNewContext(null);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(abs.getAccessPath().getPlainValue(), fragments,
                abs.getAccessPath().getTaintSubFields());
        return abs.deriveNewAbstraction(ap, null);
    }
}
