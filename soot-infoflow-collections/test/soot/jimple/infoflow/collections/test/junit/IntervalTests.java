package soot.jimple.infoflow.collections.test.junit;

import org.junit.Assert;
import org.junit.Test;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.util.Tristate;

public class IntervalTests {
    @Test
    public void intersectTest() {
        IntervalContext i = new IntervalContext(0, 2);
        IntervalContext j = new IntervalContext(2, 4);

        Assert.assertEquals(Tristate.MAYBE(), i.intersects(j));
        Assert.assertEquals(Tristate.MAYBE(), i.intersects(i));

        IntervalContext k = new IntervalContext(1);
        Assert.assertEquals(Tristate.TRUE(), k.intersects(k));
    }

    @Test
    public void lessThanEqualTest() {
        IntervalContext i = new IntervalContext(0, 2);
        IntervalContext j = new IntervalContext(2, 4);
        // i_ub=2 <= j_lb=2
        Assert.assertEquals(Tristate.TRUE(), i.lessThanEqual(j));
        // i=2, j=2
        Assert.assertEquals(Tristate.MAYBE(), j.lessThanEqual(i));
    }

    private Tristate lessThan(IntervalContext i, IntervalContext j) {
        return j.lessThanEqual(i).negate();
    }

    @Test
    public void lessThanTest() {
        IntervalContext i = new IntervalContext(0, 2);
        IntervalContext j = new IntervalContext(2, 4);

        Assert.assertEquals(Tristate.MAYBE(), lessThan(i, i));
        // i_ub=0 < j_ub=4
        Assert.assertEquals(Tristate.MAYBE(), lessThan(i, j));
        // j_ub=2 </ i_lb=2
        Assert.assertEquals(Tristate.FALSE(), lessThan(j, i));

        IntervalContext k = new IntervalContext(1);
        Assert.assertEquals(Tristate.FALSE(), lessThan(k, k));
    }
}
